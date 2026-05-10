package org.example.shield.ai.application;

import org.example.shield.ai.dto.LegalChunk;
import org.example.shield.ai.dto.MixedRetrievalResult;
import org.example.shield.ai.dto.Precedent;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Layer 3: RAG 컨텍스트 빌더.
 * 검색된 법률 조문 청크 (및 C-5 이후 판례) 를 시스템 프롬프트에 삽입할 문자열로 포맷한다.
 */
@Component
public class RagContextBuilder {

    /**
     * LegalChunk 목록을 프롬프트 컨텍스트 문자열로 변환 (법령 전용, 기존 호환).
     *
     * @param chunks        검색된 법률 조문 청크 목록
     * @param intentSummary 의도 분류 요약 (메타 정보로 포함)
     * @return 포맷된 컨텍스트 문자열 (빈 chunks면 빈 문자열)
     */
    public String build(List<LegalChunk> chunks, String intentSummary) {
        if (chunks == null || chunks.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        appendLawsSection(sb, chunks, intentSummary, /*includeCasesHeader*/ false);
        return sb.toString();
    }

    /**
     * 법령 + 판례 병합 결과를 프롬프트 컨텍스트 문자열로 변환 (C-5, Issue #42).
     *
     * <p>출력 구조:
     * <ol>
     *   <li>{@code ## 참고 법령} — 법령 조문 (없으면 해당 섹션 생략)</li>
     *   <li>{@code ## 참고 판례} — 대법원/하급심 판례 (없으면 해당 섹션 생략)</li>
     * </ol>
     * 두 섹션 모두 비어 있으면 빈 문자열을 반환한다.</p>
     *
     * @param mixed         법령·판례 병합 검색 결과
     * @param intentSummary 의도 분류 요약
     */
    public String build(MixedRetrievalResult mixed, String intentSummary) {
        if (mixed == null || mixed.isEmpty()) {
            return "";
        }

        List<LegalChunk> laws = mixed.laws();
        List<Precedent> cases = mixed.cases();

        StringBuilder sb = new StringBuilder();
        boolean hasCases = cases != null && !cases.isEmpty();

        if (laws != null && !laws.isEmpty()) {
            appendLawsSection(sb, laws, intentSummary, hasCases);
        }

        if (hasCases) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            appendCasesSection(sb, cases);
        }

        return sb.toString();
    }

    private void appendLawsSection(StringBuilder sb,
                                   List<LegalChunk> chunks,
                                   String intentSummary,
                                   boolean casesFollow) {
        sb.append("## 참고 법령 (출처는 반드시 응답에 인용할 것)\n\n");
        sb.append("분류: ").append(intentSummary).append("\n\n");
        sb.append("다음은 본 사건과 관련된 현행 법령 조문입니다. 응답 시 이 정보를 우선 참고하고,\n");
        sb.append("인용 시 반드시 법령명과 조항 번호를 함께 표시하세요.\n");
        if (casesFollow) {
            sb.append("아래 '참고 판례' 섹션은 법리 해석 근거로만 활용하고, 조문 내용과 명확히 구분해 인용하세요.\n");
        }
        sb.append("법령에 없는 내용은 추측하지 말고 \"관련 법령에서 확인할 수 없습니다\"라고 답하세요.\n");

        for (int i = 0; i < chunks.size(); i++) {
            LegalChunk chunk = chunks.get(i);
            sb.append("\n---\n");
            sb.append("[").append(i + 1).append("] ");
            sb.append(chunk.lawName()).append(" ").append(chunk.articleNo());
            sb.append(" (").append(chunk.articleTitle()).append(")\n");
            sb.append("시행일: ").append(chunk.effectiveDate());
            if (chunk.sourceUrl() != null && !chunk.sourceUrl().isEmpty()) {
                sb.append(" / 출처: ").append(chunk.sourceUrl());
            }
            sb.append("\n\n");
            sb.append(chunk.content()).append("\n");
        }
    }

    private void appendCasesSection(StringBuilder sb, List<Precedent> cases) {
        sb.append("## 참고 판례 (법리 해석 근거, 인용 시 사건번호·선고일 함께 표시)\n\n");
        sb.append("다음은 본 사건과 유사한 쟁점의 판례입니다. 법령 조문과 구분해 인용하고,\n");
        sb.append("판례의 사실관계가 본 사건과 다를 경우 그 차이를 명시하세요.\n");

        for (int i = 0; i < cases.size(); i++) {
            Precedent p = cases.get(i);
            sb.append("\n---\n");
            sb.append("[").append(i + 1).append("] ");
            // 예: [대법원 2025다213466 · 2025-03-15]
            sb.append("[").append(nz(p.court())).append(" ").append(nz(p.caseNo()));
            if (p.decisionDate() != null && !p.decisionDate().isEmpty()) {
                sb.append(" · ").append(p.decisionDate());
            }
            sb.append("]");
            if (p.caseName() != null && !p.caseName().isEmpty()) {
                sb.append(" ").append(p.caseName());
            }
            sb.append("\n");
            if (p.caseType() != null && !p.caseType().isEmpty()) {
                sb.append("유형: ").append(p.caseType());
            }
            if (p.sourceUrl() != null && !p.sourceUrl().isEmpty()) {
                if (p.caseType() != null && !p.caseType().isEmpty()) {
                    sb.append(" / ");
                }
                sb.append("출처: ").append(p.sourceUrl());
            }
            sb.append("\n\n");
            if (p.headnote() != null && !p.headnote().isEmpty()) {
                sb.append("판시사항: ").append(p.headnote()).append("\n");
            }
            if (p.holding() != null && !p.holding().isEmpty()) {
                sb.append("판결요지: ").append(p.holding()).append("\n");
            }
        }
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }
}
