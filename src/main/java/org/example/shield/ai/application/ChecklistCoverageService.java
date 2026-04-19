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
 * coverageRatio = matched / total 을 계산.
 *
 * AND gate: effectiveAllCompleted = LLM_allCompleted && (coverageRatio >= 0.85)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChecklistCoverageService {

    private final MessageReader messageReader;

    private static final double COVERAGE_THRESHOLD = 0.85;

    /**
     * 온톨로지 L1 대분류별 필수 키워드 매핑.
     */
    private static final Map<String, List<List<String>>> DOMAIN_CHECKLIST = Map.of(
            "부동산 거래", List.of(
                    List.of("매매", "임대차", "전세", "보증금", "부동산", "아파트", "주택", "토지", "건물"),
                    List.of("계약", "계약서", "특약"),
                    List.of("금액", "얼마", "비용", "보증금"),
                    List.of("상대방", "집주인", "임대인", "매도인", "매수인"),
                    List.of("시기", "언제", "만료", "기간"),
                    List.of("증거", "사진", "계약서", "등기부", "문자", "카톡"),
                    List.of("원하는", "결과", "해결", "반환", "이전")
            ),
            "이혼·위자료·재산분할", List.of(
                    List.of("이혼", "별거", "혼인", "결혼"),
                    List.of("사유", "이유", "원인", "외도", "폭행", "부정"),
                    List.of("재산", "분할", "부동산", "예금", "자산"),
                    List.of("위자료", "배상", "금액"),
                    List.of("자녀", "양육", "친권", "아이"),
                    List.of("증거", "자료", "사진", "녹음"),
                    List.of("원하는", "결과", "합의", "조정")
            ),
            "상속·유류분·유언", List.of(
                    List.of("상속", "유산", "사망", "돌아가"),
                    List.of("상속인", "형제", "자녀", "배우자", "관계"),
                    List.of("재산", "부동산", "예금", "금액"),
                    List.of("유언", "유언장", "공증"),
                    List.of("유류분", "몫", "비율"),
                    List.of("분쟁", "다툼", "갈등"),
                    List.of("원하는", "결과", "분할")
            ),
            "근로계약·해고·임금", List.of(
                    List.of("해고", "임금", "월급", "퇴직", "근로", "직장", "회사"),
                    List.of("근무", "기간", "입사", "퇴사"),
                    List.of("고용", "형태", "정규직", "계약직"),
                    List.of("급여", "월급", "시급", "얼마"),
                    List.of("근로계약서", "계약서"),
                    List.of("경위", "어떻게", "사건", "사유"),
                    List.of("증거", "자료", "문자", "녹음"),
                    List.of("원하는", "결과", "복직", "배상")
            ),
            "손해배상·불법행위", List.of(
                    List.of("사고", "교통", "의료", "폭행", "피해", "손해"),
                    List.of("일시", "언제", "날짜", "시간"),
                    List.of("장소", "어디서", "어디에서"),
                    List.of("피해", "피해 내용", "다친", "손해", "금액"),
                    List.of("가해자", "상대방", "관계"),
                    List.of("신고", "경찰", "병원", "진단서"),
                    List.of("증거", "CCTV", "진단서", "녹음", "사진"),
                    List.of("원하는", "결과", "배상", "처벌", "합의")
            ),
            "채무·보증·개인파산·회생", List.of(
                    List.of("빚", "채무", "대출", "보증", "파산", "회생"),
                    List.of("금액", "얼마", "원금", "이자"),
                    List.of("채권자", "상대방", "은행", "사채"),
                    List.of("기간", "언제", "만기"),
                    List.of("계약서", "차용증", "문서"),
                    List.of("상환", "변제", "독촉"),
                    List.of("원하는", "결과", "면책", "탕감")
            ),
            "임대차보호", List.of(
                    List.of("임대차", "전세", "월세", "보증금", "임차"),
                    List.of("계약", "갱신", "만료", "기간"),
                    List.of("금액", "보증금", "차임", "월세"),
                    List.of("집주인", "임대인", "관계"),
                    List.of("대항력", "확정일자", "전입신고"),
                    List.of("증거", "계약서", "이체내역", "문자"),
                    List.of("원하는", "결과", "반환", "갱신", "명도")
            ),
            "기업·상사거래", List.of(
                    List.of("계약", "거래", "납품", "공급", "회사", "법인"),
                    List.of("상대방", "거래처", "업체"),
                    List.of("금액", "대금", "미지급"),
                    List.of("계약서", "문서", "발주서"),
                    List.of("경위", "위반", "불이행"),
                    List.of("증거", "자료", "이메일"),
                    List.of("원하는", "결과", "배상", "해지")
            )
    );

    /**
     * 체크리스트 커버리지 계산.
     *
     * @param consultationId 상담 ID
     * @param domain         분류된 법률 분야 (온톨로지 L1 대분류명)
     * @return coverageRatio (0.0 ~ 1.0)
     */
    public double compute(UUID consultationId, String domain) {
        if (domain == null || domain.isBlank()) {
            log.debug("도메인이 없어 커버리지 계산 불가. 기본 0.0 반환");
            return 0.0;
        }

        List<List<String>> checklist = DOMAIN_CHECKLIST.get(domain);

        if (checklist == null || checklist.isEmpty()) {
            log.warn("체크리스트가 정의되지 않은 분야: {}", domain);
            return 1.0;
        }

        List<Message> messages = messageReader.findAllByConsultationId(consultationId);
        String allUserText = messages.stream()
                .filter(m -> m.getRole() == MessageRole.USER)
                .map(Message::getContent)
                .map(content -> Normalizer.normalize(content, Normalizer.Form.NFC))
                .reduce("", (a, b) -> a + " " + b)
                .toLowerCase();

        int matched = 0;
        for (List<String> keywordGroup : checklist) {
            boolean found = keywordGroup.stream()
                    .anyMatch(kw -> allUserText.contains(kw.toLowerCase()));
            if (found) matched++;
        }

        double ratio = (double) matched / checklist.size();
        log.debug("체크리스트 커버리지: domain={}, matched={}/{}, ratio={}",
                domain, matched, checklist.size(), ratio);
        return ratio;
    }

    public boolean isEffectivelyCompleted(boolean llmAllCompleted, double coverageRatio) {
        return llmAllCompleted && coverageRatio >= COVERAGE_THRESHOLD;
    }

    public double getThreshold() {
        return COVERAGE_THRESHOLD;
    }
}
