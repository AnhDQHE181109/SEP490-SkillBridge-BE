package com.skillbridge.controller.client.contact;

import com.skillbridge.dto.contact.request.*;
import com.skillbridge.dto.contact.response.*;
import com.skillbridge.entity.auth.User;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.service.contact.ContactDetailService;
import com.skillbridge.service.contact.ContactListService;
import com.skillbridge.util.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Client Contact Controller
 * Handles contact list and detail endpoints for client portal
 */
@RestController
@RequestMapping("/client/contacts")
@CrossOrigin(origins = "*")
public class ClientContactController {

    @Autowired
    private ContactListService contactListService;

    @Autowired
    private ContactDetailService contactDetailService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * Get contacts for authenticated client
     * GET /api/client/contacts
     * 
     * Query parameters:
     * - search: Search query (optional)
     * - status: Status filter (optional, "All" or specific status)
     * - page: Page number (default: 0)
     * - size: Page size (default: 20)
     */
    @GetMapping
    public ResponseEntity<?> getContacts(
        @RequestParam(required = false) String search,
        @RequestParam(required = false, defaultValue = "All") String status,
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
        ContactListResponse response = contactListService.getContactsForClient(
            userId,
            search,
            status,
            page,
            size
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Create a new contact for authenticated client
     * POST /api/client/contacts
     */
    @PostMapping
    public ResponseEntity<?> createContact(
        @RequestBody CreateContactRequest request,
        Authentication authentication,
        HttpServletRequest httpRequest
    ) {
        User currentUser = getCurrentUser(authentication, httpRequest);
        
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        Integer userId = currentUser.getId();
        CreateContactResponse response = contactListService.createContact(userId, request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get contact detail by ID for authenticated client
     * GET /api/client/contacts/{contactId}
     */
    @GetMapping("/{contactId}")
    public ResponseEntity<?> getContactDetail(
        @PathVariable Integer contactId,
        Authentication authentication,
        HttpServletRequest request
    ) {
        User currentUser = getCurrentUser(authentication, request);
        
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            Integer userId = currentUser.getId();
        ContactDetailDTO response = contactDetailService.getContactDetail(contactId, userId);
        return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            // Contact not found or does not belong to this client
            return ResponseEntity.status(404).build();
        } catch (Exception e) {
            // Other unexpected errors
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Add communication log to a contact
     * POST /api/client/contacts/{contactId}/logs
     */
    @PostMapping("/{contactId}/logs")
    public ResponseEntity<?> addCommunicationLog(
        @PathVariable Integer contactId,
        @Valid @RequestBody AddLogRequest request,
        Authentication authentication,
        HttpServletRequest httpRequest
    ) {
        User currentUser = getCurrentUser(authentication, httpRequest);
        
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        Integer userId = currentUser.getId();
        CommunicationLogDTO response = contactDetailService.addCommunicationLog(
            contactId, 
            userId, 
            request.getMessage()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Add comment to proposal
     * POST /api/client/contacts/{contactId}/proposal/comment
     */
    @PostMapping("/{contactId}/proposal/comment")
    public ResponseEntity<?> addProposalComment(
        @PathVariable Integer contactId,
        @Valid @RequestBody CommentRequest request,
        Authentication authentication,
        HttpServletRequest httpRequest
    ) {
        User currentUser = getCurrentUser(authentication, httpRequest);
        
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        Integer userId = currentUser.getId();
        CommentResponse response = contactDetailService.addProposalComment(
            contactId, 
            userId, 
            request.getMessage()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel consultation
     * POST /api/client/contacts/{contactId}/cancel
     */
    @PostMapping("/{contactId}/cancel")
    public ResponseEntity<?> cancelConsultation(
        @PathVariable Integer contactId,
        @Valid @RequestBody CancelRequest request,
        Authentication authentication,
        HttpServletRequest httpRequest
    ) {
        User currentUser = getCurrentUser(authentication, httpRequest);
        
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        Integer userId = currentUser.getId();
        CancelResponse response = contactDetailService.cancelConsultation(
            contactId, 
            userId, 
            request.getReason()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Approve proposal
     * POST /api/client/contacts/{contactId}/proposal/approve
     */
    @PostMapping("/{contactId}/proposal/approve")
    public ResponseEntity<?> approveProposal(
        @PathVariable Integer contactId,
        Authentication authentication,
        HttpServletRequest request
    ) {
        User currentUser = getCurrentUser(authentication, request);
        
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        Integer userId = currentUser.getId();
        ApproveResponse response = contactDetailService.approveProposal(contactId, userId);
        return ResponseEntity.ok(response);
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

