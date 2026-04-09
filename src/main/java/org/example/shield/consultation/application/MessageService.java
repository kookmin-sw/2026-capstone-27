package org.example.shield.consultation.application;

import lombok.RequiredArgsConstructor;
import org.example.shield.common.response.PageResponse;
import org.example.shield.consultation.controller.dto.MessageResponse;
import org.example.shield.consultation.controller.dto.SendMessageResponse;
import org.example.shield.consultation.domain.Consultation;
import org.example.shield.consultation.domain.ConsultationReader;
import org.example.shield.consultation.domain.Message;
import org.example.shield.consultation.domain.MessageReader;
import org.example.shield.consultation.domain.MessageWriter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {

    private final MessageReader messageReader;
    private final MessageWriter messageWriter;
    private final ConsultationReader consultationReader;

    @Transactional
    public SendMessageResponse sendMessage(UUID consultationId, String content) {
        Consultation consultation = consultationReader.findById(consultationId);

        // USER 메시지 저장
        Message userMessage = Message.createUserMessage(consultationId, content);
        Message saved = messageWriter.save(userMessage);

        // TODO: AI 연동 시 여기서 AiClient 호출 → 아래 목 응답 제거
        Message aiMessage = Message.createAiMessage(
                consultationId,
                "해당 내용을 확인했습니다. 추가로 관련 서류나 계약서가 있으시면 알려주세요.",
                "mock", null, null, null);
        Message savedAi = messageWriter.save(aiMessage);

        // 상담 마지막 메시지 + updatedAt 갱신
        consultation.updateLastMessage(savedAi.getContent(), savedAi.getCreatedAt());
        consultation.touch();

        return SendMessageResponse.from(savedAi, false);
    }

    public PageResponse<MessageResponse> getMessages(UUID consultationId, Pageable pageable) {
        // 상담 존재 확인
        consultationReader.findById(consultationId);

        Page<Message> messages = messageReader.findAllByConsultationId(consultationId, pageable);
        Page<MessageResponse> responsePage = messages.map(MessageResponse::from);

        return PageResponse.from(responsePage);
    }
}
