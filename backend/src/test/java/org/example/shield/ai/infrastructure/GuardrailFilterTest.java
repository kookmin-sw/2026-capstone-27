package org.example.shield.ai.infrastructure;

import org.example.shield.ai.dto.BriefParsedResponse;
import org.example.shield.ai.dto.ChatParsedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GuardrailFilterTest {

    private GuardrailFilter filter;

    @BeforeEach
    void setUp() {
        filter = new GuardrailFilter();
    }

    @Test
    @DisplayName("금칙어 없는 정상 응답은 그대로 반환")
    void normalResponse() {
        ChatParsedResponse response = new ChatParsedResponse();
        response.setNextQuestion("보증금은 얼마인가요?");

        ChatParsedResponse result = filter.filterChatResponse(response);
        assertThat(result.getNextQuestion()).isEqualTo("보증금은 얼마인가요?");
    }

    @Test
    @DisplayName("법적 해석 금칙어 검출 시 대체 메시지")
    void legalInterpretationBlocked() {
        ChatParsedResponse response = new ChatParsedResponse();
        response.setNextQuestion("법적으로 소송을 제기할 수 있습니다.");

        ChatParsedResponse result = filter.filterChatResponse(response);
        assertThat(result.getNextQuestion()).contains("변호사를 통해 확인");
    }

    @Test
    @DisplayName("판례 인용 금칙어 검출 시 대체 메시지")
    void caseReferenceBlocked() {
        ChatParsedResponse response = new ChatParsedResponse();
        response.setNextQuestion("판례에 따르면 이런 경우 손해배상이 인정됩니다.");

        ChatParsedResponse result = filter.filterChatResponse(response);
        assertThat(result.getNextQuestion()).contains("변호사를 통해 확인");
    }

    @Test
    @DisplayName("승소 예측 금칙어 검출 시 대체 메시지")
    void winPredictionBlocked() {
        ChatParsedResponse response = new ChatParsedResponse();
        response.setNextQuestion("승소 가능성이 높습니다.");

        ChatParsedResponse result = filter.filterChatResponse(response);
        assertThat(result.getNextQuestion()).contains("변호사를 통해 확인");
    }

    @Test
    @DisplayName("의뢰서 strategy 금칙어 검출 시 대체")
    void briefStrategyBlocked() {
        BriefParsedResponse response = new BriefParsedResponse();
        response.setStrategy("판례에 따르면 강제집행이 가능합니다.");

        BriefParsedResponse result = filter.filterBriefResponse(response);
        assertThat(result.getStrategy()).contains("변호사와 상담");
    }

    @Test
    @DisplayName("null 응답 처리")
    void nullResponse() {
        assertThat(filter.filterChatResponse(null)).isNull();
        assertThat(filter.filterBriefResponse(null)).isNull();
    }
}
