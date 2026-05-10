package org.example.shield.ai.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.shield.ai.config.CohereApiConfig;
import org.example.shield.ai.dto.ChatParsedResponse;
import org.example.shield.consultation.exception.AnalysisFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Issue #56 — CohereClient.parseResponse 3단 방어 단위 테스트.
 *
 * <p>AI 모델이 response_format=json_object 를 우회해 평문/마크다운/혼합 응답을
 * 내놓을 때 파싱이 복구되는지 검증한다.</p>
 */
class CohereClientParseResponseTest {

    private CohereClient client;
    private Method parseResponse;

    @BeforeEach
    void setUp() throws Exception {
        // @RequiredArgsConstructor 대신 리플렉션으로 필드 주입 (WebClient bean 불필요)
        client = newInstance();
        setField(client, "objectMapper", new ObjectMapper());
        setField(client, "config", new CohereApiConfig());
        setField(client, "cohereWebClient", WebClient.builder().build());
        parseResponse = CohereClient.class.getDeclaredMethod(
                "parseResponse", String.class, Class.class);
        parseResponse.setAccessible(true);
    }

    private CohereClient newInstance() throws Exception {
        var ctor = CohereClient.class.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        Object[] args = new Object[ctor.getParameterCount()];
        return (CohereClient) ctor.newInstance(args);
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private ChatParsedResponse invoke(String raw) throws Exception {
        try {
            return (ChatParsedResponse) parseResponse.invoke(client, raw, ChatParsedResponse.class);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException re) throw re;
            throw e;
        }
    }

    @Test
    void 정상_JSON_원문_파싱() throws Exception {
        String raw = "{\"nextQuestion\":\"어떤 상황인가요?\",\"aiDomains\":null,\"aiSubDomains\":null,\"aiTags\":null,\"allCompleted\":false}";
        ChatParsedResponse r = invoke(raw);
        assertThat(r.getNextQuestion()).isEqualTo("어떤 상황인가요?");
        assertThat(r.isAllCompleted()).isFalse();
    }

    @Test
    void 마크다운_json_펜스_안의_JSON_복구() throws Exception {
        String raw = """
                ```json
                {"nextQuestion":"보증금 얼마인가요?","allCompleted":false}
                ```
                """;
        ChatParsedResponse r = invoke(raw);
        assertThat(r.getNextQuestion()).isEqualTo("보증금 얼마인가요?");
    }

    @Test
    void 평문_앞에_인사말이_붙은_경우_JSON_블록_복구() throws Exception {
        // 실제 장애 케이스 재현: 모델이 평문으로 시작해버린 상황
        String raw = "체불된 임금이 있으신가요? 아래는 구조화 결과입니다.\n"
                + "{\"nextQuestion\":\"언제부터 못 받으셨나요?\",\"allCompleted\":false}";
        ChatParsedResponse r = invoke(raw);
        assertThat(r.getNextQuestion()).isEqualTo("언제부터 못 받으셨나요?");
    }

    @Test
    void 중첩_중괄호가_있어도_균형_맞는_첫_객체_복구() throws Exception {
        String raw = "안내: {\"nextQuestion\":\"정보\",\"allCompleted\":false,\"meta\":{\"x\":1}} 끝";
        ChatParsedResponse r = invoke(raw);
        assertThat(r.getNextQuestion()).isEqualTo("정보");
    }

    @Test
    void 문자열_리터럴_안의_중괄호는_구조로_인식하지_않음() throws Exception {
        // nextQuestion 문자열 값 안에 } 가 섞여있어도 이스케이프 처리가 정확해야 함
        String raw = "{\"nextQuestion\":\"} 괄호 포함 질문\",\"allCompleted\":false}";
        ChatParsedResponse r = invoke(raw);
        assertThat(r.getNextQuestion()).isEqualTo("} 괄호 포함 질문");
    }

    @Test
    void JSON_이_완전히_없으면_AnalysisFailedException() {
        String raw = "체불된 임금이 있으신가요? 정보를 알려주세요."; // JSON 블록 전혀 없음
        assertThatThrownBy(() -> invoke(raw))
                .isInstanceOf(AnalysisFailedException.class)
                .hasMessageContaining("JSON 파싱 실패");
    }
}
