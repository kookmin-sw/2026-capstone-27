package org.example.shield.ai.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Issue #40 — L1 체크리스트 YAML 3레벨 구조 스키마 검증 테스트.
 *
 * <p>검증 내용:
 * <ol>
 *   <li>8개 L1 YAML 파일이 모두 존재하고 로드 가능</li>
 *   <li>각 YAML의 meta.l1 이 온톨로지 L1 이름과 일치</li>
 *   <li>각 YAML의 l2_checklists 키 = 온톨로지 해당 L1 의 L2 자식 집합 (누락 0 / 여분 0)</li>
 *   <li>각 L2 의 l3_checklists 키 = 온톨로지 해당 L2 의 L3 자식 집합 (누락 0 / 여분 0)</li>
 *   <li>l1_checklist.required / domain_specific 항목 최소 1개 이상</li>
 * </ol>
 */
class ChecklistYamlSchemaTest {

    /** L1 이름 → slug 매핑 (프로덕션 상수 {@link ChecklistSlugMap} 와 동일 소스). */
    private static final Map<String, String> L1_SLUG_MAP = ChecklistSlugMap.L1_TO_SLUG;

    private static final String CHECKLIST_DIR = "ai/checklists/";
    private static final String ONTOLOGY_PATH = "ontology/legal-ontology-slim.json";

    private final YAMLMapper yamlMapper = new YAMLMapper();
    private final ObjectMapper jsonMapper = new ObjectMapper();

    @Test
    @DisplayName("L1 YAML 8개 모두 존재 및 로드 가능")
    void allEightYamlFilesExist() throws Exception {
        for (Map.Entry<String, String> entry : L1_SLUG_MAP.entrySet()) {
            JsonNode root = loadYaml(entry.getValue());
            assertThat(root).as("YAML root for %s", entry.getValue()).isNotNull();
            assertThat(root.has("meta")).as("meta 섹션 — %s", entry.getValue()).isTrue();
            assertThat(root.has("l1_checklist")).as("l1_checklist 섹션 — %s", entry.getValue()).isTrue();
            assertThat(root.has("l2_checklists")).as("l2_checklists 섹션 — %s", entry.getValue()).isTrue();
        }
    }

    @Test
    @DisplayName("meta.l1 이 온톨로지 L1 이름과 일치 / slug 일치")
    void metaMatchesOntologyL1() throws Exception {
        Map<String, JsonNode> ontologyL1 = loadOntologyL1Nodes();
        assertThat(ontologyL1).hasSize(8);

        for (Map.Entry<String, String> entry : L1_SLUG_MAP.entrySet()) {
            String l1Name = entry.getKey();
            String slug = entry.getValue();

            JsonNode root = loadYaml(slug);
            assertThat(root.path("meta").path("l1").asText())
                    .as("meta.l1 — %s", slug).isEqualTo(l1Name);
            assertThat(root.path("meta").path("slug").asText())
                    .as("meta.slug — %s", slug).isEqualTo(slug);
            assertThat(ontologyL1).as("온톨로지에 L1 '%s' 존재", l1Name).containsKey(l1Name);
        }
    }

    @Test
    @DisplayName("l2_checklists 키 집합 = 온톨로지 L2 자식 집합 (누락 0 / 여분 0)")
    void l2KeysMatchOntology() throws Exception {
        Map<String, JsonNode> ontologyL1 = loadOntologyL1Nodes();

        for (Map.Entry<String, String> entry : L1_SLUG_MAP.entrySet()) {
            String l1Name = entry.getKey();
            String slug = entry.getValue();

            JsonNode yamlRoot = loadYaml(slug);
            Set<String> yamlL2Keys = fieldNameSet(yamlRoot.path("l2_checklists"));

            Set<String> ontoL2Names = childNameSet(ontologyL1.get(l1Name));

            assertThat(yamlL2Keys)
                    .as("L2 키 집합 — %s (L1='%s')", slug, l1Name)
                    .containsExactlyInAnyOrderElementsOf(ontoL2Names);
        }
    }

    @Test
    @DisplayName("각 L2 의 l3_checklists 키 집합 = 온톨로지 L3 자식 집합 (누락 0 / 여분 0)")
    void l3KeysMatchOntology() throws Exception {
        Map<String, JsonNode> ontologyL1 = loadOntologyL1Nodes();

        for (Map.Entry<String, String> entry : L1_SLUG_MAP.entrySet()) {
            String l1Name = entry.getKey();
            String slug = entry.getValue();

            JsonNode yamlRoot = loadYaml(slug);
            JsonNode l2Checklists = yamlRoot.path("l2_checklists");

            // L1 노드의 L2 자식들 맵 구성
            Map<String, JsonNode> ontoL2Map = new HashMap<>();
            JsonNode l1Node = ontologyL1.get(l1Name);
            for (JsonNode l2 : l1Node.path("c")) {
                ontoL2Map.put(l2.path("name").asText(), l2);
            }

            Iterator<String> l2Names = l2Checklists.fieldNames();
            while (l2Names.hasNext()) {
                String l2Name = l2Names.next();
                JsonNode l2Body = l2Checklists.path(l2Name);
                JsonNode l3Block = l2Body.path("l3_checklists");

                Set<String> yamlL3Keys = fieldNameSet(l3Block);
                Set<String> ontoL3Names = childNameSet(ontoL2Map.get(l2Name));

                assertThat(yamlL3Keys)
                        .as("L3 키 집합 — %s > %s", slug, l2Name)
                        .containsExactlyInAnyOrderElementsOf(ontoL3Names);
            }
        }
    }

    @Test
    @DisplayName("l1_checklist.required / domain_specific 최소 1개 항목")
    void l1ChecklistNonEmpty() throws Exception {
        for (String slug : L1_SLUG_MAP.values()) {
            JsonNode yamlRoot = loadYaml(slug);
            JsonNode l1 = yamlRoot.path("l1_checklist");

            assertThat(l1.path("required").isArray())
                    .as("required 배열 — %s", slug).isTrue();
            assertThat(l1.path("required").size())
                    .as("required 항목 수 — %s", slug).isGreaterThan(0);

            assertThat(l1.path("domain_specific").isArray())
                    .as("domain_specific 배열 — %s", slug).isTrue();
            assertThat(l1.path("domain_specific").size())
                    .as("domain_specific 항목 수 — %s", slug).isGreaterThan(0);
        }
    }

    @Test
    @DisplayName("각 L3 체크리스트 항목 배열은 비어있지 않음")
    void l3ChecklistItemsNonEmpty() throws Exception {
        for (Map.Entry<String, String> entry : L1_SLUG_MAP.entrySet()) {
            String slug = entry.getValue();
            JsonNode yamlRoot = loadYaml(slug);
            JsonNode l2Checklists = yamlRoot.path("l2_checklists");

            Iterator<String> l2Names = l2Checklists.fieldNames();
            while (l2Names.hasNext()) {
                String l2Name = l2Names.next();
                JsonNode l3Block = l2Checklists.path(l2Name).path("l3_checklists");
                Iterator<String> l3Names = l3Block.fieldNames();
                while (l3Names.hasNext()) {
                    String l3Name = l3Names.next();
                    JsonNode items = l3Block.path(l3Name);
                    assertThat(items.isArray())
                            .as("L3 항목 배열 — %s > %s > %s", slug, l2Name, l3Name).isTrue();
                    assertThat(items.size())
                            .as("L3 항목 수 — %s > %s > %s", slug, l2Name, l3Name)
                            .isGreaterThan(0);
                }
            }
        }
    }

    // ---------- helpers ----------

    private JsonNode loadYaml(String slug) throws Exception {
        ClassPathResource res = new ClassPathResource(CHECKLIST_DIR + slug + ".yaml");
        assertThat(res.exists()).as("YAML 파일 존재 — %s.yaml", slug).isTrue();
        try (InputStream in = res.getInputStream()) {
            return yamlMapper.readTree(in);
        }
    }

    /** 온톨로지 JSON 을 로드하여 L1 이름 → L1 노드 맵을 반환. */
    private Map<String, JsonNode> loadOntologyL1Nodes() throws Exception {
        ClassPathResource res = new ClassPathResource(ONTOLOGY_PATH);
        try (InputStream in = res.getInputStream()) {
            JsonNode root = jsonMapper.readTree(in);
            Map<String, JsonNode> map = new LinkedHashMap<>();
            for (JsonNode l1 : root.path("c")) {
                map.put(l1.path("name").asText(), l1);
            }
            return map;
        }
    }

    private Set<String> fieldNameSet(JsonNode node) {
        Set<String> set = new HashSet<>();
        if (node == null || node.isMissingNode() || !node.isObject()) {
            return set;
        }
        Iterator<String> it = node.fieldNames();
        while (it.hasNext()) {
            set.add(it.next());
        }
        return set;
    }

    private Set<String> childNameSet(JsonNode parent) {
        Set<String> set = new HashSet<>();
        if (parent == null || parent.isMissingNode()) {
            return set;
        }
        List<String> names = new ArrayList<>();
        for (JsonNode child : parent.path("c")) {
            names.add(child.path("name").asText());
        }
        set.addAll(names);
        return set;
    }
}
