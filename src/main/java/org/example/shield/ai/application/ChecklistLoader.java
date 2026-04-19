package org.example.shield.ai.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * L1 체크리스트 YAML 파싱 유틸 (Issue #40 A 후속).
 *
 * <p>{@link ChecklistSlugMap} 을 통해 한글 L1 이름 → slug 매핑 후
 * {@code classpath:ai/checklists/<slug>.yaml} 을 Jackson YAMLMapper 로 로드한다.</p>
 *
 * <p>이 클래스는 {@link ChecklistCoverageService} 가 YAML 트리를 순회해
 * 체크리스트 항목을 수집할 때 사용된다. 순수 문자열 로드만 필요한 경우
 * {@link PromptService#loadChecklist(String)} 을 계속 사용하면 된다.</p>
 */
@Component
@Slf4j
public class ChecklistLoader {

    private static final String CHECKLIST_DIR = "ai/checklists/";

    private final YAMLMapper yamlMapper = new YAMLMapper();

    /**
     * L1 한글 이름으로 YAML 을 파싱해 루트 JsonNode 반환.
     *
     * @param l1Name 온톨로지 L1 한글 이름 (예: "부동산 거래")
     * @return YAML 루트 노드, 또는 slug 매핑 실패/파일 없음/파싱 오류 시 null
     */
    public JsonNode loadAsTree(String l1Name) {
        String slug = ChecklistSlugMap.slugFor(l1Name);
        if (slug == null) {
            log.warn("지원하지 않는 L1 분야입니다: {}", l1Name);
            return null;
        }
        String path = CHECKLIST_DIR + slug + ".yaml";
        ClassPathResource res = new ClassPathResource(path);
        if (!res.exists()) {
            log.warn("체크리스트 파일을 찾을 수 없습니다: {} (L1={})", path, l1Name);
            return null;
        }
        try (InputStream in = res.getInputStream()) {
            return yamlMapper.readTree(in);
        } catch (IOException e) {
            log.error("체크리스트 YAML 파싱 실패: {}", path, e);
            return null;
        }
    }
}
