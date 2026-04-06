package org.example.shield.ai.infrastructure;

/**
 * Grok API HTTP 클라이언트.
 * AiClient 인터페이스 구현체.
 *
 * Layer: infrastructure
 * Called by: ClassifyService, AnalysisService, MessageService
 * Calls: Grok API (https://api.x.ai/v1/chat/completions)
 *
 * TODO: @Component + implements AiClient
 * - classify(content): 사건 유형 분류
 *   1. system 프롬프트 (분류 전문가) + content를 Grok API로 전송
 *   2. 응답에서 primaryField 추출
 *
 * - generateBrief(prompt): 의뢰서 생성
 *   1. system 프롬프트 (의뢰서 작성 전문가) + 대화 내역을 Grok API로 전송
 *   2. 응답에서 title, content, keywords 추출
 *
 * - 공통:
 *   WebClient로 POST https://api.x.ai/v1/chat/completions 호출
 *   Authorization: Bearer {XAI_API_KEY}
 *   30초 타임아웃
 *   실패 시 AnalysisFailedException
 *
 * - application.yml 설정:
 *   xai.base-url: https://api.x.ai/v1
 *   xai.api-key: ${XAI_API_KEY}
 *   xai.model: grok-3
 */
public class GrokClient {
}
