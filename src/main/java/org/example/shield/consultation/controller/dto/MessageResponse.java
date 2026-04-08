package org.example.shield.consultation.controller.dto;

/**
 * 메시지 전송 응답 DTO.
 *
 * TODO: record로 구현
 * - content: String (AI 답변)
 * - timestamp: LocalDateTime
 * - allCompleted: boolean (전체 대화 완료 여부)
 * - classification: ClassificationDto (allCompleted일 때만, 아니면 null)
 *   - primaryField: List<String> (AI 분류 결과)
 *   - tags: List<String> (AI 분류 태그)
 */
public class MessageResponse {
}
