package com.skillbridge.service.admin;

import com.skillbridge.dto.admin.request.CreateUserRequest;
import com.skillbridge.dto.admin.request.UpdateUserRequest;
import com.skillbridge.dto.admin.request.UserListRequest;
import com.skillbridge.dto.admin.response.UserListResponseDTO;
import com.skillbridge.dto.admin.response.UserResponseDTO;
import com.skillbridge.entity.auth.User;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.service.auth.PasswordService;
import com.skillbridge.service.common.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AdminUserService}
 */
@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordService passwordService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AdminUserService adminUserService;

    private User salesManager;
    private User adminUser;

    @BeforeEach
    void setUp() {
        salesManager = createUser(1, "Alice", "alice@example.com", "SALES_MANAGER", true);
        adminUser = createUser(2, "Admin", "admin@example.com", "ADMIN", true);
    }

    @Test
    @DisplayName("getUsers - default filters only include sales roles")
    void testGetUsers_DefaultFilters() {
        UserListRequest request = new UserListRequest();
        request.setPage(0);
        request.setPageSize(10);

        Page<User> page = new PageImpl<>(List.of(salesManager, adminUser), PageRequest.of(0, 10), 2);
        when(userRepository.findUsersWithFilters(null, null, null, PageRequest.of(0, 10))).thenReturn(page);

        UserListResponseDTO response = adminUserService.getUsers(request);

        assertEquals(1, response.getUsers().size());
        assertEquals("Alice", response.getUsers().get(0).getFullName());
        verify(userRepository).findUsersWithFilters(null, null, null, PageRequest.of(0, 10));
    }

    @Test
    @DisplayName("getUsers - with role filter returns all roles from repository")
    void testGetUsers_WithRoleFilter() {
        UserListRequest request = new UserListRequest();
        request.setRole("ADMIN");
        request.setPageSize(5);

        Page<User> page = new PageImpl<>(List.of(adminUser), PageRequest.of(0, 5), 1);
        when(userRepository.findUsersWithFilters(null, "ADMIN", null, PageRequest.of(0, 5))).thenReturn(page);

        UserListResponseDTO response = adminUserService.getUsers(request);

        assertEquals(1, response.getUsers().size());
        assertEquals("Admin", response.getUsers().get(0).getFullName());
        verify(userRepository).findUsersWithFilters(null, "ADMIN", null, PageRequest.of(0, 5));
    }

    @Test
    @DisplayName("getUserById - throws when not found")
    void testGetUserById_NotFound() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> adminUserService.getUserById(99));
    }

    @Test
    @DisplayName("updateUser - updates allowed fields")
    void testUpdateUser_Success() {
        when(userRepository.findById(1)).thenReturn(Optional.of(salesManager));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserRequest updateRequest = new UpdateUserRequest();
        updateRequest.setFullName("Alice Updated");
        updateRequest.setRole("SALES_REP");
        updateRequest.setPhone("999");

        UserResponseDTO response = adminUserService.updateUser(1, updateRequest);

        assertEquals("Alice Updated", response.getFullName());
        assertEquals("SALES_REP", response.getRole());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("createUser - throws when email exists")
    void testCreateUser_EmailExists() {
        CreateUserRequest request = new CreateUserRequest("Bob", "SALES_MANAGER", "bob@example.com", null);
        when(userRepository.existsByEmail("bob@example.com")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> adminUserService.createUser(request));
    }

    @Test
    @DisplayName("createUser - saves user and sends welcome email")
    void testCreateUser_Success() {
        CreateUserRequest request = new CreateUserRequest("Bob", "SALES_MANAGER", "bob@example.com", "123");

        when(userRepository.existsByEmail("bob@example.com")).thenReturn(false);
        when(passwordService.generateRandomPassword()).thenReturn("Plain123!");
        when(passwordService.hashPassword("Plain123!")).thenReturn("hashed");

        User savedUser = createUser(3, "Bob", "bob@example.com", "SALES_MANAGER", true);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponseDTO response = adminUserService.createUser(request);

        assertEquals("Bob", response.getFullName());
        verify(passwordService).generateRandomPassword();
        verify(passwordService).hashPassword("Plain123!");
        verify(emailService).sendWelcomeEmail(savedUser, "Plain123!");
    }

    @Test
    @DisplayName("deleteUser - sets isActive to false")
    void testDeleteUser() {
        when(userRepository.findById(1)).thenReturn(Optional.of(salesManager));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adminUserService.deleteUser(1);

        assertFalse(salesManager.getIsActive());
        verify(userRepository).save(salesManager);
    }

    private User createUser(Integer id, String name, String email, String role, boolean active) {
        User user = new User();
        user.setId(id);
        user.setFullName(name);
        user.setEmail(email);
        user.setRole(role);
        user.setPhone("123");
        user.setIsActive(active);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }
}

