package com.skillbridge.repository;

import com.skillbridge.entity.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    
    // For backward compatibility
    default Optional<User> findByUsername(String username) {
        return findByEmail(username);
    }
}
