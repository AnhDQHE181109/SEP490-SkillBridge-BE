package com.skillbridge.controller.api.sales;

import com.skillbridge.dto.sales.response.ErrorResponse;
import com.skillbridge.dto.sales.response.PresignedUrlResponse;
import com.skillbridge.entity.auth.User;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.service.common.DocumentPermissionService;
import com.skillbridge.service.common.S3Service;
import com.skillbridge.util.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Sales Document Controller
 * Handles document access and presigned URL generation
 */
@RestController
@RequestMapping("/sales/documents")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:4200"}, 
             allowCredentials = "true",
             maxAge = 3600)
public class SalesDocumentController {

    @Autowired
    private S3Service s3Service;

    @Autowired
    private DocumentPermissionService documentPermissionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * Get presigned URL for downloading document
     * GET /sales/documents/presigned-url?s3Key={s3Key}
     */
    @GetMapping("/presigned-url")
    public ResponseEntity<?> getPresignedUrl(
            @RequestParam String s3Key,
            Authentication authentication,
            HttpServletRequest request
    ) {
        User currentUser = getCurrentUser(authentication, request);

        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        String role = currentUser.getRole();
        if (role == null || (!role.equals("SALES_MANAGER") && !role.equals("SALES_REP"))) {
            return ResponseEntity.status(403).build();
        }

        // Check permission
        if (!documentPermissionService.hasPermission(s3Key, currentUser)) {
            return ResponseEntity.status(403).body(new ErrorResponse("Access denied. You do not have permission to access this document"));
        }

        try {
            // Generate presigned URL with 10 minutes expiration
            String presignedUrl = s3Service.getPresignedUrl(s3Key, 10);
            PresignedUrlResponse response = new PresignedUrlResponse(presignedUrl, s3Key, 10);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to generate presigned URL: " + e.getMessage()));
        }
    }

    /**
     * Delete document (only admin or owner)
     * DELETE /sales/documents/{s3Key}
     */
    @DeleteMapping("/{s3Key}")
    public ResponseEntity<?> deleteDocument(
            @PathVariable String s3Key,
            Authentication authentication,
            HttpServletRequest request
    ) {
        User currentUser = getCurrentUser(authentication, request);

        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        // Check if user can delete
        if (!documentPermissionService.canDelete(s3Key, currentUser)) {
            return ResponseEntity.status(403).body(new ErrorResponse("Access denied. Only admin or owner can delete documents"));
        }

        try {
            s3Service.deleteFile(s3Key);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to delete document: " + e.getMessage()));
        }
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

