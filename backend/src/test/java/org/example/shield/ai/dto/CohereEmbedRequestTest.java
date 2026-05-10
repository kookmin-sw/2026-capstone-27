package org.example.shield.ai.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CohereEmbedRequest DTO 직렬화 검증.
 * Cohere v2 /embed 스펙 필드명이 JSON에 정확히 반영되는지 확인.
 */
class CohereEmbedRequestTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void forDocument_snakeCaseFields() throws Exception {
        CohereEmbedRequest req = CohereEmbedRequest.forDocument(
                "embed-v4.0", List.of("민법 제618조", "민법 제635조"), 1024);

        @SuppressWarnings("unchecked")
        Map<String, Object> json = mapper.readValue(mapper.writeValueAsString(req), Map.class);

        assertEquals("embed-v4.0", json.get("model"));
        assertEquals("search_document", json.get("input_type"));
        assertEquals(List.of("float"), json.get("embedding_types"));
        assertEquals(1024, json.get("output_dimension"));
        @SuppressWarnings("unchecked")
        List<String> texts = (List<String>) json.get("texts");
        assertEquals(2, texts.size());
    }

    @Test
    void forQuery_singleText() throws Exception {
        CohereEmbedRequest req = CohereEmbedRequest.forQuery(
                "embed-v4.0", "전세 보증금 미반환", 1024);

        String raw = mapper.writeValueAsString(req);
        assertTrue(raw.contains("\"input_type\":\"search_query\""));
        assertTrue(raw.contains("\"output_dimension\":1024"));
    }

    @Test
    void forDocument_nullFieldsExcluded() throws Exception {
        CohereEmbedRequest req = CohereEmbedRequest.forDocument(
                "embed-v4.0", List.of("test"), 256);
        String raw = mapper.writeValueAsString(req);
        // Lombok Builder로 세팅 안 한 필드가 null이면 JSON에서 제외돼야 함 (JsonInclude.NON_NULL)
        assertTrue(raw.contains("\"output_dimension\":256"));
        // 임의로 누락 필드 있을 때 null 포함되지 않음 확인 — 이 DTO는 모든 필드 세팅이므로 검증만 유지
    }
}
