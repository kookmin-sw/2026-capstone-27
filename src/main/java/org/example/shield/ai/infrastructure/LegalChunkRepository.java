package org.example.shield.ai.infrastructure;

import org.example.shield.ai.domain.LegalChunkDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * legal_chunks 컬렉션 Spring Data MongoDB Repository.
 */
public interface LegalChunkRepository extends MongoRepository<LegalChunkDocument, String> {

    List<LegalChunkDocument> findByLawIdIn(List<String> lawIds);

    List<LegalChunkDocument> findByLawIdInAndAbolitionDateIsNull(List<String> lawIds);
}
