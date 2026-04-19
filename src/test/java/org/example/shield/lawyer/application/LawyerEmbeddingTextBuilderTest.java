package org.example.shield.lawyer.application;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LawyerEmbeddingTextBuilderTest {

    private final LawyerEmbeddingTextBuilder builder = new LawyerEmbeddingTextBuilder();

    @Test
    void 모든_섹션이_있으면_템플릿대로_조립한다() {
        String text = builder.build(
                List.of("형사", "민사"),
                List.of("사기", "횡령"),
                List.of("변호", "항소"),
                "10년 경력의 형사 전문 변호사");

        assertThat(text).contains("[전문 분야]");
        assertThat(text).contains("[세부 분야]");
        assertThat(text).contains("[태그]");
        assertThat(text).contains("[자기소개]");
        assertThat(text).contains("10년 경력의 형사 전문 변호사");
    }

    @Test
    void domains는_3회_반복된다() {
        String text = builder.build(
                List.of("형사"),
                List.of(),
                List.of(),
                null);

        // "형사" 가 3번 나타나야 함
        int count = text.split("형사", -1).length - 1;
        assertThat(count).isEqualTo(3);
    }

    @Test
    void subDomains는_2회_반복된다() {
        String text = builder.build(
                List.of(),
                List.of("사기"),
                List.of(),
                null);

        int count = text.split("사기", -1).length - 1;
        assertThat(count).isEqualTo(2);
    }

    @Test
    void tags는_1회만_나타난다() {
        String text = builder.build(
                List.of(),
                List.of(),
                List.of("변호"),
                null);

        int count = text.split("변호", -1).length - 1;
        assertThat(count).isEqualTo(1);
    }

    @Test
    void 빈_섹션은_건너뛴다() {
        String text = builder.build(
                null,
                List.of(),
                List.of("변호"),
                null);

        assertThat(text).doesNotContain("[전문 분야]");
        assertThat(text).doesNotContain("[세부 분야]");
        assertThat(text).doesNotContain("[자기소개]");
        assertThat(text).contains("[태그]");
    }

    @Test
    void 모두_비면_빈_문자열() {
        String text = builder.build(null, null, null, null);
        assertThat(text).isEmpty();
    }

    @Test
    void bio_만_있어도_섹션이_조립된다() {
        String text = builder.build(null, null, null, "자기소개 본문");
        assertThat(text).isEqualTo("[자기소개]\n자기소개 본문");
    }

    @Test
    void blank_요소는_필터링된다() {
        String text = builder.build(
                List.of("형사", "  ", ""),
                List.of(),
                List.of(),
                null);

        // 빈/공백 요소가 걸러져서 "형사" 만 남고 3회 반복
        int count = text.split("형사", -1).length - 1;
        assertThat(count).isEqualTo(3);
    }

    @Test
    void 문서와_쿼리_동일_입력에_대해_동일_출력을_보장한다() {
        List<String> domains = List.of("형사");
        List<String> subDomains = List.of("사기");
        List<String> tags = List.of("변호");
        String bio = "소개";

        String docText = builder.build(domains, subDomains, tags, bio);
        String queryText = builder.build(domains, subDomains, tags, bio);

        assertThat(docText).isEqualTo(queryText);
    }
}
