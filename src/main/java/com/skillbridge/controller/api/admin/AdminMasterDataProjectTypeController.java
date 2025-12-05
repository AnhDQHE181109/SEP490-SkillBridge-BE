package com.skillbridge.controller.api.admin;

import com.skillbridge.dto.admin.response.ProjectTypeListResponse;
import com.skillbridge.service.admin.AdminProjectTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Admin Master Data Project Type Controller
 * Handles project type management endpoints for Admin Master Data
 * Note: context-path is /api, so full path will be /api/admin/master-data/project-types
 */
@RestController
@RequestMapping("/admin/master-data/project-types")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:4200"}, 
             allowCredentials = "true",
             maxAge = 3600)
public class AdminMasterDataProjectTypeController {

    @Autowired
    private AdminProjectTypeService adminProjectTypeService;

    /**
     * Get all project types with pagination and search
     * GET /api/admin/master-data/project-types
     * 
     * Query parameters:
     * - page: Page number (default: 0)
     * - size: Page size (default: 20)
     * - search: Search term (optional)
     */
    @GetMapping
    public ResponseEntity<?> getAllProjectTypes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search
    ) {
        try {
            ProjectTypeListResponse response = adminProjectTypeService.getAllProjectTypes(page, size, search);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to get project types: " + e.getMessage()));
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

