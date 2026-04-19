package org.example.shield.ai.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.ai.config.CohereApiConfig;
import org.example.shield.ai.domain.LegalChunkEntity;
import org.example.shield.ai.dto.CivilLawSeed;
import org.example.shield.ai.infrastructure.CohereClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 특별법 18개 전문 인제스트 서비스 (Phase C-2).
 *
 * <p>{@code resources/seed/special-laws/*.json}에 저장된 각 법령별 시드 파일을 순회하며
 * {@link CivilLawIngestService}와 동일한 흐름으로 임베딩·upsert를 수행한다.</p>
 *
 * <p>CivilLaw 서비스와의 차이:</p>
 * <ul>
 *   <li>시드 파일 N개 순회 (민법은 단일 파일)</li>
 *   <li>카테고리 매핑은 {@link SpecialLawCategoryResolver}(LSI → 카테고리 역인덱스)
 *       로부터 law 단위로 일괄 적용 (민법은 조문번호별 세밀 매핑)</li>
 *   <li>upsert 로직은 {@link CivilLawUpsertService}를 재사용</li>
 *   <li>Seed DTO({@link CivilLawSeed})도 스키마 동일하여 재사용</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SpecialLawIngestService {

    private static final String SEED_PATTERN = "classpath:seed/special-laws/*.json";
    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ObjectMapper objectMapper;
    private final CohereClient cohereClient;
    private final CohereApiConfig cohereConfig;
    private final CategoryLawMappingService categoryMappingService;
    private final CivilLawUpsertService upsertService;

    @Value("${cohere.embed.batch-delay-ms:1500}")
    private long batchDelayMs;

    /**
     * 인제스트 실행.
     *
     * @param dryRun {@code true}면 임베딩 API 호출·DB 쓰기 없이 파싱·매핑만 검증
     * @return 처리 요약 (법령별 결과 + 합계)
     */
    public IngestSummary run(boolean dryRun) {
        long startNanos = System.nanoTime();

        List<CivilLawSeed> seeds = loadAllSeeds();
        log.info("특별법 인제스트 시작: laws={}, dryRun={}", seeds.size(), dryRun);

        List<LawResult> lawResults = new ArrayList<>();
        for (CivilLawSeed seed : seeds) {
            LawResult r = processLaw(seed, dryRun);
            lawResults.add(r);
            log.info("법령 완료: {} articles={} batches={} embedded={} upserted={} failed={}",
                    seed.meta().lawName(), r.totalArticles(), r.totalBatches(),
                    r.totalEmbedded(), r.totalUpserted(), r.totalFailed());
        }

        int totalArticles = lawResults.stream().mapToInt(LawResult::totalArticles).sum();
        int totalBatches = lawResults.stream().mapToInt(LawResult::totalBatches).sum();
        int totalEmbedded = lawResults.stream().mapToInt(LawResult::totalEmbedded).sum();
        int totalUpserted = lawResults.stream().mapToInt(LawResult::totalUpserted).sum();
        int totalFailed = lawResults.stream().mapToInt(LawResult::totalFailed).sum();

        int elapsedMs = (int) ((System.nanoTime() - startNanos) / 1_000_000);
        IngestSummary summary = new IngestSummary(
                seeds.size(), totalArticles, totalBatches, totalEmbedded, totalUpserted,
                totalFailed, lawResults, elapsedMs, dryRun);
        log.info("특별법 인제스트 완료: {}", summary);
        return summary;
    }

    // ------------------------------------------------------------------
    // 단일 법령 처리
    // ------------------------------------------------------------------

    private LawResult processLaw(CivilLawSeed seed, boolean dryRun) {
        List<CivilLawSeed.Article> articles = seed.articles();
        String lawName = seed.meta().lawName();
        String lawId = seed.meta().lawId();
        // Meta DTO 확장 없이 동적으로 lsi 추출 — meta 맵에 lsi 키가 있지만
        // CivilLawSeed.Meta에 해당 필드가 없으므로 각 Article의 source_law_id 재사용
        String lsi = articles.isEmpty() ? null : articles.get(0).sourceLawId();
        List<String> categories = categoryMappingService.resolveCategoriesByLsi(lsi);

        int totalBatches = 0;
        int totalEmbedded = 0;
        int totalUpserted = 0;
        int totalFailed = 0;

        int batchSize = cohereConfig.getEmbedBatchSize();
        int expectedBatches = (int) Math.ceil((double) articles.size() / batchSize);

        for (int i = 0; i < articles.size(); i += batchSize) {
            int end = Math.min(i + batchSize, articles.size());
            List<CivilLawSeed.Article> batch = articles.subList(i, end);
            totalBatches++;

            try {
                BatchResult r = processBatch(batch, lawName, categories, dryRun);
                totalEmbedded += r.embedded();
                totalUpserted += r.upserted();
                log.debug("  [{}] 배치 {}/{}: embedded={}, upserted={}",
                        lawName, totalBatches, expectedBatches, r.embedded(), r.upserted());
            } catch (Exception e) {
                totalFailed += batch.size();
                log.error("  [{}] 배치 {}/{} 실패: range=[{}..{}], error={}",
                        lawName, totalBatches, expectedBatches,
                        batch.get(0).articleNo(),
                        batch.get(batch.size() - 1).articleNo(),
                        e.getMessage());
            }

            if (!dryRun && end < articles.size() && batchDelayMs > 0) {
                try {
                    Thread.sleep(batchDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("인제스트 중단: 스레드 인터럽트");
                    break;
                }
            }
        }

        return new LawResult(lawId, lawName, articles.size(),
                totalBatches, totalEmbedded, totalUpserted, totalFailed);
    }

    private BatchResult processBatch(List<CivilLawSeed.Article> batch,
                                     String lawName,
                                     List<String> categories,
                                     boolean dryRun) {
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
                        "[" + lawName + "] 임베딩 개수 불일치: 요청=" + batch.size()
                                + ", 응답=" + embeddings.size());
            }
        }

        if (dryRun) {
            return new BatchResult(batch.size(), 0);
        }

        String embedModel = cohereConfig.getEmbedModel();
        int upserted = upsertService.upsertBatch(
                batch, embeddings, embedModel,
                (article, vector) -> toEntity(article, (short) 0, vector, embedModel, categories));
        return new BatchResult(embeddings.size(), upserted);
    }

    private LegalChunkEntity toEntity(CivilLawSeed.Article a,
                                      Short chunkIndex,
                                      float[] embedding,
                                      String embeddingModel,
                                      List<String> categories) {
        LocalDate effectiveDate = parseDate(a.effectiveDate());
        String[] cats = categories == null ? new String[0] : categories.toArray(new String[0]);

        return LegalChunkEntity.builder()
                .lawId(a.lawId())
                .lawName(a.lawName())
                .articleNo(a.articleNo())
                .chunkIndex(chunkIndex)
                .articleTitle(a.articleTitle())
                .content(a.content())
                .effectiveDate(effectiveDate)
                .sourceUrl("https://law.go.kr/lawService.do?MST=" + a.sourceMst())
                .categoryIds(cats)
                .lodUri("http://lod.law.go.kr/resource/LSI" + a.sourceLawId())
                .legislationTerms(new String[0])
                .embedding(embedding)
                .embeddingModel(embeddingModel)
                .build();
    }

    /** 임베딩 입력 포맷: "<법령명> <조문>(제목)\n본문". 민법과 동일 전략. */
    private String buildEmbeddingInput(CivilLawSeed.Article a) {
        StringBuilder sb = new StringBuilder();
        sb.append(a.lawName()).append(" ").append(a.articleNo());
        if (a.articleTitle() != null && !a.articleTitle().isBlank()) {
            sb.append("(").append(a.articleTitle()).append(")");
        }
        sb.append("\n").append(a.content());
        return sb.toString();
    }

    private LocalDate parseDate(String yyyymmdd) {
        if (yyyymmdd == null || yyyymmdd.length() != 8) return null;
        try {
            return LocalDate.parse(yyyymmdd, YYYYMMDD);
        } catch (Exception e) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Seed 로더
    // ------------------------------------------------------------------

    private List<CivilLawSeed> loadAllSeeds() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(SEED_PATTERN);
            if (resources.length == 0) {
                throw new IllegalStateException(
                        "특별법 시드 파일이 없습니다: " + SEED_PATTERN
                                + " — scripts/fetch_special_laws.py 먼저 실행하세요.");
            }
            // 파일명 오름차순 (결정적 실행 순서)
            Arrays.sort(resources, Comparator.comparing(Resource::getFilename));

            List<CivilLawSeed> seeds = new ArrayList<>();
            for (Resource r : resources) {
                try (InputStream in = r.getInputStream()) {
                    CivilLawSeed seed = objectMapper.readValue(in, CivilLawSeed.class);
                    seeds.add(seed);
                } catch (Exception e) {
                    log.error("시드 로드 실패: {} — {}", r.getFilename(), e.getMessage());
                }
            }
            return seeds;
        } catch (Exception e) {
            throw new IllegalStateException("특별법 시드 스캔 실패", e);
        }
    }

    // ------------------------------------------------------------------
    // Result types
    // ------------------------------------------------------------------

    private record BatchResult(int embedded, int upserted) {
    }

    public record LawResult(
            String lawId,
            String lawName,
            int totalArticles,
            int totalBatches,
            int totalEmbedded,
            int totalUpserted,
            int totalFailed
    ) {
    }

    public record IngestSummary(
            int totalLaws,
            int totalArticles,
            int totalBatches,
            int totalEmbedded,
            int totalUpserted,
            int totalFailed,
            List<LawResult> perLaw,
            int elapsedMs,
            boolean dryRun
    ) {
        @Override
        public String toString() {
            return String.format(
                    "IngestSummary{laws=%d, articles=%d, batches=%d, embedded=%d, upserted=%d, failed=%d, elapsedMs=%d, dryRun=%s}",
                    totalLaws, totalArticles, totalBatches, totalEmbedded, totalUpserted,
                    totalFailed, elapsedMs, dryRun);
        }
    }
}
