package com.skillbridge.service;

import com.skillbridge.dto.LoginResponse;
import com.skillbridge.dto.LoginRequest;
import com.skillbridge.dto.RegisterRequest;
import com.skillbridge.entity.User;
import com.skillbridge.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {

    // Temporary password store since the DB doesn't have password field yet
    // In a real app, you'd add a password column to the users table
    private final Map<String, String> passwordStore = new ConcurrentHashMap<>();

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        
        // Create a test user if not exists
        createTestUserIfNotExists();
    }

    private void createTestUserIfNotExists() {
        if (userRepository.findByEmail("admin@skillbridge.com").isEmpty()) {
            User testUser = new User();
            testUser.setEmail("admin@skillbridge.com");
            testUser.setFullName("Administrator");
            testUser.setCompanyName("SkillBridge");
            testUser.setRole("ADMIN");
            testUser.setIsActive(true);
            testUser.setCreatedAt(LocalDateTime.now());
            testUser.setUpdatedAt(LocalDateTime.now());
            
            User savedUser = userRepository.save(testUser);
            passwordStore.put(savedUser.getEmail(), "admin123");
            
            System.out.println("Created test user: admin@skillbridge.com / admin123");
        }
    }

    public User findByUsername(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public LoginResponse login(LoginRequest req) {
        User user = findByUsername(req.getUsername());
        if (user == null) {
            return null;
        }
        
        // Check password from our temporary store
        String storedPassword = passwordStore.get(user.getEmail());
        if (storedPassword == null || !storedPassword.equals(req.getPassword())) {
            return null;
        }
        
        String token = "token-" + UUID.randomUUID();
        return new LoginResponse(
            String.valueOf(user.getId()), 
            user.getEmail(), 
            user.getDisplayName(), 
            token
        );
    }

    public LoginResponse register(RegisterRequest req) {
        // Check if user already exists
        if (userRepository.findByEmail(req.getUsername()).isPresent()) {
            return null;
        }
        
        // Create new user in database
        User newUser = new User();
        newUser.setEmail(req.getUsername());
        newUser.setFullName(req.getDisplayName() != null ? req.getDisplayName() : req.getUsername());
        newUser.setRole("CLIENT");
        newUser.setIsActive(true);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());
        
        User savedUser = userRepository.save(newUser);
        
        // Store password temporarily
        passwordStore.put(savedUser.getEmail(), req.getPassword());
        
        String token = "token-" + UUID.randomUUID();
        return new LoginResponse(
            String.valueOf(savedUser.getId()), 
            savedUser.getEmail(), 
            savedUser.getDisplayName(), 
            token
        );
    }
}
