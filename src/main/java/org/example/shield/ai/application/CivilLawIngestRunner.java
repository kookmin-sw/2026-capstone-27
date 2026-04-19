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
 * 민법 인제스트 CLI Runner.
 *
 * <p>다음 조건을 모두 만족할 때만 실행된다:</p>
 * <ul>
 *   <li>{@code ingest} 프로파일 활성화 ({@code SPRING_PROFILES_ACTIVE=ingest})</li>
 *   <li>CLI 인자 {@code --ingest=civil-law} 포함</li>
 * </ul>
 *
 * <p>인자 옵션:</p>
 * <ul>
 *   <li>{@code --ingest=civil-law} — 실제 인제스트</li>
 *   <li>{@code --dryRun=true} — 임베딩 API 호출 및 DB 쓰기 없이 파싱/매핑만 검증</li>
 * </ul>
 *
 * <p>실행 예:</p>
 * <pre>
 * SPRING_PROFILES_ACTIVE=ingest ./gradlew bootRun --args='--ingest=civil-law'
 * SPRING_PROFILES_ACTIVE=ingest ./gradlew bootRun --args='--ingest=civil-law --dryRun=true'
 * </pre>
 *
 * <p>인제스트 후에는 Spring 컨텍스트를 정상 종료해 웹 서버가 뜨지 않도록 한다.</p>
 */
@Component
@Profile("ingest")
@Order(10)
@RequiredArgsConstructor
@Slf4j
public class CivilLawIngestRunner implements ApplicationRunner {

    private final CivilLawIngestService ingestService;
    private final ConfigurableApplicationContext context;

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption("ingest")
                || !args.getOptionValues("ingest").contains("civil-law")) {
            log.info("CivilLawIngestRunner: --ingest=civil-law 인자 없음, 스킵");
            return;
        }

        boolean dryRun = args.containsOption("dryRun")
                && args.getOptionValues("dryRun").contains("true");

        log.info("===== 민법 인제스트 시작 (dryRun={}) =====", dryRun);
        int exitCode;
        try {
            CivilLawIngestService.IngestSummary summary = ingestService.run(dryRun);
            log.info("===== 민법 인제스트 종료: {} =====", summary);
            exitCode = summary.totalFailed() == 0 ? 0 : 2;
        } catch (Exception e) {
            log.error("===== 민법 인제스트 실패: {} =====", e.getMessage(), e);
            exitCode = 1;
        }

        // 같은 실행에서 --ingest=special-laws도 지정된 경우 SpecialLawIngestRunner가
        // 이어서 돌아야 하므로, 특별법 러너가 별도로 종료를 담당한다.
        // Civil 단독 실행일 때만 여기서 종료 처리.
        boolean alsoSpecialLaws = args.containsOption(OPTION)
                && args.getOptionValues(OPTION).contains("special-laws");
        if (alsoSpecialLaws) {
            log.info("--ingest=special-laws도 지정됨 → 종료는 Special 러너에 위임");
            if (exitCode != 0) {
                log.warn("Civil 인제스트 exitCode={} 보존", exitCode);
            }
            return;
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
        }, "ingest-shutdown").start();
    }

    private static final String OPTION = "ingest";
}
