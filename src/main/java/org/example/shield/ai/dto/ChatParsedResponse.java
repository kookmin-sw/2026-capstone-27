package org.example.shield.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Phase 1 대화 응답 파싱 결과.
 * LLM이 온톨로지 기반으로 분류한 대분류/중분류/소분류.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatParsedResponse {

    private String nextQuestion;

    @JsonDeserialize(using = FlexibleStringListDeserializer.class)
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<String> aiDomains = new ArrayList<>();

    @JsonDeserialize(using = FlexibleStringListDeserializer.class)
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<String> aiSubDomains = new ArrayList<>();

    @JsonDeserialize(using = FlexibleStringListDeserializer.class)
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<String> aiTags = new ArrayList<>();

    private boolean allCompleted;

    /**
     * 문자열("VALUE") 또는 배열(["VALUE"])을 모두 List&lt;String&gt;으로 변환.
     */
    static class FlexibleStringListDeserializer extends JsonDeserializer<List<String>> {
        @Override
        public List<String> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            List<String> result = new ArrayList<>();
            if (node.isArray()) {
                for (JsonNode elem : node) {
                    result.add(elem.asText());
                }
            } else if (node.isTextual()) {
                String text = node.asText();
                if (!text.isBlank()) {
                    result.add(text);
                }
            }
            return result;
        }
    }
}
