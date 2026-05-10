package org.example.shield.ai.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 카테고리-법령 매핑 POJO.
 * YAML 파일(category-law-mappings.yml)에서 로드되는 인메모리 도메인 객체.
 */
@Getter
@Setter
@NoArgsConstructor
public class CategoryLawMapping {

    private String categoryId;
    private String name;
    private List<LawRef> primaryLawIds;
    private List<LawRef> secondaryLawIds;
    /**
     * 이 카테고리(온톨로지 노드)에 매핑되는 {@code legal_chunks.category_ids} 토큰 목록.
     * 예: {@code law-001-02} → {@code [group:leasing, group:jeonse]}.
     * null/empty면 retrieval 단계에서 카테고리 soft-filter가 적용되지 않는다.
     */
    private List<String> categoryIds;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class LawRef {
        private String lawId;
        private String name;
    }
}
