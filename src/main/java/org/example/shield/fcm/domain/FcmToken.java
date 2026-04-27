package org.example.shield.fcm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.shield.common.domain.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "fcm_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FcmToken extends BaseEntity {

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true, columnDefinition = "text")
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", length = 20)
    private DeviceType deviceType;

    private FcmToken(UUID userId, String token, DeviceType deviceType) {
        this.userId = userId;
        this.token = token;
        this.deviceType = deviceType;
    }

    public static FcmToken create(UUID userId, String token, DeviceType deviceType) {
        return new FcmToken(userId, token, deviceType);
    }

    public void reassign(UUID userId, DeviceType deviceType) {
        this.userId = userId;
        this.deviceType = deviceType;
    }
}
