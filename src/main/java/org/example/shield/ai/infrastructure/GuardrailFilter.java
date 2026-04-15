package org.example.shield.ai.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.example.shield.ai.dto.BriefParsedResponse;
import org.example.shield.ai.dto.ChatParsedResponse;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Layer 2 응답 가드레일 필터.
 * 법적 가드레일: 금칙어 패턴 매칭 → 대체/제거.
 *
 * 절대 금지: 법률 해석, 판례 인용, 승패 예측, 변호사 추천, 법적 조언
 * 허용: 사실관계 정리, 절차 안내, 정보 구조화, 증거자료 안내
 */
@Component
@Slf4j
public class GuardrailFilter {

    /**
     * 금칙어 패턴 목록 (NFC 정규화 후 매칭).
     */
    private static final List<Pattern> FORBIDDEN_PATTERNS = List.of(
            // 법률 해석/조언
            Pattern.compile("법[적률]으?로\\s*.{0,20}(할 수 있|해야 합|가능합|않습)"),
            Pattern.compile("(법원|법관|판사).*?(판단|결정|판결).*?(할 것|예상)"),
            Pattern.compile("(소송|재판|고소).*?(이기|승소|패소|유리|불리).*?(가능성|확률|예상)"),

            // 판례 인용
            Pattern.compile("판례에\\s*(따르면|의하면|보면)"),
            Pattern.compile("(대법원|헌법재판소|고등법원)\\s*(판결|결정|판례)"),
            Pattern.compile("\\d{2,4}(다|도|누|가합|나)\\d+"),  // 판례 번호 패턴

            // 승패 예측
            Pattern.compile("승소\\s*(가능성|확률|할 수)"),
            Pattern.compile("이길\\s*(수|가능성|확률)"),

            // 변호사 추천
            Pattern.compile("(추천|소개).*?변호사"),
            Pattern.compile("변호사.*?(추천|소개|연결)")
    );

    /**
     * 금칙어 검출 시 대체 메시지.
     */
    private static final String FALLBACK_MESSAGE =
            "법률적 판단이나 해석은 변호사를 통해 확인하시기 바랍니다. " +
            "저는 사실관계 정리를 도와드리겠습니다. 추가로 말씀해 주실 내용이 있으신가요?";

    /**
     * Phase 1 대화 응답 가드레일.
     */
    public ChatParsedResponse filterChatResponse(ChatParsedResponse response) {
        if (response == null || response.getNextQuestion() == null) {
            return response;
        }

        String normalized = Normalizer.normalize(response.getNextQuestion(), Normalizer.Form.NFC);

        if (containsForbiddenPattern(normalized)) {
            log.warn("가드레일 금칙어 검출 — nextQuestion 대체. 원문: {}",
                    normalized.substring(0, Math.min(200, normalized.length())));
            response.setNextQuestion(FALLBACK_MESSAGE);
        }

        return response;
    }

    /**
     * Phase 2 의뢰서 응답 가드레일.
     * strategy와 keyIssues[].description은 법률 판단 삽입 위험이 높음 (P0-VI).
     */
    public BriefParsedResponse filterBriefResponse(BriefParsedResponse response) {
        if (response == null) {
            return response;
        }

        // strategy 필터링
        if (response.getStrategy() != null) {
            String normalized = Normalizer.normalize(response.getStrategy(), Normalizer.Form.NFC);
            if (containsForbiddenPattern(normalized)) {
                log.warn("의뢰서 strategy 금칙어 검출 — 제거");
                response.setStrategy("구체적인 법률 전략은 변호사와 상담 후 결정하시기 바랍니다.");
            }
        }

        // keyIssues[].description 필터링
        if (response.getKeyIssues() != null) {
            for (BriefParsedResponse.KeyIssue issue : response.getKeyIssues()) {
                if (issue.getDescription() != null) {
                    String normalized = Normalizer.normalize(issue.getDescription(), Normalizer.Form.NFC);
                    if (containsForbiddenPattern(normalized)) {
                        log.warn("의뢰서 keyIssue description 금칙어 검출 — 대체");
                        issue.setDescription("상세 법률 분석은 변호사 검토가 필요합니다.");
                    }
                }
            }
        }

        return response;
    }

    private boolean containsForbiddenPattern(String text) {
        for (Pattern p : FORBIDDEN_PATTERNS) {
            if (p.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }
}
