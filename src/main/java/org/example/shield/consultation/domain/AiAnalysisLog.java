package org.example.shield.consultation.domain;

/**
 * AI 분석 로그 엔티티 - ai_analysis_logs 테이블 매핑.
 *
 * TODO: @Entity 구현
 * - id: UUID (PK)
 * - consultationId: UUID (FK -> consultations.id)
 * - analysisType: String ("classification" / "brief_generation")
 * - modelUsed: String ("command-a-03-2025" 등 Cohere 모델명)
 * - prompt: String (Cohere에 보낸 프롬프트 원본)
 * - rawResponse: String (Cohere 응답 원본)
 * - processingTimeMs: long
 * - createdAt: LocalDateTime
 */
public class AiAnalysisLog {
}
