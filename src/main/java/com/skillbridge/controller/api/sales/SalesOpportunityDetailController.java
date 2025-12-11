package com.skillbridge.controller.api.sales;

import com.skillbridge.dto.sales.request.AssignReviewerRequest;
import com.skillbridge.dto.sales.request.CreateOpportunityRequest;
import com.skillbridge.dto.sales.request.SubmitReviewRequest;
import com.skillbridge.dto.sales.request.UpdateOpportunityRequest;
import com.skillbridge.dto.sales.response.ErrorResponse;
import com.skillbridge.dto.sales.response.OpportunityDetailDTO;
import com.skillbridge.dto.sales.response.ProposalDTO;
import com.skillbridge.entity.auth.User;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.service.sales.ProposalService;
import com.skillbridge.service.sales.SalesOpportunityDetailService;
import com.skillbridge.util.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Sales Opportunity Detail Controller
 * Handles opportunity detail and proposal endpoints for Sales Portal
 * Note: context-path is /api, so full path will be /api/sales/opportunities/{id}/...
 */
@RestController
@RequestMapping("/sales/opportunities")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:4200"}, 
             allowCredentials = "true",
             maxAge = 3600)
// Note: @PreAuthorize disabled in dev mode, role check done manually in controller
// @PreAuthorize("hasAnyRole('SALES_MANAGER', 'SALES_REP')")
public class SalesOpportunityDetailController {

    @Autowired
    private SalesOpportunityDetailService salesOpportunityDetailService;

    @Autowired
    private ProposalService proposalService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * Create opportunity from contact
     * POST /sales/opportunities/create-from-contact/{contactId}
     */
    @PostMapping("/create-from-contact/{contactId}")
    public ResponseEntity<?> createFromContact(
            @PathVariable Integer contactId,
            @RequestBody CreateOpportunityRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        User currentUser = getCurrentUser(authentication, httpRequest);

        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        String role = currentUser.getRole();
        if (role == null || (!role.equals("SALES_MANAGER") && !role.equals("SALES_REP"))) {
            return ResponseEntity.status(403).build();
        }

        try {
            OpportunityDetailDTO opportunity = salesOpportunityDetailService.createFromContact(contactId, request, currentUser);
            return ResponseEntity.ok(opportunity);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get opportunity by ID
     * GET /sales/opportunities/{opportunityId}
     */
    @GetMapping("/{opportunityId}")
    public ResponseEntity<?> getOpportunityById(
            @PathVariable String opportunityId,
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

        try {
            OpportunityDetailDTO opportunity = salesOpportunityDetailService.getOpportunityById(opportunityId, currentUser);
            return ResponseEntity.ok(opportunity);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Update opportunity
     * PUT /sales/opportunities/{opportunityId}
     */
    @PutMapping("/{opportunityId}")
    public ResponseEntity<?> updateOpportunity(
            @PathVariable String opportunityId,
            @RequestBody UpdateOpportunityRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        User currentUser = getCurrentUser(authentication, httpRequest);

        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        String role = currentUser.getRole();
        if (role == null || (!role.equals("SALES_MANAGER") && !role.equals("SALES_REP"))) {
            return ResponseEntity.status(403).build();
        }

        try {
            OpportunityDetailDTO opportunity = salesOpportunityDetailService.updateOpportunity(opportunityId, request, currentUser);
            return ResponseEntity.ok(opportunity);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Create proposal for opportunity
     * POST /sales/opportunities/{opportunityId}/proposals
     */
    @PostMapping("/{opportunityId}/proposals")
    public ResponseEntity<?> createProposal(
            @PathVariable String opportunityId,
            @RequestParam("title") String title,
            @RequestParam(value = "reviewerId", required = false) Integer reviewerId,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
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

        try {
            // Validate files are required when reviewer is provided (not draft)
            if (reviewerId != null && (files == null || files.length == 0)) {
                return ResponseEntity.status(400).body(new ErrorResponse("Documents are required"));
            }
            
            ProposalDTO proposal = proposalService.createProposal(opportunityId, title, reviewerId, files, currentUser);
            return ResponseEntity.ok(proposal);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Update proposal
     * PUT /sales/proposals/{proposalId}
     */
    @PutMapping("/proposals/{proposalId}")
    public ResponseEntity<?> updateProposal(
            @PathVariable Integer proposalId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "reviewerId", required = false) Integer reviewerId,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
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

        try {
            ProposalDTO proposal = proposalService.updateProposal(proposalId, title, reviewerId, files, currentUser);
            return ResponseEntity.ok(proposal);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Assign reviewer to proposal
     * POST /sales/proposals/{proposalId}/assign-reviewer
     */
    @PostMapping("/proposals/{proposalId}/assign-reviewer")
    public ResponseEntity<?> assignReviewer(
            @PathVariable Integer proposalId,
            @RequestBody AssignReviewerRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        User currentUser = getCurrentUser(authentication, httpRequest);

        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        String role = currentUser.getRole();
        if (role == null || (!role.equals("SALES_MANAGER") && !role.equals("SALES_REP"))) {
            return ResponseEntity.status(403).build();
        }

        try {
            ProposalDTO proposal = proposalService.assignReviewer(proposalId, request.getReviewerId(), currentUser);
            return ResponseEntity.ok(proposal);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Submit review
     * POST /sales/proposals/{proposalId}/submit-review
     */
    @PostMapping("/proposals/{proposalId}/submit-review")
    public ResponseEntity<?> submitReview(
            @PathVariable Integer proposalId,
            @RequestBody SubmitReviewRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        User currentUser = getCurrentUser(authentication, httpRequest);

        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        String role = currentUser.getRole();
        if (role == null || (!role.equals("SALES_MANAGER") && !role.equals("SALES_REP"))) {
            return ResponseEntity.status(403).build();
        }

        try {
            ProposalDTO proposal = proposalService.submitReview(proposalId, request, currentUser);
            return ResponseEntity.ok(proposal);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Mark opportunity as lost
     * POST /sales/opportunities/{opportunityId}/mark-lost
     */
    @PostMapping("/{opportunityId}/mark-lost")
    public ResponseEntity<?> markAsLost(
            @PathVariable String opportunityId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        User currentUser = getCurrentUser(authentication, httpRequest);

        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        String role = currentUser.getRole();
        if (role == null || (!role.equals("SALES_MANAGER") && !role.equals("SALES_REP"))) {
            return ResponseEntity.status(403).build();
        }

        try {
            OpportunityDetailDTO opportunity = salesOpportunityDetailService.markAsLost(opportunityId, currentUser);
            return ResponseEntity.ok(opportunity);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Convert opportunity to contract
     * POST /sales/opportunities/{opportunityId}/convert-to-contract
     */
    @PostMapping("/{opportunityId}/convert-to-contract")
    public ResponseEntity<?> convertToContract(
            @PathVariable String opportunityId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        User currentUser = getCurrentUser(authentication, httpRequest);

        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        String role = currentUser.getRole();
        if (role == null || (!role.equals("SALES_MANAGER") && !role.equals("SALES_REP"))) {
            return ResponseEntity.status(403).build();
        }

        try {
            OpportunityDetailDTO opportunity = salesOpportunityDetailService.convertToContract(opportunityId, currentUser);
            return ResponseEntity.ok(opportunity);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(new ErrorResponse(e.getMessage()));
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

