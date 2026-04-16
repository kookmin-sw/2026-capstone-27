package org.example.shield.ai.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.ai.application.AiClient;
import org.example.shield.ai.config.GroqApiConfig;
import org.example.shield.ai.dto.BriefParsedResponse;
import org.example.shield.ai.dto.ChatParsedResponse;
import org.example.shield.ai.dto.GrokCallResult;
import org.example.shield.ai.dto.GroqRequest;
import org.example.shield.ai.dto.GroqResponse;
import org.example.shield.consultation.exception.AnalysisFailedException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

/**
 * Groq Chat Completions API HTTP 클라이언트.
 * POST https://api.groq.com/openai/v1/chat/completions
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GroqClient implements AiClient {

    private final WebClient groqWebClient;
    private final ObjectMapper objectMapper;
    private final GroqApiConfig config;

    @Override
    public GrokCallResult<ChatParsedResponse> callChat(
            String model, List<GroqRequest.Message> messages) {

        GroqRequest request = GroqRequest.forChat(model, messages);
        return callAndParse(request, ChatParsedResponse.class,
                Duration.ofMillis(config.getChatReadTimeout()));
    }

    @Override
    public GrokCallResult<BriefParsedResponse> callBrief(
            String model, List<GroqRequest.Message> messages) {

        GroqRequest request = GroqRequest.forBrief(model, messages);
        return callAndParse(request, BriefParsedResponse.class,
                Duration.ofMillis(config.getBriefReadTimeout()));
    }

    /**
     * Groq API 호출 + 응답 파싱.
     * 429/5xx 재시도 3회, JSON 파싱 실패 시 markdown extraction fallback.
     */
    private <T> GrokCallResult<T> callAndParse(GroqRequest request, Class<T> type, Duration timeout) {
        long startTime = System.currentTimeMillis();

        try {
            GroqResponse groqResponse = groqWebClient.post()
                    .uri("/v1/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GroqResponse.class)
                    .timeout(timeout)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(this::isRetryable))
                    .block();

            if (groqResponse == null) {
                throw new AnalysisFailedException("Groq API 응답이 null입니다");
            }

            int latencyMs = (int) (System.currentTimeMillis() - startTime);
            String contentJson = groqResponse.extractContent();

            if (contentJson == null || contentJson.isBlank()) {
                throw new AnalysisFailedException("Groq API 응답 content가 비어있습니다");
            }

            T parsed = parseResponse(contentJson, type);

            Integer tokensIn = groqResponse.getUsage() != null
                    ? groqResponse.getUsage().getPromptTokens() : null;
            Integer tokensOut = groqResponse.getUsage() != null
                    ? groqResponse.getUsage().getCompletionTokens() : null;

            log.info("Groq API 호출 성공: id={}, tokensIn={}, tokensOut={}, latency={}ms",
                    groqResponse.getId(), tokensIn, tokensOut, latencyMs);

            return new GrokCallResult<>(
                    groqResponse.getId(),
                    parsed,
                    tokensIn,
                    tokensOut,
                    latencyMs
            );

        } catch (AnalysisFailedException e) {
            throw e;
        } catch (Exception e) {
            int latencyMs = (int) (System.currentTimeMillis() - startTime);
            log.error("Groq API 호출 실패: latency={}ms, error={}", latencyMs, e.getMessage(), e);
            throw new AnalysisFailedException("Groq API 호출 실패: " + e.getMessage());
        }
    }

    private <T> T parseResponse(String contentJson, Class<T> type) {
        try {
            return objectMapper.readValue(contentJson, type);
        } catch (Exception e) {
            log.warn("JSON 파싱 실패, 원문: {}", contentJson.substring(0, Math.min(500, contentJson.length())));

            String cleaned = extractJsonFromMarkdown(contentJson);
            if (!cleaned.equals(contentJson)) {
                try {
                    return objectMapper.readValue(cleaned, type);
                } catch (Exception e2) {
                    log.error("마크다운 추출 후에도 JSON 파싱 실패: {}", e2.getMessage());
                }
            }

            throw new AnalysisFailedException("Groq 응답 JSON 파싱 실패: " + e.getMessage());
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
