package com.skillbridge.service.auth;

import com.skillbridge.dto.auth.request.LoginRequest;
import com.skillbridge.dto.auth.response.LoginResponse;
import com.skillbridge.dto.auth.response.UserDTO;
import com.skillbridge.entity.auth.User;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.util.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Sales Authentication Service
 * Handles authentication for Sales users (Sales Manager and Sales Rep)
 */
@Service
public class SalesAuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * Login user with email and password (Sales users only)
     * Only users with SALES_MANAGER or SALES_REP role can login
     */
    public LoginResponse login(LoginRequest request) {
        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        // Check if user has SALES role (SALES_MANAGER or SALES_REP)
        String role = user.getRole();
        if (role == null || (!role.equals("SALES_MANAGER") && !role.equals("SALES_REP"))) {
            throw new RuntimeException("Access denied. This account does not have sales permissions");
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

