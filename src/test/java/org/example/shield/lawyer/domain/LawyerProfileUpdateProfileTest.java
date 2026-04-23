package org.example.shield.lawyer.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Issue #61 — LawyerProfile.updateProfile 의 부분 업데이트 (null 유지) 동작 검증.
 */
class LawyerProfileUpdateProfileTest {

    private LawyerProfile newProfile() {
        return LawyerProfile.builder()
                .userId(UUID.randomUUID())
                .barAssociationNumber("KBA-2020-12345")
                .domains(List.of("임대차보호"))
                .subDomains(List.of("주택임대차보호"))
                .experienceYears(8)
                .tags(List.of("보증금 반환"))
                .bio("서울대 법학전문대학원 졸업")
                .certifications(List.of("변호사"))
                .region("서울")
                .build();
    }

    @Test
    @DisplayName("bio 만 전달하면 experienceYears 는 기존 값이 유지된다")
    void partialUpdate_keepExperienceYears_whenOnlyBioProvided() {
        LawyerProfile profile = newProfile();

        profile.updateProfile(null, null, null, null, null, "새 자기소개", null);

        assertThat(profile.getBio()).isEqualTo("새 자기소개");
        assertThat(profile.getExperienceYears()).isEqualTo(8);
        assertThat(profile.getRegion()).isEqualTo("서울");
        assertThat(profile.getDomains()).containsExactly("임대차보호");
    }

    @Test
    @DisplayName("experienceYears 만 전달하면 bio 는 기존 값이 유지된다")
    void partialUpdate_keepBio_whenOnlyExperienceYearsProvided() {
        LawyerProfile profile = newProfile();

        profile.updateProfile(null, null, 10, null, null, null, null);

        assertThat(profile.getExperienceYears()).isEqualTo(10);
        assertThat(profile.getBio()).isEqualTo("서울대 법학전문대학원 졸업");
    }

    @Test
    @DisplayName("모든 필드를 전달하면 전체가 갱신된다")
    void fullUpdate_overridesAllFields() {
        LawyerProfile profile = newProfile();

        profile.updateProfile(
                List.of("이혼·위자료·재산분할"),
                List.of("협의이혼"),
                15,
                List.of("변호사", "세무사"),
                List.of("재산분할"),
                "갱신된 소개",
                "경기 성남"
        );

        assertThat(profile.getDomains()).containsExactly("이혼·위자료·재산분할");
        assertThat(profile.getSubDomains()).containsExactly("협의이혼");
        assertThat(profile.getExperienceYears()).isEqualTo(15);
        assertThat(profile.getCertifications()).containsExactly("변호사", "세무사");
        assertThat(profile.getTags()).containsExactly("재산분할");
        assertThat(profile.getBio()).isEqualTo("갱신된 소개");
        assertThat(profile.getRegion()).isEqualTo("경기 성남");
    }

    @Test
    @DisplayName("모든 인자가 null 이면 아무 필드도 변경되지 않는다")
    void noUpdate_whenAllArgsAreNull() {
        LawyerProfile profile = newProfile();

        profile.updateProfile(null, null, null, null, null, null, null);

        assertThat(profile.getDomains()).containsExactly("임대차보호");
        assertThat(profile.getSubDomains()).containsExactly("주택임대차보호");
        assertThat(profile.getExperienceYears()).isEqualTo(8);
        assertThat(profile.getCertifications()).containsExactly("변호사");
        assertThat(profile.getTags()).containsExactly("보증금 반환");
        assertThat(profile.getBio()).isEqualTo("서울대 법학전문대학원 졸업");
        assertThat(profile.getRegion()).isEqualTo("서울");
    }

    @Test
    @DisplayName("빈 리스트를 전달하면 명시적 초기화로 적용된다 (null 과 구분)")
    void partialUpdate_emptyListIsExplicitReset() {
        LawyerProfile profile = newProfile();

        profile.updateProfile(null, null, null, List.of(), null, null, null);

        assertThat(profile.getCertifications()).isEmpty();
        assertThat(profile.getDomains()).containsExactly("임대차보호"); // null 인 필드는 그대로
    }
}
