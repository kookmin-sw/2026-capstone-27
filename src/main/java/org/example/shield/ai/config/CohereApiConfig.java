package org.example.shield.ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import lombok.Getter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * Cohere Chat API v2 클라이언트 설정.
 * base-url: https://api.cohere.com  (엔드포인트는 클라이언트에서 "/v2/chat")
 */
@Configuration
@Getter
public class CohereApiConfig {

    @Value("${cohere.api-key}")
    private String apiKey;

    @Value("${cohere.base-url:https://api.cohere.com}")
    private String baseUrl;

    @Value("${cohere.model.chat:command-a-03-2025}")
    private String chatModel;

    @Value("${cohere.model.brief:command-a-03-2025}")
    private String briefModel;

    @Value("${cohere.timeout.connect:5000}")
    private int connectTimeout;

    @Value("${cohere.timeout.read-chat:30000}")
    private int chatReadTimeout;

    @Value("${cohere.timeout.read-brief:60000}")
    private int briefReadTimeout;

    @Value("${cohere.chat.max-history-messages:20}")
    private int maxHistoryMessages;

    @Value("${cohere.classify.model:command-a-03-2025}")
    private String classifyModel;

    @Value("${cohere.classify.temperature:0.1}")
    private double classifyTemperature;

    @Value("${cohere.classify.max-tokens:512}")
    private int classifyMaxTokens;

    @Value("${cohere.timeout.read-classify:15000}")
    private int classifyReadTimeout;

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public WebClient cohereWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout);

        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(2 * 1024 * 1024))  // 2MB
                .build();
    }
}
