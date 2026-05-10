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

    /**
     * 이미 수집된 체크리스트 항목을 마크다운 체크박스 요약으로 반환.
     *
     * <p>LLM 이 대화 히스토리에서 답변된 내용을 스스로 파싱하지 못해 동일 질문을
     * 재생성하는 문제를 완화하기 위해 system 프롬프트에 사전 계산된 상태를 주입한다.
     * 매칭 규칙은 {@link #compute} 와 동일한 토큰 기반 휴리스틱이라 false positive/negative
     * 가 발생할 수 있지만, AND gate 와 달리 여기서는 '질문 생략 힌트'로만 쓰이므로
     * 약간의 오차는 허용된다.</p>
     *
     * @param l1Name L1 한글 이름 (null/blank → 빈 문자열 반환)
     * @param l2Name L2 한글 이름 (null 허용)
     * @param l3Name L3 한글 이름 (null 허용; L2 가 null 이면 무시)
     * @param chatHistory DB 중복 조회 방지를 위해 호출자가 전달한 메시지 목록
     * @return "## 이미 수집된 항목..." 형식 마크다운, 항목 없으면 빈 문자열
     */
    public String buildCollectedSummary(
            String l1Name, String l2Name, String l3Name, List<Message> chatHistory) {
        if (l1Name == null || l1Name.isBlank()) return "";
        JsonNode root = checklistLoader.loadAsTree(l1Name);
        if (root == null) return "";

        List<String> items = collectItems(root, l2Name, l3Name);
        if (items.isEmpty()) return "";

        String haystack = buildHaystackFromHistory(chatHistory);

        StringBuilder sb = new StringBuilder("## 이미 수집된 항목 (재질문 금지)\n");
        int matched = 0;
        int firstUncheckedIdx = -1;
        for (int i = 0; i < items.size(); i++) {
            String item = items.get(i);
            Set<String> tokens = ChecklistTokenizer.tokensOf(item);
            boolean hit = !haystack.isEmpty() && ChecklistTokenizer.anyTokenMatches(tokens, haystack);
            sb.append(hit ? "- [x] " : "- [ ] ").append(item).append('\n');
            if (hit) {
                matched++;
            } else if (firstUncheckedIdx < 0) {
                firstUncheckedIdx = i;
            }
        }
        sb.append('\n');
        sb.append("위 `[x]` 항목은 이미 답변됐습니다. 같은 정보를 다시 묻지 마세요. ");
        if (firstUncheckedIdx >= 0) {
            sb.append("`[ ]` 항목 중 가장 중요한 것 하나만 다음 질문으로 던지세요.");
        } else {
            sb.append("모든 항목이 수집됐으니 마무리 단계로 전환하세요.");
        }
        log.debug("collected summary: matched={}/{}, L1={}, L2={}, L3={}",
                matched, items.size(), l1Name, l2Name, l3Name);
        return sb.toString();
    }

    /**
     * 의뢰서 생성용 — 대화에서 수집되지 못한 체크리스트 항목을 추론 가이드 블록으로.
     *
     * <p>{@link #buildCollectedSummary} 와 동일한 토큰 휴리스틱을 쓰되 unmatched 항목만
     * 모아 brief 프롬프트에 힌트로 주입한다. 10턴 상한으로 상담이 조기 종료됐을 때
     * LLM 이 대화 근거를 바탕으로 합리적 추론을 수행하도록 유도하는 용도다.</p>
     *
     * @param l1Name L1 한글 이름 (null/blank → 빈 문자열)
     * @param l2Name L2 한글 이름 (null 허용)
     * @param l3Name L3 한글 이름 (null 허용; L2 가 null 이면 무시)
     * @param chatHistory 현재 상담 메시지 목록
     * @return "## 미수집 슬롯..." 마크다운, 누락 항목이 없거나 L1 미확정이면 빈 문자열
     */
    public String buildMissingSlotsGuidance(
            String l1Name, String l2Name, String l3Name, List<Message> chatHistory) {
        if (l1Name == null || l1Name.isBlank()) return "";
        JsonNode root = checklistLoader.loadAsTree(l1Name);
        if (root == null) return "";

        List<String> items = collectItems(root, l2Name, l3Name);
        if (items.isEmpty()) return "";

        String haystack = buildHaystackFromHistory(chatHistory);

        List<String> missing = new ArrayList<>();
        for (String item : items) {
            Set<String> tokens = ChecklistTokenizer.tokensOf(item);
            boolean hit = !haystack.isEmpty() && ChecklistTokenizer.anyTokenMatches(tokens, haystack);
            if (!hit) missing.add(item);
        }
        if (missing.isEmpty()) return "";

        StringBuilder sb = new StringBuilder("## 미수집 슬롯 (대화 내용으로 추론 필요)\n");
        for (String m : missing) {
            sb.append("- ").append(m).append('\n');
        }
        sb.append('\n');
        sb.append("위 항목은 체크리스트 필수 슬롯이지만 대화에서 명시적으로 답변되지 않았습니다. ");
        sb.append("대화 문맥에 근거가 있다면 합리적으로 추론해 채우고, 근거가 불충분하면 \"미확인\"으로 명시하세요. ");
        sb.append("근거 없이 새 사실을 만들어내지 마세요.");

        log.debug("missing slots: {}/{}, L1={}, L2={}, L3={}",
                missing.size(), items.size(), l1Name, l2Name, l3Name);
        return sb.toString();
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
        return buildHaystackFromHistory(messages);
    }

    /** 주어진 메시지 목록의 USER 턴만 모아 NFC+소문자로 정규화한 haystack 반환. */
    private String buildHaystackFromHistory(List<Message> messages) {
        if (messages == null || messages.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Message m : messages) {
            if (m.getRole() == MessageRole.USER && m.getContent() != null) {
                sb.append(' ').append(m.getContent());
            }
        }
        return ChecklistTokenizer.normalizeForMatch(sb.toString());
    }
}
