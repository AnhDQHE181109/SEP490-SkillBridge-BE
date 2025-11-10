package com.skillbridge.controller.client.proposal;

import com.skillbridge.dto.proposal.response.ProposalListResponse;
import com.skillbridge.service.proposal.ProposalListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

    /**
     * Get proposals for authenticated client
     * GET /api/client/proposals
     * 
     * Query parameters:
     * - search: Search query (optional)
     * - status: Status filter (optional, "All" or specific status)
     * - page: Page number (default: 0)
     * - size: Page size (default: 20)
     * 
     * Note: Authentication is handled by SecurityConfig
     * The clientUserId should be extracted from JWT token or session
     * For now, we'll use a header or request parameter (should be replaced with JWT extraction)
     */
    @GetMapping
    public ResponseEntity<ProposalListResponse> getProposals(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestHeader(value = "X-User-Id", required = false) Integer userId
    ) {
        try {
            // TODO: Extract userId from JWT token when JWT authentication is fully implemented
            // For now, using header (in production, extract from authentication token)
            
            if (userId == null) {
                // Temporary: For testing, use a default user ID
                // This should be replaced with JWT token extraction
                userId = 1; // Remove this after JWT implementation
            }

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
}

