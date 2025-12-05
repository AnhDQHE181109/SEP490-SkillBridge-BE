package com.skillbridge.controller.api.auth;

import com.skillbridge.dto.auth.request.ChangePasswordRequest;
import com.skillbridge.dto.auth.request.ForgotPasswordRequest;
import com.skillbridge.dto.auth.request.LoginRequest;
import com.skillbridge.dto.auth.request.ResetPasswordRequest;
import com.skillbridge.dto.auth.response.ChangePasswordResponse;
import com.skillbridge.dto.auth.response.ForgotPasswordResponse;
import com.skillbridge.dto.auth.response.LoginResponse;
import com.skillbridge.dto.auth.response.LogoutResponse;
import com.skillbridge.dto.auth.response.ResetPasswordResponse;
import com.skillbridge.service.auth.AuthService;
import com.skillbridge.util.JwtTokenProvider;
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
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

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
     * Change password endpoint
     * POST /api/auth/change-password
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {
        try {
            // Extract user ID from JWT token
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Authorization token is required"));
            }
            
            String token = authHeader.substring(7);
            Integer userId = jwtTokenProvider.getUserIdFromToken(token);
            
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Invalid or expired token"));
            }
            
            // Validate that new password matches confirm password
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("New password and confirm password do not match"));
            }
            
            // Change password
            authService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());
            
            ChangePasswordResponse response = new ChangePasswordResponse();
            response.setSuccess(true);
            response.setMessage("Password changed successfully");
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("An error occurred while changing password"));
        }
    }
    
    /**
     * Reset password endpoint
     * POST /api/auth/reset-password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        try {
            // Validate that new password matches confirm password
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("New password and confirm password do not match"));
            }
            
            // Reset password
            authService.resetPassword(request.getToken(), request.getNewPassword());
            
            ResetPasswordResponse response = new ResetPasswordResponse();
            response.setSuccess(true);
            response.setMessage("Password reset successfully");
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("An error occurred while resetting password"));
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

