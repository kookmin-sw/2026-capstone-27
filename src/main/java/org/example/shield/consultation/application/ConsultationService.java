package org.example.shield.consultation.application;

import lombok.RequiredArgsConstructor;
import org.example.shield.brief.domain.BriefReader;
import org.example.shield.common.response.PageResponse;
import org.example.shield.consultation.controller.dto.ClassifyResponse;
import org.example.shield.consultation.controller.dto.ConsultationResponse;
import org.example.shield.consultation.controller.dto.CreateConsultationResponse;
import org.example.shield.consultation.domain.Consultation;
import org.example.shield.consultation.domain.ConsultationReader;
import org.example.shield.consultation.domain.ConsultationWriter;
import org.example.shield.consultation.domain.Message;
import org.example.shield.consultation.domain.MessageWriter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultationService {

    private final ConsultationReader consultationReader;
    private final ConsultationWriter consultationWriter;
    private final MessageWriter messageWriter;
    private final BriefReader briefReader;

    private static final String WELCOME_MESSAGE =
            "반갑습니다. SHIELD 법률 AI입니다. 어떤 법률 문제로 어려움을 겪고 계신가요? 구체적인 상황을 말씀해 주시면 정보 정리를 도와드리겠습니다.";

    @Transactional
    public CreateConsultationResponse createConsultation(UUID userId, List<String> domains,
                                                         List<String> subDomains, List<String> tags) {
        Consultation consultation = Consultation.create(userId, domains, subDomains, tags);
        Consultation saved = consultationWriter.save(consultation);

        Message welcomeMsg = Message.createAiMessage(
                saved.getId(), WELCOME_MESSAGE, null, null, null, null);
        Message savedMsg = messageWriter.save(welcomeMsg);

        saved.updateLastMessage(savedMsg.getContent(), savedMsg.getCreatedAt());

        return new CreateConsultationResponse(
                saved.getId(),
                saved.getStatus().name(),
                WELCOME_MESSAGE,
                saved.getCreatedAt()
        );
    }

    public ConsultationResponse getConsultation(UUID consultationId) {
        Consultation consultation = consultationReader.findById(consultationId);
        ConsultationResponse.BriefSummary briefSummary = briefReader
                .findOptionalByConsultationId(consultationId)
                .map(b -> new ConsultationResponse.BriefSummary(b.getId(), b.getTitle(), b.getStatus().name()))
                .orElse(null);
        return ConsultationResponse.from(consultation, briefSummary);
    }

    public PageResponse<ConsultationResponse> getMyConsultations(UUID userId, Pageable pageable) {
        Page<Consultation> consultations = consultationReader.findAllByUserId(userId, pageable);
        Page<ConsultationResponse> responsePage = consultations.map(c -> ConsultationResponse.from(c, null));
        return PageResponse.from(responsePage);
    }

    @Transactional
    public ClassifyResponse updateClassification(UUID consultationId, List<String> domains,
                                                  List<String> subDomains, List<String> tags) {
        Consultation consultation = consultationReader.findById(consultationId);
        consultation.updateUserClassification(domains, subDomains, tags);
        consultationWriter.save(consultation);

        return new ClassifyResponse(domains, subDomains, tags);
    }
}
