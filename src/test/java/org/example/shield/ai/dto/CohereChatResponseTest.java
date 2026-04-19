package org.example.shield.ai.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CohereChatResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String COHERE_RESPONSE_JSON = """
            {
              "id": "5a50480a-cf52-46f0-af01-53d18539bd31",
              "message": {
                "role": "assistant",
                "content": [
                  {
                    "type": "text",
                    "text": "{\\"nextQuestion\\":\\"What happened?\\",\\"allCompleted\\":false}"
                  }
                ]
              },
              "finish_reason": "COMPLETE",
              "meta": {
                "api_version": { "version": "2" },
                "billed_units": { "input_tokens": 17, "output_tokens": 12 },
                "tokens": { "input_tokens": 215, "output_tokens": 12 }
              }
            }
            """;

    @Test
    @DisplayName("Cohere v2 응답 JSON 역직렬화 — extractContent() 동작 확인")
    void deserialize_extractContent() throws Exception {
        CohereChatResponse response = objectMapper.readValue(COHERE_RESPONSE_JSON, CohereChatResponse.class);

        assertThat(response.getId()).isEqualTo("5a50480a-cf52-46f0-af01-53d18539bd31");
        assertThat(response.getFinishReason()).isEqualTo("COMPLETE");
        assertThat(response.getMessage()).isNotNull();
        assertThat(response.getMessage().getContent()).hasSize(1);
        assertThat(response.extractContent()).contains("nextQuestion");
    }

    @Test
    @DisplayName("meta.billed_units / meta.tokens 역직렬화 — @JsonProperty snake_case 매핑")
    void deserialize_meta_tokens() throws Exception {
        CohereChatResponse response = objectMapper.readValue(COHERE_RESPONSE_JSON, CohereChatResponse.class);

        assertThat(response.getMeta()).isNotNull();
        assertThat(response.getMeta().getBilledUnits()).isNotNull();
        assertThat(response.getMeta().getBilledUnits().getInputTokens()).isEqualTo(17);
        assertThat(response.getMeta().getBilledUnits().getOutputTokens()).isEqualTo(12);
        assertThat(response.getMeta().getTokens()).isNotNull();
        assertThat(response.getMeta().getTokens().getInputTokens()).isEqualTo(215);
        assertThat(response.getMeta().getTokens().getOutputTokens()).isEqualTo(12);
    }

    @Test
    @DisplayName("message.content 비어있을 때 — extractContent() null 반환")
    void extractContent_emptyContent() throws Exception {
        String json = """
                {
                  "id": "empty-content",
                  "message": { "role": "assistant", "content": [] },
                  "finish_reason": "ERROR"
                }
                """;

        CohereChatResponse response = objectMapper.readValue(json, CohereChatResponse.class);
        assertThat(response.extractContent()).isNull();
    }

    @Test
    @DisplayName("message 자체가 없을 때 — extractContent() null 반환")
    void extractContent_nullMessage() throws Exception {
        String json = """
                {
                  "id": "no-message",
                  "finish_reason": "ERROR"
                }
                """;

        CohereChatResponse response = objectMapper.readValue(json, CohereChatResponse.class);
        assertThat(response.extractContent()).isNull();
    }

    @Test
    @DisplayName("미지의 필드 무시 — @JsonIgnoreProperties(ignoreUnknown = true)")
    void deserialize_unknownFields_ignored() throws Exception {
        CohereChatResponse response = objectMapper.readValue(COHERE_RESPONSE_JSON, CohereChatResponse.class);
        // api_version 등 미지 필드가 있어도 예외 없이 파싱
        assertThat(response).isNotNull();
    }
}
