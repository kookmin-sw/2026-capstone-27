package org.example.shield.lawyer.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.ai.config.CohereApiConfig;
import org.example.shield.ai.infrastructure.CohereClient;
import org.example.shield.lawyer.domain.LawyerEmbedding;
import org.example.shield.lawyer.domain.LawyerProfile;
import org.example.shield.lawyer.infrastructure.LawyerEmbeddingRepository;
import org.example.shield.lawyer.infrastructure.LawyerProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 변호사 임베딩 생성·재계산 서비스 (Issue #50).
 *
 * <p>변호사 승인 / 프로필 수정 이벤트 훅에서 호출된다.
 * 동일 {@code sourceHash} 면 임베딩 호출을 건너뛰어 Cohere 비용과 지연을 절감.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LawyerEmbeddingService {

    private final LawyerProfileRepository lawyerProfileRepository;
    private final LawyerEmbeddingRepository lawyerEmbeddingRepository;
    private final LawyerEmbeddingTextBuilder textBuilder;
    private final CohereClient cohereClient;
    private final CohereApiConfig cohereConfig;

    /**
     * 변호사 id 로 프로필을 조회해 임베딩을 생성/갱신한다.
     * 프로필 또는 텍스트가 비면 skip.
     */
    @Transactional
    public void upsertEmbedding(UUID lawyerId) {
        Optional<LawyerProfile> profileOpt = lawyerProfileRepository.findById(lawyerId);
        if (profileOpt.isEmpty()) {
            log.warn("변호사 임베딩 skip: 프로필 없음 lawyerId={}", lawyerId);
            return;
        }
        upsertEmbedding(profileOpt.get());
    }

    /**
     * 변호사 프로필에서 텍스트를 조립 → 해시 비교 → 변경 시에만 Cohere 호출·저장.
     */
    @Transactional
    public void upsertEmbedding(LawyerProfile profile) {
        UUID lawyerId = profile.getId();
        String text = textBuilder.build(
                profile.getDomains(),
                profile.getSubDomains(),
                profile.getTags(),
                profile.getBio());

        if (text.isBlank()) {
            log.info("변호사 임베딩 skip: 빈 텍스트 lawyerId={}", lawyerId);
            return;
        }

        String hash = sha256(text);
        String model = cohereConfig.getEmbedModel();

        Optional<LawyerEmbedding> existingOpt = lawyerEmbeddingRepository.findById(lawyerId);
        if (existingOpt.isPresent()) {
            LawyerEmbedding existing = existingOpt.get();
            if (hash.equals(existing.getSourceHash()) && model.equals(existing.getEmbeddingModel())) {
                log.debug("변호사 임베딩 skip: 해시 동일 lawyerId={}", lawyerId);
                return;
            }
            float[] vec = embed(text);
            existing.updateEmbedding(vec, model, hash, text);
            log.info("변호사 임베딩 갱신 lawyerId={}", lawyerId);
            return;
        }

        float[] vec = embed(text);
        LawyerEmbedding entity = LawyerEmbedding.create(lawyerId, vec, model, hash, text);
        lawyerEmbeddingRepository.save(entity);
        log.info("변호사 임베딩 신규 생성 lawyerId={}", lawyerId);
    }

    private float[] embed(String text) {
        List<float[]> vectors = cohereClient.embedDocuments(
                cohereConfig.getEmbedModel(), List.of(text));
        if (vectors == null || vectors.isEmpty() || vectors.get(0) == null) {
            throw new IllegalStateException("Cohere 임베딩 응답이 비어있음");
        }
        return vectors.get(0);
    }

    private String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 사용 불가", e);
        }
    }
}
