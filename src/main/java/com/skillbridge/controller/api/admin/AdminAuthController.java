package com.skillbridge.controller.api.admin;

import com.skillbridge.dto.auth.request.LoginRequest;
import com.skillbridge.dto.auth.response.LoginResponse;
import com.skillbridge.service.auth.AdminAuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Admin Authentication Controller
 * Handles authentication endpoints for Admin Portal (login)
 * Note: context-path is /api, so full path will be /api/admin/auth
 */
@RestController
@RequestMapping("/admin/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:4200"}, 
             allowCredentials = "true",
             maxAge = 3600)
public class AdminAuthController {

    @Autowired
    private AdminAuthService adminAuthService;

    /**
     * Login endpoint for Admin users
     * POST /admin/auth/login (full path: /api/admin/auth/login due to context-path)
     * Only users with ADMIN role can login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = adminAuthService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Error response class
     */
    private static class ErrorResponse {
        private String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}

