package org.example.shield.lawyer.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.common.enums.VerificationStatus;
import org.example.shield.lawyer.domain.LawyerProfile;
import org.example.shield.lawyer.infrastructure.LawyerProfileRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * 기존 VERIFIED 변호사 임베딩 백필 Runner (Issue #50).
 *
 * <p>실행 조건:</p>
 * <ul>
 *   <li>{@code ingest} 프로파일 (SPRING_PROFILES_ACTIVE=ingest)</li>
 *   <li>CLI 인자 {@code --backfill=lawyer-embeddings}</li>
 * </ul>
 *
 * <p>예시:</p>
 * <pre>
 * SPRING_PROFILES_ACTIVE=ingest ./gradlew bootRun --args='--backfill=lawyer-embeddings'
 * </pre>
 *
 * <p>VERIFIED 변호사를 페이지 단위(100개)로 순회하며
 * {@link LawyerEmbeddingService#upsertEmbedding(LawyerProfile)} 호출.
 * 동일 해시면 Cohere 호출은 자동 skip.</p>
 */
@Component
@Profile("ingest")
@Order(30)
@RequiredArgsConstructor
@Slf4j
public class LawyerEmbeddingBackfillRunner implements ApplicationRunner {

    private static final int PAGE_SIZE = 100;

    private final LawyerProfileRepository lawyerProfileRepository;
    private final LawyerEmbeddingService lawyerEmbeddingService;
    private final ConfigurableApplicationContext context;

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption("backfill")
                || !args.getOptionValues("backfill").contains("lawyer-embeddings")) {
            log.info("LawyerEmbeddingBackfillRunner: --backfill=lawyer-embeddings 인자 없음, 스킵");
            return;
        }

        log.info("===== 변호사 임베딩 백필 시작 =====");
        int processed = 0;
        int failed = 0;
        int exitCode = 0;

        try {
            int pageNumber = 0;
            while (true) {
                Pageable pageable = PageRequest.of(pageNumber, PAGE_SIZE, Sort.by("createdAt"));
                Page<LawyerProfile> page = lawyerProfileRepository
                        .findAllByVerificationStatus(VerificationStatus.VERIFIED, pageable);

                if (page.isEmpty()) break;

                for (LawyerProfile profile : page.getContent()) {
                    try {
                        lawyerEmbeddingService.upsertEmbedding(profile);
                        processed++;
                    } catch (Exception ex) {
                        failed++;
                        log.warn("백필 개별 실패 lawyerId={} error={}", profile.getId(), ex.getMessage());
                    }
                }

                log.info("진행: page={} processed={} failed={}", pageNumber, processed, failed);
                if (!page.hasNext()) break;
                pageNumber++;
            }

            log.info("===== 변호사 임베딩 백필 종료: processed={} failed={} =====", processed, failed);
            exitCode = failed == 0 ? 0 : 2;
        } catch (Exception e) {
            log.error("===== 변호사 임베딩 백필 실패: {} =====", e.getMessage(), e);
            exitCode = 1;
        }

        // 웹 서버 기동 없이 즉시 종료
        int rc = exitCode;
        new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            int code = org.springframework.boot.SpringApplication.exit(context, () -> rc);
            System.exit(code);
        }, "lawyer-embedding-backfill-shutdown").start();
    }
}
