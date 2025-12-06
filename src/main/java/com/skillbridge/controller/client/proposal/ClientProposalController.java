package com.skillbridge.controller.client.proposal;

import com.skillbridge.dto.proposal.response.ProposalListResponse;
import com.skillbridge.entity.auth.User;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.service.proposal.ProposalListService;
import com.skillbridge.util.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Client Proposal Controller
 * Handles proposal list endpoints for client portal
 */
@RestController
@RequestMapping("/client/proposals")
@CrossOrigin(origins = "*")
public class ClientProposalController {

    @Autowired
    private ProposalListService proposalListService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * Get proposals for authenticated client
     * GET /api/client/proposals
     * 
     * Query parameters:
     * - search: Search query (optional)
     * - status: Status filter (optional, "All" or specific status)
     * - page: Page number (default: 0)
     * - size: Page size (default: 20)
     */
    @GetMapping
    public ResponseEntity<?> getProposals(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Authentication authentication,
        HttpServletRequest request
    ) {
        User currentUser = getCurrentUser(authentication, request);
        
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        Integer userId = currentUser.getId();
        
        try {
            ProposalListResponse response = proposalListService.getProposalsForClient(
                userId,
                search,
                status,
                page,
                size
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Log error for debugging
            System.err.println("Error fetching proposals: " + e.getMessage());
            e.printStackTrace();
            
            // Return empty response instead of error to prevent frontend crash
            ProposalListResponse emptyResponse = new ProposalListResponse();
            emptyResponse.setProposals(java.util.Collections.emptyList());
            emptyResponse.setCurrentPage(0);
            emptyResponse.setTotalPages(0);
            emptyResponse.setTotalElements(0);
            
            return ResponseEntity.ok(emptyResponse);
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

