package org.example.shield.ai.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.ai.domain.LegalCaseEntity;
import org.example.shield.ai.domain.LegalCaseJpaRepository;
import org.example.shield.ai.dto.LegalCaseSeed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * 판례 인제스트 upsert 트랜잭션 경계 (Phase C-4).
 *
 * <p>{@link CivilLawUpsertService}와 동일한 철학: Spring AOP {@code @Transactional}은
 * 같은 클래스 내 self-invocation 시 프록시가 적용되지 않기 때문에 서비스 경계를 분리한다.</p>
 *
 * <p>자연키 {@code (case_no, court, decision_date)} 기준 upsert:</p>
 * <ul>
 *   <li>존재하면 본문(headnote/holding/reasoning/full_text/category_ids 등) + 임베딩 갱신</li>
 *   <li>없으면 새 row 저장</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LegalCaseUpsertService {

    private final LegalCaseJpaRepository caseRepository;

    /**
     * 배치 단위 upsert.
     *
     * @param batch       판례 시드 리스트 (LegalCaseSeed.Case)
     * @param embeddings  판례별 임베딩 벡터 (batch와 동일 순서)
     * @param embedModel  임베딩 모델 ID ("embed-v4.0")
     * @param entityMaker Case + 벡터 → LegalCaseEntity 변환 함수 (신규 저장 시 사용)
     * @return upsert 처리된 판례 수
     */
    @Transactional
    public int upsertBatch(List<LegalCaseSeed> batch,
                           List<float[]> embeddings,
                           String embedModel,
                           EntityMaker entityMaker) {
        int count = 0;
        for (int i = 0; i < batch.size(); i++) {
            LegalCaseSeed seed = batch.get(i);
            LegalCaseSeed.Case c = seed.caseData();
            float[] vec = embeddings.get(i);

            LocalDate decisionDate = LocalDate.parse(c.decisionDate());
            Optional<LegalCaseEntity> existing = caseRepository.findByNaturalKey(
                    c.caseNo(), c.court(), decisionDate);
            if (existing.isPresent()) {
                LegalCaseEntity e = existing.get();
                e.updateContent(
                        c.caseName(),
                        c.headnote(),
                        c.holding(),
                        c.reasoning(),
                        c.fullText(),
                        toArray(c.citedArticles()),
                        toArray(c.citedCases()),
                        toArray(c.categoryIds()),
                        c.disposition(),
                        c.judgmentType());
                e.updateEmbedding(vec, embedModel);
                count++;
            } else {
                LegalCaseEntity e = entityMaker.apply(seed, vec);
                caseRepository.save(e);
                count++;
            }
        }
        return count;
    }

    private static String[] toArray(List<String> list) {
        if (list == null || list.isEmpty()) return new String[0];
        return list.toArray(new String[0]);
    }

    /**
     * Seed + 임베딩 벡터 → LegalCaseEntity 변환 함수 인터페이스.
     * source/sourceUrl 등 공통 값은 호출자가 closure에 캡처한다.
     */
    @FunctionalInterface
    public interface EntityMaker extends BiFunction<LegalCaseSeed, float[], LegalCaseEntity> {
    }
}
