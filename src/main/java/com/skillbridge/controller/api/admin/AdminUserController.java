package com.skillbridge.controller.api.admin;

import com.skillbridge.dto.admin.request.CreateUserRequest;
import com.skillbridge.dto.admin.request.UpdateUserRequest;
import com.skillbridge.dto.admin.request.UserListRequest;
import com.skillbridge.dto.admin.response.UserListResponseDTO;
import com.skillbridge.dto.admin.response.UserResponseDTO;
import com.skillbridge.service.admin.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Admin User Controller
 * Handles user management endpoints for Admin User List
 * Note: context-path is /api, so full path will be /api/admin/users
 */
@RestController
@RequestMapping("/admin/users")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:4200"}, 
             allowCredentials = "true",
             maxAge = 3600)
public class AdminUserController {

    @Autowired
    private AdminUserService adminUserService;

    /**
     * Get list of users with pagination, search, and filter
     * GET /api/admin/users
     * 
     * Query parameters:
     * - page: Page number (default: 0)
     * - pageSize: Page size (default: 10)
     * - search: Search query (optional)
     * - role: Filter by role (optional: SALES_MANAGER, SALES_REP)
     * - status: Filter by status (optional: active, deleted)
     */
    @GetMapping
    public ResponseEntity<?> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status
    ) {
        try {
            UserListRequest request = new UserListRequest();
            request.setPage(page);
            request.setPageSize(pageSize);
            request.setSearch(search);
            request.setRole(role);
            request.setStatus(status);

            UserListResponseDTO response = adminUserService.getUsers(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to get users: " + e.getMessage()));
        }
    }

    /**
     * Get user by ID
     * GET /api/admin/users/{id}
     * 
     * Path parameter: id (user ID)
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Integer id) {
        try {
            UserResponseDTO response = adminUserService.getUserById(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // Handle user not found
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to get user: " + e.getMessage()));
        }
    }

    /**
     * Create a new user
     * POST /api/admin/users
     * 
     * Request body: CreateUserRequest
     * - fullName: String (required)
     * - role: String (required, must be SALES_MANAGER or SALES_REP)
     * - email: String (required, valid email format)
     * - phone: String (optional)
     */
    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request) {
        try {
            UserResponseDTO response = adminUserService.createUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            // Handle business logic errors (e.g., email already exists)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to create user: " + e.getMessage()));
        }
    }

    /**
     * Update an existing user
     * PUT /api/admin/users/{id}
     * 
     * Path parameter: id (user ID)
     * Request body: UpdateUserRequest
     * - fullName: String (required)
     * - role: String (required, must be SALES_MANAGER or SALES_REP)
     * - phone: String (optional)
     * Note: Email is NOT included in request (cannot be edited)
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Integer id, @Valid @RequestBody UpdateUserRequest request) {
        try {
            UserResponseDTO response = adminUserService.updateUser(id, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // Handle user not found or validation errors
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to update user: " + e.getMessage()));
        }
    }

    /**
     * Delete user (soft delete)
     * DELETE /api/admin/users/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Integer id) {
        try {
            adminUserService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to delete user: " + e.getMessage()));
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

