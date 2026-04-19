package org.example.shield.consultation.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.example.shield.ai.application.ChecklistCoverageService;
import org.example.shield.ai.application.CohereService;
import org.example.shield.ai.application.OntologyService;
import org.example.shield.ai.application.RagPipelineService;
import org.example.shield.ai.config.CohereApiConfig;
import org.example.shield.ai.dto.AiCallResult;
import org.example.shield.ai.dto.ChatParsedResponse;
import org.example.shield.ai.infrastructure.SanitizeService;
import org.example.shield.common.exception.ChatAiException;
import org.example.shield.consultation.controller.dto.SendMessageResponse;
import org.example.shield.consultation.domain.Consultation;
import org.example.shield.consultation.domain.ConsultationReader;
import org.example.shield.consultation.domain.Message;
import org.example.shield.consultation.domain.MessageReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * {@link MessageService} 단위 테스트.
 *
 * <p>PR-C 에서 DB 트랜잭션 경계가 {@link ChatTransactionalBoundary} 로 분리되어
 * MessageService 자체는 non-transactional 조율자 역할만 한다. 따라서 이 테스트는
 * boundary 를 mock 으로 두고 조율 흐름만 검증한다 (실제 트랜잭션 동작은 통합
 * 테스트 범위).</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageServiceTest {

    @Mock private MessageReader messageReader;
    @Mock private ConsultationReader consultationReader;
    @Mock private CohereService cohereService;
    @Mock private CohereApiConfig cohereApiConfig;
    @Mock private SanitizeService sanitizeService;
    @Mock private ChecklistCoverageService checklistCoverageService;
    @Mock private RagPipelineService ragPipelineService;
    @Mock private Consultation consultation;
    @Mock private ChatTransactionalBoundary chatTxBoundary;

    // 실제 MeterRegistry — 카운터 증감까지 검증한다.
    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();
    @Spy
    private ChatMetrics chatMetrics = new ChatMetrics(meterRegistry);

    // 실제 온톨로지 JSON 을 로드한 real OntologyService 주입 — 환각 필터 동작을 end-to-end 로 검증.
    @Spy
    private OntologyService ontologyService = createRealOntologyService();

    @InjectMocks
    private MessageService messageService;

    private static OntologyService createRealOntologyService() {
        try {
            String json;
            try (InputStream in = new ClassPathResource("ontology/legal-ontology-slim.json").getInputStream()) {
                json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            OntologyService svc = new OntologyService(json, new ObjectMapper());
            var m = OntologyService.class.getDeclaredMethod("loadOntology");
            m.setAccessible(true);
            m.invoke(svc);
            return svc;
        } catch (Exception e) {
            throw new IllegalStateException("온톨로지 로드 실패", e);
        }
    }

    private UUID consultationId;

    @BeforeEach
    void setUp() {
        consultationId = UUID.randomUUID();

        given(consultationReader.findById(consultationId)).willReturn(consultation);
        given(consultation.getFirstDomain()).willReturn(null); // RAG skip
        given(sanitizeService.sanitizeUserText(anyString())).willReturn("clean");
        given(messageReader.findAllByConsultationId(consultationId)).willReturn(Collections.emptyList());
        given(cohereApiConfig.getChatModel()).willReturn("command-a-03-2025");
        // boundary 의 finalize 는 기본적으로 저장된 Message 를 리턴하도록 느슨하게 stub
        given(chatTxBoundary.finalizeAiResponse(eq(consultationId), any(AiFinalizePayload.class)))
                .willAnswer(inv -> {
                    AiFinalizePayload p = inv.getArgument(1);
                    return Message.createAiMessage(consultationId, p.nextQuestion(),
                            p.model(), p.tokensInput(), p.tokensOutput(), p.latencyMs());
                });
    }

    // ── Issue #45 — blank 응답 차단 ──────────────────────────────────────

    @Test
    @DisplayName("nextQuestion blank → ChatAiException + USER 메시지는 boundary 로 저장됨 + blank_response 카운터 증가")
    void sendMessage_blankNextQuestion_throwsChatAiException() {
        ChatParsedResponse parsed = new ChatParsedResponse();
        parsed.setNextQuestion("   ");
        given(cohereService.chat(any(), anyString(), anyString(), any()))
                .willReturn(new AiCallResult<>("resp-blank-1", parsed, 100, 0, 250));

        assertThatThrownBy(() -> messageService.sendMessage(consultationId, "사용자 입력"))
                .isInstanceOf(ChatAiException.class);

        // USER 는 먼저 저장되어야 한다 (독립 tx)
        verify(chatTxBoundary, times(1)).saveUserMessage(consultationId, "사용자 입력");
        // AI 응답 finalize 는 호출되지 않는다
        verify(chatTxBoundary, never()).finalizeAiResponse(any(), any());

        // 메트릭 — blank 카운터 1건, send_message timer outcome=blank
        assertThat(meterRegistry.counter(ChatMetrics.METRIC_BLANK_RESPONSE, "stage", "chat").count())
                .isEqualTo(1.0);
        assertThat(meterRegistry.timer(ChatMetrics.METRIC_SEND_MESSAGE, "outcome", "blank").count())
                .isEqualTo(1L);
        assertThat(meterRegistry.timer(ChatMetrics.METRIC_COHERE_CALL, "outcome", "blank").count())
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("nextQuestion null → ChatAiException")
    void sendMessage_nullNextQuestion_throwsChatAiException() {
        ChatParsedResponse parsed = new ChatParsedResponse();
        parsed.setNextQuestion(null);
        given(cohereService.chat(any(), anyString(), anyString(), any()))
                .willReturn(new AiCallResult<>("resp-null-1", parsed, 100, 0, 250));

        assertThatThrownBy(() -> messageService.sendMessage(consultationId, "사용자 입력"))
                .isInstanceOf(ChatAiException.class);
        verify(chatTxBoundary, times(1)).saveUserMessage(consultationId, "사용자 입력");
        verify(chatTxBoundary, never()).finalizeAiResponse(any(), any());
    }

    @Test
    @DisplayName("정상 nextQuestion → finalizeAiResponse 에 payload 전달 + success 메트릭")
    void sendMessage_validNextQuestion_savesAiMessage() {
        ChatParsedResponse parsed = new ChatParsedResponse();
        parsed.setNextQuestion("추가로 어떤 피해를 입으셨나요?");
        parsed.setAiDomains(List.of());
        parsed.setAiSubDomains(List.of());
        parsed.setAiTags(List.of());
        given(cohereService.chat(any(), anyString(), anyString(), any()))
                .willReturn(new AiCallResult<>("resp-ok-1", parsed, 100, 42, 250));

        SendMessageResponse response = messageService.sendMessage(consultationId, "사용자 입력");

        assertThat(response).isNotNull();

        ArgumentCaptor<AiFinalizePayload> captor = ArgumentCaptor.forClass(AiFinalizePayload.class);
        verify(chatTxBoundary, times(1)).saveUserMessage(consultationId, "사용자 입력");
        verify(chatTxBoundary, times(1)).finalizeAiResponse(eq(consultationId), captor.capture());

        AiFinalizePayload p = captor.getValue();
        assertThat(p.responseId()).isEqualTo("resp-ok-1");
        assertThat(p.nextQuestion()).isEqualTo("추가로 어떤 피해를 입으셨나요?");
        assertThat(p.model()).isEqualTo("command-a-03-2025");
        assertThat(p.tokensInput()).isEqualTo(100);
        assertThat(p.tokensOutput()).isEqualTo(42);
        assertThat(p.latencyMs()).isEqualTo(250);
        assertThat(p.hasAnyClassification()).isFalse();

        // 메트릭
        assertThat(meterRegistry.timer(ChatMetrics.METRIC_SEND_MESSAGE, "outcome", "success").count())
                .isEqualTo(1L);
        assertThat(meterRegistry.timer(ChatMetrics.METRIC_COHERE_CALL, "outcome", "success").count())
                .isEqualTo(1L);
    }

    // ── Cohere 실패 시 메트릭 / tx 분리 검증 ──────────────────────────────

    @Test
    @DisplayName("Cohere 호출 예외 → USER 메시지는 저장된 상태로 남고, cohere.call failure 메트릭 기록")
    void sendMessage_cohereFailure_userPreservedAndMetered() {
        given(cohereService.chat(any(), anyString(), anyString(), any()))
                .willThrow(new RuntimeException("Cohere 500"));

        assertThatThrownBy(() -> messageService.sendMessage(consultationId, "사용자 입력"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Cohere 500");

        // USER 는 별도 tx 로 이미 커밋되었으므로 boundary 호출은 1회 수행
        verify(chatTxBoundary, times(1)).saveUserMessage(consultationId, "사용자 입력");
        verify(chatTxBoundary, never()).finalizeAiResponse(any(), any());

        assertThat(meterRegistry.timer(ChatMetrics.METRIC_COHERE_CALL, "outcome", "failure").count())
                .isEqualTo(1L);
        assertThat(meterRegistry.timer(ChatMetrics.METRIC_SEND_MESSAGE, "outcome", "error").count())
                .isEqualTo(1L);
    }

    // ── 온톨로지 환각 필터 (AI 분류) — 순수 로직 ──────────────────────────

    @Test
    @DisplayName("L1=부동산 거래, AI L2=[부동산 임대차(정상),재산분할(환각)] → 정상만 payload 에 실린다")
    void sendMessage_filtersHallucinatedSubDomains() {
        Consultation real = Consultation.create(consultationId,
                List.of("부동산 거래"), null, null);
        given(consultationReader.findById(consultationId)).willReturn(real);
        given(ragPipelineService.execute(any(), anyString(), any())).willReturn("");

        ChatParsedResponse parsed = new ChatParsedResponse();
        parsed.setNextQuestion("임대차 보증금 조건을 알려주세요.");
        parsed.setAiDomains(List.of("부동산 거래"));
        parsed.setAiSubDomains(List.of("부동산 임대차", "재산분할")); // 1개 환각
        parsed.setAiTags(List.of("보증금 및 차임"));
        given(cohereService.chat(any(), anyString(), anyString(), any()))
                .willReturn(new AiCallResult<>("resp-hallu", parsed, 100, 42, 250));

        messageService.sendMessage(consultationId, "사용자 입력");

        ArgumentCaptor<AiFinalizePayload> captor = ArgumentCaptor.forClass(AiFinalizePayload.class);
        verify(chatTxBoundary).finalizeAiResponse(eq(consultationId), captor.capture());
        AiFinalizePayload p = captor.getValue();
        assertThat(p.aiSubDomains()).containsExactly("부동산 임대차");
        assertThat(p.aiTags()).containsExactly("보증금 및 차임");
    }

    @Test
    @DisplayName("부모(userDomains)가 없으면 환각 필터 skip — AI L2/L3 가 그대로 payload 에 실린다")
    void sendMessage_noParent_skipsValidation() {
        Consultation real = Consultation.create(consultationId, null, null, null);
        given(consultationReader.findById(consultationId)).willReturn(real);

        ChatParsedResponse parsed = new ChatParsedResponse();
        parsed.setNextQuestion("더 자세히 설명해주세요.");
        parsed.setAiDomains(List.of("부동산 거래"));
        parsed.setAiSubDomains(List.of("부동산 임대차"));
        parsed.setAiTags(List.of("보증금 및 차임"));
        given(cohereService.chat(any(), anyString(), anyString(), any()))
                .willReturn(new AiCallResult<>("resp-noparent", parsed, 100, 42, 250));

        messageService.sendMessage(consultationId, "사용자 입력");

        ArgumentCaptor<AiFinalizePayload> captor = ArgumentCaptor.forClass(AiFinalizePayload.class);
        verify(chatTxBoundary).finalizeAiResponse(eq(consultationId), captor.capture());
        AiFinalizePayload p = captor.getValue();
        assertThat(p.aiDomains()).containsExactly("부동산 거래");
        assertThat(p.aiSubDomains()).containsExactly("부동산 임대차");
        assertThat(p.aiTags()).containsExactly("보증금 및 차임");
    }

    // ── PII 안내 응답 — early return 경로 ──────────────────────────────────

    @Test
    @DisplayName("PII 감지 → boundary.savePiiAiMessage 로 안내 메시지 저장, Cohere 호출 없음")
    void sendMessage_piiDetected_earlyReturn() {
        given(sanitizeService.sanitizeUserText(anyString()))
                .willThrow(new SanitizeService.PiiDetectedException("주민등록번호가 감지되었습니다."));
        given(chatTxBoundary.savePiiAiMessage(eq(consultationId), anyString()))
                .willReturn(Message.createAiMessage(consultationId, "주민등록번호가 감지되었습니다.",
                        null, null, null, null));

        SendMessageResponse response = messageService.sendMessage(consultationId, "주민번호 1234");

        assertThat(response).isNotNull();
        verify(chatTxBoundary, times(1)).savePiiAiMessage(eq(consultationId), anyString());
        verify(chatTxBoundary, never()).saveUserMessage(any(), any());
        verify(chatTxBoundary, never()).finalizeAiResponse(any(), any());
        verify(cohereService, never()).chat(any(), any(), any(), any());

        assertThat(meterRegistry.timer(ChatMetrics.METRIC_SEND_MESSAGE, "outcome", "pii").count())
                .isEqualTo(1L);
    }

    // ── Issue #45 후속 — @Transactional(noRollbackFor=ChatAiException) 회귀 테스트 ──

    /**
     * {@link MessageService#sendMessage} 는 반드시
     * {@code @Transactional(noRollbackFor = ChatAiException.class)} 로
     * 지정되어 있어야 한다. 이 어노테이션이 제거되거나 속성이 누락되면
     * AI blank 응답 시 USER 메시지와 {@code lastResponseId} 까지 롤백되어
     * Issue #45 가 재발한다 (사용자 입력 유실).
     *
     * <p>통합 테스트로 실제 트랜잭션 동작을 검증하는 것이 이상적이지만,
     * 현재 테스트 슬라이스는 단위 레벨이므로 어노테이션 계약을
     * 명시적으로 고정해 회귀를 방지한다.</p>
     */
    @Test
    @DisplayName("sendMessage 는 @Transactional(noRollbackFor = ChatAiException.class) 계약을 유지해야 한다 (Issue #45 회귀)")
    void sendMessage_transactionalContract_noRollbackForChatAiException() throws NoSuchMethodException {
        Method method = MessageService.class.getDeclaredMethod("sendMessage", UUID.class, String.class);
        Transactional annotation = method.getAnnotation(Transactional.class);

        assertThat(annotation)
                .as("sendMessage 에 @Transactional 이 지정되어 있어야 한다")
                .isNotNull();
        assertThat(annotation.noRollbackFor())
                .as("noRollbackFor 에 ChatAiException 이 포함되어야 USER 메시지 유실이 방지된다")
                .contains(ChatAiException.class);
    }

    /**
     * Mockito 단위 테스트 레벨에서 "AI 실패 시에도 USER 메시지는
     * 저장된 후 예외가 던져진다"는 순서 계약을 명시적으로 검증한다.
     * (실제 커밋 여부는 통합 테스트 범위)
     */
    @Test
    @DisplayName("nextQuestion blank 시에도 USER 메시지는 먼저 저장된 뒤 ChatAiException 이 발생한다")
    void sendMessage_blankNextQuestion_userMessageSavedBeforeException() {
        // given
        Consultation real = Consultation.create(consultationId, null, null, null);
        given(consultationReader.findById(consultationId)).willReturn(real);
        given(ragPipelineService.execute(any(), anyString(), any())).willReturn("");

        ChatParsedResponse parsed = new ChatParsedResponse();
        parsed.setNextQuestion("   "); // blank
        given(cohereService.chat(any(), anyString(), anyString(), any()))
                .willReturn(new AiCallResult<>("resp-blank-regression", parsed, 100, 0, 250));
        given(messageWriter.save(any(Message.class))).willAnswer(inv -> inv.getArgument(0));

        // when / then
        assertThatThrownBy(() -> messageService.sendMessage(consultationId, "사용자 입력"))
                .isInstanceOf(ChatAiException.class);

        // USER 메시지만 저장되어야 한다 (AI 메시지는 blank 차단으로 저장 X)
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageWriter, times(1)).save(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("사용자 입력");

        // lastResponseId 도 dirty state 로 남아 있어야 한다 (감사 로깅 목적)
        assertThat(real.getLastResponseId()).isEqualTo("resp-blank-regression");
    }
}
