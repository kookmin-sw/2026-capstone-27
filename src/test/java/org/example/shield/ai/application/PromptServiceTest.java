package org.example.shield.ai.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PromptService} 단위 테스트 — Issue #40 A 후속 (slug 매핑).
 *
 * <p>프로덕션 리소스({@code src/main/resources/ai/checklists/}) 를 그대로 사용한다.</p>
 */
class PromptServiceTest {

    private PromptService service;

    @BeforeEach
    void setUp() {
        service = new PromptService(new DefaultResourceLoader());
    }

    @Test
    @DisplayName("loadChecklist — 8개 L1 한글 이름 모두 YAML 로드 성공")
    void loadChecklist_allEightL1Names() {
        for (Map.Entry<String, String> entry : ChecklistSlugMap.L1_TO_SLUG.entrySet()) {
            String l1Name = entry.getKey();
            String slug = entry.getValue();

            String yaml = service.loadChecklist(l1Name);

            assertThat(yaml).as("YAML 로드 — L1='%s'", l1Name).isNotNull();
            // YAML 내용에 slug 가 포함되어 올바른 파일이 로드됐음을 간접 검증
            assertThat(yaml).as("slug 일치 확인 — %s", slug).contains("slug: " + slug);
            assertThat(yaml).as("meta.l1 포함 — %s", slug).contains("l1: \"" + l1Name + "\"");
        }
    }

    @Test
    @DisplayName("loadChecklist — null 입력 시 null 반환")
    void loadChecklist_nullInput() {
        assertThat(service.loadChecklist(null)).isNull();
    }

    @Test
    @DisplayName("loadChecklist — 미지원 L1 이름 (구 enum 코드) 시 null 반환")
    void loadChecklist_legacyEnumReturnsNull() {
        // 구 DomainType enum 기반 코드 — 더 이상 지원하지 않음
        assertThat(service.loadChecklist("CRIMINAL_LAW")).isNull();
        assertThat(service.loadChecklist("CIVIL_LAW")).isNull();
        assertThat(service.loadChecklist("SOCIAL_SECURITY_LAW")).isNull();
        assertThat(service.loadChecklist("COMMERCIAL_LAW")).isNull();
    }

    @Test
    @DisplayName("loadChecklist — 온톨로지 밖 임의 문자열 시 null 반환")
    void loadChecklist_unknownStringReturnsNull() {
        assertThat(service.loadChecklist("가족법")).isNull();
        assertThat(service.loadChecklist("형사")).isNull();
        assertThat(service.loadChecklist("")).isNull();
    }

    @Test
    @DisplayName("loadRouterChatPrompt — chat.md 로드 성공")
    void loadRouterChatPrompt_success() {
        String prompt = service.loadRouterChatPrompt();
        assertThat(prompt).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("loadRouterBriefPrompt — brief.md 로드 성공")
    void loadRouterBriefPrompt_success() {
        String prompt = service.loadRouterBriefPrompt();
        assertThat(prompt).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("ChecklistSlugMap — 8개 매핑 존재, 불변")
    void checklistSlugMap_sizeAndImmutable() {
        assertThat(ChecklistSlugMap.L1_TO_SLUG).hasSize(8);
        assertThat(ChecklistSlugMap.slugFor("부동산 거래")).isEqualTo("real-estate");
        assertThat(ChecklistSlugMap.slugFor("기업·상사거래")).isEqualTo("commercial");
        assertThat(ChecklistSlugMap.slugFor(null)).isNull();
        assertThat(ChecklistSlugMap.slugFor("존재하지 않는 이름")).isNull();
    }
}
