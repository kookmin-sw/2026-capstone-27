package org.example.shield.ai.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GroqRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("forChat() 직렬화 — messages, model, temperature, max_completion_tokens 포함")
    void forChat_serialization() throws Exception {
        List<GroqRequest.Message> messages = List.of(
                GroqRequest.Message.system("You are a helpful assistant."),
                GroqRequest.Message.user("Hello")
        );

        GroqRequest request = GroqRequest.forChat("llama-3.3-70b-versatile", messages);
        String json = objectMapper.writeValueAsString(request);

        assertThat(json).contains("\"model\":\"llama-3.3-70b-versatile\"");
        assertThat(json).contains("\"messages\"");
        assertThat(json).contains("\"temperature\"");
        assertThat(json).contains("\"max_completion_tokens\":1024");
        assertThat(json).contains("\"top_p\"");
        // forChat should NOT include response_format
        assertThat(json).doesNotContain("response_format");
    }

    @Test
    @DisplayName("forBrief() 직렬화 — response_format: json_object 포함")
    void forBrief_serialization() throws Exception {
        List<GroqRequest.Message> messages = List.of(
                GroqRequest.Message.system("Generate a brief in JSON."),
                GroqRequest.Message.user("My legal issue is...")
        );

        GroqRequest request = GroqRequest.forBrief("llama-3.3-70b-versatile", messages);
        String json = objectMapper.writeValueAsString(request);

        assertThat(json).contains("\"max_completion_tokens\":4096");
        assertThat(json).contains("\"response_format\"");
        assertThat(json).contains("\"type\":\"json_object\"");
        // Must NOT contain json_schema (unsupported on llama-3.3-70b-versatile)
        assertThat(json).doesNotContain("json_schema");
    }

    @Test
    @DisplayName("Message factory 메서드 — role 및 content 올바른 설정")
    void message_factories() {
        GroqRequest.Message system = GroqRequest.Message.system("sys");
        GroqRequest.Message user = GroqRequest.Message.user("usr");
        GroqRequest.Message assistant = GroqRequest.Message.assistant("ast");

        assertThat(system.getRole()).isEqualTo("system");
        assertThat(system.getContent()).isEqualTo("sys");
        assertThat(user.getRole()).isEqualTo("user");
        assertThat(user.getContent()).isEqualTo("usr");
        assertThat(assistant.getRole()).isEqualTo("assistant");
        assertThat(assistant.getContent()).isEqualTo("ast");
    }
}
