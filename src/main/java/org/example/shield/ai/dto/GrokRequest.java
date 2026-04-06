package org.example.shield.ai.dto;

/**
 * Grok API 요청 DTO.
 *
 * TODO:
 * - model: String ("grok-3")
 * - messages: List<Message>
 *   - role: "system" → AI 역할 정의
 *   - role: "user" → 프롬프트 (대화 내역 + 지침)
 *
 * 우리 chat_messages의 USER/AI와는 다름.
 * chat_messages의 대화 내역을 하나의 텍스트로 조립해서
 * Grok API의 "user" role에 넣어서 전송.
 */
public class GrokRequest {
}
