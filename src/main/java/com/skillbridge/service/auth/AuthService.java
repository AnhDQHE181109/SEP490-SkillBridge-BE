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

    @Value("${password.reset.token.expiration:3600}") // 1 hour in seconds
    private int tokenExpirationSeconds;

    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    /**
     * Login user with email and password
     */
    public LoginResponse login(LoginRequest request) {
        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

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

            // Prepare reset link
            String resetLink = String.format(
                    "%s/client/reset-password?token=%s",
                    baseUrl,
                    token
            );

            // Send password reset email (commented until SES configured)
            sendPasswordResetEmail(user, resetLink);
        }

        // Always return success for security (don't reveal if email exists)
    }

    /**
     * Send password reset email
     */
    private void sendPasswordResetEmail(User user, String resetLink) {
        try {
            EmailTemplate template = emailTemplateRepository
                    .findByTemplateName("password_reset")
                    .orElse(null);

            String subject = "Password Reset Request";
            String body = "Hello " + (user.getFullName() != null ? user.getFullName() : "") + ",\n\n" +
                    "You requested a password reset. Please click the link below to reset your password:\n\n" +
                    resetLink + "\n\n" +
                    "This link will expire in " + (tokenExpirationSeconds / 60) + " minutes.\n\n" +
                    "If you did not request this, please ignore this email.\n\n" +
                    "Best regards,\nSkillBridge Team";

            if (template != null) {
                subject = template.getSubject();
                body = template.getBody()
                        .replace("{name}", user.getFullName() != null ? user.getFullName() : "")
                        .replace("{reset_link}", resetLink)
                        .replace("{expiration_minutes}", String.valueOf(tokenExpirationSeconds / 60));
            }

            // Log email content (for development/testing)
            System.out.println("=== Password Reset Email Content Prepared (SES not enabled) ===");
            System.out.println("To: " + user.getEmail());
            System.out.println("Subject: " + subject);
            System.out.println("Body: " + body);
            System.out.println("Reset Link: " + resetLink);
            System.out.println("==============================================================");

        } catch (Exception e) {
            // Log error but don't fail the request (security: don't reveal if email exists)
            System.err.println("Failed to prepare password reset email: " + e.getMessage());
            e.printStackTrace();
        }
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

