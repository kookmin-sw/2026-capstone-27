package org.example.shield.ai.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ChecklistTokenizerTest {

    @Test
    @DisplayName("괄호/쉼표/공백 구분자 split")
    void splitByPunctuation() {
        Set<String> tokens = ChecklistTokenizer.tokensOf("보증금 금액 (피해액, 거래가, 보증금 등)");
        assertThat(tokens).contains("보증금", "금액", "피해액", "거래가");
        assertThat(tokens).doesNotContain("등"); // 불용어
    }

    @Test
    @DisplayName("슬래시/중간점 구분자 split")
    void splitBySlashAndMiddleDot() {
        Set<String> tokens = ChecklistTokenizer.tokensOf("부동산 종류 (아파트/주택/토지/상가/오피스텔)");
        assertThat(tokens).contains("아파트", "주택", "토지", "상가", "오피스텔", "부동산", "종류");
    }

    @Test
    @DisplayName("한글 1글자 토큰 제외")
    void filterShortHangul() {
        Set<String> tokens = ChecklistTokenizer.tokensOf("가 나 다 보증금");
        assertThat(tokens).containsExactly("보증금");
    }

    @Test
    @DisplayName("불용어 제외 — 및/등/또는/관련/여부")
    void stopwordsFiltered() {
        Set<String> tokens = ChecklistTokenizer.tokensOf("계약서 및 증거 자료 존재 여부");
        assertThat(tokens).contains("계약서", "증거", "자료");
        assertThat(tokens).doesNotContain("및", "여부", "존재");
    }

    @Test
    @DisplayName("null/빈 문자열 → 빈 집합")
    void nullOrBlankReturnsEmpty() {
        assertThat(ChecklistTokenizer.tokensOf(null)).isEmpty();
        assertThat(ChecklistTokenizer.tokensOf("")).isEmpty();
        assertThat(ChecklistTokenizer.tokensOf("   ")).isEmpty();
    }

    @Test
    @DisplayName("anyTokenMatches — 토큰 하나라도 포함되면 true")
    void anyTokenMatchesBasic() {
        Set<String> tokens = Set.of("보증금", "계약서");
        assertThat(ChecklistTokenizer.anyTokenMatches(tokens, "보증금 관련 질문입니다")).isTrue();
        assertThat(ChecklistTokenizer.anyTokenMatches(tokens, "전혀 관계없는 문장")).isFalse();
        assertThat(ChecklistTokenizer.anyTokenMatches(Set.of(), "아무 텍스트")).isFalse();
        assertThat(ChecklistTokenizer.anyTokenMatches(tokens, "")).isFalse();
    }

    @Test
    @DisplayName("normalizeForMatch — NFC + 소문자화")
    void normalizeBasic() {
        String out = ChecklistTokenizer.normalizeForMatch("Hello WORLD 한글");
        assertThat(out).isEqualTo("hello world 한글");
        assertThat(ChecklistTokenizer.normalizeForMatch(null)).isEmpty();
    }
}
