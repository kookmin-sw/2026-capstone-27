package org.example.shield.lawyer.controller.dto;

import org.example.shield.lawyer.domain.LawyerProfile;

import java.time.LocalDateTime;

/**
 * 변호사 가입(/api/lawyers/me/register) 응답.
 *
 * 가입이 완료되면 User.role 이 USER → LAWYER 로 승격되며
 * 새 JWT 토큰 쌍(access + refresh) 이 발급된다.
 *
 * - accessToken : 응답 body 에 포함해 프론트가 즉시 교체한다.
 * - refreshToken : 기존 로그인 엔드포인트와 동일하게 HttpOnly secure 쿠키(refreshToken)로 교체한다.
 *   (body 에는 포함하지 않음. XSS 노출 이슈 및 기존 관례 유지)
 */
public record LawyerRegisterResponse(
        String accessToken,
        String role,
        String verificationStatus,
        String barAssociationNumber,
        LocalDateTime requestedAt,
        LocalDateTime verifiedAt
) {
    public static LawyerRegisterResponse of(String accessToken, String role, LawyerProfile profile) {
        return new LawyerRegisterResponse(
                accessToken,
                role,
                profile.getVerificationStatus().name(),
                profile.getBarAssociationNumber(),
                // TODO: LawyerProfile 에 verificationRequestedAt 필드 추가 후 매핑 변경
                profile.getCreatedAt(),
                profile.getVerifiedAt()
        );
    }
}
