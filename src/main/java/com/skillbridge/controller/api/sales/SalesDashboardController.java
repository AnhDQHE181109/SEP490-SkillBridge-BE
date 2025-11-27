package com.skillbridge.controller.api.sales;

import com.skillbridge.dto.sales.response.ErrorResponse;
import com.skillbridge.dto.sales.response.SalesDashboardActivitiesDTO;
import com.skillbridge.dto.sales.response.SalesDashboardApprovalsDTO;
import com.skillbridge.dto.sales.response.SalesDashboardSummaryDTO;
import com.skillbridge.entity.auth.User;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.service.sales.SalesDashboardService;
import com.skillbridge.util.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Sales Dashboard Controller
 * Handles dashboard endpoints for Sales Portal
 * Note: context-path is /api, so full path will be /api/sales/dashboard
 */
@RestController
@RequestMapping("/sales/dashboard")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:4200"}, 
             allowCredentials = "true",
             maxAge = 3600)
public class SalesDashboardController {

    @Autowired
    private SalesDashboardService salesDashboardService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * Get dashboard summary statistics
     * GET /api/sales/dashboard/summary
     * 
     * Role-based filtering:
     * - Sales Manager: Returns all system data including revenue
     * - Sales Rep: Returns only assigned data, no revenue
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(
            Authentication authentication,
            HttpServletRequest request
    ) {
        User currentUser = getCurrentUser(authentication, request);
        
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        // Check if user has sales role
        String role = currentUser.getRole();
        if (role == null || (!role.equals("SALES_MANAGER") && !role.equals("SALES_REP"))) {
            return ResponseEntity.status(403).build();
        }

        try {
            SalesDashboardSummaryDTO summary = salesDashboardService.getSummary(currentUser);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Failed to get dashboard summary: " + e.getMessage()));
        }
    }

    /**
     * Get approvals waiting from clients
     * GET /api/sales/dashboard/approvals
     * 
     * Role-based filtering:
     * - Sales Manager: Returns all approvals
     * - Sales Rep: Returns only approvals for assigned items
     */
    @GetMapping("/approvals")
    public ResponseEntity<?> getApprovals(
            Authentication authentication,
            HttpServletRequest request
    ) {
        User currentUser = getCurrentUser(authentication, request);
        
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        // Check if user has sales role
        String role = currentUser.getRole();
        if (role == null || (!role.equals("SALES_MANAGER") && !role.equals("SALES_REP"))) {
            return ResponseEntity.status(403).build();
        }

        try {
            SalesDashboardApprovalsDTO approvals = salesDashboardService.getApprovalsWaiting(currentUser);
            return ResponseEntity.ok(approvals);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Failed to get approvals: " + e.getMessage()));
        }
    }

    /**
     * Get recent client activities
     * GET /api/sales/dashboard/activities
     * 
     * Role-based filtering:
     * - Sales Manager: Returns all activities
     * - Sales Rep: Returns only activities for assigned items
     */
    @GetMapping("/activities")
    public ResponseEntity<?> getActivities(
            Authentication authentication,
            HttpServletRequest request
    ) {
        User currentUser = getCurrentUser(authentication, request);
        
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        // Check if user has sales role
        String role = currentUser.getRole();
        if (role == null || (!role.equals("SALES_MANAGER") && !role.equals("SALES_REP"))) {
            return ResponseEntity.status(403).build();
        }

        try {
            SalesDashboardActivitiesDTO activities = salesDashboardService.getRecentActivities(currentUser);
            return ResponseEntity.ok(activities);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Failed to get activities: " + e.getMessage()));
        }
    }

    /**
     * Get current user from authentication or JWT token
     */
    private User getCurrentUser(Authentication authentication, HttpServletRequest request) {
        // Try to get user from authentication first
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            return userRepository.findByEmail(username).orElse(null);
        }

        // Fallback to JWT token from request
        String token = extractTokenFromRequest(request);
        if (token != null) {
            String username = jwtTokenProvider.getUsernameFromToken(token);
            User user = userRepository.findByEmail(username).orElse(null);
            if (user != null && jwtTokenProvider.validateToken(token, user)) {
                return user;
            }
        }

        return null;
    }

    /**
     * Extract JWT token from request header or cookie
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        // Try Authorization header first
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // Try cookie
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}

