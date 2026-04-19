package org.example.shield.lawyer.application;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 변호사 임베딩용 텍스트 조립기 (Issue #50).
 *
 * <p>문서(변호사 프로필) 와 쿼리(Brief 분류 결과) 양쪽에 동일한 템플릿을 사용해
 * 같은 벡터 공간에 매핑되도록 보장한다.</p>
 *
 * <pre>
 * [전문 분야]
 * {domains 3회 반복, ". " 구분}
 * [세부 분야]
 * {subDomains 2회 반복, ". " 구분}
 * [태그]
 * {tags 1회, ". " 구분}
 * [자기소개]
 * {bio 전문}
 * </pre>
 *
 * <p>반복 가중치로 domains &gt; subDomains &gt; tags 순 영향력을 준다.
 * null / 빈 리스트는 섹션을 건너뛴다.</p>
 */
@Component
public class LawyerEmbeddingTextBuilder {

    private static final int DOMAINS_REPEAT = 3;
    private static final int SUB_DOMAINS_REPEAT = 2;
    private static final int TAGS_REPEAT = 1;

    /**
     * 변호사 프로필 / 쿼리 공통 텍스트 빌드.
     *
     * @param domains    전문 분야 (가중치 3)
     * @param subDomains 세부 분야 (가중치 2)
     * @param tags       태그 (가중치 1)
     * @param bio        자기소개 (null 허용)
     * @return 임베딩에 태울 조립 텍스트 (섹션이 모두 비면 빈 문자열)
     */
    public String build(List<String> domains,
                        List<String> subDomains,
                        List<String> tags,
                        String bio) {
        StringBuilder sb = new StringBuilder();

        appendSection(sb, "[전문 분야]", domains, DOMAINS_REPEAT);
        appendSection(sb, "[세부 분야]", subDomains, SUB_DOMAINS_REPEAT);
        appendSection(sb, "[태그]", tags, TAGS_REPEAT);

        if (bio != null && !bio.isBlank()) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append("[자기소개]\n").append(bio.trim());
        }

        return sb.toString();
    }

    private void appendSection(StringBuilder sb,
                               String header,
                               List<String> values,
                               int repeat) {
        if (values == null || values.isEmpty()) {
            return;
        }
        List<String> cleaned = values.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .toList();
        if (cleaned.isEmpty()) {
            return;
        }

        if (sb.length() > 0) {
            sb.append('\n');
        }
        sb.append(header).append('\n');

        String joined = String.join(". ", cleaned);
        for (int i = 0; i < repeat; i++) {
            if (i > 0) {
                sb.append(". ");
            }
            sb.append(joined);
        }
    }
}
