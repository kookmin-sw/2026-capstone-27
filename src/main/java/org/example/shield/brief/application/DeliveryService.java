package org.example.shield.brief.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.brief.controller.dto.DeliveryListResponse;
import org.example.shield.brief.controller.dto.DeliveryResponse;
import org.example.shield.brief.controller.dto.DeliveryStatusResponse;
import org.example.shield.brief.controller.dto.InboxDetailResponse;
import org.example.shield.brief.controller.dto.InboxResponse;
import org.example.shield.brief.controller.dto.InboxStatsResponse;
import org.example.shield.brief.domain.Brief;
import org.example.shield.brief.domain.BriefDelivery;
import org.example.shield.brief.domain.BriefDeliveryReader;
import org.example.shield.brief.domain.BriefDeliveryWriter;
import org.example.shield.brief.domain.BriefReader;
import org.example.shield.brief.exception.BriefNotFoundException;
import org.example.shield.common.enums.BriefStatus;
import org.example.shield.common.enums.DeliveryStatus;
import org.example.shield.common.enums.PrivacySetting;
import org.example.shield.common.enums.VerificationStatus;
import org.example.shield.lawyer.domain.LawyerProfile;
import org.example.shield.lawyer.domain.LawyerReader;
import org.example.shield.common.exception.BusinessException;
import org.example.shield.common.exception.ErrorCode;
import org.example.shield.common.response.PageResponse;
import org.example.shield.user.domain.User;
import org.example.shield.user.domain.UserReader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryService {

    private final BriefReader briefReader;
    private final BriefDeliveryReader deliveryReader;
    private final BriefDeliveryWriter deliveryWriter;
    private final UserReader userReader;
    private final LawyerReader lawyerReader;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public DeliveryStatusResponse updateDeliveryStatus(UUID deliveryId, UUID lawyerId,
                                                        String statusStr, String rejectionReason) {
        log.info("의뢰서 수락/거절 요청. deliveryId={}, lawyerId={}, status={}", deliveryId, lawyerId, statusStr);

        BriefDelivery delivery = deliveryReader.findById(deliveryId);

        if (!delivery.getLawyerId().equals(lawyerId)) {
            throw new BusinessException(ErrorCode.DELIVERY_NOT_FOUND) {};
        }

        if (delivery.getStatus() != DeliveryStatus.DELIVERED) {
            throw new BusinessException(ErrorCode.DELIVERY_ALREADY_PROCESSED) {};
        }

        switch (statusStr.toUpperCase()) {
            case "CONFIRMED" -> delivery.accept();
            case "REJECTED" -> {
                if (rejectionReason == null || rejectionReason.isBlank()) {
                    throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE) {};
                }
                delivery.reject(rejectionReason);
            }
            default -> throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE) {};
        }

        deliveryWriter.save(delivery);

        Brief brief = briefReader.findById(delivery.getBriefId());
        User client = userReader.findById(brief.getUserId());
        User lawyer = userReader.findById(lawyerId);

        eventPublisher.publishEvent(new DeliveryStatusEvent(
                client.getEmail(),
                client.getName(),
                lawyer.getName(),
                brief.getTitle(),
                delivery.getStatus().name(),
                rejectionReason
        ));

        return new DeliveryStatusResponse(
                delivery.getId(),
                delivery.getStatus().name(),
                delivery.getRespondedAt()
        );
    }

    @Transactional
    public DeliveryResponse createDelivery(UUID briefId, UUID lawyerId, UUID userId) {
        Brief brief = briefReader.findById(briefId);
        validateOwner(brief, userId);

        if (deliveryReader.existsByBriefIdAndLawyerId(briefId, lawyerId)) {
            throw new BusinessException(ErrorCode.DELIVERY_ALREADY_EXISTS) {};
        }

        if (brief.getStatus() != BriefStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.BRIEF_NOT_CONFIRMED) {};
        }

        User lawyer = userReader.findById(lawyerId);
        LawyerProfile lawyerProfile = lawyerReader.findByUserId(lawyerId);
        if (lawyerProfile.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new BusinessException(ErrorCode.LAWYER_NOT_VERIFIED) {};
        }

        BriefDelivery delivery = BriefDelivery.create(briefId, lawyerId);
        BriefDelivery saved = deliveryWriter.save(delivery);

        brief.markDelivered();

        return DeliveryResponse.of(saved, lawyer.getName());
    }

    public DeliveryListResponse getDeliveries(UUID briefId, UUID userId) {
        Brief brief = briefReader.findById(briefId);
        validateOwner(brief, userId);

        List<BriefDelivery> deliveries = deliveryReader.findAllByBriefId(briefId);

        // N+1 방지: 변호사 ID를 모아서 한 번에 조회
        List<UUID> lawyerIds = deliveries.stream().map(BriefDelivery::getLawyerId).toList();
        Map<UUID, User> lawyerMap = userReader.findAllByIds(lawyerIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<DeliveryResponse> responses = deliveries.stream()
                .map(d -> {
                    User lawyer = lawyerMap.get(d.getLawyerId());
                    return DeliveryResponse.of(d, lawyer != null ? lawyer.getName() : "알 수 없음");
                })
                .toList();

        return new DeliveryListResponse(responses);
    }

    public PageResponse<InboxResponse> getInbox(UUID lawyerId, DeliveryStatus status, Pageable pageable) {
        Page<BriefDelivery> deliveries = (status != null)
                ? deliveryReader.findAllByLawyerIdAndStatus(lawyerId, status, pageable)
                : deliveryReader.findAllByLawyerId(lawyerId, pageable);

        List<UUID> briefIds = deliveries.getContent().stream()
                .map(BriefDelivery::getBriefId).toList();
        Map<UUID, Brief> briefMap = briefReader.findAllByIds(briefIds).stream()
                .collect(Collectors.toMap(Brief::getId, Function.identity()));

        Page<InboxResponse> responsePage = deliveries.map(d -> {
            Brief brief = briefMap.get(d.getBriefId());
            return InboxResponse.of(d, brief);
        });

        return PageResponse.from(responsePage);
    }

    public InboxStatsResponse getInboxStats(UUID lawyerId) {
        long total = deliveryReader.countByLawyerId(lawyerId);
        long pending = deliveryReader.countByLawyerIdAndStatus(lawyerId, DeliveryStatus.DELIVERED);
        long confirmed = deliveryReader.countByLawyerIdAndStatus(lawyerId, DeliveryStatus.CONFIRMED);
        long rejected = deliveryReader.countByLawyerIdAndStatus(lawyerId, DeliveryStatus.REJECTED);
        return new InboxStatsResponse(total, pending, confirmed, rejected);
    }

    @Transactional
    public InboxDetailResponse getInboxDetail(UUID deliveryId, UUID lawyerId) {
        BriefDelivery delivery = deliveryReader.findById(deliveryId);

        if (!delivery.getLawyerId().equals(lawyerId)) {
            throw new BusinessException(ErrorCode.DELIVERY_NOT_FOUND) {};
        }

        // 열람 시 viewedAt 기록
        delivery.markViewed();

        Brief brief = briefReader.findById(delivery.getBriefId());
        User client = userReader.findById(brief.getUserId());

        // TODO: 전체공개/부분공개 설정에 따른 마스킹 정책 세분화 (API 명세 확정 후 구현)
        // 현재: PUBLIC = 이름+이메일 노출, PARTIAL = 이름 마스킹 + 이메일 미노출
        String clientName = brief.getPrivacySetting() == PrivacySetting.PUBLIC
                ? client.getName() : maskName(client.getName());
        String clientEmail = brief.getPrivacySetting() == PrivacySetting.PUBLIC
                ? client.getEmail() : null;

        return InboxDetailResponse.of(delivery, brief, clientName, clientEmail);
    }

    private String maskName(String name) {
        if (name == null || name.length() <= 1) return name;
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }

    private void validateOwner(Brief brief, UUID userId) {
        if (!brief.getUserId().equals(userId)) {
            throw new BriefNotFoundException(brief.getId());
        }
    }
}
