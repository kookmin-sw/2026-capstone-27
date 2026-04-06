package org.example.shield.common.domain;

/**
 * 공통 엔티티 - 모든 엔티티가 상속.
 *
 * TODO: @MappedSuperclass + @EntityListeners(AuditingEntityListener.class) 구현
 * - id: UUID (PK, @GeneratedValue(strategy = GenerationType.UUID))
 * - createdAt: LocalDateTime (@CreatedDate)
 * - updatedAt: LocalDateTime (@LastModifiedDate)
 */
public abstract class BaseEntity {
}
