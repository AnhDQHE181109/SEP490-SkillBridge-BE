package com.skillbridge.controller.api.auth;

import com.skillbridge.dto.auth.request.ForgotPasswordRequest;
import com.skillbridge.dto.auth.request.LoginRequest;
import com.skillbridge.dto.auth.response.ForgotPasswordResponse;
import com.skillbridge.dto.auth.response.LoginResponse;
import com.skillbridge.dto.auth.response.LogoutResponse;
import com.skillbridge.service.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller
 * Handles authentication endpoints (login, logout, forgot password)
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:4200"}, 
             allowCredentials = "true",
             maxAge = 3600)
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * Login endpoint
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Forgot password endpoint
     * POST /api/auth/forgot-password
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        try {
            authService.requestPasswordReset(request.getEmail());
            
            ForgotPasswordResponse response = new ForgotPasswordResponse();
            response.setMessage("If an account exists with this email, a password reset link has been sent.");
            response.setSuccess(true);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Always return success for security (don't reveal if email exists)
            ForgotPasswordResponse response = new ForgotPasswordResponse();
            response.setMessage("If an account exists with this email, a password reset link has been sent.");
            response.setSuccess(true);
            
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Logout endpoint
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(HttpServletRequest request) {
        // Optional: Extract token from request for blacklisting
        // For now, just return success (stateless JWT doesn't need server-side logout)
        
        LogoutResponse response = new LogoutResponse();
        response.setMessage("Logged out successfully");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Error Response DTO
     */
    public static class ErrorResponse {
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

