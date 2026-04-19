package org.example.shield.ai.application;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.common.enums.MessageRole;
import org.example.shield.consultation.domain.Message;
import org.example.shield.consultation.domain.MessageReader;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 체크리스트 커버리지 검증 서비스 (P0-II, Issue #40 3레벨 커버리지).
 *
 * <p>LLM 의 {@code allCompleted} 신호를 코드 레벨에서 독립 검증한다.
 * {@code coverageRatio = matchedItems / totalItems} 로 계산하고,
 * AND gate: {@code effectiveAllCompleted = LLM_allCompleted && (coverageRatio >= 0.85)}.</p>
 *
 * <p>커버리지 항목 수집은 {@link ChecklistLoader} 가 파싱한 L1 YAML 트리에서
 * 다음을 누적한다:
 * <ul>
 *   <li>L1: {@code l1_checklist.required} + {@code l1_checklist.domain_specific}</li>
 *   <li>L2(주어진 경우): {@code l2_checklists[l2].focus}</li>
 *   <li>L3(주어진 경우): {@code l2_checklists[l2].l3_checklists[l3]}</li>
 * </ul>
 * 각 항목 문자열은 {@link ChecklistTokenizer} 로 토큰화되며,
 * 항목의 토큰 중 하나라도 사용자 메시지 전체 문자열에 포함되면 matched 로 간주한다.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChecklistCoverageService {

    private final MessageReader messageReader;
    private final ChecklistLoader checklistLoader;

    private static final double COVERAGE_THRESHOLD = 0.85;

    /**
     * L1 만 사용하는 커버리지 계산 (기존 호출처 호환).
     */
    public double compute(UUID consultationId, String l1Name) {
        return compute(consultationId, l1Name, null, null);
    }

    /**
     * 3레벨 커버리지 계산. L2/L3 는 null 허용.
     *
     * @param consultationId 상담 ID
     * @param l1Name 온톨로지 L1 한글 이름 (예: "부동산 거래")
     * @param l2Name 온톨로지 L2 한글 이름 또는 null
     * @param l3Name 온톨로지 L3 한글 이름 또는 null (L2 가 null 이면 무시)
     * @return 0.0 ~ 1.0. 지원되지 않는 L1 / 항목이 하나도 수집되지 않으면 0.0
     */
    public double compute(UUID consultationId, String l1Name, String l2Name, String l3Name) {
        if (l1Name == null || l1Name.isBlank()) {
            log.debug("L1 도메인이 없어 커버리지 계산 불가. 0.0 반환");
            return 0.0;
        }

        JsonNode root = checklistLoader.loadAsTree(l1Name);
        if (root == null) {
            // 지원되지 않는 도메인 — 안전하게 0.0 반환 (AND gate 통과 방지)
            return 0.0;
        }

        List<String> items = collectItems(root, l2Name, l3Name);
        if (items.isEmpty()) {
            log.warn("체크리스트 항목이 비어있음: L1={}, L2={}, L3={}", l1Name, l2Name, l3Name);
            return 0.0;
        }

        String haystack = loadUserHaystack(consultationId);
        if (haystack.isBlank()) {
            log.debug("사용자 메시지가 비어있어 커버리지 0.0. consultationId={}", consultationId);
            return 0.0;
        }

        int matched = 0;
        for (String item : items) {
            Set<String> tokens = ChecklistTokenizer.tokensOf(item);
            if (ChecklistTokenizer.anyTokenMatches(tokens, haystack)) {
                matched++;
            }
        }

        double ratio = (double) matched / items.size();
        log.debug("체크리스트 커버리지: L1={}, L2={}, L3={}, matched={}/{}, ratio={}",
                l1Name, l2Name, l3Name, matched, items.size(), ratio);
        return ratio;
    }

    public boolean isEffectivelyCompleted(boolean llmAllCompleted, double coverageRatio) {
        return llmAllCompleted && coverageRatio >= COVERAGE_THRESHOLD;
    }

    public double getThreshold() {
        return COVERAGE_THRESHOLD;
    }

    // ---------- helpers ----------

    /** YAML 트리에서 요청 범위에 해당하는 항목 문자열 전부 수집. */
    private List<String> collectItems(JsonNode root, String l2Name, String l3Name) {
        List<String> items = new ArrayList<>();

        // L1: required + domain_specific
        JsonNode l1 = root.path("l1_checklist");
        addAllStrings(items, l1.path("required"));
        addAllStrings(items, l1.path("domain_specific"));

        if (l2Name == null || l2Name.isBlank()) {
            return items;
        }
        JsonNode l2Node = root.path("l2_checklists").path(l2Name);
        if (l2Node.isMissingNode() || !l2Node.isObject()) {
            log.debug("L2 노드 없음: {}", l2Name);
            return items;
        }
        addAllStrings(items, l2Node.path("focus"));

        if (l3Name == null || l3Name.isBlank()) {
            return items;
        }
        JsonNode l3Items = l2Node.path("l3_checklists").path(l3Name);
        if (l3Items.isMissingNode() || !l3Items.isArray()) {
            log.debug("L3 항목 없음: L2={}, L3={}", l2Name, l3Name);
            return items;
        }
        addAllStrings(items, l3Items);

        return items;
    }

    private void addAllStrings(List<String> dst, JsonNode arr) {
        if (arr == null || !arr.isArray()) return;
        for (JsonNode n : arr) {
            if (n.isTextual()) {
                String s = n.asText();
                if (!s.isBlank()) dst.add(s);
            }
        }
    }

    /** 해당 상담의 모든 USER 메시지를 NFC+소문자로 합친 문자열. */
    private String loadUserHaystack(UUID consultationId) {
        List<Message> messages = messageReader.findAllByConsultationId(consultationId);
        StringBuilder sb = new StringBuilder();
        for (Message m : messages) {
            if (m.getRole() == MessageRole.USER && m.getContent() != null) {
                sb.append(' ').append(m.getContent());
            }
        }
        return ChecklistTokenizer.normalizeForMatch(sb.toString());
    }
}
