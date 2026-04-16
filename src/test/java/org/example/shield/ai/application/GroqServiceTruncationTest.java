package org.example.shield.ai.application;

import org.example.shield.ai.dto.GroqRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GroqServiceTruncationTest {

    @Test
    @DisplayName("truncateMessages — 메시지가 maxMessages+1 이하면 그대로 반환")
    void truncateMessages_withinLimit() throws Exception {
        List<GroqRequest.Message> messages = List.of(
                GroqRequest.Message.system("system prompt"),
                GroqRequest.Message.user("msg1"),
                GroqRequest.Message.assistant("reply1")
        );

        List<GroqRequest.Message> result = invokeTruncate(messages, 20);
        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("truncateMessages — 메시지가 maxMessages+1 초과 시 시스템 프롬프트 + 최근 N개 유지")
    void truncateMessages_exceedsLimit() throws Exception {
        List<GroqRequest.Message> messages = new ArrayList<>();
        messages.add(GroqRequest.Message.system("system prompt"));
        for (int i = 1; i <= 10; i++) {
            messages.add(GroqRequest.Message.user("user" + i));
            messages.add(GroqRequest.Message.assistant("reply" + i));
        }
        // 21 messages total: 1 system + 20 conversation

        List<GroqRequest.Message> result = invokeTruncate(messages, 4);

        assertThat(result).hasSize(5); // system + last 4
        assertThat(result.get(0).getRole()).isEqualTo("system");
        assertThat(result.get(0).getContent()).isEqualTo("system prompt");
        // Last 4 messages should be user10's pair and the very end
        assertThat(result.get(result.size() - 1).getContent()).isEqualTo("reply10");
    }

    @Test
    @DisplayName("truncateMessages — maxMessages=0이면 시스템 프롬프트만 유지")
    void truncateMessages_zeroMax() throws Exception {
        List<GroqRequest.Message> messages = List.of(
                GroqRequest.Message.system("system prompt"),
                GroqRequest.Message.user("msg1")
        );

        List<GroqRequest.Message> result = invokeTruncate(messages, 0);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRole()).isEqualTo("system");
    }

    /**
     * 리플렉션으로 private truncateMessages 호출.
     */
    private List<GroqRequest.Message> invokeTruncate(List<GroqRequest.Message> messages, int max)
            throws Exception {
        Method method = GroqService.class.getDeclaredMethod(
                "truncateMessages", List.class, int.class);
        method.setAccessible(true);
        GroqService service = createMinimalService();
        @SuppressWarnings("unchecked")
        List<GroqRequest.Message> result = (List<GroqRequest.Message>) method.invoke(service, messages, max);
        return result;
    }

    private GroqService createMinimalService() throws Exception {
        var constructor = GroqService.class.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        // GroqService has 6 constructor params — pass null for all (we only test truncateMessages)
        return (GroqService) constructor.newInstance(null, null, null, null, null, null);
    }
}
