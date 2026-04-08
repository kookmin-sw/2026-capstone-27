package org.example.shield.consultation.domain;

/**
 * Message 조회 인터페이스.
 *
 * TODO:
 * - findAllByConsultationId(UUID consultationId): List<Message> (sequence 순)
 * - findLastByConsultationId(UUID consultationId): Optional<Message>
 * - getMaxSequence(UUID consultationId): int
 */
public interface MessageReader {
}
