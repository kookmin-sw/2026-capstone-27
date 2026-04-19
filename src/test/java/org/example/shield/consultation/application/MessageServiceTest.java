package org.example.shield.consultation.application;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.example.shield.consultation.domain.ConsultationWriter;
import org.example.shield.consultation.domain.Message;
import org.example.shield.consultation.domain.MessageReader;
import org.example.shield.consultation.domain.MessageWriter;
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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * {@link MessageService} 단위 테스트 (Issue #45).
 *
 * <p>Cohere chat() 결과 {@code nextQuestion} 이 null/blank 인 경우
 * 빈 CHATBOT 메시지를 DB 에 저장하지 않고 {@link ChatAiException} 으로
 * 조기 실패하는지 검증한다. 정상 응답은 기존대로 저장된다.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageServiceTest {

    @Mock private MessageReader messageReader;
    @Mock private MessageWriter messageWriter;
    @Mock private ConsultationReader consultationReader;
    @Mock private ConsultationWriter consultationWriter;
    @Mock private CohereService cohereService;
    @Mock private CohereApiConfig cohereApiConfig;
    @Mock private SanitizeService sanitizeService;
    @Mock private ChecklistCoverageService checklistCoverageService;
    @Mock private RagPipelineService ragPipelineService;
    @Mock private Consultation consultation;

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
            // loadOntology 는 package-private(@PostConstruct) — 다른 패키지에서는 reflection 으로 호출
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
        given(consultation.getFirstDomain()).willReturn(null);                 // RAG skip
        given(sanitizeService.sanitizeUserText(anyString())).willReturn("clean");
        given(messageReader.findAllByConsultationId(consultationId)).willReturn(Collections.emptyList());
        given(cohereApiConfig.getChatModel()).willReturn("command-a-03-2025");
    }

    @Test
    @DisplayName("nextQuestion 이 blank 이면 ChatAiException 을 던지고 AI 메시지를 저장하지 않는다")
    void sendMessage_blankNextQuestion_throwsChatAiException() {
        // given
        ChatParsedResponse parsed = new ChatParsedResponse();
        parsed.setNextQuestion("   ");   // whitespace-only
        AiCallResult<ChatParsedResponse> result = new AiCallResult<>(
                "resp-blank-1", parsed, 100, 0, 250);
        given(cohereService.chat(any(), anyString(), anyString(), any())).willReturn(result);

        // when / then
        assertThatThrownBy(() -> messageService.sendMessage(consultationId, "사용자 입력"))
                .isInstanceOf(ChatAiException.class);

        // USER 메시지는 이미 저장된 상태라 verify 는 정확히 1회 (AI 메시지는 저장 X)
        verify(messageWriter, times(1)).save(any(Message.class));
        verify(consultationWriter, never()).save(any(Consultation.class));
    }

    @Test
    @DisplayName("nextQuestion 이 null 이면 ChatAiException 을 던진다")
    void sendMessage_nullNextQuestion_throwsChatAiException() {
        // given
        ChatParsedResponse parsed = new ChatParsedResponse();
        parsed.setNextQuestion(null);
        AiCallResult<ChatParsedResponse> result = new AiCallResult<>(
                "resp-null-1", parsed, 100, 0, 250);
        given(cohereService.chat(any(), anyString(), anyString(), any())).willReturn(result);

        // when / then
        assertThatThrownBy(() -> messageService.sendMessage(consultationId, "사용자 입력"))
                .isInstanceOf(ChatAiException.class);

        verify(messageWriter, times(1)).save(any(Message.class));
        verify(consultationWriter, never()).save(any(Consultation.class));
    }

    @Test
    @DisplayName("정상 nextQuestion 이면 AI 메시지를 저장하고 응답을 반환한다")
    void sendMessage_validNextQuestion_savesAiMessage() {
        // given
        ChatParsedResponse parsed = new ChatParsedResponse();
        parsed.setNextQuestion("추가로 어떤 피해를 입으셨나요?");
        parsed.setAiDomains(List.of());
        parsed.setAiSubDomains(List.of());
        parsed.setAiTags(List.of());
        AiCallResult<ChatParsedResponse> result = new AiCallResult<>(
                "resp-ok-1", parsed, 100, 42, 250);
        given(cohereService.chat(any(), anyString(), anyString(), any())).willReturn(result);

        // AI 메시지 저장 반환값 stub (mock Message 로 최소 동작만 보장)
        Message savedAi = Message.createAiMessage(
                consultationId, parsed.getNextQuestion(),
                "command-a-03-2025", 100, 42, 250);
        given(messageWriter.save(any(Message.class))).willReturn(savedAi);

        // when
        SendMessageResponse response = messageService.sendMessage(consultationId, "사용자 입력");

        // then
        assertThat(response).isNotNull();
        // USER + AI 합 2회 저장
        verify(messageWriter, times(2)).save(any(Message.class));
        // Consultation 상태 업데이트 경로 도달 확인
        verify(consultation).updateLastResponseId("resp-ok-1");
        verify(consultationWriter, times(1)).save(consultation);
    }

    // =====================================================================
    // Issue #48 온톨로지 환각 필터 통합 검증 (real OntologyService)
    // =====================================================================

    @Test
    @DisplayName("L1=부동산 거래, AI 가 잘못된 L2=재산분할(이혼 도메인) 반환 → 필터링되어 null 저장")
    void sendMessage_환각L2필터링() {
        // given — 실제 Consultation 으로 교체 (update 검증)
        Consultation real = Consultation.create(consultationId,
                List.of("부동산 거래"), null, null);
        given(consultationReader.findById(consultationId)).willReturn(real);
        given(ragPipelineService.execute(any(), anyString(), any())).willReturn("");

        ChatParsedResponse parsed = new ChatParsedResponse();
        parsed.setNextQuestion("어떤 유형의 부동산 거래인가요?");
        parsed.setAiDomains(List.of("부동산 거래"));
        // 환각: 재산분할 은 이혼·위자료·재산분할 트리 소속 → 부동산 거래 의 직계 자식 아님
        parsed.setAiSubDomains(List.of("재산분할"));
        parsed.setAiTags(List.of());
        AiCallResult<ChatParsedResponse> result = new AiCallResult<>(
                "resp-hallu-1", parsed, 100, 42, 250);
        given(cohereService.chat(any(), anyString(), anyString(), any())).willReturn(result);
        given(messageWriter.save(any(Message.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        messageService.sendMessage(consultationId, "사용자 입력");

        // then — L1 은 user 고정, AI 의 L2 는 온톨로지 위반으로 제거되어 null 저장
        assertThat(real.getUserDomains()).containsExactly("부동산 거래");
        assertThat(real.getAiDomains()).isNull();        // user 가 있으므로 ai 미반영
        assertThat(real.getAiSubDomains()).isNull();     // 환각 제거 → 유효 0개 → null
        assertThat(real.getAiTags()).isNull();
    }

    @Test
    @DisplayName("L1=부동산 거래, AI L2=[부동산 임대차(정상),재산분할(환각)] → 정상만 남음")
    void sendMessage_환각L2부분필터링() {
        Consultation real = Consultation.create(consultationId,
                List.of("부동산 거래"), null, null);
        given(consultationReader.findById(consultationId)).willReturn(real);
        given(ragPipelineService.execute(any(), anyString(), any())).willReturn("");

        ChatParsedResponse parsed = new ChatParsedResponse();
        parsed.setNextQuestion("임대차 관련 상세식 알려주세요.");
        parsed.setAiDomains(List.of("부동산 거래"));
        parsed.setAiSubDomains(List.of("부동산 임대차", "재산분할")); // 1개만 정상
        parsed.setAiTags(List.of("보증금 및 차임"));                    // 부동산 임대차 자식 → 정상
        given(cohereService.chat(any(), anyString(), anyString(), any()))
                .willReturn(new AiCallResult<>("resp-hallu-2", parsed, 100, 42, 250));
        given(messageWriter.save(any(Message.class))).willAnswer(inv -> inv.getArgument(0));

        messageService.sendMessage(consultationId, "사용자 입력");

        // 정상 L2 1개만 남고, L3 도 정상 부모(부동산 임대차) 중 유효하므로 저장
        assertThat(real.getAiSubDomains()).containsExactly("부동산 임대차");
        assertThat(real.getAiTags()).containsExactly("보증금 및 차임");
    }

    @Test
    @DisplayName("부모(userDomains)가 없으면 검증 skip — AI 가 반환한 L2 그대로 저장")
    void sendMessage_부모없으면검증skip() {
        Consultation real = Consultation.create(consultationId, null, null, null);
        given(consultationReader.findById(consultationId)).willReturn(real);

        ChatParsedResponse parsed = new ChatParsedResponse();
        parsed.setNextQuestion("더 자세히 설명해주세요.");
        parsed.setAiDomains(List.of("부동산 거래"));
        parsed.setAiSubDomains(List.of("부동산 임대차"));
        parsed.setAiTags(List.of("보증금 및 차임"));
        given(cohereService.chat(any(), anyString(), anyString(), any()))
                .willReturn(new AiCallResult<>("resp-noparent", parsed, 100, 42, 250));
        given(messageWriter.save(any(Message.class))).willAnswer(inv -> inv.getArgument(0));

        messageService.sendMessage(consultationId, "사용자 입력");

        // user 가 없으므로 AI 값이 그대로 반영 (부모가 null 이면 검증 skip)
        assertThat(real.getAiDomains()).containsExactly("부동산 거래");
        assertThat(real.getAiSubDomains()).containsExactly("부동산 임대차");
        assertThat(real.getAiTags()).containsExactly("보증금 및 차임");
    }
}
