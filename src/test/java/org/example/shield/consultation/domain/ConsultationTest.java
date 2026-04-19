package org.example.shield.consultation.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Consultation 분류 행동 검증 (Issue #48, per-level lock).
 *
 * 핵심 규칙:
 * - getFirstDomain/SubDomain/Tag 는 userXxx 우선, aiXxx 폴백.
 * - updateAiClassification 은 레벨별로 독립: userXxx 가 비어있는 레벨만 aiXxx 가 채움.
 * - 모든 레벨이 user 로 채워지면 AI 입력은 전부 무시되고 false 반환.
 */
class ConsultationTest {

    @Nested
    @DisplayName("getFirstSubDomain")
    class GetFirstSubDomain {

        @Test
        @DisplayName("userSubDomains 가 있으면 그 값을 반환")
        void userPriority() {
            Consultation c = Consultation.create(UUID.randomUUID(),
                    List.of("부동산 거래"),
                    List.of("부동산 매매"),
                    List.of("매매 계약"));

            assertThat(c.getFirstSubDomain()).isEqualTo("부동산 매매");
        }

        @Test
        @DisplayName("user 값이 모두 비어있으면 aiSubDomains 폴백")
        void aiFallback() {
            Consultation c = Consultation.create(UUID.randomUUID(), null, null, null);
            boolean applied = c.updateAiClassification(
                    List.of("부동산 거래"),
                    List.of("부동산 임대차"),
                    List.of("주택임대차"));

            assertThat(applied).isTrue();
            assertThat(c.getFirstSubDomain()).isEqualTo("부동산 임대차");
        }

        @Test
        @DisplayName("user/ai 모두 null 이면 null")
        void bothNull() {
            Consultation c = Consultation.create(UUID.randomUUID(), null, null, null);

            assertThat(c.getFirstSubDomain()).isNull();
        }
    }

    @Nested
    @DisplayName("getFirstTag")
    class GetFirstTag {

        @Test
        @DisplayName("userTags 가 있으면 그 값을 반환")
        void userPriority() {
            Consultation c = Consultation.create(UUID.randomUUID(),
                    List.of("이혼·위자료·재산분할"),
                    List.of("재산분할"),
                    List.of("기여도 산정"));

            assertThat(c.getFirstTag()).isEqualTo("기여도 산정");
        }

        @Test
        @DisplayName("user 값이 모두 비어있으면 aiTags 폴백")
        void aiFallback() {
            Consultation c = Consultation.create(UUID.randomUUID(), null, null, null);
            c.updateAiClassification(
                    List.of("상속·유류분·유언"),
                    List.of("상속"),
                    List.of("법정상속분"));

            assertThat(c.getFirstTag()).isEqualTo("법정상속분");
        }

        @Test
        @DisplayName("user/ai 모두 null 이면 null")
        void bothNull() {
            Consultation c = Consultation.create(UUID.randomUUID(), null, null, null);

            assertThat(c.getFirstTag()).isNull();
        }
    }

    @Nested
    @DisplayName("getFirstDomain")
    class GetFirstDomain {

        @Test
        @DisplayName("userDomains 우선")
        void userPriority() {
            Consultation c = Consultation.create(UUID.randomUUID(),
                    List.of("근로계약·해고·임금"), null, null);

            assertThat(c.getFirstDomain()).isEqualTo("근로계약·해고·임금");
        }

        @Test
        @DisplayName("aiDomains 폴백")
        void aiFallback() {
            Consultation c = Consultation.create(UUID.randomUUID(), null, null, null);
            c.updateAiClassification(List.of("손해배상·불법행위"), null, null);

            assertThat(c.getFirstDomain()).isEqualTo("손해배상·불법행위");
        }
    }

    @Nested
    @DisplayName("updateAiClassification per-level lock (Issue #48)")
    class PerLevelLock {

        @Test
        @DisplayName("L1만 선택: L2/L3 는 AI 가 채움")
        void updateAiClassification_L1만선택_L2L3는AI가채움() {
            Consultation c = Consultation.create(UUID.randomUUID(),
                    List.of("부동산 거래"), null, null);

            boolean applied = c.updateAiClassification(
                    List.of("이혼·위자료·재산분할"),   // L1 은 userDomains 가 있으므로 무시되어야 함
                    List.of("부동산 임대차"),
                    List.of("보증금 및 차임"));

            assertThat(applied).isTrue();
            // L1: user 우선, ai 는 반영 안 됨
            assertThat(c.getUserDomains()).containsExactly("부동산 거래");
            assertThat(c.getAiDomains()).isNull();
            // L2/L3: user 가 비었으므로 ai 가 채움
            assertThat(c.getAiSubDomains()).containsExactly("부동산 임대차");
            assertThat(c.getAiTags()).containsExactly("보증금 및 차임");
        }

        @Test
        @DisplayName("L1/L2/L3 모두 선택: AI 는 전부 무시")
        void updateAiClassification_L1L2L3모두선택_AI무시() {
            Consultation c = Consultation.create(UUID.randomUUID(),
                    List.of("부동산 거래"),
                    List.of("부동산 매매"),
                    List.of("매매 계약"));

            boolean applied = c.updateAiClassification(
                    List.of("이혼·위자료·재산분할"),
                    List.of("재산분할"),
                    List.of("기여도 산정"));

            assertThat(applied).isFalse();
            assertThat(c.getAiDomains()).isNull();
            assertThat(c.getAiSubDomains()).isNull();
            assertThat(c.getAiTags()).isNull();
            // user 값은 그대로 유지
            assertThat(c.getFirstDomain()).isEqualTo("부동산 거래");
            assertThat(c.getFirstSubDomain()).isEqualTo("부동산 매매");
            assertThat(c.getFirstTag()).isEqualTo("매매 계약");
        }

        @Test
        @DisplayName("L1/L2 선택 + L3 만 AI: L3 만 반영")
        void L1L2선택_L3만AI() {
            Consultation c = Consultation.create(UUID.randomUUID(),
                    List.of("부동산 거래"),
                    List.of("부동산 임대차"),
                    null);

            boolean applied = c.updateAiClassification(
                    List.of("이혼·위자료·재산분할"),
                    List.of("재산분할"),
                    List.of("보증금 및 차임"));

            assertThat(applied).isTrue();
            assertThat(c.getAiDomains()).isNull();
            assertThat(c.getAiSubDomains()).isNull();
            assertThat(c.getAiTags()).containsExactly("보증금 및 차임");
            assertThat(c.getFirstTag()).isEqualTo("보증금 및 차임");
        }
    }
}
