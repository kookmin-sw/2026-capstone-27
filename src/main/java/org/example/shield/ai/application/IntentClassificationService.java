package org.example.shield.ai.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.ai.dto.AiCallResult;
import org.example.shield.ai.dto.CohereChatRequest;
import org.example.shield.ai.dto.IntentClassificationResult;
import org.example.shield.ai.dto.IntentClassificationResult.Keywords;
import org.example.shield.ai.dto.IntentClassificationResult.MatchedNode;
import org.example.shield.consultation.domain.Message;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Layer 1: 의도 분류 서비스.
 * 슬림 온톨로지 JSON + 최근 대화 내역을 LLM에 전달하여 법률 의도를 분류.
 */
@Service
@Slf4j
public class IntentClassificationService {

    private final CohereService cohereService;
    private final ObjectMapper objectMapper;
    private final String slimOntologyJson;
    private final ResourceLoader resourceLoader;
    private final int contextWindowMessages;

    private String intentClassifierPromptTemplate;

    public IntentClassificationService(
            CohereService cohereService,
            ObjectMapper objectMapper,
            @Qualifier("slimOntologyJson") String slimOntologyJson,
            ResourceLoader resourceLoader,
            @Value("${cohere.classify.context-window-messages:6}") int contextWindowMessages) {
        this.cohereService = cohereService;
        this.objectMapper = objectMapper;
        this.slimOntologyJson = slimOntologyJson;
        this.resourceLoader = resourceLoader;
        this.contextWindowMessages = contextWindowMessages;
    }

    @PostConstruct
    void loadPromptTemplate() {
        try {
            this.intentClassifierPromptTemplate = StreamUtils.copyToString(
                    resourceLoader.getResource("classpath:ai/prompts/rag/intent-classifier.md")
                            .getInputStream(),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("의도 분류 프롬프트 로드 실패", e);
        }
    }

    /**
     * 대화 내역을 분석하여 법률 의도를 분류.
     *
     * @param recentMessages 최근 메시지 목록 (최대 3턴)
     * @param domain         현재 상담 대분류 (폴백용)
     * @return IntentClassificationResult
     */
    public IntentClassificationResult classify(List<Message> recentMessages, String domain) {
        try {
            String promptTemplate = intentClassifierPromptTemplate;
            String conversationHistory = buildConversationHistory(recentMessages);

            String systemPrompt = promptTemplate
                    .replace("{ONTOLOGY_JSON}", slimOntologyJson)
                    .replace("{CONVERSATION_HISTORY}", conversationHistory);

            List<CohereChatRequest.Message> messages = List.of(
                    CohereChatRequest.Message.system(systemPrompt),
                    CohereChatRequest.Message.user("위 대화 내역을 분석하여 법률 의도를 JSON으로 분류해주세요.")
            );

            AiCallResult<String> result = cohereService.callClassify(messages);
            return parseClassificationResult(result.data());

        } catch (Exception e) {
            log.warn("의도 분류 실패, 폴백 적용: domain={}, error={}", domain, e.getMessage());
            return createFallbackResult(domain);
        }
    }

    /**
     * 프롬프트 조립용 — 테스트에서 접근 가능하도록 패키지 접근 수준.
     */
    String buildSystemPrompt(List<Message> recentMessages) {
        String promptTemplate = intentClassifierPromptTemplate;
        String conversationHistory = buildConversationHistory(recentMessages);
        return promptTemplate
                .replace("{ONTOLOGY_JSON}", slimOntologyJson)
                .replace("{CONVERSATION_HISTORY}", conversationHistory);
    }

    private String buildConversationHistory(List<Message> messages) {
        StringBuilder sb = new StringBuilder();
        int start = Math.max(0, messages.size() - contextWindowMessages);
        for (int i = start; i < messages.size(); i++) {
            Message msg = messages.get(i);
            String role = switch (msg.getRole()) {
                case USER -> "사용자";
                case CHATBOT -> "상담봇";
                default -> null;
            };
            if (role == null) continue;
            sb.append(role).append(": ").append(msg.getContent()).append("\n");
        }
        return sb.toString().trim();
    }

    IntentClassificationResult parseClassificationResult(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);

            String intentSummary = root.path("intent_summary").asText("");

            List<MatchedNode> matchedNodes = new ArrayList<>();
            JsonNode nodesNode = root.path("matched_nodes");
            if (nodesNode.isArray()) {
                for (JsonNode node : nodesNode) {
                    matchedNodes.add(new MatchedNode(
                            node.path("id").asText(),
                            node.path("name").asText(),
                            node.path("confidence").asDouble(0.0)
                    ));
                }
            }

            List<String> coreKeywords = new ArrayList<>();
            JsonNode coreNode = root.path("keywords").path("core");
            if (coreNode.isArray()) {
                coreNode.forEach(n -> coreKeywords.add(n.asText()));
            }

            List<String> expandedKeywords = new ArrayList<>();
            JsonNode expandedNode = root.path("keywords").path("expanded");
            if (expandedNode.isArray()) {
                expandedNode.forEach(n -> expandedKeywords.add(n.asText()));
            }

            List<String> retrievalQueries = new ArrayList<>();
            JsonNode queriesNode = root.path("retrieval_queries");
            if (queriesNode.isArray()) {
                queriesNode.forEach(n -> retrievalQueries.add(n.asText()));
            }

            return new IntentClassificationResult(
                    intentSummary,
                    matchedNodes,
                    new Keywords(coreKeywords, expandedKeywords),
                    retrievalQueries
            );

        } catch (Exception e) {
            log.error("의도 분류 JSON 파싱 실패: {}", e.getMessage());
            throw new RuntimeException("의도 분류 JSON 파싱 실패", e);
        }
    }

    private IntentClassificationResult createFallbackResult(String domain) {
        return new IntentClassificationResult(
                "분류 실패 — 기본 분야 기반 폴백",
                List.of(),
                new Keywords(List.of(), List.of()),
                List.of(domain != null ? domain + " 관련 법률 조항" : "법률 상담")
        );
    }
}
