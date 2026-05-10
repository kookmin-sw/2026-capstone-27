package org.example.shield.consultation.application;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Phase 1 Chat 파이프라인 전용 Micrometer 메트릭 센서.
 *
 * <p>계측 대상:</p>
 * <ul>
 *   <li>{@code shield.chat.cohere.call} — Cohere Chat v2 호출 지연
 *       (timer, tag: {@code outcome=success|failure|blank})</li>
 *   <li>{@code shield.chat.blank_response} — blank {@code nextQuestion} 발생 카운터
 *       (counter, tag: {@code stage=chat})</li>
 *   <li>{@code shield.chat.send_message} — sendMessage 전체 파이프라인 지연
 *       (timer, tag: {@code outcome=success|blank|pii|error})</li>
 * </ul>
 *
 * <p>메트릭 네이밍은 기존 {@code shield.rag.*} 컨벤션({@code RagMetrics})과
 * 일관성을 맞춘다. {@code /actuator/prometheus} 에 자동 노출되며,
 * 공통 {@code application} 태그는 {@code spring.application.name} 으로 부여된다.</p>
 *
 * <p>중앙화 이유는 {@code RagMetrics} 와 동일 — 호출 지점마다 Counter 를
 * 찾지 않도록 API 를 단순화하고, 테스트에서 {@code SimpleMeterRegistry} 로
 * 대체하기 쉽게 한다.</p>
 */
@Component
public class ChatMetrics {

    public static final String METRIC_COHERE_CALL = "shield.chat.cohere.call";
    public static final String METRIC_BLANK_RESPONSE = "shield.chat.blank_response";
    public static final String METRIC_SEND_MESSAGE = "shield.chat.send_message";

    private final MeterRegistry registry;

    public ChatMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    // === Cohere chat() 호출 ===

    public Timer.Sample startCohereCall() {
        return Timer.start(registry);
    }

    public void stopCohereCallSuccess(Timer.Sample sample) {
        sample.stop(registry.timer(METRIC_COHERE_CALL, Tags.of("outcome", "success")));
    }

    public void stopCohereCallBlank(Timer.Sample sample) {
        sample.stop(registry.timer(METRIC_COHERE_CALL, Tags.of("outcome", "blank")));
    }

    public void stopCohereCallFailure(Timer.Sample sample) {
        sample.stop(registry.timer(METRIC_COHERE_CALL, Tags.of("outcome", "failure")));
    }

    // === Blank response counter ===

    public void incrementBlankResponse() {
        Counter.builder(METRIC_BLANK_RESPONSE)
                .tag("stage", "chat")
                .register(registry)
                .increment();
    }

    // === sendMessage 전체 파이프라인 ===

    public void recordSendMessage(long startNanos, String outcome) {
        registry.timer(METRIC_SEND_MESSAGE, Tags.of("outcome", outcome))
                .record(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
    }
}
