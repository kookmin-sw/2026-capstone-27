package org.example.shield.brief.infrastructure;

import org.example.shield.brief.domain.BriefDelivery;
import org.example.shield.common.enums.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BriefDeliveryRepository extends JpaRepository<BriefDelivery, UUID> {
    List<BriefDelivery> findAllByBriefId(UUID briefId);
    Page<BriefDelivery> findAllByLawyerId(UUID lawyerId, Pageable pageable);
    Page<BriefDelivery> findAllByLawyerIdAndStatus(UUID lawyerId, DeliveryStatus status, Pageable pageable);
    boolean existsByBriefIdAndLawyerId(UUID briefId, UUID lawyerId);
    long countByLawyerId(UUID lawyerId);
    long countByLawyerIdAndStatus(UUID lawyerId, DeliveryStatus status);
}
