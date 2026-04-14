package org.example.shield.brief.infrastructure;

import lombok.RequiredArgsConstructor;
import org.example.shield.brief.domain.BriefDelivery;
import org.example.shield.brief.domain.BriefDeliveryReader;
import org.example.shield.common.enums.DeliveryStatus;
import org.example.shield.common.exception.BusinessException;
import org.example.shield.common.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class BriefDeliveryReaderImpl implements BriefDeliveryReader {

    private final BriefDeliveryRepository briefDeliveryRepository;

    @Override
    public BriefDelivery findById(UUID id) {
        return briefDeliveryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOT_FOUND) {});
    }

    @Override
    public List<BriefDelivery> findAllByBriefId(UUID briefId) {
        return briefDeliveryRepository.findAllByBriefId(briefId);
    }

    @Override
    public Page<BriefDelivery> findAllByLawyerId(UUID lawyerId, Pageable pageable) {
        return briefDeliveryRepository.findAllByLawyerId(lawyerId, pageable);
    }

    @Override
    public Page<BriefDelivery> findAllByLawyerIdAndStatus(UUID lawyerId, DeliveryStatus status, Pageable pageable) {
        return briefDeliveryRepository.findAllByLawyerIdAndStatus(lawyerId, status, pageable);
    }

    @Override
    public boolean existsByBriefIdAndLawyerId(UUID briefId, UUID lawyerId) {
        return briefDeliveryRepository.existsByBriefIdAndLawyerId(briefId, lawyerId);
    }

    @Override
    public long countByLawyerId(UUID lawyerId) {
        return briefDeliveryRepository.countByLawyerId(lawyerId);
    }

    @Override
    public long countByLawyerIdAndStatus(UUID lawyerId, DeliveryStatus status) {
        return briefDeliveryRepository.countByLawyerIdAndStatus(lawyerId, status);
    }
}
