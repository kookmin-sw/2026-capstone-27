package org.example.shield.chat.domain;

/**
 * AI 분석 로그 MongoDB Document.
 *
 * TODO: @Document("ai_analysis_logs") 구현
 * - _id: String (MongoDB ObjectId)
 * - consultationId: String
 * - analysisType: String ("classification" / "structuring")
 * - modelUsed: String ("grok-3")
 * - prompt: String (Grok에 보낸 프롬프트 원본)
 * - rawResponse: String (Grok 응답 원본)
 * - processingTimeMs: long
 * - createdAt: LocalDateTime
 */
public class AiAnalysisLog {
}
