package org.example.shield.ai.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.common.enums.MessageRole;
import org.example.shield.consultation.domain.Message;
import org.example.shield.consultation.domain.MessageReader;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 체크리스트 커버리지 검증 서비스 (P0-II).
 *
 * LLM의 allCompleted 신호를 코드 레벨에서 독립 검증.
 * checklists/{domain}.yaml 필수 항목을 chatHistory에서 키워드 매칭하여
 * coverageRatio = matched / total 을 계산.
 *
 * AND gate: effectiveAllCompleted = LLM_allCompleted && (coverageRatio >= 0.85)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChecklistCoverageService {

    private final MessageReader messageReader;

    /** 커버리지 임계값 — Sprint 2.5에서 조정 가능 */
    private static final double COVERAGE_THRESHOLD = 0.85;

    /**
     * 분야별 필수 키워드 매핑.
     * 각 키워드 그룹 중 하나라도 chatHistory에 존재하면 해당 항목 수집됨으로 판단.
     */
    private static final Map<String, List<List<String>>> DOMAIN_CHECKLIST = Map.of(
            "CRIMINAL_LAW", List.of(
                    List.of("폭행", "사기", "명예훼손", "절도", "사건 유형", "어떤 사건"),
                    List.of("일시", "언제", "날짜", "시간"),
                    List.of("장소", "어디서", "어디에서"),
                    List.of("피해", "피해 내용", "다친", "손해"),
                    List.of("가해자", "상대방", "관계"),
                    List.of("특정", "이름", "연락처", "신원"),
                    List.of("신고", "경찰", "사건번호"),
                    List.of("증거", "CCTV", "진단서", "녹음"),
                    List.of("원하는", "결과", "처벌", "합의", "배상")
            ),
            "CIVIL_LAW", List.of(
                    List.of("분쟁", "유형", "어떤 문제"),
                    List.of("상대방", "누구", "개인", "법인"),
                    List.of("발생", "시기", "언제"),
                    List.of("금액", "얼마", "비용"),
                    List.of("계약서", "문서", "서류"),
                    List.of("법적 조치", "소송", "고소"),
                    List.of("원하는", "결과", "해결")
            ),
            "COMMERCIAL_LAW", List.of(
                    List.of("분쟁", "유형", "어떤 문제"),
                    List.of("회사", "법인", "사업"),
                    List.of("상대방", "누구"),
                    List.of("발생", "시기", "언제"),
                    List.of("금액", "얼마"),
                    List.of("계약서", "문서"),
                    List.of("원하는", "결과")
            ),
            "SOCIAL_SECURITY_LAW", List.of(
                    List.of("유형", "해고", "임금", "산재", "연금"),
                    List.of("근무", "기간", "입사", "퇴사"),
                    List.of("고용", "형태", "정규직", "계약직"),
                    List.of("급여", "월급", "시급", "얼마"),
                    List.of("근로계약서", "계약서"),
                    List.of("경위", "어떻게", "사건"),
                    List.of("증거", "자료"),
                    List.of("원하는", "결과")
            )
    );

    /**
     * 체크리스트 커버리지 계산.
     *
     * @param consultationId 상담 ID
     * @param primaryField   분류된 법률 분야 코드
     * @return coverageRatio (0.0 ~ 1.0)
     */
    public double compute(UUID consultationId, String primaryField) {
        if (primaryField == null || primaryField.isBlank()) {
            log.debug("primaryField가 없어 커버리지 계산 불가. 기본 0.0 반환");
            return 0.0;
        }

        // 상위 분야 매핑 (하위 유형 → 상위 분야)
        String topDomain = mapToTopDomain(primaryField);
        List<List<String>> checklist = DOMAIN_CHECKLIST.get(topDomain);

        if (checklist == null || checklist.isEmpty()) {
            log.warn("체크리스트가 정의되지 않은 분야: {}", topDomain);
            return 1.0;  // 체크리스트 없으면 통과
        }

        // 전체 사용자 메시지 수집
        List<Message> messages = messageReader.findAllByConsultationId(consultationId);
        String allUserText = messages.stream()
                .filter(m -> m.getRole() == MessageRole.USER)
                .map(Message::getContent)
                .map(content -> Normalizer.normalize(content, Normalizer.Form.NFC))
                .reduce("", (a, b) -> a + " " + b)
                .toLowerCase();

        // 키워드 매칭
        int matched = 0;
        for (List<String> keywordGroup : checklist) {
            boolean found = keywordGroup.stream()
                    .anyMatch(kw -> allUserText.contains(kw.toLowerCase()));
            if (found) matched++;
        }

        double ratio = (double) matched / checklist.size();
        log.debug("체크리스트 커버리지: domain={}, matched={}/{}, ratio={}",
                topDomain, matched, checklist.size(), ratio);
        return ratio;
    }

    /**
     * AND gate: LLM allCompleted && coverageRatio >= threshold
     */
    public boolean isEffectivelyCompleted(boolean llmAllCompleted, double coverageRatio) {
        return llmAllCompleted && coverageRatio >= COVERAGE_THRESHOLD;
    }

    public double getThreshold() {
        return COVERAGE_THRESHOLD;
    }

    /**
     * 하위 유형 코드를 상위 분야로 매핑.
     * DEPOSIT_FRAUD → CIVIL_LAW, UNFAIR_DISMISSAL → SOCIAL_SECURITY_LAW 등
     */
    private String mapToTopDomain(String primaryField) {
        return switch (primaryField) {
            // 형법 하위 유형
            case "CRIMINAL_LAW" -> "CRIMINAL_LAW";

            // 민법 하위 유형
            case "CIVIL_LAW", "DEPOSIT_FRAUD", "CONTRACT_DISPUTE",
                 "DAMAGES", "INHERITANCE" -> "CIVIL_LAW";

            // 상법 하위 유형
            case "COMMERCIAL_LAW", "CORPORATE_DISPUTE",
                 "INSURANCE", "COMMERCIAL_TRANSACTION" -> "COMMERCIAL_LAW";

            // 사회보장법 하위 유형
            case "SOCIAL_SECURITY_LAW", "UNFAIR_DISMISSAL", "WAGE_THEFT",
                 "INDUSTRIAL_ACCIDENT", "PENSION" -> "SOCIAL_SECURITY_LAW";

            default -> {
                log.warn("알 수 없는 primaryField: {}", primaryField);
                yield primaryField;
            }
        };
    }
}
