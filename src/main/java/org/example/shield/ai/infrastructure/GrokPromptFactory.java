package org.example.shield.ai.infrastructure;

/**
 * Grok API 프롬프트 팩토리.
 *
 * Layer: infrastructure
 * Called by: GrokClient
 *
 * TODO:
 * - classifyPrompt():
 *   "당신은 부동산 법률 분류 전문가입니다.
 *    사용자의 입력을 읽고 사건 유형을 판단해주세요.
 *    분류: DEPOSIT_FRAUD / LEASE_DISPUTE / PRESALE / PROPERTY_TRADE / OTHER"
 *
 * - briefPrompt(legalField):
 *   - DEPOSIT_FRAUD → "당신은 전세사기 전문가입니다. 보증금 반환 중심으로 의뢰서를 작성해주세요."
 *   - LEASE_DISPUTE → "당신은 임대차 분쟁 전문가입니다. 임대차 계약 중심으로 의뢰서를 작성해주세요."
 *   - PRESALE → "당신은 분양 계약 전문가입니다. 분양 관련 의뢰서를 작성해주세요."
 *   - PROPERTY_TRADE → "당신은 매매/등기 전문가입니다. 부동산 매매 중심으로 의뢰서를 작성해주세요."
 *   + "줄글 형태의 의뢰서를 작성해주세요."
 *
 * - chatbotPrompt():
 *   "당신은 부동산 법률 상담 챗봇입니다. 사건 파악에 필요한 질문을 해주세요."
 */
public class GrokPromptFactory {
}
