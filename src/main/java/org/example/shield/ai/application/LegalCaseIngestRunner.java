package org.example.shield.ai.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 판례 인제스트 CLI Runner (Phase C-4).
 *
 * <p>실행 조건:</p>
 * <ul>
 *   <li>{@code ingest} 프로파일 활성화</li>
 *   <li>CLI 인자 {@code --ingest=cases}</li>
 * </ul>
 *
 * <p>실행 예:</p>
 * <pre>
 * SPRING_PROFILES_ACTIVE=ingest ./gradlew bootRun --args='--ingest=cases'
 * SPRING_PROFILES_ACTIVE=ingest ./gradlew bootRun --args='--ingest=cases --dryRun=true'
 * </pre>
 *
 * <p>{@link CivilLawIngestRunner}, {@link SpecialLawIngestRunner}와 동일 프로파일에서
 * 공존 가능. {@link Order}로 특별법(20) 뒤인 30으로 두어 실행 순서를 명시.</p>
 */
@Component
@Profile("ingest")
@Order(30)
@RequiredArgsConstructor
@Slf4j
public class LegalCaseIngestRunner implements ApplicationRunner {

    private static final String OPTION = "ingest";
    private static final String TARGET = "cases";

    private final LegalCaseIngestService ingestService;
    private final ConfigurableApplicationContext context;

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption(OPTION) || !args.getOptionValues(OPTION).contains(TARGET)) {
            log.info("LegalCaseIngestRunner: --ingest=cases 인자 없음, 스킵");
            return;
        }

        boolean dryRun = args.containsOption("dryRun")
                && args.getOptionValues("dryRun").contains("true");

        log.info("===== 판례 인제스트 시작 (dryRun={}) =====", dryRun);
        int exitCode;
        try {
            LegalCaseIngestService.IngestSummary summary = ingestService.run(dryRun);
            log.info("===== 판례 인제스트 종료: {} =====", summary);
            exitCode = summary.totalFailed() == 0 ? 0 : 2;
        } catch (Exception e) {
            log.error("===== 판례 인제스트 실패: {} =====", e.getMessage(), e);
            exitCode = 1;
        }

        int rc = exitCode;
        new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            int code = SpringApplication.exit(context, () -> rc);
            System.exit(code);
        }, "ingest-shutdown-cases").start();
    }
}
