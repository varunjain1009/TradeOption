package com.tradeoption.repository;

import com.tradeoption.domain.User;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepository {

    private final RocksDBRepository rocksDBRepository;
    private static final String USER_PREFIX = "user:";

    public UserRepository(RocksDBRepository rocksDBRepository) {
        this.rocksDBRepository = rocksDBRepository;
    }

    public void save(User user) {
        // Key: user:{username} -> User object
        // This allows O(1) lookup by username for login
        rocksDBRepository.save(USER_PREFIX + user.getUsername(), user);
    }

    public Optional<User> findByUsername(String username) {
        User user = rocksDBRepository.find(USER_PREFIX + username, User.class);
        return Optional.ofNullable(user);
    }

    public boolean existsByUsername(String username) {
        return findByUsername(username).isPresent();
    }
}
