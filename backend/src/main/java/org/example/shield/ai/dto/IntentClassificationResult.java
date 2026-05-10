package org.example.shield.ai.dto;

import java.util.List;

/**
 * Layer 1 의도 분류 결과 DTO.
 * LLM이 슬림 온톨로지 기반으로 분류한 사용자 법률 의도.
 */
public record IntentClassificationResult(
        String intentSummary,
        List<MatchedNode> matchedNodes,
        Keywords keywords,
        List<String> retrievalQueries
) {
    public record MatchedNode(String id, String name, double confidence) {}
    public record Keywords(List<String> core, List<String> expanded) {}

    public List<String> matchedNodeIds() {
        return matchedNodes.stream().map(MatchedNode::id).toList();
    }
}
