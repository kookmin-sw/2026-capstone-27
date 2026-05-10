package org.example.shield.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.shield.common.enums.UserRole;

import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false, unique = true)
    private String googleId;

    private String refreshToken;

    private String profileImageUrl;

    @Column(length = 20)
    private String phone;

    @Builder
    public User(String email, String name, UserRole role, String provider,
                String googleId, String profileImageUrl, String phone) {
        this.email = email;
        this.name = name;
        this.role = role;
        this.provider = provider;
        this.googleId = googleId;
        this.profileImageUrl = profileImageUrl;
        this.phone = phone;
    }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void updateProfile(String name, String phone) {
        if (name != null && !name.isBlank()) this.name = name;
        if (phone != null && !phone.isBlank()) this.phone = phone;
    }

    /**
     * 소셜 로그인으로 가입한 일반 유저(USER)가 변호사 추가정보 입력을 완료했을 때
     * 역할을 LAWYER 로 승격시킨다. 이미 LAWYER/ADMIN 인 경우에는 변경하지 않는다.
     */
    public void promoteToLawyer() {
        if (this.role == UserRole.USER) {
            this.role = UserRole.LAWYER;
        }
    }
}
