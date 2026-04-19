package org.example.shield.ai.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 특별법 인제스트 CLI Runner.
 *
 * <p>실행 조건:</p>
 * <ul>
 *   <li>{@code ingest} 프로파일 활성화</li>
 *   <li>CLI 인자 {@code --ingest=special-laws}</li>
 * </ul>
 *
 * <p>실행 예:</p>
 * <pre>
 * SPRING_PROFILES_ACTIVE=ingest ./gradlew bootRun --args='--ingest=special-laws'
 * SPRING_PROFILES_ACTIVE=ingest ./gradlew bootRun --args='--ingest=special-laws --dryRun=true'
 * </pre>
 *
 * <p>{@link CivilLawIngestRunner}와 동일한 {@code ingest} 프로파일 내에서 공존 가능하며,
 * 인자에 따라 선택적으로 실행된다. {@link Order}로 Civil 이후에 실행되도록 뒀다 —
 * 같은 실행에서 {@code --ingest=civil-law --ingest=special-laws} 모두 줬을 때를 대비.</p>
 */
@Component
@Profile("ingest")
@Order(20)
@RequiredArgsConstructor
@Slf4j
public class SpecialLawIngestRunner implements ApplicationRunner {

    private static final String OPTION = "ingest";
    private static final String TARGET = "special-laws";

    private final SpecialLawIngestService ingestService;
    private final ConfigurableApplicationContext context;

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption(OPTION) || !args.getOptionValues(OPTION).contains(TARGET)) {
            log.info("SpecialLawIngestRunner: --ingest=special-laws 인자 없음, 스킵");
            return;
        }

        boolean dryRun = args.containsOption("dryRun")
                && args.getOptionValues("dryRun").contains("true");

        log.info("===== 특별법 인제스트 시작 (dryRun={}) =====", dryRun);
        int exitCode;
        try {
            SpecialLawIngestService.IngestSummary summary = ingestService.run(dryRun);
            log.info("===== 특별법 인제스트 종료: {} =====", summary);
            exitCode = summary.totalFailed() == 0 ? 0 : 2;
        } catch (Exception e) {
            log.error("===== 특별법 인제스트 실패: {} =====", e.getMessage(), e);
            exitCode = 1;
        }

        // civil-law와 함께 실행되는 경우 중복 종료 방지 — 여기서는 exit하지 않고
        // ApplicationRunner 순서가 마지막(@Order(20))이라는 가정 하에 종료 처리
        int rc = exitCode;
        new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            int code = org.springframework.boot.SpringApplication.exit(context, () -> rc);
            System.exit(code);
        }, "ingest-shutdown-special").start();
    }
}
