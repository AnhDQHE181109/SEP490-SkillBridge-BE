package com.skillbridge.controller;

import com.skillbridge.dto.contract.response.ProjectCloseRequestDetailDTO;
import com.skillbridge.entity.auth.User;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.service.contract.ProjectCloseRequestService;
import com.skillbridge.util.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Client Project Close Request Controller
 * Handles project close request endpoints for Client Portal
 * Story-41: Project Close Request for SOW Contract
 */
@RestController
@RequestMapping("/client")
@CrossOrigin(origins = "*")
public class ClientProjectCloseRequestController {
    
    private static final Logger logger = LoggerFactory.getLogger(ClientProjectCloseRequestController.class);
    
    @Autowired
    private ProjectCloseRequestService projectCloseRequestService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    /**
     * Get latest Close Request for a SOW
     * GET /client/sows/{sowId}/close-requests/latest
     */
    @GetMapping("/sows/{sowId}/close-requests/latest")
    public ResponseEntity<?> getLatestCloseRequest(
        @PathVariable Integer sowId,
        Authentication authentication,
        HttpServletRequest request
    ) {
        try {
            User currentUser = getCurrentUser(authentication, request);
            
            if (currentUser == null) {
                logger.warn("Unauthorized access attempt to GET /client/sows/{}/close-requests/latest", sowId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Authentication required");
            }
            
            ProjectCloseRequestDetailDTO response = projectCloseRequestService.getLatestCloseRequest(
                sowId, currentUser
            );
            
            // If no close request found, return 200 with null payload
            if (response == null) {
                return ResponseEntity.ok(null);
            }
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error getting latest close request", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error getting latest close request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to get close request: " + e.getMessage());
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
}

