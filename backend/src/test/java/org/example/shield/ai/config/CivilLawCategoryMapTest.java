package org.example.shield.ai.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CivilLawCategoryMap} 검증.
 *
 * <p>실제 리소스 파일을 읽어서 book/chapter/section/group 태그가
 * 조문 번호에 맞게 정상 부여되는지 확인한다.</p>
 */
class CivilLawCategoryMapTest {

    private CivilLawCategoryMap map;

    @BeforeEach
    void setUp() {
        map = new CivilLawCategoryMap();
        map.load();
    }

    @Test
    @DisplayName("로드 결과: books=5, chapters>=30, mandatoryGroups>=5")
    void loadSucceeds() {
        assertThat(map.getBooks()).hasSize(5);
        assertThat(map.getChapters()).hasSizeGreaterThanOrEqualTo(30);
        assertThat(map.getSections()).hasSizeGreaterThan(0);
        assertThat(map.getMandatoryGroups()).hasSizeGreaterThanOrEqualTo(5);
    }

    @Test
    @DisplayName("제1조: 총칙 + 통칙 태그")
    void resolveArticle1() {
        List<String> tags = map.resolveCategoryIds(1);
        assertThat(tags).anyMatch(t -> t.startsWith("book:제1편 총칙"));
        assertThat(tags).anyMatch(t -> t.startsWith("chapter:제1장 통칙"));
    }

    @Test
    @DisplayName("제303조: 물권 + 전세권 + group:jeonse 태그")
    void resolveJeonseArticle() {
        List<String> tags = map.resolveCategoryIds(303);
        assertThat(tags).anyMatch(t -> t.startsWith("book:제2편 물권"));
        assertThat(tags).anyMatch(t -> t.startsWith("chapter:제6장 전세권"));
        assertThat(tags).contains("group:jeonse");
    }

    @Test
    @DisplayName("제618조: 채권 + 계약 + 임대차 + group:leasing")
    void resolveLeasingArticle() {
        List<String> tags = map.resolveCategoryIds(618);
        assertThat(tags).anyMatch(t -> t.startsWith("book:제3편 채권"));
        assertThat(tags).anyMatch(t -> t.startsWith("chapter:제2장 계약"));
        assertThat(tags).anyMatch(t -> t.startsWith("section:제7절 임대차"));
        assertThat(tags).contains("group:leasing");
    }

    @Test
    @DisplayName("제1118조: 상속편 마지막 조문")
    void resolveLastArticle() {
        List<String> tags = map.resolveCategoryIds(1118);
        assertThat(tags).anyMatch(t -> t.startsWith("book:제5편 상속"));
    }

    @Test
    @DisplayName("범위 밖 조문(9999): 어떤 태그도 부여되지 않음")
    void resolveOutOfRange() {
        List<String> tags = map.resolveCategoryIds(9999);
        assertThat(tags).isEmpty();
    }
}
