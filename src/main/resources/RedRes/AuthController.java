package com.skillbridge.controller;

import com.skillbridge.dto.LoginRequest;
import com.skillbridge.dto.LoginResponse;
import com.skillbridge.dto.RegisterRequest;
import com.skillbridge.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request == null || request.getEmail() == null || request.getPassword() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("email and password required");
        }

        LoginResponse resp = userService.login(request);
        if (resp == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid credentials");
        }
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (request == null || request.getEmail() == null || request.getPassword() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("email and password required");
        }

        LoginResponse resp = userService.register(request);
        if (resp == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("email already exists");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok("logged out");
    }
}
