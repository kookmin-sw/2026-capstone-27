package org.example.shield.ai.infrastructure;

/**
 * Grok API HTTP 클라이언트.
 *
 * Layer: infrastructure
 * Called by: GrokService
 * Calls: Grok API (https://api.x.ai/v1/chat/completions)
 *
 * TODO:
 * - chat(systemPrompt, messages):
 *   1. system 프롬프트 + 대화 이력을 Grok API 형식으로 조합
 *   2. WebClient로 POST https://api.x.ai/v1/chat/completions 호출
 *   3. Authorization: Bearer {grok.api-key} 헤더 설정
 *   4. 응답에서 choices[0].message.content 추출하여 반환
 *   5. 30초 타임아웃 설정
 *   6. 실패 시 AnalysisFailedException 던지기
 *
 * - application.yml 설정:
 *   grok.base-url: https://api.x.ai/v1
 *   grok.api-key: ${GROK_API_KEY}
 *   grok.model: grok-3
 */
public class GrokClient {
}
