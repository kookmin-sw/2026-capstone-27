package org.example.shield.brief.infrastructure;

import lombok.RequiredArgsConstructor;
import org.example.shield.brief.domain.Brief;
import org.example.shield.brief.domain.BriefWriter;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BriefWriterImpl implements BriefWriter {

    private final BriefRepository briefRepository;

    @Override
    public Brief save(Brief brief) {
        return briefRepository.save(brief);
    }
}
