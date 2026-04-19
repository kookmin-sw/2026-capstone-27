package org.example.shield.ai.application;

import org.example.shield.ai.dto.CohereChatRequest;
import org.example.shield.ai.infrastructure.SanitizeService;
import org.example.shield.common.enums.MessageRole;
import org.example.shield.consultation.domain.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link CohereService#appendHistory} 공통 메서드 단위 테스트.
 *
 * <p>PR-B 리팩터링에서 {@code buildChatMessages} / {@code buildBriefMessages}
 * 의 대화 내역 순회 로직을 {@code appendHistory} 로 추출했다. 이 테스트는 그
 * 공통 경로의 방어적 skip 계약(Issue #45)을 고정한다:</p>
 * <ul>
 *   <li>원본 content 가 null/blank 이면 role 과 무관하게 skip</li>
 *   <li>USER 의 경우 sanitize 결과가 blank 로 수축해도 skip</li>
 *   <li>정상 content 는 USER 는 sanitize 후, CHATBOT 은 원본 그대로 추가</li>
 * </ul>
 *
 * <p>리플렉션으로 private 메서드를 직접 호출해 {@code aiClient}/{@code promptService}
 * 같은 불필요한 협력자 없이 순수 로직만 검증한다.</p>
 */
class CohereServiceHistoryAppendTest {

    private SanitizeService sanitizeService;
    private CohereService service;
    private Method appendHistory;

    @BeforeEach
    void setUp() throws Exception {
        sanitizeService = mock(SanitizeService.class);
        when(sanitizeService.sanitizeUserText(anyString()))
                .thenAnswer(inv -> inv.getArgument(0)); // 기본은 원문 유지

        service = createServiceWithSanitize(sanitizeService);
        appendHistory = CohereService.class.getDeclaredMethod(
                "appendHistory", List.class, List.class, String.class);
        appendHistory.setAccessible(true);
    }

    @Test
    @DisplayName("appendHistory — 정상 USER/CHATBOT 메시지는 순서대로 변환되어 추가된다")
    void appendHistory_valid_addsInOrder() throws Exception {
        List<Message> history = List.of(
                messageWithId(MessageRole.USER, "사용자 질문 1"),
                messageWithId(MessageRole.CHATBOT, "챗봇 응답 1"),
                messageWithId(MessageRole.USER, "사용자 질문 2")
        );
        List<CohereChatRequest.Message> msgs = new ArrayList<>();

        appendHistory.invoke(service, msgs, history, "chat");

        assertThat(msgs).hasSize(3);
        assertThat(msgs.get(0).getRole()).isEqualTo("user");
        assertThat(msgs.get(0).getContent()).isEqualTo("사용자 질문 1");
        assertThat(msgs.get(1).getRole()).isEqualTo("assistant");
        assertThat(msgs.get(1).getContent()).isEqualTo("챗봇 응답 1");
        assertThat(msgs.get(2).getRole()).isEqualTo("user");
    }

    @Test
    @DisplayName("appendHistory — null/blank content 메시지는 role 무관하게 skip")
    void appendHistory_blankRawContent_skipped() throws Exception {
        List<Message> history = List.of(
                messageWithId(MessageRole.USER, "정상 질문"),
                messageWithId(MessageRole.CHATBOT, ""),        // blank CHATBOT → skip
                messageWithId(MessageRole.USER, "   "),        // whitespace USER → skip
                messageWithId(MessageRole.CHATBOT, null),      // null CHATBOT → skip
                messageWithId(MessageRole.CHATBOT, "정상 응답")
        );
        List<CohereChatRequest.Message> msgs = new ArrayList<>();

        appendHistory.invoke(service, msgs, history, "chat");

        assertThat(msgs).hasSize(2);
        assertThat(msgs.get(0).getContent()).isEqualTo("정상 질문");
        assertThat(msgs.get(1).getContent()).isEqualTo("정상 응답");
    }

    @Test
    @DisplayName("appendHistory — USER 원본이 blank 면 sanitizeService 를 아예 호출하지 않는다 (비용 회피)")
    void appendHistory_blankUser_doesNotCallSanitize() throws Exception {
        SanitizeService strictSanitize = mock(SanitizeService.class);
        // sanitize 가 호출되면 예외로 실패시켜 호출 여부를 감지
        when(strictSanitize.sanitizeUserText(anyString()))
                .thenThrow(new AssertionError("sanitizeUserText 는 blank 원본에 대해 호출되면 안 된다"));

        CohereService strictService = createServiceWithSanitize(strictSanitize);
        List<Message> history = List.of(
                messageWithId(MessageRole.USER, ""),
                messageWithId(MessageRole.USER, "   ")
        );
        List<CohereChatRequest.Message> msgs = new ArrayList<>();

        appendHistory.invoke(strictService, msgs, history, "brief");

        assertThat(msgs).isEmpty();
    }

    @Test
    @DisplayName("appendHistory — sanitize 결과가 blank 로 수축하는 USER 메시지는 skip")
    void appendHistory_sanitizeShrinksToBlank_skipped() throws Exception {
        when(sanitizeService.sanitizeUserText("[[PII]]")).thenReturn("   ");

        List<Message> history = List.of(
                messageWithId(MessageRole.USER, "[[PII]]"),
                messageWithId(MessageRole.USER, "정상 질문")
        );
        List<CohereChatRequest.Message> msgs = new ArrayList<>();

        appendHistory.invoke(service, msgs, history, "chat");

        assertThat(msgs).hasSize(1);
        assertThat(msgs.get(0).getContent()).isEqualTo("정상 질문");
    }

    @Test
    @DisplayName("appendHistory — 빈 history 면 아무 것도 추가하지 않는다")
    void appendHistory_emptyHistory_noop() throws Exception {
        List<CohereChatRequest.Message> msgs = new ArrayList<>();
        appendHistory.invoke(service, msgs, List.<Message>of(), "chat");
        assertThat(msgs).isEmpty();
    }

    // ── helpers ──────────────────────────────────────────────────────────

    /** Message 는 @GeneratedValue 로 id 가 null 이므로, 로그에 쓰일 id 를 리플렉션으로 주입. */
    private Message messageWithId(MessageRole role, String content) throws Exception {
        Message msg;
        if (role == MessageRole.USER) {
            msg = Message.createUserMessage(UUID.randomUUID(), content);
        } else {
            msg = Message.createAiMessage(UUID.randomUUID(), content, null, null, null, null);
        }
        Field idField = Message.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(msg, UUID.randomUUID());
        return msg;
    }

    /**
     * CohereService 의 모든 의존성에 null 을 넣되 sanitizeService 만 실제 mock 을
     * 주입한다. appendHistory 는 sanitizeService 만 사용하므로 이것으로 충분.
     */
    private CohereService createServiceWithSanitize(SanitizeService sanitize) throws Exception {
        Constructor<?> constructor = CohereService.class.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        Object[] args = new Object[constructor.getParameterCount()];
        CohereService s = (CohereService) constructor.newInstance(args);

        Field f = CohereService.class.getDeclaredField("sanitizeService");
        f.setAccessible(true);
        f.set(s, sanitize);
        return s;
    }
}
