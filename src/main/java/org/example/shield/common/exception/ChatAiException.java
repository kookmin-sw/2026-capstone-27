package org.example.shield.common.exception;

/**
 * AI 채팅 응답 생성 실패 시 발생하는 예외.
 *
 * <p>Cohere Chat v2 호출 자체는 성공했으나 결과 파싱·후처리 단계에서
 * nextQuestion 이 blank 로 내려오는 등 사용자에게 의미 있는 응답을
 * 만들 수 없는 경우 또는 AI 호출 과정에서 복구 불가능한 오류가 난
 * 경우에 사용한다.</p>
 *
 * <p>{@link GlobalExceptionHandler#handleBusinessException} 에서
 * {@link BusinessException} 으로 잡혀 HTTP 500 으로 매핑된다.</p>
 */
public class ChatAiException extends BusinessException {

    public ChatAiException() {
        super(ErrorCode.CHAT_AI_FAILURE);
    }

    public ChatAiException(String detailMessage) {
        super(ErrorCode.CHAT_AI_FAILURE, detailMessage);
    }

    public ChatAiException(String detailMessage, Throwable cause) {
        super(ErrorCode.CHAT_AI_FAILURE, detailMessage, cause);
    }
}
