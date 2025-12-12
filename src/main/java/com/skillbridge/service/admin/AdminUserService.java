package com.skillbridge.service.admin;

import com.skillbridge.dto.admin.request.CreateUserRequest;
import com.skillbridge.dto.admin.request.UserListRequest;
import com.skillbridge.dto.admin.response.PageInfo;
import com.skillbridge.dto.admin.response.UserListResponseDTO;
import com.skillbridge.dto.admin.response.UserResponseDTO;
import com.skillbridge.entity.auth.User;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.service.auth.PasswordService;
import com.skillbridge.service.common.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin User Service
 * Handles all user operations for Admin User List
 */
@Service
@Transactional
public class AdminUserService {


    private static final Logger log = LoggerFactory.getLogger(AdminUserService.class);
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private EmailService emailService;

    /**
     * Get all users with pagination, search, and filter
     */
    public UserListResponseDTO getUsers(UserListRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getPageSize());
        Page<User> userPage;

        // Convert status string to Boolean
        Boolean isActive = null;
        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            if ("active".equalsIgnoreCase(request.getStatus())) {
                isActive = true;
            } else if ("deleted".equalsIgnoreCase(request.getStatus())) {
                isActive = false;
            }
        }

        // Build search term
        String searchTerm = (request.getSearch() != null && !request.getSearch().trim().isEmpty()) 
            ? request.getSearch().trim() 
            : null;

        // Determine role filter - if not specified, only show SALES_MANAGER and SALES_REP
        String roleFilter = request.getRole();
        if (roleFilter == null || roleFilter.isEmpty()) {
            // Default: only show SALES_MANAGER and SALES_REP
            // We'll filter in the query by using a list of allowed roles
            // For now, we'll use the repository method and filter in stream
        }

        // Use repository method with filters
        userPage = userRepository.findUsersWithFilters(
            searchTerm,
            roleFilter,
            isActive,
            pageable
        );

        // Filter to only SALES_MANAGER and SALES_REP roles if no role filter specified
        List<UserResponseDTO> users = userPage.getContent().stream()
            .filter(user -> {
                // If role filter is specified, already filtered by repository
                if (roleFilter != null && !roleFilter.isEmpty()) {
                    return true;
                }
                // Otherwise, only show SALES_MANAGER and SALES_REP
                return "SALES_MANAGER".equals(user.getRole()) || "SALES_REP".equals(user.getRole());
            })
            .map(this::convertToDTO)
            .collect(Collectors.toList());

        PageInfo pageInfo = new PageInfo(
            userPage.getTotalElements(),
            userPage.getTotalPages(),
            userPage.getNumber(),
            userPage.getSize()
        );

        UserListResponseDTO response = new UserListResponseDTO();
        response.setUsers(users);
        response.setPage(pageInfo);

        return response;
    }

    /**
     * Get user by ID
     * @param id User ID
     * @return UserResponseDTO with user information
     * @throws RuntimeException if user not found
     */
    public UserResponseDTO getUserById(Integer id) {
        log.info("Fetching user with ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found with ID: {}", id);
                    return new RuntimeException("User not found");
                });
        return convertToDTO(user);
    }

    /**
     * Update an existing user
     * @param id User ID
     * @param request UpdateUserRequest with user details (email is NOT included)
     * @return UserResponseDTO with updated user information
     * @throws RuntimeException if user not found
     */
    public UserResponseDTO updateUser(Integer id, com.skillbridge.dto.admin.request.UpdateUserRequest request) {
        log.info("Updating user with ID: {}", id);
        
        User user = userRepository.findById(id)
            .orElseThrow(() -> {
                log.warn("User not found with ID: {}", id);
                return new RuntimeException("User not found");
            });

        // Update allowed fields only
        user.setFullName(request.getFullName());
        user.setRole(request.getRole());
        user.setPhone(request.getPhone());
        // Email is NOT updated - preserve existing email
        // Password is NOT updated - preserve existing password

        // Save user to database
        user = userRepository.save(user);
        log.info("User updated successfully. User ID: {}", user.getId());

        // Convert to DTO and return
        return convertToDTO(user);
    }

    /**
     * Create a new user
     * @param request CreateUserRequest with user details
     * @return UserResponseDTO with created user information
     * @throws RuntimeException if email already exists
     */
    public UserResponseDTO createUser(CreateUserRequest request) {
        log.info("Creating new user with email: {}", request.getEmail());

        // Validate email uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Email already exists: {}", request.getEmail());
            throw new RuntimeException("Email already exists. Please use a different email.");
        }

        // Generate secure random password
        String plainPassword = passwordService.generateRandomPassword();
        String hashedPassword = passwordService.hashPassword(plainPassword);

        // Create user entity
        User user = new User();
        user.setFullName(request.getFullName());
        user.setRole(request.getRole());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(hashedPassword);
        user.setFirstPassword(plainPassword);
        user.setIsActive(true);

        // Save user to database
        user = userRepository.save(user);
        log.info("User created successfully. User ID: {}", user.getId());

        // Send welcome email asynchronously (non-blocking)
        try {
            emailService.sendWelcomeEmail(user, plainPassword);
            log.info("Welcome email sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            // Log error but don't fail user creation
            log.error("Failed to send welcome email to user: " + user.getEmail(), e);
        }

        // Convert to DTO and return
        return convertToDTO(user);
    }

    /**
     * Delete user (soft delete - set is_active = false)
     */
    public void deleteUser(Integer userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Soft delete - set is_active to false
        user.setIsActive(false);
        userRepository.save(user);
    }

    /**
     * Convert User entity to UserResponseDTO
     */
    private UserResponseDTO convertToDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getFullName(),
                user.getRole(),
                user.getEmail(),
                user.getPhone(),
                user.getIsActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}

