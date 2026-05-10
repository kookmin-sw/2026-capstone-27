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

    /**
     * 역인덱스: {@code law_id}(예: "LSI249999") → 해당 법령이 primary/secondary로
     * 등록된 온톨로지 노드 ID 목록. Phase C-2 특별법 인제스트가 LSI 기준으로
     * category_ids를 주입할 때 사용한다.
     */
    private final Map<String, List<String>> lawIdToNodeIds = new HashMap<>();

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
                mapping.setCategoryIds(parseCategoryIds(value.get("category_ids")));

                mappingCache.put(categoryId, mapping);

                // 역인덱스 구축: law_id → [node_id,...]
                for (LawRef ref : mapping.getPrimaryLawIds()) {
                    indexByLawId(ref.getLawId(), categoryId);
                }
                for (LawRef ref : mapping.getSecondaryLawIds()) {
                    indexByLawId(ref.getLawId(), categoryId);
                }
            }

            log.info("카테고리-법령 매핑 로드 완료: {}개 카테고리, 역인덱스 LSI {}건",
                    mappingCache.size(), lawIdToNodeIds.size());

        } catch (IOException e) {
            log.error("category-law-mappings.yml 로드 실패", e);
            throw new RuntimeException("카테고리-법령 매핑 로드 실패", e);
        }
    }

    /**
     * 분류된 카테고리 노드 ID 목록 → 매핑된 {@code legal_chunks.category_ids} 토큰 목록으로 변환.
     *
     * <p>B-8a에서 도입. 온톨로지 노드(예: {@code law-001-02})는 의미 네임스페이스가
     * DB의 {@code category_ids}({@code group:jeonse} 등)와 상이하므로,
     * {@code category-law-mappings.yml}에 명시적으로 등록된 매핑을 치환한다.
     * 매핑이 없는 노드는 결과에 포함되지 않는다 (작아지면 전체 필터 미적용 동등).</p>
     *
     * <p>L3 노드(예: {@code law-001-02-01})도 {@link #resolveLawIds} 동일 방식으로
     * L2 부모로 폴백한다.</p>
     *
     * @param categoryNodeIds 온톨로지 카테고리 노드 ID 목록
     * @return 중복 제거된 category_ids 토큰 목록. null/empty면 빈 리스트
     */
    public List<String> resolveCategoryIds(List<String> categoryNodeIds) {
        if (categoryNodeIds == null || categoryNodeIds.isEmpty()) {
            return List.of();
        }
        Set<String> tokens = new LinkedHashSet<>();
        for (String nodeId : categoryNodeIds) {
            CategoryLawMapping mapping = resolveWithL3Fallback(nodeId);
            if (mapping == null || mapping.getCategoryIds() == null) {
                continue;
            }
            tokens.addAll(mapping.getCategoryIds());
        }
        return new ArrayList<>(tokens);
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

    /**
     * 역방향 조회: LSI(법제처 법령ID) → 해당 법령이 등록된 모든 온톨로지 노드 ID + 노드 category_ids 토큰.
     *
     * <p>Phase C-2 특별법 인제스트용. 시드 파일 단위로 LSI를 주면 해당 법령의
     * {@code legal_chunks.category_ids} 열에 넣을 토큰 목록을 돌려준다.
     * 포함 규칙:</p>
     * <ul>
     *   <li>해당 LSI가 primary 또는 secondary로 등록된 노드 ID 전부 (예: "law-007-01")</li>
     *   <li>위 노드에 선언된 {@code category_ids} 토큰 전부 (예: "group:leasing")</li>
     * </ul>
     *
     * <p>{@code lsiOrLawId}는 "LSI249999" 접두 포함 형식 또는 숫자만("249999") 모두 허용.</p>
     *
     * @param lsiOrLawId "LSI249999" 또는 "249999"
     * @return 중복 제거된 카테고리 토큰 목록. 매핑 없으면 빈 리스트
     */
    public List<String> resolveCategoriesByLsi(String lsiOrLawId) {
        if (lsiOrLawId == null || lsiOrLawId.isBlank()) return List.of();
        String key = lsiOrLawId.startsWith("LSI") ? lsiOrLawId : "LSI" + lsiOrLawId;

        List<String> nodeIds = lawIdToNodeIds.get(key);
        if (nodeIds == null || nodeIds.isEmpty()) {
            return List.of();
        }

        Set<String> out = new LinkedHashSet<>();
        for (String nodeId : nodeIds) {
            out.add(nodeId);
            CategoryLawMapping m = mappingCache.get(nodeId);
            if (m != null && m.getCategoryIds() != null) {
                out.addAll(m.getCategoryIds());
            }
        }
        return new ArrayList<>(out);
    }

    private void indexByLawId(String lawId, String nodeId) {
        if (lawId == null || lawId.isBlank() || "EXTERNAL".equals(lawId)) return;
        List<String> list = lawIdToNodeIds.computeIfAbsent(lawId, k -> new ArrayList<>());
        if (!list.contains(nodeId)) list.add(nodeId);
    }

    /**
     * L3 노드(하이픈 3개 이상) 요청 시 정확 매칭 후 없으면 L2 부모로 폴백.
     * {@link #resolveLawIds}와 동일 규칙을 {@link #resolveCategoryIds}에도 적용하기 위해 추출.
     */
    private CategoryLawMapping resolveWithL3Fallback(String nodeId) {
        CategoryLawMapping mapping = mappingCache.get(nodeId);
        if (mapping == null && nodeId != null && nodeId.chars().filter(ch -> ch == '-').count() > 2) {
            String parentId = nodeId.substring(0, nodeId.lastIndexOf('-'));
            mapping = mappingCache.get(parentId);
            if (mapping != null) {
                log.debug("L3 노드 {} → L2 부모 {} 매핑으로 폴백", nodeId, parentId);
            }
        }
        return mapping;
    }

    @SuppressWarnings("unchecked")
    private List<String> parseCategoryIds(Object raw) {
        if (raw == null) return List.of();
        if (!(raw instanceof List<?> list)) return List.of();
        List<String> out = new ArrayList<>(list.size());
        for (Object item : list) {
            if (item instanceof String s && !s.isBlank()) {
                out.add(s);
            }
        }
        return out;
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
