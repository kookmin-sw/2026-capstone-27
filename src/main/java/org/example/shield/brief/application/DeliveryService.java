package org.example.shield.brief.application;

import lombok.RequiredArgsConstructor;
import org.example.shield.brief.controller.dto.DeliveryListResponse;
import org.example.shield.brief.controller.dto.DeliveryResponse;
import org.example.shield.brief.domain.Brief;
import org.example.shield.brief.domain.BriefDelivery;
import org.example.shield.brief.domain.BriefReader;
import org.example.shield.brief.exception.BriefNotFoundException;
import org.example.shield.brief.infrastructure.BriefDeliveryRepository;
import org.example.shield.common.enums.BriefStatus;
import org.example.shield.common.exception.BusinessException;
import org.example.shield.common.exception.ErrorCode;
import org.example.shield.user.domain.User;
import org.example.shield.user.domain.UserReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryService {

    private final BriefReader briefReader;
    private final BriefDeliveryRepository deliveryRepository;
    private final UserReader userReader;

    @Transactional
    public DeliveryResponse createDelivery(UUID briefId, UUID lawyerId, UUID userId) {
        Brief brief = briefReader.findById(briefId);
        validateOwner(brief, userId);

        if (brief.getStatus() != BriefStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.BRIEF_NOT_CONFIRMED) {};
        }

        // 중복 전달 방지
        boolean alreadySent = deliveryRepository.findAllByBriefId(briefId).stream()
                .anyMatch(d -> d.getLawyerId().equals(lawyerId));
        if (alreadySent) {
            throw new BusinessException(ErrorCode.DELIVERY_ALREADY_EXISTS) {};
        }

        BriefDelivery delivery = BriefDelivery.create(briefId, lawyerId);
        BriefDelivery saved = deliveryRepository.save(delivery);

        // 의뢰서 상태를 DELIVERED로 변경
        brief.markDelivered();

        User lawyer = userReader.findById(lawyerId);
        return DeliveryResponse.of(saved, lawyer.getName());
    }

    public DeliveryListResponse getDeliveries(UUID briefId, UUID userId) {
        Brief brief = briefReader.findById(briefId);
        validateOwner(brief, userId);

        List<BriefDelivery> deliveries = deliveryRepository.findAllByBriefId(briefId);

        List<DeliveryResponse> responses = deliveries.stream()
                .map(d -> {
                    User lawyer = userReader.findById(d.getLawyerId());
                    return DeliveryResponse.of(d, lawyer.getName());
                })
                .toList();

        return new DeliveryListResponse(responses);
    }

    private void validateOwner(Brief brief, UUID userId) {
        if (!brief.getUserId().equals(userId)) {
            throw new BriefNotFoundException(brief.getId());
        }
    }
}
