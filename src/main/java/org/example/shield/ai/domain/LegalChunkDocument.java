package org.example.shield.ai.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

/**
 * MongoDB legal_chunks 컬렉션 문서.
 * 법률 조문 청크 단위로 저장되며, 텍스트 검색 및 벡터 검색 대상.
 */
@Document(collection = "legal_chunks")
@Getter
@Setter
@NoArgsConstructor
public class LegalChunkDocument {

    @Id
    private String id;

    @Field("law_id")
    private String lawId;

    @Field("law_name")
    private String lawName;

    @Field("article_no")
    private String articleNo;

    @Field("article_title")
    private String articleTitle;

    private String content;

    @Field("effective_date")
    private String effectiveDate;

    @Field("source_url")
    private String sourceUrl;

    @Field("abolition_date")
    private String abolitionDate;

    @Field("category_ids")
    private List<String> categoryIds;

    private List<Double> embedding;

    @Field("lod_uri")
    private String lodUri;

    @Field("legislation_terms")
    private List<String> legislationTerms;
}
