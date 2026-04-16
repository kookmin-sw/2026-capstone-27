package org.example.shield.ai.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GroqResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GROQ_RESPONSE_JSON = """
            {
              "id": "chatcmpl-abc123",
              "object": "chat.completion",
              "choices": [
                {
                  "index": 0,
                  "message": {
                    "role": "assistant",
                    "content": "{\\"nextQuestion\\":\\"What happened?\\",\\"allCompleted\\":false}"
                  },
                  "finish_reason": "stop"
                }
              ],
              "usage": {
                "prompt_tokens": 150,
                "completion_tokens": 42,
                "total_tokens": 192,
                "queue_time": 0.037,
                "prompt_time": 0.001,
                "completion_time": 0.035,
                "total_time": 0.036
              },
              "x_groq": {
                "id": "req_xyz789"
              }
            }
            """;

    @Test
    @DisplayName("Groq 응답 JSON 역직렬화 — extractContent() 동작 확인")
    void deserialize_extractContent() throws Exception {
        GroqResponse response = objectMapper.readValue(GROQ_RESPONSE_JSON, GroqResponse.class);

        assertThat(response.getId()).isEqualTo("chatcmpl-abc123");
        assertThat(response.getChoices()).hasSize(1);
        assertThat(response.getChoices().get(0).getFinishReason()).isEqualTo("stop");
        assertThat(response.extractContent()).contains("nextQuestion");
    }

    @Test
    @DisplayName("Usage 필드 역직렬화 — @JsonProperty snake_case 매핑 확인")
    void deserialize_usage_snakeCase() throws Exception {
        GroqResponse response = objectMapper.readValue(GROQ_RESPONSE_JSON, GroqResponse.class);

        assertThat(response.getUsage()).isNotNull();
        assertThat(response.getUsage().getPromptTokens()).isEqualTo(150);
        assertThat(response.getUsage().getCompletionTokens()).isEqualTo(42);
        assertThat(response.getUsage().getTotalTokens()).isEqualTo(192);
    }

    @Test
    @DisplayName("빈 choices — extractContent() null 반환")
    void extractContent_emptyChoices() throws Exception {
        String json = """
                {
                  "id": "chatcmpl-empty",
                  "choices": [],
                  "usage": { "prompt_tokens": 10, "completion_tokens": 0, "total_tokens": 10 }
                }
                """;

        GroqResponse response = objectMapper.readValue(json, GroqResponse.class);
        assertThat(response.extractContent()).isNull();
    }

    @Test
    @DisplayName("미지의 필드 무시 — @JsonIgnoreProperties(ignoreUnknown = true)")
    void deserialize_unknownFields_ignored() throws Exception {
        GroqResponse response = objectMapper.readValue(GROQ_RESPONSE_JSON, GroqResponse.class);
        // x_groq, queue_time 등 미지 필드가 있어도 예외 없이 파싱
        assertThat(response).isNotNull();
    }
}
