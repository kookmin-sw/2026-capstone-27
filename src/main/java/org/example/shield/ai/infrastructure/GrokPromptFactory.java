package org.example.shield.ai.infrastructure;

/**
 * Grok API 프롬프트 팩토리.
 *
 * DEPRECATED: 프롬프트는 파일 기반으로 관리.
 * → PromptService에서 classpath:ai/prompts/ 디렉토리의 .md 파일을 로드.
 * → 체크리스트는 classpath:ai/checklists/ 디렉토리의 .yaml 파일로 관리.
 *
 * 이 클래스는 하위 호환성을 위해 유지하되, 실제 사용하지 않음.
 */
@Deprecated
public class GrokPromptFactory {
}
