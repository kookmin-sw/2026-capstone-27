package org.example.shield.lawyer.application;

import org.example.shield.ai.config.CohereApiConfig;
import org.example.shield.ai.infrastructure.CohereClient;
import org.example.shield.lawyer.domain.LawyerEmbedding;
import org.example.shield.lawyer.domain.LawyerProfile;
import org.example.shield.lawyer.infrastructure.LawyerEmbeddingRepository;
import org.example.shield.lawyer.infrastructure.LawyerProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LawyerEmbeddingServiceTest {

    @Mock
    private LawyerProfileRepository lawyerProfileRepository;

    @Mock
    private LawyerEmbeddingRepository lawyerEmbeddingRepository;

    @Mock
    private CohereClient cohereClient;

    @Mock
    private CohereApiConfig cohereConfig;

    private final LawyerEmbeddingTextBuilder textBuilder = new LawyerEmbeddingTextBuilder();

    @InjectMocks
    private LawyerEmbeddingService service;

    @BeforeEach
    void setUp() throws Exception {
        // textBuilder 는 실제 구현 주입 (@InjectMocks 가 Mock 만 주입해서 수동 설정)
        Field f = LawyerEmbeddingService.class.getDeclaredField("textBuilder");
        f.setAccessible(true);
        f.set(service, textBuilder);
    }

    private LawyerProfile createProfile(UUID lawyerId) throws Exception {
        LawyerProfile profile = LawyerProfile.builder()
                .userId(UUID.randomUUID())
                .barAssociationNumber("123")
                .domains(List.of("형사"))
                .subDomains(List.of("사기"))
                .tags(List.of("변호"))
                .experienceYears(10)
                .bio("소개")
                .region("서울")
                .build();
        // BaseEntity.id 강제 주입 (protected)
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
    void 신규_임베딩은_Cohere_호출하고_save() throws Exception {
        UUID lawyerId = UUID.randomUUID();
        LawyerProfile profile = createProfile(lawyerId);

        when(cohereConfig.getEmbedModel()).thenReturn("embed-v4.0");
        when(lawyerEmbeddingRepository.findById(lawyerId)).thenReturn(Optional.empty());
        when(cohereClient.embedDocuments(anyString(), anyList()))
                .thenReturn(List.of(new float[]{0.1f, 0.2f}));

        service.upsertEmbedding(profile);

        ArgumentCaptor<LawyerEmbedding> cap = ArgumentCaptor.forClass(LawyerEmbedding.class);
        verify(lawyerEmbeddingRepository, times(1)).save(cap.capture());
        LawyerEmbedding saved = cap.getValue();
        assertThat(saved.getLawyerId()).isEqualTo(lawyerId);
        assertThat(saved.getEmbeddingModel()).isEqualTo("embed-v4.0");
        assertThat(saved.getSourceHash()).hasSize(64); // SHA-256 hex
        assertThat(saved.getEmbedding()).containsExactly(0.1f, 0.2f);
    }

    @Test
    void 동일_해시는_Cohere_재호출_없이_skip() throws Exception {
        UUID lawyerId = UUID.randomUUID();
        LawyerProfile profile = createProfile(lawyerId);

        when(cohereConfig.getEmbedModel()).thenReturn("embed-v4.0");

        // 기존 임베딩 엔티티: 동일 텍스트 해시 + 모델
        String expectedText = textBuilder.build(
                profile.getDomains(), profile.getSubDomains(),
                profile.getTags(), profile.getBio());
        String expectedHash = sha256(expectedText);

        LawyerEmbedding existing = LawyerEmbedding.create(
                lawyerId, new float[]{0.9f}, "embed-v4.0", expectedHash, expectedText);
        when(lawyerEmbeddingRepository.findById(lawyerId)).thenReturn(Optional.of(existing));

        service.upsertEmbedding(profile);

        verify(cohereClient, never()).embedDocuments(anyString(), anyList());
        verify(lawyerEmbeddingRepository, never()).save(any());
    }

    @Test
    void 해시_다르면_updateEmbedding_호출() throws Exception {
        UUID lawyerId = UUID.randomUUID();
        LawyerProfile profile = createProfile(lawyerId);

        when(cohereConfig.getEmbedModel()).thenReturn("embed-v4.0");

        LawyerEmbedding existing = LawyerEmbedding.create(
                lawyerId, new float[]{0.0f}, "embed-v4.0", "old-hash", "old-text");
        when(lawyerEmbeddingRepository.findById(lawyerId)).thenReturn(Optional.of(existing));
        when(cohereClient.embedDocuments(anyString(), anyList()))
                .thenReturn(List.of(new float[]{0.5f, 0.6f}));

        service.upsertEmbedding(profile);

        assertThat(existing.getEmbedding()).containsExactly(0.5f, 0.6f);
        assertThat(existing.getSourceHash()).isNotEqualTo("old-hash");
        verify(lawyerEmbeddingRepository, never()).save(any()); // dirty checking, save 불필요
    }

    @Test
    void 빈_텍스트는_skip() throws Exception {
        UUID lawyerId = UUID.randomUUID();
        LawyerProfile profile = LawyerProfile.builder()
                .userId(UUID.randomUUID())
                .barAssociationNumber("123")
                .build();
        Field idField = findField(profile.getClass(), "id");
        idField.setAccessible(true);
        idField.set(profile, lawyerId);

        service.upsertEmbedding(profile);

        verify(cohereClient, never()).embedDocuments(anyString(), anyList());
        verify(lawyerEmbeddingRepository, never()).save(any());
    }

    @Test
    void UUID_오버로드는_프로필_없으면_skip() {
        UUID lawyerId = UUID.randomUUID();
        when(lawyerProfileRepository.findById(lawyerId)).thenReturn(Optional.empty());

        service.upsertEmbedding(lawyerId);

        verify(cohereClient, never()).embedDocuments(anyString(), anyList());
    }

    private String sha256(String text) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
