package com.skillbridge.auth.controller;

import com.skillbridge.auth.dto.LoginRequest;
import com.skillbridge.auth.dto.LoginResponse;
import com.skillbridge.auth.dto.RegisterRequest;
import com.skillbridge.auth.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight in-memory authentication controller for testing with Postman.
 *
 * Endpoints:
 * POST /api/auth/login
 * POST /api/auth/register
 * POST /api/auth/logout
 *
 * NOTE: This is intentionally in-memory and uses a hardcoded test user.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    // In-memory user store: username -> User
    private final Map<String, User> users = new ConcurrentHashMap<>();

    // create a default test user (admin / admin123)
    public AuthController() {
        User defaultUser = new User(UUID.randomUUID().toString(), "admin", "admin123", "Administrator");
        users.put(defaultUser.getUsername(), defaultUser);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request == null || request.getUsername() == null || request.getPassword() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("username and password required");
        }

        User user = users.get(request.getUsername());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid credentials");
        }

        if (!user.getPassword().equals(request.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid credentials");
        }

        // return a mock token and basic user info
        String token = "mock-token-" + UUID.randomUUID();
        LoginResponse resp = new LoginResponse(user.getId(), user.getUsername(), user.getDisplayName(), token);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (request == null || request.getUsername() == null || request.getPassword() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("username, password required");
        }

        if (users.containsKey(request.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("username already exists");
        }

        User user = new User(UUID.randomUUID().toString(), request.getUsername(), request.getPassword(), request.getDisplayName());
        users.put(user.getUsername(), user);

        LoginResponse resp = new LoginResponse(user.getId(), user.getUsername(), user.getDisplayName(), "mock-token-registered");
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        // mock logout: just accept the request and return 200
        return ResponseEntity.ok("logged out");
    }
}
