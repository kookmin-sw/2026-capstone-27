package org.example.shield.brief.infrastructure;

import lombok.RequiredArgsConstructor;
import org.example.shield.brief.domain.Brief;
import org.example.shield.brief.domain.BriefReader;
import org.example.shield.brief.exception.BriefNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class BriefReaderImpl implements BriefReader {

    private final BriefRepository briefRepository;

    @Override
    public Brief findById(UUID id) {
        return briefRepository.findById(id)
                .orElseThrow(() -> new BriefNotFoundException(id));
    }

    @Override
    public Page<Brief> findAllByUserId(UUID userId, Pageable pageable) {
        return briefRepository.findAllByUserId(userId, pageable);
    }

    @Override
    public Optional<Brief> findOptionalByConsultationId(UUID consultationId) {
        return briefRepository.findByConsultationId(consultationId);
    }
}
