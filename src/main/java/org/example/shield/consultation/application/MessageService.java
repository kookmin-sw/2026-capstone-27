package org.example.shield.consultation.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.ai.application.CategoryLawMappingService;
import org.example.shield.ai.application.ChecklistCoverageService;
import org.example.shield.ai.application.CohereService;
import org.example.shield.ai.application.IntentClassificationService;
import org.example.shield.ai.application.LegalRetrievalService;
import org.example.shield.ai.application.RagContextBuilder;
import org.example.shield.ai.config.CohereApiConfig;
import org.example.shield.ai.dto.ChatParsedResponse;
import org.example.shield.ai.dto.IntentClassificationResult;
import org.example.shield.ai.dto.LegalChunk;
import org.example.shield.ai.dto.AiCallResult;
import org.example.shield.ai.infrastructure.SanitizeService;
import org.example.shield.common.response.PageResponse;
import org.example.shield.consultation.controller.dto.MessageResponse;
import org.example.shield.consultation.controller.dto.SendMessageResponse;
import org.example.shield.consultation.domain.Consultation;
import org.example.shield.consultation.domain.ConsultationReader;
import org.example.shield.consultation.domain.ConsultationWriter;
import org.example.shield.consultation.domain.Message;
import org.example.shield.consultation.domain.MessageReader;
import org.example.shield.consultation.domain.MessageWriter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MessageService {

    private final MessageReader messageReader;
    private final MessageWriter messageWriter;
    private final ConsultationReader consultationReader;
    private final ConsultationWriter consultationWriter;
    private final CohereService cohereService;
    private final CohereApiConfig cohereApiConfig;
    private final SanitizeService sanitizeService;
    private final ChecklistCoverageService checklistCoverageService;
    private final IntentClassificationService intentClassificationService;
    private final CategoryLawMappingService categoryLawMappingService;
    private final LegalRetrievalService legalRetrievalService;
    private final RagContextBuilder ragContextBuilder;

    @Transactional
    public SendMessageResponse sendMessage(UUID consultationId, String content) {
        Consultation consultation = consultationReader.findById(consultationId);

        // 0. 사용자 입력 sanitization (P0-III)
        String sanitizedText;
        try {
            sanitizedText = sanitizeService.sanitizeUserText(content);
        } catch (SanitizeService.PiiDetectedException e) {
            // PII 검출 시 사용자에게 안내 메시지 반환
            Message piiMessage = Message.createAiMessage(
                    consultationId, e.getMessage(), null, null, null, null);
            Message savedPii = messageWriter.save(piiMessage);
            return SendMessageResponse.from(savedPii, false);
        }

        // 1. USER 메시지 저장 (원문 보존)
        Message userMessage = Message.createUserMessage(consultationId, content);
        messageWriter.save(userMessage);

        // 대화 내역 1회 조회 — RAG와 chat() 양쪽에서 공유 (중복 DB 쿼리 방지)
        List<Message> chatHistory = messageReader.findAllByConsultationId(consultationId);

        // [RAG] Layer 1-2-3 (primaryField가 설정된 이후에만 실행)
        String ragContext = "";
        if (consultation.getPrimaryField() != null && !consultation.getPrimaryField().isEmpty()) {
            try {
                String primaryField = consultation.getPrimaryField().get(0);

                // Layer 1: 의도 분류
                IntentClassificationResult classification =
                        intentClassificationService.classify(chatHistory, primaryField);

                // Layer 2: 법률 검색
                List<String> lawIds = categoryLawMappingService.resolveLawIds(
                        classification.matchedNodeIds());
                // B-8a: 온톨로지 노드 → DB category_ids 토큰 매핑 (soft-filter)
                List<String> categoryIds = categoryLawMappingService.resolveCategoryIds(
                        classification.matchedNodeIds());
                String vectorQuery = classification.retrievalQueries().isEmpty()
                        ? primaryField + " 관련 법률"
                        : classification.retrievalQueries().get(0);
                List<LegalChunk> chunks = legalRetrievalService.retrieve(
                        vectorQuery,
                        classification.keywords().core(),
                        categoryIds,
                        lawIds,
                        3);

                // Layer 3: 컨텍스트 빌드
                ragContext = ragContextBuilder.build(chunks, classification.intentSummary());
                if (!ragContext.isEmpty()) {
                    log.info("RAG 컨텍스트 생성 완료: consultationId={}, chunks={}", consultationId, chunks.size());
                }
            } catch (Exception e) {
                log.warn("RAG 파이프라인 실패, 폴백 (RAG 없이 진행): consultationId={}, error={}",
                        consultationId, e.getMessage());
                ragContext = "";
            }
        }

        // 2. Cohere API 호출 (Phase 1 대화 — RAG 컨텍스트 포함, 조회된 chatHistory 재사용)
        AiCallResult<ChatParsedResponse> result = cohereService.chat(consultation, sanitizedText, ragContext, chatHistory);
        ChatParsedResponse parsed = result.data();

        // 3. 응답 ID 저장 (감사 로깅용)
        consultation.updateLastResponseId(result.responseId());

        // 4. 분류 결과 처리 (P0-V: primary_field_locked 가드)
        if (parsed.getPrimaryField() != null && !parsed.getPrimaryField().isEmpty()) {
            boolean updated = consultation.updateClassificationFromLlm(parsed.getPrimaryField());
            if (!updated) {
                log.warn("LLM attempted to override locked primaryField: consultationId={}, llm={}, stored={}",
                        consultationId, parsed.getPrimaryField(), consultation.getPrimaryField());
            }
        }

        // 5. tags 업데이트
        if (parsed.getTags() != null && !parsed.getTags().isEmpty()) {
            consultation.updateTags(parsed.getTags());
        }

        // 6. AI 메시지 저장
        Message aiMessage = Message.createAiMessage(
                consultationId,
                parsed.getNextQuestion(),
                cohereApiConfig.getChatModel(),  // model name from config
                result.tokensInput(),
                result.tokensOutput(),
                result.latencyMs()
        );
        Message savedAi = messageWriter.save(aiMessage);

        // 7. allCompleted AND gate (P0-II — 코드 레벨 검증)
        boolean effectiveAllCompleted = false;
        if (parsed.isAllCompleted()) {
            String primaryFieldStr = (consultation.getPrimaryField() != null
                    && !consultation.getPrimaryField().isEmpty())
                    ? consultation.getPrimaryField().get(0) : null;

            double coverageRatio = checklistCoverageService.compute(
                    consultationId, primaryFieldStr);
            effectiveAllCompleted = checklistCoverageService.isEffectivelyCompleted(
                    true, coverageRatio);

            if (!effectiveAllCompleted) {
                log.warn("LLM reported allCompleted=true but coverage={} < {}: consultationId={}",
                        coverageRatio, checklistCoverageService.getThreshold(), consultationId);
            }
        }

        // 8. 상담 마지막 메시지 + updatedAt 갱신
        consultation.updateLastMessage(savedAi.getContent(), savedAi.getCreatedAt());
        consultation.touch();
        consultationWriter.save(consultation);

        return SendMessageResponse.from(savedAi, effectiveAllCompleted);
    }

    public PageResponse<MessageResponse> getMessages(UUID consultationId, Pageable pageable) {
        // 상담 존재 확인
        consultationReader.findById(consultationId);

        Page<Message> messages = messageReader.findAllByConsultationId(consultationId, pageable);
        Page<MessageResponse> responsePage = messages.map(MessageResponse::from);

        return PageResponse.from(responsePage);
    }
}
