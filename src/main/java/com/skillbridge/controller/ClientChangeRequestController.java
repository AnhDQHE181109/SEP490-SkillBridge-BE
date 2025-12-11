package com.skillbridge.controller;

import com.skillbridge.dto.contract.request.CreateChangeRequestRequest;
import com.skillbridge.dto.contract.response.ChangeRequestResponse;
import com.skillbridge.entity.auth.User;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.service.contract.ChangeRequestService;
import com.skillbridge.util.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Client Change Request Controller
 * Handles HTTP requests for change request operations
 */
@RestController
@RequestMapping("/client/contracts/{contractId}/change-requests")
@CrossOrigin(origins = "*")
public class ClientChangeRequestController {
    
    private static final Logger logger = LoggerFactory.getLogger(ClientChangeRequestController.class);
    
    @Autowired
    private ChangeRequestService changeRequestService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    /**
     * Create a new change request
     */
    @PostMapping
    public ResponseEntity<?> createChangeRequest(
        @PathVariable Integer contractId,
        @RequestParam("title") String title,
        @RequestParam("type") String type,
        @RequestParam("description") String description,
        @RequestParam("reason") String reason,
        @RequestParam("desiredStartDate") @DateTimeFormat(pattern = "yyyy/MM/dd") LocalDate desiredStartDate,
        @RequestParam("desiredEndDate") @DateTimeFormat(pattern = "yyyy/MM/dd") LocalDate desiredEndDate,
        @RequestParam("expectedExtraCost") BigDecimal expectedExtraCost,
        @RequestParam(value = "attachments", required = false) List<MultipartFile> attachments,
        Authentication authentication,
        HttpServletRequest request
    ) {
        try {
            User currentUser = getCurrentUser(authentication, request);
            
            if (currentUser == null) {
                logger.warn("Unauthorized access attempt to POST /client/contracts/{}/change-requests", contractId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Authentication required");
            }
            
            Integer userId = currentUser.getId();
            
            // Create request DTO
            CreateChangeRequestRequest createRequest = new CreateChangeRequestRequest();
            createRequest.setTitle(title);
            createRequest.setType(type);
            createRequest.setDescription(description);
            createRequest.setReason(reason);
            createRequest.setDesiredStartDate(desiredStartDate);
            createRequest.setDesiredEndDate(desiredEndDate);
            createRequest.setExpectedExtraCost(expectedExtraCost);
            
            // Create change request
            ChangeRequestResponse response = changeRequestService.createChangeRequest(
                contractId,
                userId,
                createRequest,
                attachments
            );
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Validation error creating change request", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Validation error: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating change request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to create change request: " + e.getMessage());
        }
    }
    
    /**
     * Save change request as draft
     */
    @PostMapping("/draft")
    public ResponseEntity<?> saveChangeRequestDraft(
        @PathVariable Integer contractId,
        @RequestParam(value = "title", required = false) String title,
        @RequestParam(value = "type", required = false) String type,
        @RequestParam(value = "description", required = false) String description,
        @RequestParam(value = "reason", required = false) String reason,
        @RequestParam(value = "desiredStartDate", required = false) @DateTimeFormat(pattern = "yyyy/MM/dd") LocalDate desiredStartDate,
        @RequestParam(value = "desiredEndDate", required = false) @DateTimeFormat(pattern = "yyyy/MM/dd") LocalDate desiredEndDate,
        @RequestParam(value = "expectedExtraCost", required = false) BigDecimal expectedExtraCost,
        @RequestParam(value = "attachments", required = false) List<MultipartFile> attachments,
        Authentication authentication,
        HttpServletRequest request
    ) {
        try {
            User currentUser = getCurrentUser(authentication, request);
            
            if (currentUser == null) {
                logger.warn("Unauthorized access attempt to POST /client/contracts/{}/change-requests/draft", contractId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Authentication required");
            }
            
            Integer userId = currentUser.getId();
            
            // Create request DTO (all fields optional for draft)
            CreateChangeRequestRequest createRequest = new CreateChangeRequestRequest();
            createRequest.setTitle(title);
            createRequest.setType(type);
            createRequest.setDescription(description);
            createRequest.setReason(reason);
            createRequest.setDesiredStartDate(desiredStartDate);
            createRequest.setDesiredEndDate(desiredEndDate);
            createRequest.setExpectedExtraCost(expectedExtraCost);
            
            // Save as draft
            ChangeRequestResponse response = changeRequestService.saveChangeRequestDraft(
                contractId,
                userId,
                createRequest,
                attachments
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error saving change request draft", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to save change request draft: " + e.getMessage());
        }
    }
    
    /**
     * Get current user from authentication or JWT token
     */
    private User getCurrentUser(Authentication authentication, HttpServletRequest request) {
        // Try to get user from authentication (works with JWT filter)
        // JWT filter sets principal as String (email), not UserDetails
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() != null) {
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

        // Fallback: Try to get user from JWT token in Authorization header
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

