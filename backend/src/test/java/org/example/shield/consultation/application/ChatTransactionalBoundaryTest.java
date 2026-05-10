package org.example.shield.consultation.application;

import org.example.shield.consultation.domain.Consultation;
import org.example.shield.consultation.domain.ConsultationReader;
import org.example.shield.consultation.domain.ConsultationWriter;
import org.example.shield.consultation.domain.Message;
import org.example.shield.consultation.domain.MessageWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * {@link ChatTransactionalBoundary} 단위 테스트.
 *
 * <p>boundary 는 DB 작업만 담당하는 얇은 컴포넌트이므로 주로 검증할 것은:</p>
 * <ol>
 *   <li>각 메서드가 {@code @Transactional} 어노테이션을 유지하는지 (트랜잭션 분리 보장)</li>
 *   <li>AiFinalizePayload 값이 Consultation / Message 에 올바르게 반영되는지</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class ChatTransactionalBoundaryTest {

    @Mock private MessageWriter messageWriter;
    @Mock private ConsultationWriter consultationWriter;
    @Mock private ConsultationReader consultationReader;
    @Mock private Consultation consultation;

    @InjectMocks
    private ChatTransactionalBoundary boundary;

    private UUID consultationId;

    @BeforeEach
    void setUp() {
        consultationId = UUID.randomUUID();
    }

    // ── 트랜잭션 계약 고정 ────────────────────────────────────────────────

    @Test
    @DisplayName("saveUserMessage / savePiiAiMessage / finalizeAiResponse 는 @Transactional 이어야 한다")
    void methods_haveTransactionalAnnotation() throws NoSuchMethodException {
        for (String name : new String[]{"saveUserMessage", "savePiiAiMessage", "finalizeAiResponse"}) {
            Method m = findMethod(name);
            Transactional tx = m.getAnnotation(Transactional.class);
            assertThat(tx)
                    .as("%s 는 @Transactional 이 유지되어야 트랜잭션 분리 효과가 보장된다", name)
                    .isNotNull();
            assertThat(tx.readOnly())
                    .as("%s 는 write 트랜잭션이어야 한다", name)
                    .isFalse();
        }
    }

    private Method findMethod(String name) {
        return java.util.Arrays.stream(ChatTransactionalBoundary.class.getDeclaredMethods())
                .filter(m -> m.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError(name + " 메서드가 존재해야 한다"));
    }

    // ── saveUserMessage ──────────────────────────────────────────────────

    @Test
    @DisplayName("saveUserMessage → Message.createUserMessage 로 USER 메시지 1건 저장")
    void saveUserMessage_savesUserRoleMessage() {
        given(messageWriter.save(any(Message.class))).willAnswer(inv -> inv.getArgument(0));

        Message saved = boundary.saveUserMessage(consultationId, "사용자 입력");

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageWriter, times(1)).save(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("사용자 입력");
        assertThat(saved).isNotNull();
    }

    // ── finalizeAiResponse ────────────────────────────────────────────────

    @Test
    @DisplayName("finalizeAiResponse → lastResponseId 업데이트 + 분류 반영 + AI 메시지 저장 + consultation save")
    void finalizeAiResponse_fullFlow() {
        given(consultationReader.findById(consultationId)).willReturn(consultation);
        given(consultation.updateAiClassification(any(), any(), any())).willReturn(true);
        given(messageWriter.save(any(Message.class))).willAnswer(inv -> inv.getArgument(0));

        AiFinalizePayload payload = new AiFinalizePayload(
                "resp-ok", "다음 질문입니다.", "command-a-03-2025",
                100, 42, 250,
                List.of("부동산 거래"), List.of("부동산 임대차"), List.of("보증금 및 차임")
        );

        Message savedAi = boundary.finalizeAiResponse(consultationId, payload);

        verify(consultation).updateLastResponseId("resp-ok");
        verify(consultation).updateAiClassification(
                List.of("부동산 거래"), List.of("부동산 임대차"), List.of("보증금 및 차임"));
        verify(consultation).touch();
        verify(consultationWriter, times(1)).save(consultation);
        verify(messageWriter, times(1)).save(any(Message.class));
        assertThat(savedAi.getContent()).isEqualTo("다음 질문입니다.");
    }

    @Test
    @DisplayName("finalizeAiResponse — 분류 payload 가 모두 null 이면 updateAiClassification 미호출")
    void finalizeAiResponse_noClassification_skipsUpdate() {
        given(consultationReader.findById(consultationId)).willReturn(consultation);
        given(messageWriter.save(any(Message.class))).willAnswer(inv -> inv.getArgument(0));

        AiFinalizePayload payload = new AiFinalizePayload(
                "resp-noai", "질문", "model-x", 10, 5, 100,
                null, null, null);

        boundary.finalizeAiResponse(consultationId, payload);

        verify(consultation).updateLastResponseId("resp-noai");
        verify(consultation, times(0)).updateAiClassification(any(), any(), any());
    }
}
