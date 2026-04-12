package org.example.shield.brief.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface BriefDeliveryReader {
    BriefDelivery findById(UUID id);
    List<BriefDelivery> findAllByBriefId(UUID briefId);
    Page<BriefDelivery> findAllByLawyerId(UUID lawyerId, Pageable pageable);
    boolean existsByBriefIdAndLawyerId(UUID briefId, UUID lawyerId);
}
