package org.example.shield.consultation.application;

import java.util.List;

/**
 * {@link ChatTransactionalBoundary#finalizeAiResponse} 에 전달할 AI 응답 처리
 * 결과 페이로드.
 *
 * <p>{@link MessageService} 가 Cohere 호출 후 트랜잭션 밖에서 준비하고,
 * 최종 DB 반영 시 boundary 에 넘긴다. 분류 값은 온톨로지 필터링이 끝난
 * 유효 값만 담아야 한다 (boundary 는 더 이상 검증하지 않음).</p>
 *
 * @param responseId    Cohere 응답 ID (감사 로깅)
 * @param nextQuestion  다음 질문 (blank 검증 완료된 값)
 * @param model         사용 모델명
 * @param tokensInput   입력 토큰 수
 * @param tokensOutput  출력 토큰 수
 * @param latencyMs     Cohere 호출 지연(ms)
 * @param aiDomains     온톨로지 검증 완료된 L1 (nullable)
 * @param aiSubDomains  온톨로지 검증 완료된 L2 (nullable)
 * @param aiTags        온톨로지 검증 완료된 L3 (nullable)
 */
public record AiFinalizePayload(
        String responseId,
        String nextQuestion,
        String model,
        Integer tokensInput,
        Integer tokensOutput,
        Integer latencyMs,
        List<String> aiDomains,
        List<String> aiSubDomains,
        List<String> aiTags
) {
    public boolean hasAnyClassification() {
        return isNonEmpty(aiDomains) || isNonEmpty(aiSubDomains) || isNonEmpty(aiTags);
    }

    private static boolean isNonEmpty(List<String> list) {
        return list != null && !list.isEmpty();
    }
}
