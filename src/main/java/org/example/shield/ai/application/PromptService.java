package org.example.shield.ai.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 시스템 프롬프트 파일 로드 서비스.
 * classpath:ai/prompts/ 디렉토리에서 .md 파일로 관리.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PromptService {

    private final ResourceLoader resourceLoader;

    /**
     * Phase 1 대화/분류 프롬프트 로드 (router/chat.md).
     * 항상 이 프롬프트를 사용하며, 분류 후 체크리스트를 동적 주입.
     */
    public String loadRouterChatPrompt() {
        return loadFile("ai/prompts/router/chat.md");
    }

    /**
     * Phase 2 의뢰서 생성 프롬프트 로드 (router/brief.md).
     */
    public String loadRouterBriefPrompt() {
        return loadFile("ai/prompts/router/brief.md");
    }

    /**
     * L1 분야 체크리스트 YAML 로드 (Issue #40 3레벨 구조).
     *
     * <p>입력은 온톨로지 L1 한글 이름(예: "부동산 거래")이며,
     * {@link ChecklistSlugMap} 을 통해 고정 slug 로 매핑 후 파일을 로드한다.
     * 매핑 불가(한글 L1 이름이 아닌 값 또는 미지원) 시 null 반환.</p>
     *
     * @param l1Name 온톨로지 L1 한글 이름 (예: "부동산 거래", "이혼·위자료·재산분할")
     * @return YAML 문자열 또는 null (slug 매핑 실패 / 파일 없음)
     */
    public String loadChecklist(String l1Name) {
        String slug = ChecklistSlugMap.slugFor(l1Name);
        if (slug == null) {
            log.warn("지원하지 않는 L1 분야입니다: {}", l1Name);
            return null;
        }
        String path = "ai/checklists/" + slug + ".yaml";
        try {
            return loadFile(path);
        } catch (RuntimeException e) {
            log.warn("체크리스트 파일을 찾을 수 없습니다: {} (L1={})", path, l1Name);
            return null;
        }
    }

    private String loadFile(String path) {
        Resource resource = resourceLoader.getResource("classpath:" + path);
        try {
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("프롬프트 파일 로드 실패: {}", path, e);
            throw new RuntimeException("프롬프트 파일 로드 실패: " + path, e);
        }
    }
}
