package com.skillbridge.service;

import com.skillbridge.dto.LoginResponse;
import com.skillbridge.dto.LoginRequest;
import com.skillbridge.dto.RegisterRequest;
import com.skillbridge.entity.User;
import com.skillbridge.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        
        // Create a test user if not exists
        createTestUserIfNotExists();
    }

    private void createTestUserIfNotExists() {
        if (userRepository.findByEmail("admin@skillbridge.com").isEmpty()) {
            User testUser = new User();
            testUser.setEmail("admin@skillbridge.com");
            testUser.setFullName("Administrator");
            testUser.setCompanyName("SkillBridge");
            // Hash the password using BCrypt before storing
            testUser.setPassword(passwordEncoder.encode("admin123"));
            testUser.setRole("ADMIN");
            testUser.setIsActive(true);
            testUser.setCreatedAt(LocalDateTime.now());
            testUser.setUpdatedAt(LocalDateTime.now());
            
            userRepository.save(testUser);
            
            System.out.println("Created test user: admin@skillbridge.com / admin123 (password hashed with BCrypt)");
        }
    }

    public User findByUsername(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public LoginResponse login(LoginRequest req) {
        User user = findByUsername(req.getEmail());
        if (user == null) {
            return null;
        }
        
        // Verify password using BCrypt - compares plain text with hashed password
        if (user.getPassword() == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            return null;
        }
        
        String token = "token-" + UUID.randomUUID();
        return new LoginResponse(
            String.valueOf(user.getId()), 
            user.getEmail(), 
            user.getFullName(), 
            token
        );
    }

    public LoginResponse register(RegisterRequest req) {
        // Check if user already exists
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            return null;
        }
        
        // Create new user in database
        User newUser = new User();
        newUser.setEmail(req.getEmail());
        newUser.setFullName(req.getFullName() != null ? req.getFullName() : req.getEmail());
        newUser.setCompanyName(req.getCompanyName());
        newUser.setPhone(req.getPhone());
        // Hash the password using BCrypt before storing
        newUser.setPassword(passwordEncoder.encode(req.getPassword()));
        newUser.setRole("CLIENT");
        newUser.setIsActive(true);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());
        
        User savedUser = userRepository.save(newUser);
        
        String token = "token-" + UUID.randomUUID();
        return new LoginResponse(
            String.valueOf(savedUser.getId()), 
            savedUser.getEmail(), 
            savedUser.getFullName(), 
            token
        );
    }
}
