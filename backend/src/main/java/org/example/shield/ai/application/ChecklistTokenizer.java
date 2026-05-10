package org.example.shield.ai.application;

import java.text.Normalizer;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 체크리스트 항목 문자열을 검색용 토큰 집합으로 변환하는 유틸 (Issue #40 A 후속).
 *
 * <p>예: "보증금 금액 (피해액, 거래가, 보증금 등)" → {"보증금", "금액", "피해액", "거래가"}</p>
 *
 * <p>규칙:
 * <ul>
 *   <li>NFC 정규화 후 소문자화</li>
 *   <li>공백/괄호/쉼표/슬래시/중간점·하이픈 등 구분자로 split</li>
 *   <li>한글/영문/숫자 외 문자는 토큰에서 제거</li>
 *   <li>한글 2글자 이상 / 영문 3글자 이상만 유효 토큰</li>
 *   <li>불용어 제외 ("및", "등", "또는", "관련", "여부", "경우", "기타")</li>
 * </ul>
 * </p>
 */
public final class ChecklistTokenizer {

    /** 한글/영문/숫자 외 모든 문자를 구분자로 취급. */
    private static final String SPLIT_REGEX = "[^\\p{IsHangul}A-Za-z0-9]+";

    /** 토큰에서 그대로 제외할 단어 (일반 한국어 기능어). */
    private static final Set<String> STOPWORDS = Set.of(
            "및", "등", "또는", "관련", "여부", "경우", "기타", "존재", "내용",
            "정보", "이력", "사항", "위한", "위해"
    );

    private ChecklistTokenizer() {
    }

    /**
     * 한 체크리스트 항목(문장)에서 토큰 집합 추출.
     *
     * @param item YAML 의 항목 원문
     * @return 소문자/정규화된 토큰 집합. 항목이 비어있거나 유효 토큰이 없으면 빈 집합.
     */
    public static Set<String> tokensOf(String item) {
        if (item == null || item.isBlank()) {
            return Collections.emptySet();
        }
        String normalized = Normalizer.normalize(item, Normalizer.Form.NFC).toLowerCase();
        String[] raw = normalized.split(SPLIT_REGEX);
        Set<String> tokens = new LinkedHashSet<>();
        for (String t : raw) {
            if (isValidToken(t)) {
                tokens.add(t);
            }
        }
        return tokens;
    }

    private static boolean isValidToken(String token) {
        if (token == null || token.isEmpty()) return false;
        if (STOPWORDS.contains(token)) return false;
        boolean hasHangul = token.chars().anyMatch(c -> Character.UnicodeBlock.of(c)
                == Character.UnicodeBlock.HANGUL_SYLLABLES);
        if (hasHangul) {
            // 한글 포함 토큰은 2글자 이상
            return token.length() >= 2;
        }
        // 영문/숫자 토큰은 3글자 이상 (a, an, of 같은 짧은 영어 제외)
        return token.length() >= 3;
    }

    /**
     * user 텍스트 하나를 커버리지 매칭용 검색 문자열로 정규화.
     * NFC + 소문자화.
     */
    public static String normalizeForMatch(String text) {
        if (text == null) return "";
        return Normalizer.normalize(text, Normalizer.Form.NFC).toLowerCase();
    }

    /**
     * 주어진 토큰 집합 중 하나라도 haystack 에 포함되면 true.
     * 빈 토큰 집합은 무조건 false (해당 항목은 매칭 불가로 간주하지 않고 커버리지 분모에서 제외하는 정책은 서비스 측에서 판단).
     */
    public static boolean anyTokenMatches(Set<String> tokens, String haystack) {
        if (tokens == null || tokens.isEmpty() || haystack == null || haystack.isEmpty()) {
            return false;
        }
        for (String t : tokens) {
            if (haystack.contains(t)) return true;
        }
        return false;
    }

}
