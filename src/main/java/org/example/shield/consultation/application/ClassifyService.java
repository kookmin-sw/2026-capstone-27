package org.example.shield.consultation.application;

/**
 * 분류 서비스 - AI Router로 사건 유형 분류.
 *
 * Layer: application
 * Called by: ConsultationController.classify()
 * Calls: ConsultationReader, ConsultationWriter, AiClient
 *
 * TODO:
 * - classify(consultationId, content):
 *   1. content를 chat_messages에 USER 메시지로 저장
 *   2. AiClient.classify(content) → 사건 유형 분류
 *   3. consultations.primary_field에 분류 결과 저장
 *   4. status: CLASSIFYING → IN_PROGRESS
 */
public class ClassifyService {
}
