package com.skillbridge.controller.api.admin;

import com.skillbridge.dto.admin.response.AdminDashboardSummaryDTO;
import com.skillbridge.service.admin.AdminDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Admin Dashboard Controller
 * Handles dashboard endpoints for Admin Portal
 * Note: context-path is /api, so full path will be /api/admin/dashboard
 */
@RestController
@RequestMapping("/admin/dashboard")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:4200"}, 
             allowCredentials = "true",
             maxAge = 3600)
public class AdminDashboardController {

    @Autowired
    private AdminDashboardService adminDashboardService;

    /**
     * Get dashboard summary statistics
     * GET /api/admin/dashboard/summary
     * 
     * Returns summary statistics for engineers, system users, skills, and project types
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getDashboardSummary() {
        try {
            AdminDashboardSummaryDTO response = adminDashboardService.getDashboardSummary();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to get dashboard summary: " + e.getMessage()));
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

