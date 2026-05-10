package org.example.shield.brief.infrastructure;

import lombok.RequiredArgsConstructor;
import org.example.shield.brief.domain.BriefDelivery;
import org.example.shield.brief.domain.BriefDeliveryWriter;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BriefDeliveryWriterImpl implements BriefDeliveryWriter {

    private final BriefDeliveryRepository briefDeliveryRepository;

    @Override
    public BriefDelivery save(BriefDelivery delivery) {
        return briefDeliveryRepository.save(delivery);
    }
}
