package org.example.shield.ai.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.ai.config.CivilLawCategoryMap;
import org.example.shield.ai.config.CohereApiConfig;
import org.example.shield.ai.domain.LegalChunkEntity;
import org.example.shield.ai.dto.CivilLawSeed;
import org.example.shield.ai.infrastructure.CohereClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 민법 전문 인제스트 서비스 (Phase B-2).
 *
 * <p>처리 흐름:</p>
 * <ol>
 *   <li>{@code seed/civil-law.json} 로드 → 1,193개 조문 DTO로 역직렬화</li>
 *   <li>각 조문 → {@link LegalChunkEntity}로 변환 (청크 분할 없음: 모든 조문이
 *       최대 1,305자로 Cohere embed-v4.0 토큰 한도 내)</li>
 *   <li>category_ids: {@link CivilLawCategoryMap}로 book/chapter/section/group 태그 부여</li>
 *   <li>배치 임베딩: 96개 단위로 Cohere embed API 호출 → 각 엔티티에 임베딩 설정</li>
 *   <li>Upsert: 자연키(law_id, article_no, chunk_index) 기준으로 기존 청크 조회 →
 *       없으면 insert, 있으면 content/embedding 갱신</li>
 * </ol>
 *
 * <p>운영 방침:</p>
 * <ul>
 *   <li>재실행 안전성: 자연키 기반 upsert로 멱등 보장</li>
 *   <li>실패 격리: 임베딩 배치 1개 실패해도 다른 배치는 계속 진행</li>
 *   <li>Rate limit 회피: 배치 간 {@code batchDelayMs} 지연 삽입 (Cohere Trial 대응)</li>
 *   <li>트랜잭션 범위: 배치 1개 단위로 {@link CivilLawUpsertService}에 위임 —
 *       같은 클래스 self-invocation {@code @Transactional} 미작동 문제 회피</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CivilLawIngestService {

    private static final String SEED_PATH = "seed/civil-law.json";
    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ObjectMapper objectMapper;
    private final CohereClient cohereClient;
    private final CohereApiConfig cohereConfig;
    private final CivilLawCategoryMap categoryMap;
    private final CivilLawUpsertService upsertService;

    /**
     * 배치 간 지연(ms). Cohere Trial API rate limit(분당 약 100건) 회피용.
     * 기본 1500ms = 분당 약 40건. 성공/실패 무관하게 배치 사이에 삽입되지만,
     * 마지막 배치 및 dryRun에서는 지연 없음.
     */
    @Value("${cohere.embed.batch-delay-ms:1500}")
    private long batchDelayMs;

    /**
     * 인제스트 실행 엔트리.
     *
     * @param dryRun {@code true}이면 임베딩을 생성하지 않고 DB도 건드리지 않음 (검증용).
     * @return 처리 요약
     */
    public IngestSummary run(boolean dryRun) {
        long startNanos = System.nanoTime();
        CivilLawSeed seed = loadSeed();
        List<CivilLawSeed.Article> articles = seed.articles();
        log.info("민법 인제스트 시작: articles={}, dryRun={}, batchDelayMs={}",
                articles.size(), dryRun, batchDelayMs);

        int totalBatches = 0;
        int totalEmbedded = 0;
        int totalUpserted = 0;
        int totalFailed = 0;
        List<String> failedArticleNos = new ArrayList<>();

        int batchSize = cohereConfig.getEmbedBatchSize();
        int expectedBatches = (int) Math.ceil((double) articles.size() / batchSize);
        for (int i = 0; i < articles.size(); i += batchSize) {
            int end = Math.min(i + batchSize, articles.size());
            List<CivilLawSeed.Article> batch = articles.subList(i, end);
            totalBatches++;

            try {
                BatchResult r = processBatch(batch, dryRun);
                totalEmbedded += r.embedded();
                totalUpserted += r.upserted();
                log.info("배치 {}/{} 완료: embedded={}, upserted={} (articles {}..{})",
                        totalBatches, expectedBatches, r.embedded(), r.upserted(), i + 1, end);
            } catch (Exception e) {
                totalFailed += batch.size();
                for (CivilLawSeed.Article a : batch) {
                    failedArticleNos.add(a.articleNo());
                }
                log.error("배치 {}/{} 처리 실패: range=[{}..{}], error={}",
                        totalBatches, expectedBatches,
                        batch.get(0).articleNo(),
                        batch.get(batch.size() - 1).articleNo(),
                        e.getMessage());
            }

            // rate limit 회피: 마지막 배치 및 dryRun은 지연 없음
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

        int elapsedMs = (int) ((System.nanoTime() - startNanos) / 1_000_000);
        IngestSummary summary = new IngestSummary(
                articles.size(), totalBatches, totalEmbedded, totalUpserted,
                totalFailed, failedArticleNos, elapsedMs, dryRun);
        log.info("민법 인제스트 완료: {}", summary);
        return summary;
    }

    /**
     * 1개 배치 처리: 임베딩 생성 + upsert.
     */
    private BatchResult processBatch(List<CivilLawSeed.Article> batch, boolean dryRun) {
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

        // upsert는 별도 @Transactional 빈에 위임 (self-invocation 회피)
        String embedModel = cohereConfig.getEmbedModel();
        int upserted = upsertService.upsertBatch(
                batch, embeddings, embedModel,
                (article, vector) -> toEntity(article, (short) 0, vector, embedModel));
        return new BatchResult(embeddings.size(), upserted);
    }

    /**
     * Article → LegalChunkEntity 변환.
     * category_ids는 book/chapter/section/group 태그를 조합.
     */
    private LegalChunkEntity toEntity(CivilLawSeed.Article a,
                                      Short chunkIndex,
                                      float[] embedding,
                                      String embeddingModel) {
        Integer no = a.articleNoInt();
        List<String> categories = no != null
                ? categoryMap.resolveCategoryIds(no)
                : Collections.emptyList();

        LocalDate effectiveDate = parseDate(a.effectiveDate());

        return LegalChunkEntity.builder()
                .lawId(a.lawId())
                .lawName(a.lawName())
                .articleNo(a.articleNo())
                .chunkIndex(chunkIndex)
                .articleTitle(a.articleTitle())
                .content(a.content())
                .effectiveDate(effectiveDate)
                .sourceUrl("https://law.go.kr/lawService.do?MST=" + a.sourceMst())
                .categoryIds(categories.toArray(new String[0]))
                .lodUri(null)
                .legislationTerms(new String[0])
                .embedding(embedding)
                .embeddingModel(embeddingModel)
                .build();
    }

    /**
     * 임베딩 입력 포맷: "민법 제N조(제목)\n본문"
     * 조문 라벨을 포함하여 구조화 정보도 벡터 공간에 반영.
     */
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

    private CivilLawSeed loadSeed() {
        try (InputStream in = new ClassPathResource(SEED_PATH).getInputStream()) {
            return objectMapper.readValue(in, CivilLawSeed.class);
        } catch (Exception e) {
            throw new IllegalStateException("민법 시드 JSON 로드 실패: " + SEED_PATH, e);
        }
    }

    // ------------------------------------------------------------------
    // Result types
    // ------------------------------------------------------------------

    private record BatchResult(int embedded, int upserted) {
    }

    public record IngestSummary(
            int totalArticles,
            int totalBatches,
            int totalEmbedded,
            int totalUpserted,
            int totalFailed,
            List<String> failedArticleNos,
            int elapsedMs,
            boolean dryRun
    ) {
        @Override
        public String toString() {
            return String.format(
                    "IngestSummary{articles=%d, batches=%d, embedded=%d, upserted=%d, failed=%d, elapsedMs=%d, dryRun=%s}",
                    totalArticles, totalBatches, totalEmbedded, totalUpserted, totalFailed, elapsedMs, dryRun);
        }
    }
}
