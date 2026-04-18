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

@Configuration
@Getter
public class GroqApiConfig {

    @Value("${groq.api-key}")
    private String apiKey;

    @Value("${groq.base-url:https://api.groq.com/openai}")
    private String baseUrl;

    @Value("${groq.model.chat:llama-3.3-70b-versatile}")
    private String chatModel;

    @Value("${groq.model.brief:llama-3.3-70b-versatile}")
    private String briefModel;

    @Value("${groq.timeout.connect:5000}")
    private int connectTimeout;

    @Value("${groq.timeout.read-chat:30000}")
    private int chatReadTimeout;

    @Value("${groq.timeout.read-brief:60000}")
    private int briefReadTimeout;

    @Value("${groq.chat.max-history-messages:20}")
    private int maxHistoryMessages;

    @Value("${groq.classify.model:llama-3.3-70b-versatile}")
    private String classifyModel;

    @Value("${groq.classify.temperature:0.1}")
    private double classifyTemperature;

    @Value("${groq.classify.max-tokens:512}")
    private int classifyMaxTokens;

    @Value("${groq.timeout.read-classify:15000}")
    private int classifyReadTimeout;

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public WebClient groqWebClient() {
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
