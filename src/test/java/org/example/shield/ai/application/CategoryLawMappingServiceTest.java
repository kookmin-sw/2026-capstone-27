package org.example.shield.ai.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryLawMappingServiceTest {

    private CategoryLawMappingService service;

    @BeforeEach
    void setUp() {
        service = new CategoryLawMappingService(new DefaultResourceLoader());
        service.loadMappings();
    }

    @Test
    @DisplayName("YAML 로드 — 모든 매핑이 로드됨")
    void loadMappings_allMappingsLoaded() {
        // category-law-mappings.yml에 정의된 카테고리 수 확인
        assertThat(service.getMappingCount()).isGreaterThanOrEqualTo(25);
    }

    @Test
    @DisplayName("resolveLawIds — L2 카테고리로 법령 ID 조회")
    void resolveLawIds_l2Category() {
        List<String> lawIds = service.resolveLawIds(List.of("law-007-01"));

        // law-007-01 primary: LSI249999 (주택임대차보호법), LSI271123 (전세사기피해자법)
        // law-007-01 secondary: LSI267649 (주택임대차보호법 시행령)
        assertThat(lawIds).contains("LSI249999", "LSI271123", "LSI267649");
    }

    @Test
    @DisplayName("resolveLawIds — L3 리프 노드는 L2 부모로 폴백")
    void resolveLawIds_l3FallbackToL2() {
        // law-007-01-03은 YAML에 없으므로 law-007-01로 폴백
        List<String> lawIds = service.resolveLawIds(List.of("law-007-01-03"));

        assertThat(lawIds).contains("LSI249999");
    }

    @Test
    @DisplayName("resolveLawIds — EXTERNAL law_id 제외")
    void resolveLawIds_excludesExternal() {
        // law-004-01 primary: EXTERNAL (근로기준법) → 제외
        List<String> lawIds = service.resolveLawIds(List.of("law-004-01"));

        assertThat(lawIds).noneMatch(id -> id.equals("EXTERNAL"));
    }

    @Test
    @DisplayName("resolveLawIds — 존재하지 않는 카테고리는 빈 결과")
    void resolveLawIds_unknownCategory() {
        List<String> lawIds = service.resolveLawIds(List.of("law-999-99"));

        assertThat(lawIds).isEmpty();
    }

    @Test
    @DisplayName("resolveLawIds — 다중 카테고리 중복 제거")
    void resolveLawIds_multipleCategories_deduplication() {
        // law-001-01과 law-001-03 모두 LSI265307(민법) 포함
        List<String> lawIds = service.resolveLawIds(List.of("law-001-01", "law-001-03"));

        long civilLawCount = lawIds.stream().filter("LSI265307"::equals).count();
        assertThat(civilLawCount).isEqualTo(1);
    }

    // === B-8a: resolveCategoryIds 관련 ===

    @Test
    @DisplayName("resolveCategoryIds — L2 노드에서 매핑된 group: 토큰 반환")
    void resolveCategoryIds_l2Mapped() {
        // law-001-02 (부동산 임대차) → [group:leasing, group:jeonse]
        List<String> ids = service.resolveCategoryIds(List.of("law-001-02"));

        assertThat(ids).containsExactlyInAnyOrder("group:leasing", "group:jeonse");
    }

    @Test
    @DisplayName("resolveCategoryIds — L3 노드는 L2 부모로 폴백")
    void resolveCategoryIds_l3FallbackToL2() {
        // law-001-02-02는 YAML에 없으므로 law-001-02로 폴백
        List<String> ids = service.resolveCategoryIds(List.of("law-001-02-02"));

        assertThat(ids).contains("group:leasing", "group:jeonse");
    }

    @Test
    @DisplayName("resolveCategoryIds — 매핑 없는 노드는 조용히 무시 (빈 리스트)")
    void resolveCategoryIds_noMappingReturnsEmpty() {
        // law-004-01 (근로계약)은 category_ids 미등록 → 빈 리스트
        List<String> ids = service.resolveCategoryIds(List.of("law-004-01"));

        assertThat(ids).isEmpty();
    }

    @Test
    @DisplayName("resolveCategoryIds — 다중 노드 토큰 중복 제거")
    void resolveCategoryIds_dedup() {
        // law-001-02 → [group:leasing, group:jeonse]
        // law-007-01 → [group:leasing, group:jeonse]
        List<String> ids = service.resolveCategoryIds(List.of("law-001-02", "law-007-01"));

        assertThat(ids).containsExactlyInAnyOrder("group:leasing", "group:jeonse");
    }

    @Test
    @DisplayName("resolveCategoryIds — null/empty 입력 안전")
    void resolveCategoryIds_nullSafe() {
        assertThat(service.resolveCategoryIds(null)).isEmpty();
        assertThat(service.resolveCategoryIds(List.of())).isEmpty();
    }
}
