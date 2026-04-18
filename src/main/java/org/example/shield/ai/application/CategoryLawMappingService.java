package org.example.shield.ai.application;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.ai.domain.CategoryLawMapping;
import org.example.shield.ai.domain.CategoryLawMapping.LawRef;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 카테고리-법령 매핑 서비스.
 * category-law-mappings.yml을 앱 시작 시 파싱하여 인메모리 Map으로 보유.
 */
@Service
@Slf4j
public class CategoryLawMappingService {

    private final ResourceLoader resourceLoader;
    private final Map<String, CategoryLawMapping> mappingCache = new HashMap<>();

    public CategoryLawMappingService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    @SuppressWarnings("unchecked")
    void loadMappings() {
        try {
            String yamlContent = StreamUtils.copyToString(
                    resourceLoader.getResource("classpath:ontology/category-law-mappings.yml")
                            .getInputStream(),
                    StandardCharsets.UTF_8);

            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(yamlContent);
            Map<String, Object> mappings = (Map<String, Object>) root.get("mappings");

            if (mappings == null) {
                log.warn("category-law-mappings.yml에 mappings 키가 없습니다");
                return;
            }

            for (Map.Entry<String, Object> entry : mappings.entrySet()) {
                String categoryId = entry.getKey();
                Map<String, Object> value = (Map<String, Object>) entry.getValue();

                CategoryLawMapping mapping = new CategoryLawMapping();
                mapping.setCategoryId(categoryId);
                mapping.setPrimaryLawIds(parseLawRefs((List<Map<String, String>>) value.get("primary")));
                mapping.setSecondaryLawIds(parseLawRefs((List<Map<String, String>>) value.get("secondary")));

                mappingCache.put(categoryId, mapping);
            }

            log.info("카테고리-법령 매핑 로드 완료: {}개 카테고리", mappingCache.size());

        } catch (IOException e) {
            log.error("category-law-mappings.yml 로드 실패", e);
            throw new RuntimeException("카테고리-법령 매핑 로드 실패", e);
        }
    }

    /**
     * 분류된 카테고리 노드 ID 목록 → 관련 law_id 목록으로 변환.
     * EXTERNAL law_id는 제외하고 로그 경고만 출력.
     *
     * @param categoryNodeIds 온톨로지 카테고리 노드 ID 목록 (예: ["law-007-01", "law-001-02"])
     * @return 중복 제거된 law_id 목록
     */
    public List<String> resolveLawIds(List<String> categoryNodeIds) {
        Set<String> lawIds = new LinkedHashSet<>();

        for (String nodeId : categoryNodeIds) {
            // 정확한 매칭 시도
            CategoryLawMapping mapping = mappingCache.get(nodeId);

            // 매칭 실패 시 부모 노드를 반복 탐색하여 폴백
            if (mapping == null) {
                String currentId = nodeId;
                while (mapping == null && currentId.contains("-")) {
                    currentId = currentId.substring(0, currentId.lastIndexOf('-'));
                    mapping = mappingCache.get(currentId);
                }
                if (mapping != null) {
                    log.debug("노드 {} → 부모 {} 매핑으로 폴백", nodeId, currentId);
                }
            }

            if (mapping == null) {
                log.warn("카테고리 매핑 없음: {}", nodeId);
                continue;
            }

            addNonExternalLawIds(lawIds, mapping.getPrimaryLawIds());
            addNonExternalLawIds(lawIds, mapping.getSecondaryLawIds());
        }

        return new ArrayList<>(lawIds);
    }

    /**
     * 테스트용 — 로드된 매핑 수 확인.
     */
    int getMappingCount() {
        return mappingCache.size();
    }

    private void addNonExternalLawIds(Set<String> lawIds, List<LawRef> refs) {
        if (refs == null) return;
        for (LawRef ref : refs) {
            if ("EXTERNAL".equals(ref.getLawId())) {
                log.debug("EXTERNAL law_id 제외: {}", ref.getName());
                continue;
            }
            lawIds.add(ref.getLawId());
        }
    }

    private List<LawRef> parseLawRefs(List<Map<String, String>> rawList) {
        if (rawList == null) return List.of();
        List<LawRef> refs = new ArrayList<>();
        for (Map<String, String> raw : rawList) {
            LawRef ref = new LawRef();
            ref.setLawId(raw.get("law_id"));
            ref.setName(raw.get("name"));
            refs.add(ref);
        }
        return refs;
    }
}
