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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
    public boolean existsByBriefIdAndStatus(UUID briefId, DeliveryStatus status) {
        return briefDeliveryRepository.existsByBriefIdAndStatus(briefId, status);
    }

    @Override
    public List<BriefDelivery> findAllByBriefIdInAndStatus(List<UUID> briefIds, DeliveryStatus status) {
        if (briefIds == null || briefIds.isEmpty()) return List.of();
        return briefDeliveryRepository.findAllByBriefIdInAndStatus(briefIds, status);
    }

    @Override
    public Map<DeliveryStatus, Long> countGroupByStatus(UUID lawyerId) {
        return briefDeliveryRepository.countGroupByStatus(lawyerId).stream()
                .collect(Collectors.toMap(
                        row -> (DeliveryStatus) row[0],
                        row -> (Long) row[1]
                ));
    }
}
