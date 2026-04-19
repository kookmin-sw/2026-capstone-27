package org.example.shield.brief.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.shield.ai.application.QueryEmbeddingService;
import org.example.shield.brief.controller.dto.MatchingResponse;
import org.example.shield.brief.domain.Brief;
import org.example.shield.brief.domain.BriefReader;
import org.example.shield.brief.exception.BriefNotFoundException;
import org.example.shield.common.enums.VerificationStatus;
import org.example.shield.common.response.PageResponse;
import org.example.shield.lawyer.application.LawyerEmbeddingTextBuilder;
import org.example.shield.lawyer.domain.LawyerProfile;
import org.example.shield.lawyer.domain.LawyerReader;
import org.example.shield.lawyer.infrastructure.LawyerEmbeddingRepository;
import org.example.shield.lawyer.infrastructure.LawyerMatchProjection;
import org.example.shield.user.domain.User;
import org.example.shield.user.domain.UserReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LawyerMatchingServiceTest {

    @Mock private BriefReader briefReader;
    @Mock private LawyerReader lawyerReader;
    @Mock private UserReader userReader;
    @Mock private LawyerEmbeddingRepository lawyerEmbeddingRepository;
    @Mock private QueryEmbeddingService queryEmbeddingService;

    private final LawyerEmbeddingTextBuilder embeddingTextBuilder = new LawyerEmbeddingTextBuilder();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private LawyerMatchingService service;

    @BeforeEach
    void injectRealBeans() throws Exception {
        set(service, "embeddingTextBuilder", embeddingTextBuilder);
        set(service, "objectMapper", objectMapper);
    }

    private void set(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    private Brief createBrief(UUID userId) throws Exception {
        Brief brief = Brief.create(
                UUID.randomUUID(), userId, "title", "형사",
                "사건 내용", List.of("사기", "횡령"), List.of(), "전략");
        // id 강제 주입
        Field idField = findField(brief.getClass(), "id");
        idField.setAccessible(true);
        idField.set(brief, UUID.randomUUID());
        return brief;
    }

    private Field findField(Class<?> clazz, String name) {
        Class<?> c = clazz;
        while (c != null) {
            try { return c.getDeclaredField(name); }
            catch (NoSuchFieldException e) { c = c.getSuperclass(); }
        }
        throw new IllegalStateException(name);
    }

    @Test
    void 브리프_소유자가_아니면_BriefNotFoundException() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        Brief brief = createBrief(ownerId);
        UUID briefId = brief.getId();
        when(briefReader.findById(briefId)).thenReturn(brief);

        assertThatThrownBy(() -> service.findMatching(briefId, otherId, PageRequest.of(0, 10)))
                .isInstanceOf(BriefNotFoundException.class);
    }

    @Test
    void 벡터_검색_정상경로_projection을_MatchingResponse로_매핑() throws Exception {
        UUID userId = UUID.randomUUID();
        Brief brief = createBrief(userId);
        UUID briefId = brief.getId();
        Pageable pageable = PageRequest.of(0, 5);

        when(briefReader.findById(briefId)).thenReturn(brief);
        when(queryEmbeddingService.embedQuery(anyString()))
                .thenReturn(new float[]{0.1f, 0.2f});

        UUID lawyerUserId = UUID.randomUUID();
        LawyerMatchProjection proj = mock(LawyerMatchProjection.class);
        when(proj.getUserId()).thenReturn(lawyerUserId);
        when(proj.getDomains()).thenReturn("[\"형사\"]");
        when(proj.getSubDomains()).thenReturn("[\"사기\"]");
        when(proj.getTags()).thenReturn("[\"사기\",\"항소\"]");
        when(proj.getBio()).thenReturn("소개");
        when(proj.getExperienceYears()).thenReturn(7);
        when(proj.getRegion()).thenReturn("서울");
        when(proj.getSimilarity()).thenReturn(0.85);

        when(lawyerEmbeddingRepository.findTopBySimilarity(anyString(), anyInt(), anyInt()))
                .thenReturn(List.of(proj));
        when(lawyerEmbeddingRepository.countVerifiedWithEmbedding()).thenReturn(1L);

        User user = mock(User.class);
        when(user.getId()).thenReturn(lawyerUserId);
        when(user.getName()).thenReturn("홍길동");
        when(user.getProfileImageUrl()).thenReturn(null);
        when(userReader.findAllByIds(anyList())).thenReturn(List.of(user));

        PageResponse<MatchingResponse> response = service.findMatching(briefId, userId, pageable);

        assertThat(response.content()).hasSize(1);
        MatchingResponse r = response.content().get(0);
        assertThat(r.lawyerId()).isEqualTo(lawyerUserId);
        assertThat(r.name()).isEqualTo("홍길동");
        assertThat(r.domains()).containsExactly("형사");
        assertThat(r.tags()).containsExactly("사기", "항소");
        assertThat(r.matchedKeywords()).containsExactly("사기"); // briefKeywords ∩ tags
        assertThat(r.score()).isEqualTo(0.85);
    }

    @Test
    void 쿼리_임베딩_실패시_키워드_fallback() throws Exception {
        UUID userId = UUID.randomUUID();
        Brief brief = createBrief(userId);
        UUID briefId = brief.getId();
        Pageable pageable = PageRequest.of(0, 5);

        when(briefReader.findById(briefId)).thenReturn(brief);
        when(queryEmbeddingService.embedQuery(anyString()))
                .thenThrow(new RuntimeException("Cohere down"));

        UUID lawyerUserId = UUID.randomUUID();
        LawyerProfile lawyer = LawyerProfile.builder()
                .userId(lawyerUserId)
                .barAssociationNumber("123")
                .domains(List.of("형사"))
                .subDomains(List.of("사기"))
                .tags(List.of("사기"))
                .experienceYears(5)
                .bio("소개")
                .region("서울")
                .build();
        Page<LawyerProfile> page = new PageImpl<>(List.of(lawyer), pageable, 1);
        when(lawyerReader.findAllByVerificationStatus(eq(VerificationStatus.VERIFIED), any(Pageable.class)))
                .thenReturn(page);

        User user = mock(User.class);
        when(user.getId()).thenReturn(lawyerUserId);
        when(user.getName()).thenReturn("이몽룡");
        when(userReader.findAllByIds(anyList())).thenReturn(List.of(user));

        PageResponse<MatchingResponse> response = service.findMatching(briefId, userId, pageable);

        assertThat(response.content()).hasSize(1);
        // 키워드 2개 중 1개 매치
        assertThat(response.content().get(0).score()).isEqualTo(0.5);
        verify(queryEmbeddingService).embedQuery(anyString());
    }
}
