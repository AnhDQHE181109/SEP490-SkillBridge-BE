package com.skillbridge.service.admin;

import com.skillbridge.dto.admin.response.UserResponseDTO;
import com.skillbridge.entity.auth.User;
import com.skillbridge.repository.auth.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdminUserService {


    private static final Logger log = LoggerFactory.getLogger(AdminUserService.class);
    @Autowired
    private UserRepository userRepository;

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
