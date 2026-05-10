package org.example.shield.ai.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 법률 온톨로지 트리 조회 서비스 (Issue #48).
 *
 * <p>{@code legal-ontology-slim.json} 을 파싱해 부모 → 직계 자식 name 리스트를
 * 메모리에 보유한다. 앱 기동 시 1회만 구축하고 이후 읽기 전용.</p>
 *
 * <p>용도:
 * <ul>
 *   <li>AI 분류 결과(L2/L3) 구조 검증 — {@link #isChildOf(String, String)}
 *   <li>사용자 선택 L1 의 허용 자식 목록 조회 — {@link #childrenOf(String)}
 * </ul>
 * </p>
 *
 * <p>기존 {@code slimOntologyJson} Bean(OntologyConfig) 을 재사용한다.</p>
 */
@Service
@Slf4j
public class OntologyService {

    private final String slimOntologyJson;
    private final ObjectMapper objectMapper;

    /** 부모 name → 직계 자식 name 리스트 (불변). */
    private Map<String, List<String>> childrenByParentName = Map.of();

    public OntologyService(@Qualifier("slimOntologyJson") String slimOntologyJson,
                           ObjectMapper objectMapper) {
        this.slimOntologyJson = slimOntologyJson;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void loadOntology() {
        try {
            JsonNode root = objectMapper.readTree(slimOntologyJson);
            Map<String, List<String>> map = new HashMap<>();
            walk(root, map);
            this.childrenByParentName = Map.copyOf(map);
            log.info("온톨로지 로드 완료: {}개 부모 노드에 자식 인덱싱", childrenByParentName.size());
        } catch (Exception e) {
            throw new IllegalStateException("온톨로지 JSON 파싱 실패", e);
        }
    }

    private void walk(JsonNode node, Map<String, List<String>> map) {
        if (!node.hasNonNull("c")) return;
        String parentName = node.path("name").asText(null);
        List<String> childNames = new ArrayList<>();
        for (JsonNode child : node.path("c")) {
            String childName = child.path("name").asText(null);
            if (childName != null) childNames.add(childName);
            walk(child, map);
        }
        if (parentName != null && !childNames.isEmpty()) {
            map.put(parentName, List.copyOf(childNames));
        }
    }

    /**
     * {@code childName} 이 {@code parentName} 의 직계 자식인지 검증.
     * 손자 이상 관계는 false.
     */
    public boolean isChildOf(String childName, String parentName) {
        if (childName == null || parentName == null) return false;
        List<String> children = childrenByParentName.get(parentName);
        return children != null && children.contains(childName);
    }

    /**
     * 부모 노드의 직계 자식 name 목록. 없으면 빈 리스트.
     */
    public List<String> childrenOf(String parentName) {
        return childrenByParentName.getOrDefault(parentName, List.of());
    }
}
