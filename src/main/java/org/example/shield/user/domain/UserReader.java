package org.example.shield.user.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserReader {

    User findById(UUID id);

    List<User> findAllByIds(List<UUID> ids);

    Optional<User> findByGoogleId(String googleId);

    Optional<User> findByEmail(String email);
}
