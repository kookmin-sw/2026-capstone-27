package org.example.shield.lawyer.infrastructure;

import java.util.UUID;

/**
 * {@link LawyerEmbeddingRepository#findTopBySimilarity} 결과 투영 (Issue #50).
 *
 * <p>JPA Native Query + Interface-based Projection. 컬럼 별칭과 getter 이름을 맞춘다.</p>
 *
 * <p>{@code domains}, {@code subDomains}, {@code tags} 는 jsonb 컬럼이며
 * 드라이버가 문자열 (JSON 배열) 로 반환한다. 서비스 레이어에서 {@code ObjectMapper}
 * 로 {@code List<String>} 으로 역직렬화한다.</p>
 */
public interface LawyerMatchProjection {
    UUID getLawyerId();
    UUID getUserId();
    String getDomains();
    String getSubDomains();
    String getTags();
    String getBio();
    Integer getExperienceYears();
    String getRegion();
    Double getSimilarity();
}
