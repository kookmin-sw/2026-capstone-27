package org.example.shield.consultation.application;

import org.example.shield.ai.application.ChecklistCoverageService;
import org.example.shield.ai.application.CohereService;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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

    @InjectMocks
    private MessageService messageService;

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
}
