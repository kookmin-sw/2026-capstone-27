package org.example.shield.ai.dto;

/**
 * Grok API 요청 DTO.
 *
 * TODO:
 * - model: String ("grok-3")
 * - messages: List<Message>
 *   - role: "system" → AI 팀이 제공한 프롬프트
 *   - role: "user" → 백엔드가 domain + chatHistory를 텍스트로 조립
 *
 * messages 테이블의 USER/AI와 Grok API의 role은 다름.
 * messages 테이블의 대화 내역을 하나의 텍스트로 조립해서
 * Grok API의 "user" role에 넣어서 전송.
 */
public class GrokRequest {
}
