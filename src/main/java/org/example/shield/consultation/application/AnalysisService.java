package org.example.shield.consultation.application;

/**
 * 상담 분석 서비스 - AI Router(분류) + MoE Expert(구조화) → 의뢰서 생성.
 *
 * Layer: application
 * Called by: ConsultationController.analyze()
 * Calls: ChatSessionRepository, GrokService, ClassificationRepository,
 *        CaseStructureRepository, BriefRepository, AiAnalysisLogRepository
 *
 * TODO:
 * - analyze(consultationId):
 *   1. MongoDB에서 채팅 메시지 전체 조회
 *   2. GrokService.classify(messages) → AI Router로 법률 분야 분류
 *   3. 분류 결과를 classifications 테이블에 저장
 *   4. GrokService.generateBrief(messages, primaryField) → MoE Expert로 줄글 의뢰서 생성
 *   5. 구조화 결과를 case_structures 테이블에 저장
 *   6. briefs 테이블에 의뢰서 생성 (content=줄글, keywords=매칭용, status=DRAFT)
 *   7. Grok 호출 원본을 ai_analysis_logs에 저장 (디버깅용)
 *   8. AnalyzeResponse (classification + brief) 반환
 *
 *   이 하나의 메서드 안에서 Grok API가 2번 호출됨 (Router 1번 + Expert 1번)
 */
public class AnalysisService {
}
