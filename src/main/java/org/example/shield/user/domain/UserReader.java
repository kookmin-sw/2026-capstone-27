package org.example.shield.user.domain;

import java.util.Optional;
import java.util.UUID;

public interface UserReader {

    User findById(UUID id);

    Optional<User> findByGoogleId(String googleId);

    Optional<User> findByEmail(String email);
}
