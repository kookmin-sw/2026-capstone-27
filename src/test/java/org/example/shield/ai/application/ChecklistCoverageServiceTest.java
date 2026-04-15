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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ChecklistCoverageServiceTest {

    @Mock
    private MessageReader messageReader;

    private ChecklistCoverageService service;

    @BeforeEach
    void setUp() {
        service = new ChecklistCoverageService(messageReader);
    }

    @Test
    @DisplayName("형법 체크리스트 — 모든 항목 언급 시 높은 커버리지")
    void criminalLawFullCoverage() {
        UUID consultationId = UUID.randomUUID();
        List<Message> messages = List.of(
                createUserMessage(consultationId, "사기를 당했습니다. 2024년 3월 15일에 발생했어요."),
                createUserMessage(consultationId, "서울 강남구 사무실에서 피해를 봤습니다. 재산적 피해가 3000만원입니다."),
                createUserMessage(consultationId, "가해자는 지인이고, 이름과 연락처 알고 있습니다."),
                createUserMessage(consultationId, "경찰에 신고했고 사건번호 받았습니다. 계약서 증거 있어요."),
                createUserMessage(consultationId, "처벌받게 하고 손해배상도 원합니다.")
        );
        given(messageReader.findAllByConsultationId(any())).willReturn(messages);

        double ratio = service.compute(consultationId, "CRIMINAL_LAW");
        assertThat(ratio).isGreaterThanOrEqualTo(0.85);
    }

    @Test
    @DisplayName("primaryField null이면 0.0 반환")
    void nullPrimaryField() {
        double ratio = service.compute(UUID.randomUUID(), null);
        assertThat(ratio).isEqualTo(0.0);
    }

    @Test
    @DisplayName("AND gate — allCompleted true + 커버리지 >= 0.85 → true")
    void andGatePass() {
        assertThat(service.isEffectivelyCompleted(true, 0.90)).isTrue();
    }

    @Test
    @DisplayName("AND gate — allCompleted true + 커버리지 < 0.85 → false")
    void andGateFail() {
        assertThat(service.isEffectivelyCompleted(true, 0.50)).isFalse();
    }

    @Test
    @DisplayName("AND gate — allCompleted false → false (커버리지 무관)")
    void andGateAllCompletedFalse() {
        assertThat(service.isEffectivelyCompleted(false, 1.0)).isFalse();
    }

    @Test
    @DisplayName("하위 유형 DEPOSIT_FRAUD → CIVIL_LAW 매핑")
    void subTypeMapping() {
        UUID consultationId = UUID.randomUUID();
        List<Message> messages = List.of(
                createUserMessage(consultationId, "보증금 분쟁이에요. 상대방은 임대인이에요."),
                createUserMessage(consultationId, "작년에 발생했고 5000만원 문제입니다."),
                createUserMessage(consultationId, "계약서 있고, 고소 한 적 없어요. 보증금 돌려받고 싶습니다.")
        );
        given(messageReader.findAllByConsultationId(any())).willReturn(messages);

        double ratio = service.compute(consultationId, "DEPOSIT_FRAUD");
        assertThat(ratio).isGreaterThan(0.0);
    }

    private Message createUserMessage(UUID consultationId, String content) {
        return Message.builder()
                .consultationId(consultationId)
                .role(MessageRole.USER)
                .content(content)
                .build();
    }
}
