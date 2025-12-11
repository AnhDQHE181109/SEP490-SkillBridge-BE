package com.skillbridge.service.auth;

import com.skillbridge.dto.auth.request.LoginRequest;
import com.skillbridge.dto.auth.response.LoginResponse;
import com.skillbridge.dto.auth.response.UserDTO;
import com.skillbridge.entity.auth.PasswordResetToken;
import com.skillbridge.entity.auth.User;
import com.skillbridge.entity.common.EmailTemplate;
import com.skillbridge.repository.auth.PasswordResetTokenRepository;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.repository.common.EmailTemplateRepository;
import com.skillbridge.service.auth.PasswordService;
import com.skillbridge.service.common.EmailService;
import com.skillbridge.util.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Authentication Service
 * Handles authentication, login, password reset
 */
@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private EmailTemplateRepository emailTemplateRepository;

    @Autowired
    private EmailService emailService;

    @Value("${password.reset.token.expiration:3600}") // 1 hour in seconds
    private int tokenExpirationSeconds;

    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    /**
     * Login user with email and password (Client users only)
     * Only users with CLIENT role can login
     */
    public LoginResponse login(LoginRequest request) {
        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        // Check if user has CLIENT role
        String role = user.getRole();
        if (role == null || !role.equals("CLIENT")) {
            throw new RuntimeException("Access denied. This account does not have client permissions");
        }

        // Check if account is active
        if (!user.getIsActive()) {
            throw new RuntimeException("Your account is inactive. Please contact support");
        }

        // Verify password
        if (!passwordService.verifyPassword(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(user);

        // Build response
        UserDTO userDTO = convertToDTO(user);
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUser(userDTO);

        return response;
    }

    /**
     * Request password reset
     */
    @Transactional
    public void requestPasswordReset(String email) {
        // Find user by email (for security, don't reveal if email exists)
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Check if account is active
            if (!user.getIsActive()) {
                // Still return success for security (don't reveal account status)
                return;
            }

            // Invalidate any existing tokens for this user
            passwordResetTokenRepository.invalidateUserTokens(user.getId());

            // Generate reset token
            String token = UUID.randomUUID().toString();

            // Calculate expiration time
            LocalDateTime expiresAt = LocalDateTime.now()
                    .plusSeconds(tokenExpirationSeconds);

            // Save reset token
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUserId(user.getId());
            resetToken.setToken(token);
            resetToken.setExpiresAt(expiresAt);
            resetToken.setUsed(false);
            passwordResetTokenRepository.save(resetToken);

            // Prepare reset link based on user role
            String resetLinkPath;
            String userRole = user.getRole() != null ? user.getRole().toUpperCase() : "CLIENT";
            
            if ("SALES_MANAGER".equals(userRole) || "SALES_REP".equals(userRole)) {
                resetLinkPath = "/sales/reset-password";
            } else if ("ADMIN".equals(userRole)) {
                resetLinkPath = "/admin/reset-password";
            } else {
                resetLinkPath = "/client/reset-password";
            }
            
            String resetLink = String.format(
                    "%s%s?token=%s",
                    baseUrl,
                    resetLinkPath,
                    token
            );

            // Send password reset email using EmailService
            int expirationMinutes = tokenExpirationSeconds / 60;
            emailService.sendPasswordResetEmail(user, resetLink, expirationMinutes);
        }

        // Always return success for security (don't reveal if email exists)
    }

    /**
     * Change password for authenticated user
     * @param userId User ID
     * @param currentPassword Current password
     * @param newPassword New password
     * @throws RuntimeException if user not found, current password incorrect, or new password invalid
     */
    @Transactional
    public void changePassword(Integer userId, String currentPassword, String newPassword) {
        // Find user by ID
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if account is active
        if (!user.getIsActive()) {
            throw new RuntimeException("Your account is inactive. Please contact support");
        }

        // Verify current password
        if (!passwordService.verifyPassword(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        // Validate new password (same as current password check - should be different)
        if (passwordService.verifyPassword(newPassword, user.getPassword())) {
            throw new RuntimeException("New password must be different from current password");
        }

        // Hash new password and update
        String hashedNewPassword = passwordService.hashPassword(newPassword);
        user.setPassword(hashedNewPassword);
        userRepository.save(user);
    }

    /**
     * Reset password using token
     * @param token Reset token
     * @param newPassword New password
     * @throws RuntimeException if token invalid, expired, or user not found
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        // Find valid token
        LocalDateTime now = LocalDateTime.now();
        PasswordResetToken resetToken = passwordResetTokenRepository.findValidToken(token, now)
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));

        // Find user by user ID from token
        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if account is active
        if (!user.getIsActive()) {
            throw new RuntimeException("Your account is inactive. Please contact support");
        }

        // Hash new password and update
        String hashedNewPassword = passwordService.hashPassword(newPassword);
        user.setPassword(hashedNewPassword);
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    /**
     * Convert User to UserDTO
     */
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setRole(user.getRole());
        dto.setCompanyName(user.getCompanyName());
        return dto;
    }
}

