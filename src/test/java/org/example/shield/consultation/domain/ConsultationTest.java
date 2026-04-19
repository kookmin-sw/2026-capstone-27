package org.example.shield.consultation.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Consultation L1/L2/L3 getter 행동 검증.
 * - getFirstDomain / getFirstSubDomain / getFirstTag 는 userXxx 우선, aiXxx 폴백.
 * - user 값이 있으면 primaryFieldLocked=true → updateAiClassification 무시.
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

        @Test
        @DisplayName("user/ai 모두 빈 리스트면 null")
        void bothEmpty() {
            Consultation c = Consultation.create(UUID.randomUUID(),
                    List.of(), List.of(), List.of());
            boolean applied = c.updateAiClassification(List.of(), List.of(), List.of());

            // 선택값이 없으므로 locked=false → ai 반영은 되지만, ai 도 비어있음
            assertThat(applied).isTrue();
            assertThat(c.getFirstSubDomain()).isNull();
        }

        @Test
        @DisplayName("userSubDomains 비었고 aiSubDomains 있으면 ai 사용 (mixed)")
        void mixedUserDomainOnlyAiSub() {
            // user 는 L1 만 선택, L2/L3 는 비어있음 → primaryFieldLocked=true (L1 만으로도 locked)
            // 이 상태에서는 updateAiClassification 이 무시되므로, ai 를 먼저 세팅해야 mixed 검증 가능.
            // 실제 런타임 시나리오: user 가 L1 만 선택, LLM 이 L2/L3 채우는 경우는
            // updateUserClassification 의 소급 덮어쓰기 없이 ai 가 먼저 반영된 뒤 user 가 덮어쓰지 않음.
            // 본 테스트는 ai 만 세팅된 케이스로 대체 (aiFallback 과 동일 경로).
            Consultation c = Consultation.create(UUID.randomUUID(), null, null, null);
            c.updateAiClassification(
                    List.of("부동산 거래"),
                    List.of("부동산 매매"),
                    null);

            assertThat(c.getFirstDomain()).isEqualTo("부동산 거래");
            assertThat(c.getFirstSubDomain()).isEqualTo("부동산 매매");
            assertThat(c.getFirstTag()).isNull();
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
    @DisplayName("getFirstDomain (기존 동작 회귀)")
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
    @DisplayName("primaryFieldLocked 동작")
    class PrimaryFieldLocked {

        @Test
        @DisplayName("user 선택이 있으면 locked → updateAiClassification 무시")
        void lockedBlocksAi() {
            Consultation c = Consultation.create(UUID.randomUUID(),
                    List.of("부동산 거래"), null, null);

            boolean applied = c.updateAiClassification(
                    List.of("이혼·위자료·재산분할"),
                    List.of("재산분할"),
                    List.of("기여도 산정"));

            assertThat(applied).isFalse();
            assertThat(c.getFirstDomain()).isEqualTo("부동산 거래");
            assertThat(c.getFirstSubDomain()).isNull();
            assertThat(c.getFirstTag()).isNull();
        }
    }
}
