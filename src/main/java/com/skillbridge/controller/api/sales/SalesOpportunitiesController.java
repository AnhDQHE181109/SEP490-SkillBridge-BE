package com.skillbridge.controller.api.sales;

import com.skillbridge.dto.sales.response.OpportunitiesListResponse;
import com.skillbridge.entity.auth.User;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.service.sales.SalesOpportunitiesService;
import com.skillbridge.util.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Sales Opportunities Controller
 * Handles opportunities list endpoints for Sales Portal
 * Note: context-path is /api, so full path will be /api/sales/opportunities
 */
@RestController
@RequestMapping("/sales/opportunities")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:4200"}, 
             allowCredentials = "true",
             maxAge = 3600)
// Note: @PreAuthorize disabled in dev mode, role check done manually in controller
// @PreAuthorize("hasAnyRole('SALES_MANAGER', 'SALES_REP')")
public class SalesOpportunitiesController {

    @Autowired
    private SalesOpportunitiesService salesOpportunitiesService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * Get opportunities list with search, filter, and pagination
     * GET /sales/opportunities (full path: /api/sales/opportunities due to context-path)
     * 
     * Role-based filtering:
     * - Sales Manager: sees all opportunities
     * - Sales Man: sees only opportunities created by them
     */
    @GetMapping
    public ResponseEntity<OpportunitiesListResponse> getOpportunities(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) Integer assigneeUserId,
            @RequestParam(required = false) Integer createdBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication,
            HttpServletRequest request
    ) {
        // Get current user from authentication or token
        User currentUser = getCurrentUser(authentication, request);
        
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        // Check if user has sales role
        String role = currentUser.getRole();
        if (role == null || (!role.equals("SALES_MANAGER") && !role.equals("SALES_REP"))) {
            return ResponseEntity.status(403).build();
        }

        // Get opportunities with role-based filtering
        OpportunitiesListResponse response = salesOpportunitiesService.getOpportunities(
                search,
                status,
                assigneeUserId,
                createdBy,
                currentUser.getId(),
                currentUser.getRole(),
                page,
                size
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get current user from authentication or JWT token
     */
    private User getCurrentUser(Authentication authentication, HttpServletRequest request) {
        // Try to get user from authentication (works in production with Spring Security)
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            org.springframework.security.core.userdetails.UserDetails userDetails = 
                (org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal();
            return userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        }

        // Fallback: Try to get user from JWT token in Authorization header (for dev mode)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                String email = jwtTokenProvider.getEmailFromToken(token);
                return userRepository.findByEmail(email).orElse(null);
            } catch (Exception e) {
                // Token invalid or expired
                return null;
            }
        }

        return null;
    }
}

