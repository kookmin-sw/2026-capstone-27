package org.example.shield.user.domain;

/**
 * 사용자 엔티티 - users 테이블 매핑.
 *
 * TODO: @Entity 구현
 * - id: UUID (PK, auto-generated)
 * - email: String (UNIQUE, NOT NULL)
 * - name: String (NOT NULL)
 * - role: String (CLIENT / LAWYER / ADMIN)
 * - provider: String (GOOGLE / KAKAO)
 * - googleId: String
 * - refreshToken: String
 * - createdAt: LocalDateTime
 * - updatedAt: LocalDateTime
 */
public class User {
}
