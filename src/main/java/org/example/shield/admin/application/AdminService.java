package org.example.shield.admin.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.admin.controller.dto.DashboardAlertsResponse;
import org.example.shield.admin.controller.dto.DashboardStatsResponse;
import org.example.shield.admin.controller.dto.LawyerDetailResponse;
import org.example.shield.admin.controller.dto.PendingLawyerResponse;
import org.example.shield.admin.controller.dto.VerificationChecksResponse;
import org.example.shield.admin.controller.dto.VerificationLogResponse;
import org.example.shield.admin.controller.dto.VerificationResponse;
import org.example.shield.admin.domain.VerificationCheckReader;
import org.example.shield.admin.domain.VerificationLog;
import org.example.shield.admin.domain.VerificationLogReader;
import org.example.shield.admin.domain.VerificationLogWriter;
import org.example.shield.admin.infrastructure.VerificationLogRepository;
import org.example.shield.common.enums.VerificationStatus;
import org.example.shield.common.exception.BusinessException;
import org.example.shield.common.exception.ErrorCode;
import org.example.shield.common.response.PageResponse;
import org.example.shield.lawyer.application.LawyerEmbeddingService;
import org.example.shield.lawyer.domain.LawyerProfile;
import org.example.shield.lawyer.domain.LawyerReader;
import org.example.shield.lawyer.domain.LawyerWriter;
import org.example.shield.lawyer.infrastructure.LawyerDocumentRepository;
import org.example.shield.lawyer.infrastructure.LawyerProfileRepository;
import org.example.shield.user.domain.User;
import org.example.shield.user.domain.UserReader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final LawyerReader lawyerReader;
    private final LawyerWriter lawyerWriter;
    private final UserReader userReader;
    private final LawyerDocumentRepository lawyerDocumentRepository;
    private final LawyerProfileRepository lawyerProfileRepository;
    private final VerificationLogWriter verificationLogWriter;
    private final VerificationLogReader verificationLogReader;
    private final VerificationLogRepository verificationLogRepository;
    private final VerificationCheckReader verificationCheckReader;
    private final LawyerEmbeddingService lawyerEmbeddingService;

    public DashboardStatsResponse getDashboardStats() {
        log.info("대시보드 통계 조회");

        long pendingCount = lawyerReader.countByVerificationStatus(VerificationStatus.PENDING);
        long reviewingCount = lawyerReader.countByVerificationStatus(VerificationStatus.REVIEWING);
        long supplementRequestedCount = lawyerReader.countByVerificationStatus(VerificationStatus.SUPPLEMENT_REQUESTED);

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayProcessedCount = verificationLogRepository.countByCreatedAtAfterAndToStatusIn(
                todayStart, List.of("VERIFIED", "REJECTED"));

        return new DashboardStatsResponse(pendingCount, reviewingCount, supplementRequestedCount, todayProcessedCount);
    }

    public DashboardAlertsResponse getDashboardAlerts() {
        log.info("대시보드 긴급 알림 조회");

        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        long overdueCount = lawyerProfileRepository.countByVerificationStatusAndCreatedAtBefore(
                VerificationStatus.PENDING, twentyFourHoursAgo);
        long missingDocumentCount = lawyerProfileRepository.countMissingDocuments();
        long duplicateSuspectCount = lawyerProfileRepository.countDuplicateBarNumbers();

        return new DashboardAlertsResponse(overdueCount, missingDocumentCount, duplicateSuspectCount);
    }

    public PageResponse<VerificationLogResponse> getVerificationLogs(String period, String status,
                                                                      Pageable pageable) {
        log.info("처리 이력 조회. period={}, status={}", period, status);

        LocalDateTime after = resolvePeriod(period);
        Page<VerificationLog> logPage;

        if (status != null && after != null) {
            logPage = verificationLogReader.findAllByToStatusAndCreatedAtAfter(status, after, pageable);
        } else if (status != null) {
            logPage = verificationLogReader.findAllByToStatus(status, pageable);
        } else if (after != null) {
            logPage = verificationLogReader.findAllByCreatedAtAfter(after, pageable);
        } else {
            logPage = verificationLogReader.findAll(pageable);
        }

        List<VerificationLog> logs = logPage.getContent();
        if (logs.isEmpty()) {
            return PageResponse.from(logPage.map(l -> null));
        }

        List<UUID> lawyerIds = logs.stream().map(VerificationLog::getLawyerId).distinct().toList();
        Map<UUID, LawyerProfile> lawyerMap = lawyerIds.stream()
                .collect(Collectors.toMap(id -> id, lawyerReader::findById));

        List<UUID> allUserIds = new java.util.ArrayList<>();
        lawyerMap.values().forEach(lp -> allUserIds.add(lp.getUserId()));
        logs.forEach(l -> allUserIds.add(l.getAdminId()));

        Map<UUID, User> userMap = userReader.findAllByIds(allUserIds.stream().distinct().toList()).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        Page<VerificationLogResponse> responsePage = logPage.map(vlog -> {
            LawyerProfile lawyer = lawyerMap.get(vlog.getLawyerId());
            User lawyerUser = userMap.get(lawyer.getUserId());
            User admin = userMap.get(vlog.getAdminId());
            return new VerificationLogResponse(
                    vlog.getId(),
                    lawyerUser != null ? lawyerUser.getName() : null,
                    vlog.getFromStatus(),
                    vlog.getToStatus(),
                    lawyer.getDomains(),
                    admin != null ? admin.getName() : null,
                    vlog.getReason(),
                    vlog.getCreatedAt()
            );
        });

        return PageResponse.from(responsePage);
    }

    private LocalDateTime resolvePeriod(String period) {
        if (period == null) return null;
        return switch (period.toLowerCase()) {
            case "today" -> LocalDate.now().atStartOfDay();
            case "week" -> LocalDate.now().minusDays(7).atStartOfDay();
            default -> null;
        };
    }

    public PageResponse<PendingLawyerResponse> getPendingLawyers(String keyword, String status,
                                                                  Pageable pageable) {
        log.info("변호사 심사 목록 조회. keyword={}, status={}", keyword, status);

        Page<LawyerProfile> lawyerPage = lawyerReader.searchByStatusAndKeyword(
                status, keyword, pageable);

        List<LawyerProfile> lawyers = lawyerPage.getContent();
        if (lawyers.isEmpty()) {
            return PageResponse.from(lawyerPage.map(lp -> null));
        }

        List<UUID> userIds = lawyers.stream()
                .map(LawyerProfile::getUserId)
                .toList();
        Map<UUID, User> userMap = userReader.findAllByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<UUID> lawyerIds = lawyers.stream()
                .map(LawyerProfile::getId)
                .toList();
        Map<UUID, Long> documentCountMap = lawyerDocumentRepository.countByLawyerIds(lawyerIds).stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long) row[1]
                ));

        Page<PendingLawyerResponse> responsePage = lawyerPage.map(lawyer -> {
            User user = userMap.get(lawyer.getUserId());
            long docCount = documentCountMap.getOrDefault(lawyer.getId(), 0L);
            return PendingLawyerResponse.from(lawyer, user, docCount);
        });

        return PageResponse.from(responsePage);
    }

    public LawyerDetailResponse getLawyerDetail(UUID lawyerId) {
        log.info("변호사 상세 조회. lawyerId={}", lawyerId);
        LawyerProfile lawyer = lawyerReader.findById(lawyerId);
        User user = userReader.findById(lawyer.getUserId());
        return LawyerDetailResponse.from(lawyer, user);
    }

    @Transactional
    public VerificationResponse processVerification(UUID lawyerId, UUID adminId,
                                                     String statusStr, String reason) {
        log.info("변호사 인증 처리. lawyerId={}, adminId={}, status={}", lawyerId, adminId, statusStr);

        LawyerProfile lawyer = lawyerReader.findById(lawyerId);
        String previousStatus = lawyer.getVerificationStatus().name();

        VerificationStatus newStatus = parseVerificationStatus(statusStr);
        validateReasonRequired(newStatus, reason);

        lawyer.updateVerificationStatus(newStatus);
        lawyerWriter.save(lawyer);

        // VERIFIED 로 전환된 경우 매칭용 임베딩을 업서트 (Issue #50)
        if (newStatus == VerificationStatus.VERIFIED) {
            try {
                lawyerEmbeddingService.upsertEmbedding(lawyer);
            } catch (Exception ex) {
                log.warn("변호사 임베딩 생성 실패 (승인은 성공) lawyerId={} error={}", lawyerId, ex.getMessage());
            }
        }

        VerificationLog log = VerificationLog.create(lawyerId, adminId, previousStatus, newStatus.name(), reason);
        verificationLogWriter.save(log);

        return new VerificationResponse(
                lawyerId,
                previousStatus,
                newStatus.name(),
                reason,
                log.getCreatedAt()
        );
    }

    public VerificationChecksResponse getVerificationChecks(UUID lawyerId) {
        log.info("검증 체크리스트 조회. lawyerId={}", lawyerId);
        return verificationCheckReader.findOptionalByLawyerId(lawyerId)
                .map(VerificationChecksResponse::from)
                .orElse(VerificationChecksResponse.empty(lawyerId));
    }

    private VerificationStatus parseVerificationStatus(String status) {
        try {
            return VerificationStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE) {};
        }
    }

    private void validateReasonRequired(VerificationStatus status, String reason) {
        if ((status == VerificationStatus.REJECTED || status == VerificationStatus.SUPPLEMENT_REQUESTED)
                && (reason == null || reason.isBlank())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE) {};
        }
    }
}
