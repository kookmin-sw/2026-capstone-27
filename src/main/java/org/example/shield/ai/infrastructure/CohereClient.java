package org.example.shield.ai.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.ai.application.AiClient;
import org.example.shield.ai.config.CohereApiConfig;
import org.example.shield.ai.dto.BriefParsedResponse;
import org.example.shield.ai.dto.ChatParsedResponse;
import org.example.shield.ai.dto.AiCallResult;
import org.example.shield.ai.dto.CohereChatRequest;
import org.example.shield.ai.dto.CohereChatResponse;
import org.example.shield.ai.dto.CohereEmbedRequest;
import org.example.shield.ai.dto.CohereEmbedResponse;
import org.example.shield.consultation.exception.AnalysisFailedException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

/**
 * Cohere Chat API v2 HTTP 클라이언트.
 * POST https://api.cohere.com/v2/chat
 *
 * 응답 구조 요약:
 * - id: chat completion ID
 * - message.content[0].text: 모델 응답 본문
 * - finish_reason: COMPLETE / MAX_TOKENS / ERROR 등
 * - meta.billed_units.{input,output}_tokens: 사용자 청구 기준 토큰
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CohereClient implements AiClient {

    private final WebClient cohereWebClient;
    private final ObjectMapper objectMapper;
    private final CohereApiConfig config;

    @Override
    public AiCallResult<ChatParsedResponse> callChat(
            String model, List<CohereChatRequest.Message> messages) {

        CohereChatRequest request = CohereChatRequest.forChat(model, messages);
        return callAndParse(request, ChatParsedResponse.class,
                Duration.ofMillis(config.getChatReadTimeout()));
    }

    @Override
    public AiCallResult<BriefParsedResponse> callBrief(
            String model, List<CohereChatRequest.Message> messages) {

        CohereChatRequest request = CohereChatRequest.forBrief(model, messages);
        return callAndParse(request, BriefParsedResponse.class,
                Duration.ofMillis(config.getBriefReadTimeout()));
    }

    /**
     * Cohere Embed API v2 호출 — 문서(조문) 배치 임베딩 생성.
     * B-2 인제스트 파이프라인이 사용.
     *
     * @param model    임베딩 모델 ID (예: "embed-v4.0")
     * @param texts    임베딩 대상 텍스트 리스트 (최대 96개 권장)
     * @return float 벡터 배열 리스트 (texts와 동일 순서)
     */
    public List<float[]> embedDocuments(String model, List<String> texts) {
        CohereEmbedRequest req = CohereEmbedRequest.forDocument(model, texts, config.getEmbedDimension());
        CohereEmbedResponse resp = callEmbed(req);
        return resp.extractAllFloatVectors();
    }

    /**
     * Cohere Embed API v2 호출 — 단일 쿼리 임베딩.
     * Layer 2 벡터 검색 시점에 사용. B-4에서 Redis 캐시와 결합 예정.
     */
    public float[] embedQuery(String model, String query) {
        CohereEmbedRequest req = CohereEmbedRequest.forQuery(model, query, config.getEmbedDimension());
        CohereEmbedResponse resp = callEmbed(req);
        return resp.extractFirstFloatVector();
    }

    private CohereEmbedResponse callEmbed(CohereEmbedRequest request) {
        long startNanos = System.nanoTime();
        try {
            CohereEmbedResponse resp = cohereWebClient.post()
                    .uri("/v2/embed")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(CohereEmbedResponse.class)
                    .timeout(Duration.ofMillis(config.getEmbedReadTimeout()))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(this::isRetryable))
                    .block();

            if (resp == null) {
                throw new AnalysisFailedException("Cohere Embed API 응답이 null입니다");
            }

            int latencyMs = (int) ((System.nanoTime() - startNanos) / 1_000_000);
            Integer inputTokens = (resp.getMeta() != null && resp.getMeta().getBilledUnits() != null)
                    ? resp.getMeta().getBilledUnits().getInputTokens() : null;
            int vectorCount = resp.extractAllFloatVectors().size();
            log.info("Cohere Embed API 호출 성공: id={}, model={}, vectors={}, inputTokens={}, latency={}ms",
                    resp.getId(), request.getModel(), vectorCount, inputTokens, latencyMs);

            return resp;
        } catch (AnalysisFailedException e) {
            throw e;
        } catch (Exception e) {
            int latencyMs = (int) ((System.nanoTime() - startNanos) / 1_000_000);
            log.error("Cohere Embed API 호출 실패: latency={}ms, error={}", latencyMs, e.getMessage(), e);
            throw new AnalysisFailedException("Cohere Embed API 호출 실패: " + e.getMessage(), e);
        }
    }

    /**
     * Cohere API 호출 — raw JSON 문자열 그대로 반환 (파싱 없음).
     * callClassify 등 응답을 직접 파싱하는 호출자용.
     */
    public AiCallResult<String> callRawJson(CohereChatRequest request, Duration timeout) {
        long startNanos = System.nanoTime();

        try {
            CohereChatResponse cohereResponse = cohereWebClient.post()
                    .uri("/v2/chat")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(CohereChatResponse.class)
                    .timeout(timeout)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(this::isRetryable))
                    .block();

            if (cohereResponse == null) {
                throw new AnalysisFailedException("Cohere API 응답이 null입니다");
            }

            int latencyMs = (int) ((System.nanoTime() - startNanos) / 1_000_000);
            String contentJson = cohereResponse.extractContent();

            Integer tokensIn = extractInputTokens(cohereResponse);
            Integer tokensOut = extractOutputTokens(cohereResponse);

            log.info("Cohere API 호출 성공 (raw): id={}, tokensIn={}, tokensOut={}, finish={}, latency={}ms",
                    cohereResponse.getId(), tokensIn, tokensOut, cohereResponse.getFinishReason(), latencyMs);

            return new AiCallResult<>(cohereResponse.getId(), contentJson, tokensIn, tokensOut, latencyMs);

        } catch (AnalysisFailedException e) {
            throw e;
        } catch (Exception e) {
            int latencyMs = (int) ((System.nanoTime() - startNanos) / 1_000_000);
            log.error("Cohere API 호출 실패 (raw): latency={}ms, error={}", latencyMs, e.getMessage(), e);
            throw new AnalysisFailedException("Cohere API 호출 실패: " + e.getMessage(), e);
        }
    }

    /**
     * Cohere API 호출 + 응답 파싱.
     * 429/5xx 재시도 3회, JSON 파싱 실패 시 markdown extraction fallback.
     */
    private <T> AiCallResult<T> callAndParse(CohereChatRequest request, Class<T> type, Duration timeout) {
        long startNanos = System.nanoTime();

        try {
            CohereChatResponse cohereResponse = cohereWebClient.post()
                    .uri("/v2/chat")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(CohereChatResponse.class)
                    .timeout(timeout)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(this::isRetryable))
                    .block();

            if (cohereResponse == null) {
                throw new AnalysisFailedException("Cohere API 응답이 null입니다");
            }

            int latencyMs = (int) ((System.nanoTime() - startNanos) / 1_000_000);
            String contentJson = cohereResponse.extractContent();

            if (contentJson == null || contentJson.isBlank()) {
                throw new AnalysisFailedException("Cohere API 응답 content가 비어있습니다");
            }

            T parsed = parseResponse(contentJson, type);

            Integer tokensIn = extractInputTokens(cohereResponse);
            Integer tokensOut = extractOutputTokens(cohereResponse);

            log.info("Cohere API 호출 성공: id={}, tokensIn={}, tokensOut={}, finish={}, latency={}ms",
                    cohereResponse.getId(), tokensIn, tokensOut, cohereResponse.getFinishReason(), latencyMs);

            return new AiCallResult<>(
                    cohereResponse.getId(),
                    parsed,
                    tokensIn,
                    tokensOut,
                    latencyMs
            );

        } catch (AnalysisFailedException e) {
            throw e;
        } catch (Exception e) {
            int latencyMs = (int) ((System.nanoTime() - startNanos) / 1_000_000);
            log.error("Cohere API 호출 실패: latency={}ms, error={}", latencyMs, e.getMessage(), e);
            throw new AnalysisFailedException("Cohere API 호출 실패: " + e.getMessage(), e);
        }
    }

    private Integer extractInputTokens(CohereChatResponse r) {
        if (r.getMeta() == null) return null;
        if (r.getMeta().getBilledUnits() != null && r.getMeta().getBilledUnits().getInputTokens() != null) {
            return r.getMeta().getBilledUnits().getInputTokens();
        }
        return r.getMeta().getTokens() != null ? r.getMeta().getTokens().getInputTokens() : null;
    }

    private Integer extractOutputTokens(CohereChatResponse r) {
        if (r.getMeta() == null) return null;
        if (r.getMeta().getBilledUnits() != null && r.getMeta().getBilledUnits().getOutputTokens() != null) {
            return r.getMeta().getBilledUnits().getOutputTokens();
        }
        return r.getMeta().getTokens() != null ? r.getMeta().getTokens().getOutputTokens() : null;
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

            throw new AnalysisFailedException("Cohere 응답 JSON 파싱 실패: " + e.getMessage(), e);
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
