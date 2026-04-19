package org.example.shield.ai.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * CohereEmbedResponse 파싱 + 벡터 추출 헬퍼 검증.
 */
class CohereEmbedResponseTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parse_v2Response_extractFirstVector() throws Exception {
        String json = """
                {
                  "id": "test-id-1",
                  "embeddings": {
                    "float": [[0.1, 0.2, 0.3, 0.4]]
                  },
                  "texts": ["입력1"],
                  "meta": {
                    "billed_units": { "input_tokens": 42 }
                  }
                }
                """;
        CohereEmbedResponse resp = mapper.readValue(json, CohereEmbedResponse.class);

        assertEquals("test-id-1", resp.getId());
        float[] v = resp.extractFirstFloatVector();
        assertNotNull(v);
        assertEquals(4, v.length);
        assertEquals(0.1f, v[0], 1e-6);
        assertEquals(0.4f, v[3], 1e-6);
        assertEquals(42, resp.getMeta().getBilledUnits().getInputTokens());
    }

    @Test
    void parse_v2Response_extractAllVectors() throws Exception {
        String json = """
                {
                  "id": "test-id-2",
                  "embeddings": {
                    "float": [[1.0, 2.0], [3.0, 4.0], [5.0, 6.0]]
                  }
                }
                """;
        CohereEmbedResponse resp = mapper.readValue(json, CohereEmbedResponse.class);
        List<float[]> all = resp.extractAllFloatVectors();
        assertEquals(3, all.size());
        assertEquals(2, all.get(0).length);
        assertEquals(3.0f, all.get(1)[0], 1e-6);
        assertEquals(6.0f, all.get(2)[1], 1e-6);
    }

    @Test
    void parse_emptyEmbeddings_returnsNull() throws Exception {
        String json = """
                {"id":"x","embeddings":{"float":[]}}
                """;
        CohereEmbedResponse resp = mapper.readValue(json, CohereEmbedResponse.class);
        assertNull(resp.extractFirstFloatVector());
        assertEquals(0, resp.extractAllFloatVectors().size());
    }
}
