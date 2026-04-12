package org.example.shield.lawyer.infrastructure;

import org.example.shield.lawyer.domain.LawyerDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface LawyerDocumentRepository extends JpaRepository<LawyerDocument, UUID> {
    List<LawyerDocument> findAllByLawyerId(UUID lawyerId);
    long countByLawyerId(UUID lawyerId);

    @Query("SELECT ld.lawyerId, COUNT(ld) FROM LawyerDocument ld WHERE ld.lawyerId IN :lawyerIds GROUP BY ld.lawyerId")
    List<Object[]> countByLawyerIds(@Param("lawyerIds") List<UUID> lawyerIds);
}
