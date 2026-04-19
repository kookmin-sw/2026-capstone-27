package org.example.shield.ai.application;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 온톨로지 L1 한글 이름 → 체크리스트 YAML slug 매핑 (Issue #40).
 *
 * <p>지시서({@code SHIELD_AI_bunryucegye_jageobjisiseo.md}) 고정 매핑이며,
 * {@code src/main/resources/ai/checklists/<slug>.yaml} 파일명과 1:1 대응됨.</p>
 *
 * <p>이 상수는 프로덕션 코드와 스키마 검증 테스트가 동일 소스를 참조하도록
 * 공유 상수로 추출되었다.</p>
 */
public final class ChecklistSlugMap {

    /** L1 한글 이름 → slug (삽입 순서 보존). */
    public static final Map<String, String> L1_TO_SLUG;

    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("부동산 거래", "real-estate");
        m.put("이혼·위자료·재산분할", "divorce");
        m.put("상속·유류분·유언", "inheritance");
        m.put("근로계약·해고·임금", "labor");
        m.put("손해배상·불법행위", "damages-tort");
        m.put("채무·보증·개인파산·회생", "debt");
        m.put("임대차보호", "lease-protection");
        m.put("기업·상사거래", "commercial");
        L1_TO_SLUG = Collections.unmodifiableMap(m);
    }

    private ChecklistSlugMap() {
    }

    /**
     * L1 한글 이름 → slug.
     *
     * @param l1Name 온톨로지 L1 한글 이름 (예: "부동산 거래")
     * @return 대응 slug 또는 null (매핑 없음)
     */
    public static String slugFor(String l1Name) {
        if (l1Name == null) return null;
        return L1_TO_SLUG.get(l1Name);
    }
}
