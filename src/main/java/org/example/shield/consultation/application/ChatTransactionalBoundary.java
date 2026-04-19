package org.example.shield.consultation.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.consultation.domain.Consultation;
import org.example.shield.consultation.domain.ConsultationReader;
import org.example.shield.consultation.domain.ConsultationWriter;
import org.example.shield.consultation.domain.Message;
import org.example.shield.consultation.domain.MessageWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Chat 파이프라인의 DB 트랜잭션 경계를 모아둔 컴포넌트.
 *
 * <p>Cohere Chat v2 호출은 외부 HTTP 요청이라 {@code sendMessage} 와 같은
 * 단일 트랜잭션 안에서 수행되면 DB 커넥션을 수초~수십초 점유할 수 있다.
 * 이 클래스는 짧은 DB-only 트랜잭션만 담당해 커넥션 보유 시간을 최소화한다.</p>
 *
 * <p>Spring 프록시 특성상 같은 클래스의 self-invocation 은 {@code @Transactional}
 * 을 태우지 못하므로, 트랜잭션 단위를 별도 bean 으로 분리했다. {@link MessageService}
 * 는 이 클래스를 주입받아 각 단계를 호출한다.</p>
 *
 * <p>트랜잭션 단위:</p>
 * <ul>
 *   <li>{@link #saveUserMessage(UUID, String)} — USER 메시지 영속화 (독립 커밋)</li>
 *   <li>{@link #finalizeAiResponse(UUID, AiFinalizePayload)} — AI 응답 이후
 *       consultation 상태 업데이트 + AI 메시지 영속화 (독립 커밋)</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatTransactionalBoundary {

    private final MessageWriter messageWriter;
    private final ConsultationWriter consultationWriter;
    private final ConsultationReader consultationReader;

    /**
     * USER 메시지를 독립 트랜잭션으로 저장한다.
     *
     * <p>후속 Cohere 호출이 실패하거나 blank 응답을 반환하더라도
     * 사용자 입력은 절대 유실되지 않는다 (Issue #45 후속 — PR-A 의 noRollbackFor
     * 보다 한 단계 더 강력한 격리).</p>
     */
    @Transactional
    public Message saveUserMessage(UUID consultationId, String content) {
        Message userMessage = Message.createUserMessage(consultationId, content);
        return messageWriter.save(userMessage);
    }

    /**
     * PII 감지 시 AI 채널로 안내 메시지만 저장하는 독립 트랜잭션 경로.
     */
    @Transactional
    public Message savePiiAiMessage(UUID consultationId, String message) {
        Message piiMessage = Message.createAiMessage(
                consultationId, message, null, null, null, null);
        return messageWriter.save(piiMessage);
    }

    /**
     * Cohere 호출 성공 후 DB 에 반영할 상태 변경을 하나의 트랜잭션으로 커밋한다.
     *
     * <p>Consultation 은 준영속(detached) 상태이므로 reader 로 재조회하고
     * payload 로 받은 값을 반영한 뒤 writer 로 저장한다. AI 메시지는
     * 별도로 영속화한다.</p>
     *
     * @param consultationId 대상 상담 ID
     * @param payload        AI 응답 처리 결과 (분류·메시지·메타)
     * @return 저장된 AI {@link Message}
     */
    @Transactional
    public Message finalizeAiResponse(UUID consultationId, AiFinalizePayload payload) {
        Consultation consultation = consultationReader.findById(consultationId);

        // 1. lastResponseId (감사 로깅)
        consultation.updateLastResponseId(payload.responseId());

        // 2. AI 분류 결과 반영 (이미 온톨로지 필터링 완료된 값)
        if (payload.hasAnyClassification()) {
            boolean updated = consultation.updateAiClassification(
                    payload.aiDomains(), payload.aiSubDomains(), payload.aiTags());
            if (!updated) {
                log.warn("LLM attempted to override locked classification: consultationId={}, aiDomains={}",
                        consultationId, payload.aiDomains());
            }
        }

        // 3. AI 메시지 영속화
        Message aiMessage = Message.createAiMessage(
                consultationId,
                payload.nextQuestion(),
                payload.model(),
                payload.tokensInput(),
                payload.tokensOutput(),
                payload.latencyMs()
        );
        Message savedAi = messageWriter.save(aiMessage);

        // 4. 상담 마지막 메시지 + updatedAt 갱신
        consultation.updateLastMessage(savedAi.getContent(), savedAi.getCreatedAt());
        consultation.touch();
        consultationWriter.save(consultation);

        return savedAi;
    }
}
