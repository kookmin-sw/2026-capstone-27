package org.example.shield.ai.dto;

/**
 * 판례(대법원·하급심) 검색 결과 DTO.
 *
 * <p>Phase C-5 (Issue #42) — {@code legal_cases} 하이브리드 검색이 반환하는 단위.
 * {@link LegalChunk} 가 법령 조문을 담는 것과 대비된다. 두 타입은 {@link RetrievedDocument}
 * sealed interface 로 묶여 RAG 컨텍스트 빌더가 함께 다룬다.</p>
 *
 * <p>필드:
 * <ul>
 *   <li>{@code caseNo} — 사건번호 (예: "2025다213466")</li>
 *   <li>{@code court} — 법원 (예: "대법원")</li>
 *   <li>{@code caseName} — 사건명 (없을 수 있음)</li>
 *   <li>{@code decisionDate} — 선고일자 (YYYY-MM-DD)</li>
 *   <li>{@code caseType} — 민사/형사/가사/행정/특허</li>
 *   <li>{@code headnote} — 판시사항 (쟁점·요점)</li>
 *   <li>{@code holding} — 판결요지 (법리 결론)</li>
 *   <li>{@code sourceUrl} — 출처 URL (법제처 등)</li>
 *   <li>{@code score} — 하이브리드 점수 (높을수록 관련성 ↑)</li>
 * </ul>
 * </p>
 */
public record Precedent(
        String caseNo,
        String court,
        String caseName,
        String decisionDate,
        String caseType,
        String headnote,
        String holding,
        String sourceUrl,
        double score
) implements RetrievedDocument {

    @Override
    public String kind() {
        return "case";
    }
}
