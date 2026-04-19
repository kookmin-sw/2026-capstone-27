package org.example.shield.ai.application;

import org.example.shield.ai.dto.LegalChunk;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RagContextBuilderTest {

    private final RagContextBuilder builder = new RagContextBuilder();

    @Test
    @DisplayName("빈 chunks — 빈 문자열 반환")
    void build_emptyChunks_returnsEmpty() {
        String result = builder.build(List.of(), "테스트 요약");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("null chunks — 빈 문자열 반환")
    void build_nullChunks_returnsEmpty() {
        String result = builder.build((List<LegalChunk>) null, "테스트 요약");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("단일 chunk — 포맷 검증")
    void build_singleChunk_formattedCorrectly() {
        LegalChunk chunk = new LegalChunk(
                "주택임대차보호법", "제3조의2", "보증금의 회수",
                "임차인은 보증금을 우선하여 변제받을 권리가 있다.",
                "2023-07-19",
                "https://law.go.kr/LSI249999/3-2",
                0.95
        );

        String result = builder.build(List.of(chunk), "전세보증금 반환 청구");

        assertThat(result).contains("## 참고 법령");
        assertThat(result).contains("분류: 전세보증금 반환 청구");
        assertThat(result).contains("[1] 주택임대차보호법 제3조의2 (보증금의 회수)");
        assertThat(result).contains("시행일: 2023-07-19");
        assertThat(result).contains("출처: https://law.go.kr/LSI249999/3-2");
        assertThat(result).contains("임차인은 보증금을 우선하여 변제받을 권리가 있다.");
    }

    @Test
    @DisplayName("다수 chunks — 번호 순서 검증")
    void build_multipleChunks_numberedSequentially() {
        List<LegalChunk> chunks = List.of(
                new LegalChunk("민법", "제750조", "불법행위의 내용",
                        "고의 또는 과실로 인한 위법행위로 타인에게 손해를 가한 자는...",
                        "2024-01-01", "", 0.9),
                new LegalChunk("민법", "제751조", "재산 이외의 손해의 배상",
                        "타인의 신체, 자유 또는 명예를 해하거나...",
                        "2024-01-01", "", 0.85)
        );

        String result = builder.build(chunks, "손해배상 청구");

        assertThat(result).contains("[1] 민법 제750조");
        assertThat(result).contains("[2] 민법 제751조");
    }

    @Test
    @DisplayName("sourceUrl 없는 chunk — 출처 미표시")
    void build_noSourceUrl_omitsSource() {
        LegalChunk chunk = new LegalChunk(
                "민법", "제834조", "협의상 이혼",
                "부부는 협의에 의하여 이혼할 수 있다.",
                "2024-01-01", null, 0.88
        );

        String result = builder.build(List.of(chunk), "이혼 절차");

        assertThat(result).doesNotContain("출처:");
    }
}
