package org.example.shield.brief.application;

import lombok.RequiredArgsConstructor;
import org.example.shield.brief.controller.dto.BriefResponse;
import org.example.shield.brief.controller.dto.BriefSummaryResponse;
import org.example.shield.brief.controller.dto.BriefUpdateRequest;
import org.example.shield.brief.controller.dto.BriefUpdateResponse;
import org.example.shield.brief.domain.Brief;
import org.example.shield.brief.domain.BriefDelivery;
import org.example.shield.brief.domain.BriefDeliveryReader;
import org.example.shield.brief.domain.BriefReader;
import org.example.shield.brief.domain.BriefWriter;
import org.example.shield.brief.exception.BriefAlreadyConfirmedException;
import org.example.shield.common.enums.BriefStatus;
import org.example.shield.common.enums.ConsultationStatus;
import org.example.shield.common.enums.DeliveryStatus;
import org.example.shield.common.enums.PrivacySetting;
import org.example.shield.common.exception.BusinessException;
import org.example.shield.common.exception.ErrorCode;
import org.example.shield.common.response.PageResponse;
import org.example.shield.consultation.domain.Consultation;
import org.example.shield.consultation.domain.ConsultationReader;
import org.example.shield.consultation.domain.ConsultationWriter;
import org.example.shield.user.domain.User;
import org.example.shield.user.domain.UserReader;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.example.shield.common.config.RedisConfig.CACHE_LAWYER_RECOMMENDATIONS;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BriefService {

    private final BriefReader briefReader;
    private final BriefWriter briefWriter;
    private final BriefDeliveryReader deliveryReader;
    private final UserReader userReader;
    private final ConsultationReader consultationReader;
    private final ConsultationWriter consultationWriter;

    public PageResponse<BriefSummaryResponse> getMyBriefs(UUID userId, String status, Pageable pageable) {
        Page<Brief> briefs;
        if (status != null && !status.isBlank()) {
            BriefStatus briefStatus = BriefStatus.valueOf(status.toUpperCase());
            briefs = briefReader.findAllByUserIdAndStatus(userId, briefStatus, pageable);
        } else {
            briefs = briefReader.findAllByUserId(userId, pageable);
        }

        // N+1 방지: 페이지의 brief 들에 대한 수락된 delivery 를 한 번에 조회
        List<UUID> briefIds = briefs.getContent().stream().map(Brief::getId).toList();
        Map<UUID, BriefDelivery> acceptedByBriefId = fetchAcceptedDeliveries(briefIds);
        Map<UUID, String> lawyerNameById = fetchLawyerNames(acceptedByBriefId.values());

        Page<BriefSummaryResponse> responsePage = briefs.map(b -> {
            BriefDelivery accepted = acceptedByBriefId.get(b.getId());
            String lawyerName = accepted != null ? lawyerNameById.get(accepted.getLawyerId()) : null;
            return BriefSummaryResponse.of(b, accepted, lawyerName);
        });
        return PageResponse.from(responsePage);
    }

    public BriefResponse getBrief(UUID briefId, UUID userId) {
        Brief brief = briefReader.findById(briefId);
        validateOwner(brief, userId);

        BriefDelivery accepted = pickAcceptedDelivery(briefId);
        String lawyerName = accepted != null
                ? userReader.findById(accepted.getLawyerId()).getName()
                : null;

        return BriefResponse.of(brief, accepted, lawyerName);
    }

    /**
     * 의뢰서 내용 수정. 키워드/내용 변경은 추천 결과를 좌우하므로 추천 캐시를
     * 모두 무효화한다 (Issue #76 Phase 3).
     *
     * <p>키 패턴 매칭으로 해당 briefId 캐시만 비우는 방식도 가능하나, 호출 빈도가
     * 낮아 안전하게 전체 무효화 채택.</p>
     */
    @Transactional
    @CacheEvict(value = CACHE_LAWYER_RECOMMENDATIONS, allEntries = true)
    public BriefUpdateResponse updateBrief(UUID briefId, UUID userId, BriefUpdateRequest request) {
        Brief brief = briefReader.findById(briefId);
        validateOwner(brief, userId);

        if (!brief.isEditable()) {
            throw new BriefAlreadyConfirmedException();
        }

        // 내용 수정
        brief.updateContent(
                request.title() != null ? request.title() : brief.getTitle(),
                request.content() != null ? request.content() : brief.getContent(),
                request.keywords() != null ? request.keywords() : brief.getKeywords(),
                request.keyIssues() != null ? request.keyIssues() : brief.getKeyIssues(),
                brief.getStrategy()
        );

        // 개인정보 설정 변경
        if (request.privacySetting() != null) {
            try {
                brief.updatePrivacySetting(PrivacySetting.valueOf(request.privacySetting()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE) {};
            }
        }

        // 상태 변경
        if (request.status() != null) {
            switch (request.status()) {
                case "CONFIRMED" -> {
                    brief.confirm();
                    Consultation consultation = consultationReader.findById(brief.getConsultationId());
                    consultation.updateStatus(ConsultationStatus.CONFIRMED);
                    consultationWriter.save(consultation);
                }
                case "DISCARDED" -> brief.discard();
                default -> throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE) {};
            }
        }

        Brief saved = briefWriter.save(brief);

        return new BriefUpdateResponse(
                saved.getId(),
                saved.getStatus().name(),
                saved.getUpdatedAt()
        );
    }

    private void validateOwner(Brief brief, UUID userId) {
        if (!brief.getUserId().equals(userId)) {
            throw new org.example.shield.brief.exception.BriefNotFoundException(brief.getId());
        }
    }

    /**
     * 단건 조회용: brief 의 CONFIRMED delivery 중 가장 먼저 수락된 1건.
     * 가드 도입 후에는 최대 1건만 존재해야 하나 기존 데이터 호환을 위해 안전망으로 정렬 후 선택.
     */
    private BriefDelivery pickAcceptedDelivery(UUID briefId) {
        return deliveryReader.findAllByBriefId(briefId).stream()
                .filter(d -> d.getStatus() == DeliveryStatus.CONFIRMED)
                .min(Comparator.comparing(
                        BriefDelivery::getRespondedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    /**
     * 목록 조회용 배치: briefId → 가장 먼저 수락된 delivery 매핑.
     */
    private Map<UUID, BriefDelivery> fetchAcceptedDeliveries(List<UUID> briefIds) {
        if (briefIds.isEmpty()) return Map.of();
        return deliveryReader.findAllByBriefIdInAndStatus(briefIds, DeliveryStatus.CONFIRMED).stream()
                .collect(Collectors.toMap(
                        BriefDelivery::getBriefId,
                        Function.identity(),
                        (a, b) -> {
                            // 안전망: 동일 brief 에 CONFIRMED 가 여러 건 있을 경우 가장 이른 수락자 선택
                            if (a.getRespondedAt() == null) return b;
                            if (b.getRespondedAt() == null) return a;
                            return a.getRespondedAt().isBefore(b.getRespondedAt()) ? a : b;
                        }
                ));
    }

    private Map<UUID, String> fetchLawyerNames(java.util.Collection<BriefDelivery> deliveries) {
        if (deliveries.isEmpty()) return Map.of();
        List<UUID> lawyerIds = deliveries.stream().map(BriefDelivery::getLawyerId).toList();
        return userReader.findAllByIds(lawyerIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));
    }
}
