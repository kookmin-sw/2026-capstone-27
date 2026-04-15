package org.example.shield.ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
@Getter
public class GrokApiConfig {

    @Value("${xai.api-key}")
    private String apiKey;

    @Value("${xai.base-url:https://api.x.ai}")
    private String baseUrl;

    @Value("${xai.model.chat:grok-4-1-fast-non-reasoning}")
    private String chatModel;

    @Value("${xai.model.brief:grok-4.20-0309-non-reasoning}")
    private String briefModel;

    @Value("${xai.timeout.connect:5000}")
    private int connectTimeout;

    @Value("${xai.timeout.read-chat:30000}")
    private int chatReadTimeout;

    @Value("${xai.timeout.read-brief:60000}")
    private int briefReadTimeout;

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public WebClient grokWebClient() {
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
