package org.example.shield.admin.application;

import org.example.shield.admin.domain.VerificationCheckReader;
import org.example.shield.admin.domain.VerificationLog;
import org.example.shield.admin.domain.VerificationLogReader;
import org.example.shield.admin.domain.VerificationLogWriter;
import org.example.shield.admin.infrastructure.VerificationLogRepository;
import org.example.shield.common.enums.VerificationStatus;
import org.example.shield.common.exception.BusinessException;
import org.example.shield.common.exception.ErrorCode;
import org.example.shield.lawyer.application.LawyerEmbeddingService;
import org.example.shield.lawyer.domain.LawyerProfile;
import org.example.shield.lawyer.domain.LawyerReader;
import org.example.shield.lawyer.domain.LawyerWriter;
import org.example.shield.lawyer.infrastructure.LawyerDocumentRepository;
import org.example.shield.lawyer.infrastructure.LawyerProfileRepository;
import org.example.shield.user.domain.UserReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Issue #64 — AdminService.processVerification 이 이미 최종 처리된 변호사에 대한
 * 재처리를 차단하는지 검증.
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceProcessVerificationTest {

    @Mock
    private LawyerReader lawyerReader;

    @Mock
    private LawyerWriter lawyerWriter;

    @Mock
    private UserReader userReader;

    @Mock
    private LawyerDocumentRepository lawyerDocumentRepository;

    @Mock
    private LawyerProfileRepository lawyerProfileRepository;

    @Mock
    private VerificationLogWriter verificationLogWriter;

    @Mock
    private VerificationLogReader verificationLogReader;

    @Mock
    private VerificationLogRepository verificationLogRepository;

    @Mock
    private VerificationCheckReader verificationCheckReader;

    @Mock
    private LawyerEmbeddingService lawyerEmbeddingService;

    @InjectMocks
    private AdminService adminService;

    private LawyerProfile profileWithStatus(UUID lawyerId, VerificationStatus status) throws Exception {
        LawyerProfile profile = LawyerProfile.builder()
                .userId(UUID.randomUUID())
                .barAssociationNumber("KBA-2020-12345")
                .domains(List.of("임대차보호"))
                .experienceYears(5)
                .tags(List.of("임대차"))
                .bio("테스트용 소개")
                .region("서울")
                .build();

        // verificationStatus 강제 주입 (생성 시 PENDING으로 초기화되므로 테스트를 위해 변경)
        Field statusField = LawyerProfile.class.getDeclaredField("verificationStatus");
        statusField.setAccessible(true);
        statusField.set(profile, status);

        // id 주입 (BaseEntity의 protected 필드)
        Field idField = findField(profile.getClass(), "id");
        idField.setAccessible(true);
        idField.set(profile, lawyerId);

        return profile;
    }

    private Field findField(Class<?> clazz, String name) {
        Class<?> c = clazz;
        while (c != null) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        throw new IllegalStateException("field not found: " + name);
    }

    @Test
    @DisplayName("이미 VERIFIED 상태인 변호사에 PATCH 호출 → 409 Conflict")
    void alreadyVerified_throwsConflict() throws Exception {
        UUID lawyerId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        LawyerProfile lawyer = profileWithStatus(lawyerId, VerificationStatus.VERIFIED);

        when(lawyerReader.findById(lawyerId)).thenReturn(lawyer);

        assertThatThrownBy(() ->
                adminService.processVerification(lawyerId, adminId, "REJECTED", "정정 사유")
        )
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VERIFICATION_ALREADY_PROCESSED);

        verify(lawyerWriter, never()).save(any());
        verify(verificationLogWriter, never()).save(any());
    }

    @Test
    @DisplayName("이미 REJECTED 상태인 변호사에 PATCH 호출 → 409 Conflict")
    void alreadyRejected_throwsConflict() throws Exception {
        UUID lawyerId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        LawyerProfile lawyer = profileWithStatus(lawyerId, VerificationStatus.REJECTED);

        when(lawyerReader.findById(lawyerId)).thenReturn(lawyer);

        assertThatThrownBy(() ->
                adminService.processVerification(lawyerId, adminId, "VERIFIED", null)
        )
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VERIFICATION_ALREADY_PROCESSED);

        verify(lawyerWriter, never()).save(any());
    }

    @Test
    @DisplayName("PENDING 상태인 변호사는 정상 처리된다")
    void pending_processedNormally() throws Exception {
        UUID lawyerId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        LawyerProfile lawyer = profileWithStatus(lawyerId, VerificationStatus.PENDING);

        when(lawyerReader.findById(lawyerId)).thenReturn(lawyer);
        when(verificationLogWriter.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));
        // VERIFIED 경로에서 임베딩 서비스 호출 발생 (실패해도 무시되므로 lenient)
        lenient().doNothing().when(lawyerEmbeddingService).upsertEmbedding(any(LawyerProfile.class));

        var result = adminService.processVerification(lawyerId, adminId, "VERIFIED", null);

        assertThat(result.newStatus()).isEqualTo("VERIFIED");
        assertThat(result.previousStatus()).isEqualTo("PENDING");
        verify(lawyerWriter).save(lawyer);
        verify(verificationLogWriter).save(any(VerificationLog.class));
    }

    @Test
    @DisplayName("REVIEWING 상태인 변호사는 정상 처리된다")
    void reviewing_processedNormally() throws Exception {
        UUID lawyerId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        LawyerProfile lawyer = profileWithStatus(lawyerId, VerificationStatus.REVIEWING);

        when(lawyerReader.findById(lawyerId)).thenReturn(lawyer);
        when(verificationLogWriter.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        var result = adminService.processVerification(lawyerId, adminId, "REJECTED", "서류 미비");

        assertThat(result.newStatus()).isEqualTo("REJECTED");
        assertThat(result.previousStatus()).isEqualTo("REVIEWING");
        verify(lawyerWriter).save(lawyer);
    }

    @Test
    @DisplayName("SUPPLEMENT_REQUESTED 상태인 변호사는 정상 처리된다")
    void supplementRequested_processedNormally() throws Exception {
        UUID lawyerId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        LawyerProfile lawyer = profileWithStatus(lawyerId, VerificationStatus.SUPPLEMENT_REQUESTED);

        when(lawyerReader.findById(lawyerId)).thenReturn(lawyer);
        when(verificationLogWriter.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().doNothing().when(lawyerEmbeddingService).upsertEmbedding(any(LawyerProfile.class));

        var result = adminService.processVerification(lawyerId, adminId, "VERIFIED", null);

        assertThat(result.newStatus()).isEqualTo("VERIFIED");
        assertThat(result.previousStatus()).isEqualTo("SUPPLEMENT_REQUESTED");
        verify(lawyerWriter).save(lawyer);
    }
}
