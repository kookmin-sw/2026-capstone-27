package org.example.shield.user.infrastructure;

import lombok.RequiredArgsConstructor;
import org.example.shield.user.domain.User;
import org.example.shield.user.domain.UserWriter;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserWriterImpl implements UserWriter {

    private final UserRepository userRepository;

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }
}
