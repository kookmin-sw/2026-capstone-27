package org.example.shield.consultation.application;

/**
 * 폼 서비스 - 폼 템플릿 조회.
 *
 * Layer: application
 * Called by: ConsultationController.getForm()
 * Calls: ConsultationReader, ConsultationWriter, FormTemplateReader
 *
 * TODO:
 * - getForm(consultationId):
 *   1. consultation.primaryField로 해당 유형 폼 템플릿 조회
 *   2. 첫 번째 CHATBOT 질문을 chat_messages에 AI 메시지로 저장
 *   3. 폼 목록 반환
 */
public class FormService {
}
