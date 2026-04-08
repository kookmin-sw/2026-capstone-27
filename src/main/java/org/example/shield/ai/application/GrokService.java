package org.example.shield.ai.application;

/**
 * Grok AI 서비스 - AiClient를 활용한 AI 기능.
 *
 * Layer: application
 * Called by: MessageService, AnalysisService
 * Calls: AiClient (GrokClient 구현체)
 *
 * TODO:
 * - chat(domain, chatHistory): 대화 API
 *   → AiClient에 { domain, chatHistory } 전송
 *   → { nextQuestion, primaryField, tags, allCompleted } 반환
 *
 * - generateBrief(domain, chatHistory): 의뢰서 생성 API
 *   → AiClient에 { domain, chatHistory } 전송
 *   → { title, content, keyIssues, keywords, strategy } 반환
 */
public class GrokService {
}
