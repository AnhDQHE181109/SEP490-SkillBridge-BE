package com.skillbridge.controller.api.sales;

import com.skillbridge.dto.contract.request.CreateProjectCloseRequestRequest;
import com.skillbridge.dto.contract.request.ResubmitProjectCloseRequestRequest;
import com.skillbridge.dto.contract.response.ProjectCloseRequestResponse;
import com.skillbridge.entity.auth.User;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.service.contract.ProjectCloseRequestService;
import com.skillbridge.util.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Sales Project Close Request Controller
 * Handles project close request endpoints for Sales Portal
 * Story-41: Project Close Request for SOW Contract
 */
@RestController
@RequestMapping("/sales/sows/{sowId}/close-requests")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:4200"}, 
             allowCredentials = "true",
             maxAge = 3600)
public class SalesProjectCloseRequestController {

    @Autowired
    private ProjectCloseRequestService projectCloseRequestService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * Create a new Project Close Request
     * POST /sales/sows/{sowId}/close-requests
     */
    @PostMapping
    public ResponseEntity<?> createCloseRequest(
            @PathVariable Integer sowId,
            @RequestBody CreateProjectCloseRequestRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        try {
            User currentUser = getCurrentUser(authentication, httpRequest);
            if (currentUser == null) {
                return ResponseEntity.status(401).body(new ErrorResponse("User not authenticated"));
            }

            String role = currentUser.getRole();
            if (role == null || (!role.equals("SALES_MANAGER") && !role.equals("SALES_REP"))) {
                return ResponseEntity.status(403).body(new ErrorResponse("Access denied: Only Sales Representatives and Sales Managers can create close requests"));
            }

            ProjectCloseRequestResponse response = projectCloseRequestService.createCloseRequest(
                    sowId, request, currentUser
            );

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Internal server error: " + e.getMessage()));
        }
    }
    
    /**
     * Resubmit a rejected Close Request
     * POST /sales/close-requests/{id}/resubmit
     */
    @PostMapping("/{id}/resubmit")
    public ResponseEntity<?> resubmitCloseRequest(
        @PathVariable("id") Integer closeRequestId,
        @RequestBody ResubmitProjectCloseRequestRequest request,
        Authentication authentication,
        HttpServletRequest httpRequest
    ) {
        try {
            User currentUser = getCurrentUser(authentication, httpRequest);
            if (currentUser == null) {
                return ResponseEntity.status(401).body(new ErrorResponse("User not authenticated"));
            }
            
            String role = currentUser.getRole();
            if (role == null || (!role.equals("SALES_MANAGER") && !role.equals("SALES_REP"))) {
                return ResponseEntity.status(403).body(new ErrorResponse("Access denied: Only Sales Representatives and Sales Managers can resubmit close requests"));
            }
            
            ProjectCloseRequestResponse response = projectCloseRequestService.resubmitCloseRequest(
                closeRequestId, request, currentUser
            );
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Get current user from authentication or JWT token
     */
    private User getCurrentUser(Authentication authentication, HttpServletRequest request) {
        // Try to get from authentication first
        if (authentication != null && authentication.getPrincipal() != null) {
            try {
                String principal = authentication.getPrincipal().toString();

                // If principal is email, find user by email
                if (principal.contains("@")) {
                    return userRepository.findByEmail(principal).orElse(null);
                }

                // Otherwise, try to parse as user ID
                try {
                    Integer userId = Integer.parseInt(principal);
                    return userRepository.findById(userId).orElse(null);
                } catch (NumberFormatException e) {
                    // If not a number, try to find by email
                    return userRepository.findByEmail(principal).orElse(null);
                }
            } catch (Exception e) {
                // Continue to try token
            }
        }

        // Fallback: try to get from JWT token in Authorization header
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                // Validate and extract user info from token
                if (!jwtTokenProvider.isTokenExpired(token)) {
                    String email = jwtTokenProvider.getUsernameFromToken(token);
                    if (email != null) {
                        return userRepository.findByEmail(email).orElse(null);
                    }
                }
            }
        } catch (Exception e) {
            // Token parsing failed, return null
        }

        return null;
    }

    /**
     * Error response class
     */
    public static class ErrorResponse {
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

