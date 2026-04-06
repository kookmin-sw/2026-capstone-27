package org.example.shield.ai.application;

/**
 * Grok AI 서비스 - AiClient를 활용한 AI 기능.
 *
 * Layer: application
 * Called by: ClassifyService, AnalysisService, MessageService
 * Calls: AiClient (GrokClient 구현체)
 *
 * TODO:
 * - classify(content): 사건 유형 분류
 *   → AiClient.classify(content)
 *   → primaryField 반환 (DEPOSIT_FRAUD / LEASE_DISPUTE / ...)
 *
 * - generateBrief(chatMessages): 의뢰서 생성
 *   → chat_messages를 프롬프트로 조립
 *   → AiClient.generateBrief(prompt)
 *   → title, content, keywords 반환
 *
 * - generateNextQuestion(chatMessages, formFields): 다음 챗봇 질문 생성
 *   → 수집 안 된 필드 확인
 *   → 해당 필드의 label을 기반으로 질문 생성
 */
public class GrokService {
}
