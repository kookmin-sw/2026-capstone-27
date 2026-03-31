package org.example.shield.ai.infrastructure;

/**
 * Grok API 프롬프트 팩토리.
 *
 * Layer: infrastructure
 * Called by: GrokService
 *
 * TODO:
 * - chatbotPrompt(): "너는 법률 상담 AI 챗봇이야. 사건 파악에 필요한 질문을 해."
 *
 * - routerPrompt(): "상담 내용을 민사/형사/노동/학교폭력 중 분류해. JSON으로 응답."
 *
 * - expertPrompt(legalField):
 *   - LABOR → "너는 노동법 전문가야. 부당해고/임금체불 중심 분석."
 *   - CIVIL → "너는 민사법 전문가야. 손해배상/계약분쟁 중심 분석."
 *   - CRIMINAL → "너는 형사법 전문가야. 범죄구성요건 중심 분석."
 *   - SCHOOL_VIOLENCE → "너는 학교폭력 전문가야. 학폭위 절차 중심 분석."
 *   + "줄글 형태의 의뢰서를 작성해. 마지막에 [키워드: ...] 추가."
 */
public class GrokPromptFactory {
}
