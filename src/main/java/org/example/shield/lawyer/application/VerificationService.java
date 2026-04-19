package org.example.shield.lawyer.application;

import lombok.RequiredArgsConstructor;
import org.example.shield.auth.application.JwtService;
import org.example.shield.auth.domain.JwtToken;
import org.example.shield.common.enums.VerificationStatus;
import org.example.shield.common.exception.BusinessException;
import org.example.shield.common.exception.ErrorCode;
import org.example.shield.lawyer.controller.dto.LawyerRegisterRequest;
import org.example.shield.lawyer.controller.dto.LawyerRegisterResponse;
import org.example.shield.lawyer.controller.dto.VerificationResponse;
import org.example.shield.lawyer.domain.LawyerProfile;
import org.example.shield.lawyer.domain.LawyerReader;
import org.example.shield.lawyer.domain.LawyerWriter;
import org.example.shield.user.domain.User;
import org.example.shield.user.domain.UserReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class VerificationService {

    private final LawyerReader lawyerReader;
    private final LawyerWriter lawyerWriter;
    private final UserReader userReader;
    private final JwtService jwtService;

    /**
     * 변호사 가입 처리 결과.
     * 컨트롤러에서 refreshToken 은 HttpOnly 쿠키로, response 는 body 로 내려준다.
     */
    public record RegisterResult(LawyerRegisterResponse response, String refreshToken) {}

    /**
     * 변호사 가입: 추가정보 입력 + 검증 신청 통합.
     *
     * 소셜 로그인으로 가입한 USER 가 이 엔드포인트를 호출하는 것을 전제로 한다.
     * 이미 LawyerProfile 이 존재하면 VERIFICATION_ALREADY_SUBMITTED 로 거절된다.
     * 처리 순서:
     *   1) LawyerProfile 생성 (verificationStatus = PENDING)
     *   2) User.role 을 USER → LAWYER 로 승격
     *   3) 새 JWT 토큰 쌍 재발급 + DB refreshToken 갱신
     */

    public RegisterResult register(UUID userId, LawyerRegisterRequest request) {
        // 이미 가입된 변호사인지 확인
        try {
            lawyerReader.findByUserId(userId);
            throw new BusinessException(ErrorCode.VERIFICATION_ALREADY_SUBMITTED) {};
        } catch (BusinessException e) {
            if (e.getErrorCode() != ErrorCode.LAWYER_NOT_FOUND) {
                throw e;
            }
        }

        LawyerProfile profile = LawyerProfile.builder()
                .userId(userId)
                .barAssociationNumber(request.barAssociationNumber())
                .domains(request.domains())
                .subDomains(request.subDomains())
                .tags(request.tags())
                .experienceYears(request.experienceYears())
                .bio(request.bio())
                .region(request.region())
                .build();

        LawyerProfile saved = lawyerWriter.save(profile);

        // USER → LAWYER 승격 + 새 JWT 재발급
        User user = userReader.findById(userId);
        user.promoteToLawyer();

        JwtToken tokenPair = jwtService.createTokenPair(user.getId(), user.getRole().name());
        user.updateRefreshToken(tokenPair.refreshToken());

        LawyerRegisterResponse response = LawyerRegisterResponse.of(
                tokenPair.accessToken(), user.getRole().name(), saved);
        return new RegisterResult(response, tokenPair.refreshToken());
    }

    public VerificationResponse requestVerification(UUID userId, String barAssociationNumber) {
        try {
            LawyerProfile existing = lawyerReader.findByUserId(userId);
            if (existing.getVerificationStatus() == VerificationStatus.PENDING
                    || existing.getVerificationStatus() == VerificationStatus.REVIEWING) {
                throw new BusinessException(ErrorCode.VERIFICATION_ALREADY_SUBMITTED) {};
            }
            existing.requestVerification(barAssociationNumber);
            return VerificationResponse.from(existing);
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.LAWYER_NOT_FOUND) {
                LawyerProfile profile = LawyerProfile.builder()
                        .userId(userId)
                        .barAssociationNumber(barAssociationNumber)
                        .build();
                LawyerProfile saved = lawyerWriter.save(profile);
                return VerificationResponse.from(saved);
            }
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public VerificationResponse getVerificationStatus(UUID userId) {
        LawyerProfile profile = lawyerReader.findByUserId(userId);
        return VerificationResponse.from(profile);
    }
}
