package org.example.shield.ai.application;

import org.example.shield.common.enums.MessageRole;
import org.example.shield.consultation.domain.Message;
import org.example.shield.consultation.domain.MessageReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * {@link ChecklistCoverageService} 단위 테스트 (Issue #40 A 후속 2).
 *
 * <p>YAML 리소스는 실제 프로덕션 파일을 그대로 읽는다.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChecklistCoverageServiceTest {

    @Mock
    private MessageReader messageReader;

    private ChecklistCoverageService service;

    @BeforeEach
    void setUp() {
        service = new ChecklistCoverageService(messageReader, new ChecklistLoader());
    }

    // ----- L1 기본 케이스 -----

    @Test
    @DisplayName("L1 '부동산 거래' — 모든 공통 항목 언급 시 >= 0.85")
    void realEstateFullCoverage() {
        UUID cid = UUID.randomUUID();
        // real-estate.yaml l1_checklist.required/domain_specific 항목을 전반적으로 언급
        List<Message> messages = List.of(
                userMsg(cid, "상대방은 집주인(임대인)이고 연락이 가능합니다. 개인 관계로 친분은 없습니다."),
                userMsg(cid, "분쟁 시기는 2024년 6월 발생일입니다. 사건은 그때 시작됐어요."),
                userMsg(cid, "관련 금액은 보증금 5000만원이고 피해액 포함 거래가 명시되어 있습니다."),
                userMsg(cid, "관련 문서는 계약서와 등기부 존재합니다. 특약 사항도 포함됐어요."),
                userMsg(cid, "증거 자료로 사진, 녹음, 이체내역, 문자, 이메일 전부 가지고 있습니다."),
                userMsg(cid, "기존 조치 이력은 내용증명 보냈고 소송까지 검토 중입니다. 신고는 안 했습니다."),
                userMsg(cid, "원하는 결과는 보증금 반환과 손해 배상 이행입니다."),
                userMsg(cid, "부동산 종류는 아파트이고 거래 유형은 임대차입니다. 소재지는 서울입니다.")
        );
        given(messageReader.findAllByConsultationId(any())).willReturn(messages);

        double ratio = service.compute(cid, "부동산 거래");
        assertThat(ratio).isGreaterThanOrEqualTo(0.85);
    }

    @Test
    @DisplayName("L1 '부동산 거래' — 빈 메시지 → 0.0")
    void emptyMessagesYieldZero() {
        UUID cid = UUID.randomUUID();
        given(messageReader.findAllByConsultationId(any())).willReturn(List.of());

        double ratio = service.compute(cid, "부동산 거래");
        assertThat(ratio).isEqualTo(0.0);
    }

    @Test
    @DisplayName("L1 null/blank → 0.0 (메시지 로드 없음)")
    void nullL1Zero() {
        assertThat(service.compute(UUID.randomUUID(), null)).isEqualTo(0.0);
        assertThat(service.compute(UUID.randomUUID(), "")).isEqualTo(0.0);
        assertThat(service.compute(UUID.randomUUID(), "  ")).isEqualTo(0.0);
    }

    @Test
    @DisplayName("미지원 L1 (구 enum 코드) → 0.0 + AND gate 통과 차단")
    void legacyEnumReturnsZero() {
        UUID cid = UUID.randomUUID();
        given(messageReader.findAllByConsultationId(any())).willReturn(
                List.of(userMsg(cid, "아무 텍스트")));

        assertThat(service.compute(cid, "CRIMINAL_LAW")).isEqualTo(0.0);
        assertThat(service.compute(cid, "CIVIL_LAW")).isEqualTo(0.0);
        assertThat(service.compute(cid, "DEPOSIT_FRAUD")).isEqualTo(0.0);
    }

    // ----- 3레벨 확장 -----

    @Test
    @DisplayName("L1+L2 — L2 focus 항목 포함으로 분모 증가")
    void l1PlusL2IncreasesDenominator() {
        UUID cid = UUID.randomUUID();
        given(messageReader.findAllByConsultationId(any())).willReturn(
                List.of(userMsg(cid, "상대방 집주인 연락 가능, 사건 발생일, 보증금 금액, 계약서 문서, 사진 녹음 증거, 내용증명 이력, 보증금 반환 결과, 아파트 임대차 서울")));

        double l1Only = service.compute(cid, "부동산 거래");
        double withL2 = service.compute(cid, "부동산 거래", "부동산 임대차", null);

        // L2 추가 시 분모가 커져서 (새 항목이 매칭 안 되면) ratio 가 낮아지거나 같음
        assertThat(withL2).isLessThanOrEqualTo(l1Only);
        assertThat(withL2).isBetween(0.0, 1.0);
    }

    @Test
    @DisplayName("L1+L2+L3 — 존재하지 않는 L2 는 무시하고 L1 기준으로 계산")
    void unknownL2IsIgnored() {
        UUID cid = UUID.randomUUID();
        given(messageReader.findAllByConsultationId(any())).willReturn(
                List.of(userMsg(cid, "상대방 집주인 사건 발생일 보증금 금액 계약서 문서 사진 증거 내용증명 결과 반환 아파트 임대차 서울")));

        double l1Only = service.compute(cid, "부동산 거래");
        double withUnknownL2 = service.compute(cid, "부동산 거래", "존재하지않는L2", null);

        // 존재하지 않는 L2 는 무시 → L1 과 동일해야 함
        assertThat(withUnknownL2).isEqualTo(l1Only);
    }

    // ----- AND gate -----

    @Test
    @DisplayName("AND gate — allCompleted true + 커버리지 >= 0.85 → true")
    void andGatePass() {
        assertThat(service.isEffectivelyCompleted(true, 0.90)).isTrue();
        assertThat(service.isEffectivelyCompleted(true, 0.85)).isTrue();
    }

    @Test
    @DisplayName("AND gate — allCompleted true + 커버리지 < 0.85 → false")
    void andGateFail() {
        assertThat(service.isEffectivelyCompleted(true, 0.80)).isFalse();
        assertThat(service.isEffectivelyCompleted(true, 0.0)).isFalse();
    }

    @Test
    @DisplayName("AND gate — allCompleted false → false (커버리지 무관)")
    void andGateAllCompletedFalse() {
        assertThat(service.isEffectivelyCompleted(false, 1.0)).isFalse();
    }

    @Test
    @DisplayName("getThreshold — 0.85")
    void thresholdIs085() {
        assertThat(service.getThreshold()).isEqualTo(0.85);
    }

    // ----- helpers -----

    private Message userMsg(UUID consultationId, String content) {
        return Message.builder()
                .consultationId(consultationId)
                .role(MessageRole.USER)
                .content(content)
                .build();
    }
}
