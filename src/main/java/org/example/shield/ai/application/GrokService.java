package org.example.shield.ai.application;

/**
 * Grok AI 서비스 - Grok API 호출 추상화.
 *
 * Layer: application
 * Called by: MessageService (챗봇), AnalysisService (분류+구조화)
 * Calls: GrokClient, GrokPromptFactory
 *
 * TODO:
 * - chatbotResponse(messages): 챗봇 대화 응답 생성
 *   → GrokPromptFactory.chatbotPrompt() + messages → GrokClient.chat()
 *
 * - classify(messages): AI Router로 법률 분야 분류
 *   → GrokPromptFactory.routerPrompt() + messages → GrokClient.chat()
 *   → 응답 JSON 파싱 → {primaryField, confidence, reasoning}
 *
 * - generateBrief(messages, legalField): MoE Expert로 줄글 의뢰서 생성
 *   → GrokPromptFactory.expertPrompt(legalField) + messages → GrokClient.chat()
 *   → 줄글 텍스트 + [키워드: ...] 파싱 → {content, keywords}
 */
public class GrokService {
}
