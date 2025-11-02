package com.skillbridge.service;

import com.skillbridge.dto.LoginResponse;
import com.skillbridge.dto.LoginRequest;
import com.skillbridge.dto.RegisterRequest;
import com.skillbridge.entity.User;
import com.skillbridge.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {

    // in-memory store for test/demo purposes
    private final Map<String, User> inMemoryUsers = new ConcurrentHashMap<>();

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        // seed admin user
        User admin = new User("admin", "admin123", "Administrator");
        admin.setId(0L);
        inMemoryUsers.put(admin.getUsername(), admin);
    }

    public User findByUsername(String username) {
        // first check in-memory
        if (inMemoryUsers.containsKey(username)) {
            return inMemoryUsers.get(username);
        }

        // fallback to repository if present and DB populated
        return userRepository.findByUsername(username).orElse(null);
    }

    public LoginResponse login(LoginRequest req) {
        User u = findByUsername(req.getUsername());
        if (u == null) return null;
        if (!u.getPassword().equals(req.getPassword())) return null;
        String token = "mock-token-" + UUID.randomUUID();
        return new LoginResponse(u.getId() == null ? "" + UUID.randomUUID() : String.valueOf(u.getId()), u.getUsername(), u.getDisplayName(), token);
    }

    public LoginResponse register(RegisterRequest req) {
        if (inMemoryUsers.containsKey(req.getUsername())) return null;
        // Register in-memory only for now
        User u = new User(req.getUsername(), req.getPassword(), req.getDisplayName());
        u.setId(System.currentTimeMillis());
        inMemoryUsers.put(u.getUsername(), u);
        return new LoginResponse(String.valueOf(u.getId()), u.getUsername(), u.getDisplayName(), "mock-token-registered");
    }
}
