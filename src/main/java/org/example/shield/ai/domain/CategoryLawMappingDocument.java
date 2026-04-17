package org.example.shield.ai.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

/**
 * MongoDB category_law_mappings 컬렉션 문서.
 * 카테고리별 관련 법령 ID 매핑 정보를 저장.
 */
@Document(collection = "category_law_mappings")
@Getter
@Setter
@NoArgsConstructor
public class CategoryLawMappingDocument {

    @Id
    private String id;

    private String name;

    private String domain;

    @Field("primary_law_ids")
    private List<String> primaryLawIds;

    @Field("secondary_law_ids")
    private List<String> secondaryLawIds;

    @Field("updated_at")
    private Instant updatedAt;
}
