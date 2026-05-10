package org.example.shield.lawyer.infrastructure;

import lombok.RequiredArgsConstructor;
import org.example.shield.lawyer.domain.LawyerProfile;
import org.example.shield.lawyer.domain.LawyerWriter;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class LawyerWriterImpl implements LawyerWriter {

    private final LawyerProfileRepository lawyerProfileRepository;

    @Override
    public LawyerProfile save(LawyerProfile profile) {
        return lawyerProfileRepository.save(profile);
    }
}
