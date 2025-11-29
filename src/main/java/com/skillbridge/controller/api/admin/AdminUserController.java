package com.skillbridge.controller.api.admin;

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

