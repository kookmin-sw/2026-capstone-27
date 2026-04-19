package org.example.shield.ai.application;

import org.example.shield.ai.dto.CohereChatRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CohereServiceTruncationTest {

    @Test
    @DisplayName("truncateMessages — 메시지가 maxMessages+1 이하면 그대로 반환")
    void truncateMessages_withinLimit() throws Exception {
        List<CohereChatRequest.Message> messages = List.of(
                CohereChatRequest.Message.system("system prompt"),
                CohereChatRequest.Message.user("msg1"),
                CohereChatRequest.Message.assistant("reply1")
        );

        List<CohereChatRequest.Message> result = invokeTruncate(messages, 20);
        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("truncateMessages — 메시지가 maxMessages+1 초과 시 시스템 프롬프트 + 최근 N개 유지")
    void truncateMessages_exceedsLimit() throws Exception {
        List<CohereChatRequest.Message> messages = new ArrayList<>();
        messages.add(CohereChatRequest.Message.system("system prompt"));
        for (int i = 1; i <= 10; i++) {
            messages.add(CohereChatRequest.Message.user("user" + i));
            messages.add(CohereChatRequest.Message.assistant("reply" + i));
        }
        // 21 messages total: 1 system + 20 conversation

        List<CohereChatRequest.Message> result = invokeTruncate(messages, 4);

        assertThat(result).hasSize(5); // system + last 4
        assertThat(result.get(0).getRole()).isEqualTo("system");
        assertThat(result.get(0).getContent()).isEqualTo("system prompt");
        // Last 4 messages should end with reply10
        assertThat(result.get(result.size() - 1).getContent()).isEqualTo("reply10");
    }

    @Test
    @DisplayName("truncateMessages — maxMessages=0이면 시스템 프롬프트만 유지")
    void truncateMessages_zeroMax() throws Exception {
        List<CohereChatRequest.Message> messages = List.of(
                CohereChatRequest.Message.system("system prompt"),
                CohereChatRequest.Message.user("msg1")
        );

        List<CohereChatRequest.Message> result = invokeTruncate(messages, 0);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRole()).isEqualTo("system");
    }

    /**
     * 리플렉션으로 private truncateMessages 호출.
     */
    private List<CohereChatRequest.Message> invokeTruncate(List<CohereChatRequest.Message> messages, int max)
            throws Exception {
        Method method = CohereService.class.getDeclaredMethod(
                "truncateMessages", List.class, int.class);
        method.setAccessible(true);
        CohereService service = createMinimalService();
        @SuppressWarnings("unchecked")
        List<CohereChatRequest.Message> result = (List<CohereChatRequest.Message>) method.invoke(service, messages, max);
        return result;
    }

    private CohereService createMinimalService() throws Exception {
        var constructor = CohereService.class.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        // CohereService has 7 constructor params — pass null for all (we only test truncateMessages)
        return (CohereService) constructor.newInstance(null, null, null, null, null, null, null);
    }
}
