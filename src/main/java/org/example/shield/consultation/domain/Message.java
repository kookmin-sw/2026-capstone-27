package org.example.shield.consultation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.shield.common.enums.MessageRole;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "messages")
@EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID consultationId;

    @Column(nullable = false, insertable = false, updatable = false)
    private Integer sequence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "message_role")
    private MessageRole role;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    private String model;

    private Integer tokensInput;

    private Integer tokensOutput;

    private Integer latencyMs;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Message(UUID consultationId, MessageRole role, String content,
                    String model, Integer tokensInput, Integer tokensOutput,
                    Integer latencyMs) {
        this.consultationId = consultationId;
        this.role = role;
        this.content = content;
        this.model = model;
        this.tokensInput = tokensInput;
        this.tokensOutput = tokensOutput;
        this.latencyMs = latencyMs;
    }

    public static Message createUserMessage(UUID consultationId, String content) {
        return Message.builder()
                .consultationId(consultationId)
                .role(MessageRole.USER)
                .content(content)
                .build();
    }

    public static Message createAiMessage(UUID consultationId, String content,
                                          String model, Integer tokensInput,
                                          Integer tokensOutput, Integer latencyMs) {
        return Message.builder()
                .consultationId(consultationId)
                .role(MessageRole.CHATBOT)
                .content(content)
                .model(model)
                .tokensInput(tokensInput)
                .tokensOutput(tokensOutput)
                .latencyMs(latencyMs)
                .build();
    }
}
