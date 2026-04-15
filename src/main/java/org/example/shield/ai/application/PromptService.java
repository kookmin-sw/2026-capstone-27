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
     * 분야별 체크리스트 YAML 로드.
     * primaryField → 파일명 변환: CRIMINAL_LAW → criminal-law
     *
     * @param primaryField 분류 코드 (예: CRIMINAL_LAW)
     * @return YAML 문자열 또는 null (파일 없을 경우)
     */
    public String loadChecklist(String primaryField) {
        String domain = toFileName(primaryField);
        String path = "ai/checklists/" + domain + ".yaml";
        try {
            return loadFile(path);
        } catch (RuntimeException e) {
            log.warn("체크리스트 파일을 찾을 수 없습니다: {}", path);
            return null;
        }
    }

    /**
     * primaryField → 파일명 변환.
     * 예: CRIMINAL_LAW → criminal-law, DEPOSIT_FRAUD → deposit-fraud
     */
    private String toFileName(String primaryField) {
        return primaryField.toLowerCase().replace("_", "-");
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
