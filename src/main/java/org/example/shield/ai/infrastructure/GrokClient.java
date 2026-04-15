package org.example.shield.ai.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.ai.application.AiClient;
import org.example.shield.ai.config.GrokApiConfig;
import org.example.shield.ai.dto.BriefParsedResponse;
import org.example.shield.ai.dto.ChatParsedResponse;
import org.example.shield.ai.dto.GrokCallResult;
import org.example.shield.ai.dto.GrokRequest;
import org.example.shield.ai.dto.GrokResponse;
import org.example.shield.consultation.exception.AnalysisFailedException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

/**
 * Grok Responses API HTTP 클라이언트.
 * POST https://api.x.ai/v1/responses
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GrokClient implements AiClient {

    private final WebClient grokWebClient;
    private final ObjectMapper objectMapper;
    private final GrokApiConfig config;

    @Override
    public GrokCallResult<ChatParsedResponse> callChatInitial(
            String model, List<GrokRequest.InputItem> inputArray, String consultationId) {

        GrokRequest request = GrokRequest.forChatInitial(model, inputArray, consultationId);
        return callAndParse(request, ChatParsedResponse.class,
                Duration.ofMillis(config.getChatReadTimeout()));
    }

    @Override
    public GrokCallResult<ChatParsedResponse> callChatFollowUp(
            String model, String userMessage, String previousResponseId, String consultationId) {

        GrokRequest request = GrokRequest.forChatFollowUp(model, userMessage,
                previousResponseId, consultationId);
        return callAndParse(request, ChatParsedResponse.class,
                Duration.ofMillis(config.getChatReadTimeout()));
    }

    @Override
    public GrokCallResult<BriefParsedResponse> callBrief(
            String model, List<GrokRequest.InputItem> inputArray) {

        GrokRequest request = GrokRequest.forBrief(model, inputArray);
        return callAndParse(request, BriefParsedResponse.class,
                Duration.ofMillis(config.getBriefReadTimeout()));
    }

    /**
     * Grok API 호출 + 응답 파싱.
     * 429/5xx 재시도 3회, JSON 파싱 실패 시 1회 재시도.
     */
    private <T> GrokCallResult<T> callAndParse(GrokRequest request, Class<T> type, Duration timeout) {
        long startTime = System.currentTimeMillis();

        try {
            GrokResponse grokResponse = grokWebClient.post()
                    .uri("/v1/responses")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GrokResponse.class)
                    .timeout(timeout)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(this::isRetryable))
                    .block();

            if (grokResponse == null) {
                throw new AnalysisFailedException("Grok API 응답이 null입니다");
            }

            int latencyMs = (int) (System.currentTimeMillis() - startTime);
            String contentJson = grokResponse.extractContent();

            if (contentJson == null || contentJson.isBlank()) {
                throw new AnalysisFailedException("Grok API 응답 content가 비어있습니다");
            }

            // JSON 파싱
            T parsed = parseResponse(contentJson, type);

            Integer tokensIn = grokResponse.getUsage() != null
                    ? grokResponse.getUsage().getInputTokens() : null;
            Integer tokensOut = grokResponse.getUsage() != null
                    ? grokResponse.getUsage().getOutputTokens() : null;

            return new GrokCallResult<>(
                    grokResponse.getId(),
                    parsed,
                    tokensIn,
                    tokensOut,
                    latencyMs
            );

        } catch (AnalysisFailedException e) {
            throw e;
        } catch (Exception e) {
            int latencyMs = (int) (System.currentTimeMillis() - startTime);
            log.error("Grok API 호출 실패: latency={}ms, error={}", latencyMs, e.getMessage(), e);
            throw new AnalysisFailedException("Grok API 호출 실패: " + e.getMessage());
        }
    }

    private <T> T parseResponse(String contentJson, Class<T> type) {
        try {
            return objectMapper.readValue(contentJson, type);
        } catch (Exception e) {
            log.warn("JSON 파싱 실패, 원문: {}", contentJson.substring(0, Math.min(500, contentJson.length())));

            // JSON이 ```json ... ``` 블록에 감싸인 경우 추출 시도
            String cleaned = extractJsonFromMarkdown(contentJson);
            if (!cleaned.equals(contentJson)) {
                try {
                    return objectMapper.readValue(cleaned, type);
                } catch (Exception e2) {
                    log.error("마크다운 추출 후에도 JSON 파싱 실패: {}", e2.getMessage());
                }
            }

            throw new AnalysisFailedException("Grok 응답 JSON 파싱 실패: " + e.getMessage());
        }
    }

    private String extractJsonFromMarkdown(String raw) {
        if (raw.contains("```json")) {
            int start = raw.indexOf("```json") + 7;
            int end = raw.indexOf("```", start);
            if (end > start) {
                return raw.substring(start, end).trim();
            }
        }
        if (raw.contains("```")) {
            int start = raw.indexOf("```") + 3;
            int end = raw.indexOf("```", start);
            if (end > start) {
                return raw.substring(start, end).trim();
            }
        }
        return raw;
    }

    private boolean isRetryable(Throwable e) {
        if (e instanceof WebClientResponseException wce) {
            int status = wce.getStatusCode().value();
            return status == 429 || status >= 500;
        }
        return false;
    }
}
