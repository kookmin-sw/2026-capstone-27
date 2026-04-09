package org.example.shield.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.shield.common.domain.BaseEntity;
import org.example.shield.common.enums.UserRole;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

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

    @Builder
    public User(String email, String name, UserRole role, String provider,
                String googleId, String profileImageUrl) {
        this.email = email;
        this.name = name;
        this.role = role;
        this.provider = provider;
        this.googleId = googleId;
        this.profileImageUrl = profileImageUrl;
    }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void updateName(String name) {
        this.name = name;
    }
}
