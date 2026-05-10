package org.example.shield.ai.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link LegalCaseEntity} 단위 테스트.
 *
 * <p>실제 DB 연동(Flyway V6 스키마와의 매핑)은 Spring Boot 부팅 컨텍스트 로드 테스트에서 간접 검증된다.
 * 본 테스트는 빌더·updater의 불변식만 검증한다.</p>
 */
class LegalCaseEntityTest {

    @Test
    @DisplayName("빌더: 필수 필드 지정 시 모든 값 정상 저장")
    void buildsWithAllFields() {
        LegalCaseEntity c = LegalCaseEntity.builder()
                .caseNo("2020다12345")
                .court("대법원")
                .caseName("건물인도 등")
                .decisionDate(LocalDate.of(2020, 5, 14))
                .caseType("민사")
                .judgmentType("판결")
                .disposition("상고기각")
                .headnote("판시사항")
                .holding("판결요지")
                .reasoning("판결이유")
                .fullText("원문")
                .citedArticles(new String[]{"민법 제312조"})
                .citedCases(new String[]{"대법원 2018다1234"})
                .categoryIds(new String[]{"cat-real-estate-lease"})
                .source("law.go.kr")
                .sourceUrl("https://www.law.go.kr/precInfoP.do?precSeq=1")
                .sourceId("P-0001")
                .embedding(new float[]{0.1f, 0.2f, 0.3f})
                .embeddingModel("embed-v4.0")
                .build();

        assertThat(c.getCaseNo()).isEqualTo("2020다12345");
        assertThat(c.getCourt()).isEqualTo("대법원");
        assertThat(c.getDecisionDate()).isEqualTo(LocalDate.of(2020, 5, 14));
        assertThat(c.getCaseType()).isEqualTo("민사");
        assertThat(c.getCitedArticles()).containsExactly("민법 제312조");
        assertThat(c.getCategoryIds()).containsExactly("cat-real-estate-lease");
        assertThat(c.getEmbedding()).hasSize(3);
        assertThat(c.getEmbeddingModel()).isEqualTo("embed-v4.0");
    }

    @Test
    @DisplayName("빌더: source 미지정 시 기본값 'law.go.kr'")
    void sourceDefaultsToLawGoKr() {
        LegalCaseEntity c = LegalCaseEntity.builder()
                .caseNo("2020다1")
                .court("대법원")
                .decisionDate(LocalDate.of(2020, 1, 1))
                .caseType("민사")
                .build();

        assertThat(c.getSource()).isEqualTo("law.go.kr");
    }

    @Test
    @DisplayName("updateEmbedding: 벡터와 모델ID를 항상 함께 갱신")
    void updateEmbeddingUpdatesBothFields() {
        LegalCaseEntity c = LegalCaseEntity.builder()
                .caseNo("2020다1")
                .court("대법원")
                .decisionDate(LocalDate.of(2020, 1, 1))
                .caseType("민사")
                .build();

        float[] newVec = {0.9f, 0.8f};
        c.updateEmbedding(newVec, "embed-v4.0");

        assertThat(c.getEmbedding()).isEqualTo(newVec);
        assertThat(c.getEmbeddingModel()).isEqualTo("embed-v4.0");
    }

    @Test
    @DisplayName("updateContent: 본문 필드 재수집 반영, embedding은 건드리지 않음")
    void updateContentDoesNotTouchEmbedding() {
        float[] originalVec = {0.1f};
        LegalCaseEntity c = LegalCaseEntity.builder()
                .caseNo("2020다1")
                .court("대법원")
                .decisionDate(LocalDate.of(2020, 1, 1))
                .caseType("민사")
                .headnote("old")
                .holding("old")
                .embedding(originalVec)
                .embeddingModel("embed-v4.0")
                .build();

        c.updateContent(
                "new name", "new head", "new hold", "new reason", "full",
                new String[]{"민법 제1조"}, new String[]{}, new String[]{"cat-x"},
                "파기환송", "판결");

        assertThat(c.getCaseName()).isEqualTo("new name");
        assertThat(c.getHeadnote()).isEqualTo("new head");
        assertThat(c.getHolding()).isEqualTo("new hold");
        assertThat(c.getReasoning()).isEqualTo("new reason");
        assertThat(c.getFullText()).isEqualTo("full");
        assertThat(c.getCitedArticles()).containsExactly("민법 제1조");
        assertThat(c.getCategoryIds()).containsExactly("cat-x");
        assertThat(c.getDisposition()).isEqualTo("파기환송");
        // embedding은 update 대상이 아니어야 함 (재임베딩은 별도 경로)
        assertThat(c.getEmbedding()).isEqualTo(originalVec);
        assertThat(c.getEmbeddingModel()).isEqualTo("embed-v4.0");
    }
}
