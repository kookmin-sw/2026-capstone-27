package org.example.shield.consultation.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.ai.application.GrokService;
import org.example.shield.ai.dto.BriefParsedResponse;
import org.example.shield.ai.dto.GrokCallResult;
import org.example.shield.brief.domain.Brief;
import org.example.shield.brief.domain.BriefWriter;
import org.example.shield.common.enums.ConsultationStatus;
import org.example.shield.common.notification.NotificationSender;
import org.example.shield.consultation.domain.Consultation;
import org.example.shield.consultation.domain.ConsultationReader;
import org.example.shield.consultation.domain.ConsultationWriter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 분석 서비스 — 의뢰서 생성 (비동기).
 *
 * Called by: ConsultationController.analyze()
 * Flow: 전체 chatHistory → Grok Phase 2 → Brief 저장 → 상태 갱신 → 알림
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisService {

    private final GrokService grokService;
    private final ConsultationReader consultationReader;
    private final ConsultationWriter consultationWriter;
    private final BriefWriter briefWriter;
    private final NotificationSender notificationSender;

    /**
     * 의뢰서 비동기 생성.
     * Controller에서 202 Accepted 응답 후 호출됨.
     */
    @Async("briefTaskExecutor")
    @Transactional
    public CompletableFuture<Void> analyzeAsync(UUID consultationId) {
        log.info("의뢰서 생성 시작: consultationId={}", consultationId);

        try {
            // 1. 상담 조회 (이미 ANALYZING 상태)
            Consultation consultation = consultationReader.findById(consultationId);

            // 2. Grok Phase 2 호출 — 의뢰서 생성
            GrokCallResult<BriefParsedResponse> result = grokService.generateBrief(consultation);
            BriefParsedResponse parsed = result.data();

            // 3. Brief 엔티티 생성 + 저장
            String legalField = (consultation.getPrimaryField() != null
                    && !consultation.getPrimaryField().isEmpty())
                    ? consultation.getPrimaryField().get(0) : "UNKNOWN";

            List<String> keyIssueStrings = parsed.getKeyIssues() != null
                    ? parsed.getKeyIssues().stream()
                        .map(ki -> ki.getTitle() + ": " + ki.getDescription())
                        .toList()
                    : List.of();

            Brief brief = Brief.create(
                    consultationId,
                    consultation.getUserId(),
                    parsed.getTitle(),
                    legalField,
                    parsed.getContent(),
                    parsed.getKeywords(),
                    keyIssueStrings,
                    parsed.getStrategy()
            );
            briefWriter.save(brief);

            // 4. 상담 상태 갱신: ANALYZING → AWAITING_CONFIRM
            consultation.updateStatus(ConsultationStatus.AWAITING_CONFIRM);
            consultationWriter.save(consultation);

            // 5. 알림 발송
            try {
                notificationSender.sendBriefReadyNotification(consultation.getUserId(), brief.getId());
            } catch (Exception e) {
                log.warn("의뢰서 완료 알림 발송 실패 (무시): {}", e.getMessage());
            }

            log.info("의뢰서 생성 완료: consultationId={}, briefId={}",
                    consultationId, brief.getId());

        } catch (Exception e) {
            log.error("의뢰서 생성 실패: consultationId={}", consultationId, e);

            try {
                Consultation consultation = consultationReader.findById(consultationId);
                consultation.updateStatus(ConsultationStatus.REJECTED);
                consultationWriter.save(consultation);

                notificationSender.sendBriefFailedNotification(consultation.getUserId());
            } catch (Exception inner) {
                log.error("의뢰서 실패 상태 갱신 중 추가 오류: {}", inner.getMessage());
            }
        }

        return CompletableFuture.completedFuture(null);
    }
}
