package org.example.shield.ai.application;

import org.example.shield.ai.domain.LegalChunkEntity;
import org.example.shield.ai.domain.LegalChunkJpaRepository;
import org.example.shield.ai.dto.CivilLawSeed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * {@link CivilLawUpsertService} 단위 테스트.
 */
class CivilLawUpsertServiceTest {

    private LegalChunkJpaRepository repository;
    private CivilLawUpsertService service;

    @BeforeEach
    void setUp() {
        repository = mock(LegalChunkJpaRepository.class);
        service = new CivilLawUpsertService(repository);
    }

    @Test
    @DisplayName("신규 조문: save 호출")
    void insertsNewArticle() {
        CivilLawSeed.Article a = new CivilLawSeed.Article(
                "law-civil", "민법", "제1조", 1, "법원", "내용",
                "제1편 총칙", "제1장 통칙", null, "20260317", "284415", "001706");
        float[] vec = {0.1f};

        when(repository.findActiveByNaturalKey(anyString(), anyString(), any()))
                .thenReturn(Optional.empty());
        when(repository.save(any(LegalChunkEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        int count = service.upsertBatch(
                List.of(a), List.of(vec), "embed-v4.0",
                (article, v) -> LegalChunkEntity.builder()
                        .lawId(article.lawId())
                        .lawName(article.lawName())
                        .articleNo(article.articleNo())
                        .chunkIndex((short) 0)
                        .content(article.content())
                        .embedding(v)
                        .embeddingModel("embed-v4.0")
                        .build());

        assertThat(count).isEqualTo(1);
        verify(repository).save(any(LegalChunkEntity.class));
    }

    @Test
    @DisplayName("기존 조문: save 대신 updateEmbedding")
    void updatesExistingArticle() {
        LegalChunkEntity existing = LegalChunkEntity.builder()
                .lawId("law-civil")
                .lawName("민법")
                .articleNo("제1조")
                .chunkIndex((short) 0)
                .content("기존 내용")
                .build();
        when(repository.findActiveByNaturalKey(anyString(), anyString(), any()))
                .thenReturn(Optional.of(existing));

        CivilLawSeed.Article a = new CivilLawSeed.Article(
                "law-civil", "민법", "제1조", 1, "법원", "내용",
                "제1편 총칙", "제1장 통칙", null, "20260317", "284415", "001706");
        float[] vec = {0.7f};

        int count = service.upsertBatch(
                List.of(a), List.of(vec), "embed-v4.0",
                (article, v) -> LegalChunkEntity.builder().build());

        assertThat(count).isEqualTo(1);
        assertThat(existing.getEmbedding()).isEqualTo(vec);
        assertThat(existing.getEmbeddingModel()).isEqualTo("embed-v4.0");
        verify(repository, never()).save(any());
    }
}
