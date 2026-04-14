package org.example.shield.user.application;

import lombok.RequiredArgsConstructor;
import org.example.shield.user.controller.dto.UserResponse;
import org.example.shield.user.controller.dto.UserUpdateRequest;
import org.example.shield.user.domain.User;
import org.example.shield.user.domain.UserReader;
import org.example.shield.user.domain.UserWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserReader userReader;
    private final UserWriter userWriter;

    public UserResponse getMyInfo(UUID userId) {
        User user = userReader.findById(userId);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateMyInfo(UUID userId, UserUpdateRequest request) {
        User user = userReader.findById(userId);
        user.updateProfile(request.name(), request.phone());
        userWriter.save(user);
        return UserResponse.from(user);
    }
}
