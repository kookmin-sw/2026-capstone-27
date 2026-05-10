package org.example.shield.ai.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.ai.config.CohereApiConfig;
import org.example.shield.ai.domain.LegalCaseEntity;
import org.example.shield.ai.dto.LegalCaseSeed;
import org.example.shield.ai.infrastructure.CohereClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 판례 183건(대법원) 전문 인제스트 서비스 (Phase C-4).
 *
 * <p>{@code resources/seed/cases/*.json}(프리픽스 {@code _}는 스키마/예시이므로 제외)에
 * 저장된 판례 시드 파일을 순회하여 Cohere embed-v4.0으로 임베딩하고 legal_cases 테이블에
 * upsert한다. 민법/특별법 인제스트와 동일한 배치 처리 + rate-limit 전략을 따른다.</p>
 *
 * <p>임베딩 입력 포맷 (민법 {@code 법령명 + 조문 + 본문}과 대칭):</p>
 * <pre>
 *   [사건명]
 *   [판시사항]
 *   [판결요지]
 *   [이유 앞부분(절단된 reasoning)]
 * </pre>
 *
 * <p>embed-v4.0은 최대 ~2048 토큰을 수용하지만 실제 지연과 비용을 고려해 인풋을 4000자
 * 이내로 잘라 보낸다. 판시사항/판결요지는 거의 항상 잘 들어가며, 이유는 앞부분만 절단.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LegalCaseIngestService {

    private static final String SEED_PATTERN = "classpath:seed/cases/*.json";
    /** _SCHEMA.md, _EXAMPLE.json 등 언더스코어 시작 파일은 시드가 아님. */
    private static final String SKIP_PREFIX = "_";

    /** 임베딩 인풋 최대 길이(문자). embed-v4.0 토큰 상한을 여유있게 우회. */
    private static final int MAX_EMBED_INPUT_CHARS = 4000;

    private final ObjectMapper objectMapper;
    private final CohereClient cohereClient;
    private final CohereApiConfig cohereConfig;
    private final LegalCaseUpsertService upsertService;

    @Value("${cohere.embed.batch-delay-ms:1500}")
    private long batchDelayMs;

    /**
     * 인제스트 실행.
     *
     * @param dryRun {@code true}면 임베딩 API 호출·DB 쓰기 없이 파싱만 검증
     * @return 처리 요약
     */
    public IngestSummary run(boolean dryRun) {
        long startNanos = System.nanoTime();

        List<LegalCaseSeed> seeds = loadAllSeeds();
        log.info("판례 인제스트 시작: cases={}, dryRun={}", seeds.size(), dryRun);

        int totalBatches = 0;
        int totalEmbedded = 0;
        int totalUpserted = 0;
        int totalFailed = 0;

        int batchSize = cohereConfig.getEmbedBatchSize();
        int expectedBatches = (int) Math.ceil((double) seeds.size() / batchSize);

        for (int i = 0; i < seeds.size(); i += batchSize) {
            int end = Math.min(i + batchSize, seeds.size());
            List<LegalCaseSeed> batch = seeds.subList(i, end);
            totalBatches++;

            try {
                BatchResult r = processBatch(batch, dryRun);
                totalEmbedded += r.embedded();
                totalUpserted += r.upserted();
                log.info("  배치 {}/{}: embedded={}, upserted={}",
                        totalBatches, expectedBatches, r.embedded(), r.upserted());
            } catch (Exception e) {
                totalFailed += batch.size();
                log.error("  배치 {}/{} 실패: range=[{}..{}], error={}",
                        totalBatches, expectedBatches,
                        batch.get(0).caseData().caseNo(),
                        batch.get(batch.size() - 1).caseData().caseNo(),
                        e.getMessage());
            }

            if (!dryRun && end < seeds.size() && batchDelayMs > 0) {
                try {
                    Thread.sleep(batchDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("인제스트 중단: 스레드 인터럽트");
                    break;
                }
            }
        }

        int elapsedMs = (int) ((System.nanoTime() - startNanos) / 1_000_000);
        IngestSummary summary = new IngestSummary(
                seeds.size(), totalBatches, totalEmbedded, totalUpserted,
                totalFailed, elapsedMs, dryRun);
        log.info("판례 인제스트 완료: {}", summary);
        return summary;
    }

    // ------------------------------------------------------------------
    // 배치 처리
    // ------------------------------------------------------------------

    private BatchResult processBatch(List<LegalCaseSeed> batch, boolean dryRun) {
        List<String> embedInputs = batch.stream()
                .map(this::buildEmbeddingInput)
                .toList();

        List<float[]> embeddings;
        if (dryRun) {
            embeddings = Collections.nCopies(batch.size(), new float[0]);
        } else {
            embeddings = cohereClient.embedDocuments(cohereConfig.getEmbedModel(), embedInputs);
            if (embeddings.size() != batch.size()) {
                throw new IllegalStateException(
                        "임베딩 개수 불일치: 요청=" + batch.size() + ", 응답=" + embeddings.size());
            }
        }

        if (dryRun) {
            return new BatchResult(batch.size(), 0);
        }

        String embedModel = cohereConfig.getEmbedModel();
        int upserted = upsertService.upsertBatch(
                batch, embeddings, embedModel, this::toEntity);
        return new BatchResult(embeddings.size(), upserted);
    }

    /**
     * 임베딩 인풋 빌더. [사건명] + [판시사항] + [판결요지] + [이유 일부] 조합.
     * 판시사항/판결요지는 거의 항상 들어가고, reasoning은 남는 공간만큼 절단.
     */
    private String buildEmbeddingInput(LegalCaseSeed seed) {
        LegalCaseSeed.Case c = seed.caseData();
        StringBuilder sb = new StringBuilder();
        if (c.caseName() != null && !c.caseName().isBlank()) {
            sb.append(c.caseName()).append("\n");
        }
        if (c.headnote() != null && !c.headnote().isBlank()) {
            sb.append(c.headnote()).append("\n");
        }
        if (c.holding() != null && !c.holding().isBlank()) {
            sb.append(c.holding()).append("\n");
        }
        // reasoning은 남는 공간 범위에서만 포함
        if (c.reasoning() != null && !c.reasoning().isBlank()) {
            int remain = MAX_EMBED_INPUT_CHARS - sb.length();
            if (remain > 200) {
                String r = c.reasoning();
                sb.append(r, 0, Math.min(r.length(), remain));
            }
        }
        String input = sb.toString();
        if (input.length() > MAX_EMBED_INPUT_CHARS) {
            input = input.substring(0, MAX_EMBED_INPUT_CHARS);
        }
        return input;
    }

    private LegalCaseEntity toEntity(LegalCaseSeed seed, float[] embedding) {
        LegalCaseSeed.Case c = seed.caseData();
        LegalCaseSeed.Meta m = seed.meta();

        return LegalCaseEntity.builder()
                .caseNo(c.caseNo())
                .court(c.court())
                .caseName(c.caseName())
                .decisionDate(LocalDate.parse(c.decisionDate()))
                .caseType(c.caseType() != null ? c.caseType() : "민사")
                .judgmentType(c.judgmentType())
                .disposition(c.disposition())
                .headnote(c.headnote())
                .holding(c.holding())
                .reasoning(c.reasoning())
                .fullText(c.fullText())
                .citedArticles(toArray(c.citedArticles()))
                .citedCases(toArray(c.citedCases()))
                .categoryIds(toArray(c.categoryIds()))
                .source(m != null && m.source() != null ? m.source() : "law.go.kr")
                .sourceUrl(m != null ? m.sourceUrl() : null)
                .sourceId(m != null ? m.sourceId() : null)
                .embedding(embedding)
                .embeddingModel(cohereConfig.getEmbedModel())
                .build();
    }

    private static String[] toArray(List<String> list) {
        if (list == null || list.isEmpty()) return new String[0];
        return list.toArray(new String[0]);
    }

    // ------------------------------------------------------------------
    // Seed 로더
    // ------------------------------------------------------------------

    private List<LegalCaseSeed> loadAllSeeds() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(SEED_PATTERN);
            Resource[] filtered = Arrays.stream(resources)
                    .filter(r -> {
                        String name = r.getFilename();
                        return name != null && !name.startsWith(SKIP_PREFIX);
                    })
                    .sorted(Comparator.comparing(Resource::getFilename))
                    .toArray(Resource[]::new);
            if (filtered.length == 0) {
                throw new IllegalStateException(
                        "판례 시드 파일이 없습니다: " + SEED_PATTERN
                                + " — scripts/fetch_cases.py 먼저 실행하세요.");
            }

            List<LegalCaseSeed> seeds = new ArrayList<>();
            for (Resource r : filtered) {
                try (InputStream in = r.getInputStream()) {
                    LegalCaseSeed seed = objectMapper.readValue(in, LegalCaseSeed.class);
                    if (seed.caseData() == null || seed.caseData().caseNo() == null
                            || seed.caseData().decisionDate() == null) {
                        log.warn("판례 시드 스킵(필수 필드 누락): {}", r.getFilename());
                        continue;
                    }
                    seeds.add(seed);
                } catch (Exception e) {
                    log.error("시드 로드 실패: {} — {}", r.getFilename(), e.getMessage());
                }
            }
            return seeds;
        } catch (Exception e) {
            throw new IllegalStateException("판례 시드 스캔 실패", e);
        }
    }

    // ------------------------------------------------------------------
    // Result types
    // ------------------------------------------------------------------

    private record BatchResult(int embedded, int upserted) {
    }

    public record IngestSummary(
            int totalCases,
            int totalBatches,
            int totalEmbedded,
            int totalUpserted,
            int totalFailed,
            int elapsedMs,
            boolean dryRun
    ) {
        @Override
        public String toString() {
            return String.format(
                    "IngestSummary{cases=%d, batches=%d, embedded=%d, upserted=%d, failed=%d, elapsedMs=%d, dryRun=%s}",
                    totalCases, totalBatches, totalEmbedded, totalUpserted, totalFailed, elapsedMs, dryRun);
        }
    }
}
