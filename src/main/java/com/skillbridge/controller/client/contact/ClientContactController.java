package com.skillbridge.controller.client.contact;

import com.skillbridge.dto.contact.request.*;
import com.skillbridge.dto.contact.response.*;
import com.skillbridge.service.contact.ContactDetailService;
import com.skillbridge.service.contact.ContactListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

    /**
     * Get contacts for authenticated client
     * GET /api/client/contacts
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
    public ResponseEntity<ContactListResponse> getContacts(
        @RequestParam(required = false) String search,
        @RequestParam(required = false, defaultValue = "All") String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestHeader(value = "X-User-Id", required = false) Integer userId
    ) {
        // TODO: Extract userId from JWT token when JWT authentication is fully implemented
        // For now, using header (in production, extract from authentication token)
        
        if (userId == null) {
            // Temporary: For testing, use a default user ID
            // This should be replaced with JWT token extraction
            userId = 1; // Remove this after JWT implementation
        }

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
     * 
     * Note: Authentication is handled by SecurityConfig
     * The clientUserId should be extracted from JWT token or session
     * For now, we'll use a header or request parameter (should be replaced with JWT extraction)
     */
    @PostMapping
    public ResponseEntity<CreateContactResponse> createContact(
        @RequestBody CreateContactRequest request,
        @RequestHeader(value = "X-User-Id", required = false) Integer userId
    ) {
        // TODO: Extract userId from JWT token when JWT authentication is fully implemented
        // For now, using header (in production, extract from authentication token)
        
        if (userId == null) {
            // Temporary: For testing, use a default user ID
            // This should be replaced with JWT token extraction
            userId = 1; // Remove this after JWT implementation
        }

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
    public ResponseEntity<ContactDetailDTO> getContactDetail(
        @PathVariable Integer contactId,
        @RequestHeader(value = "X-User-Id", required = false) Integer userId
    ) {
        if (userId == null) {
            userId = 1; // Temporary: For testing
        }

        ContactDetailDTO response = contactDetailService.getContactDetail(contactId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Add communication log to a contact
     * POST /api/client/contacts/{contactId}/logs
     */
    @PostMapping("/{contactId}/logs")
    public ResponseEntity<CommunicationLogDTO> addCommunicationLog(
        @PathVariable Integer contactId,
        @RequestBody AddLogRequest request,
        @RequestHeader(value = "X-User-Id", required = false) Integer userId
    ) {
        if (userId == null) {
            userId = 1; // Temporary: For testing
        }

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
    public ResponseEntity<CommentResponse> addProposalComment(
        @PathVariable Integer contactId,
        @RequestBody CommentRequest request,
        @RequestHeader(value = "X-User-Id", required = false) Integer userId
    ) {
        if (userId == null) {
            userId = 1; // Temporary: For testing
        }

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
    public ResponseEntity<CancelResponse> cancelConsultation(
        @PathVariable Integer contactId,
        @RequestBody CancelRequest request,
        @RequestHeader(value = "X-User-Id", required = false) Integer userId
    ) {
        if (userId == null) {
            userId = 1; // Temporary: For testing
        }

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
    public ResponseEntity<ApproveResponse> approveProposal(
        @PathVariable Integer contactId,
        @RequestHeader(value = "X-User-Id", required = false) Integer userId
    ) {
        if (userId == null) {
            userId = 1; // Temporary: For testing
        }

        ApproveResponse response = contactDetailService.approveProposal(contactId, userId);
        return ResponseEntity.ok(response);
    }
}

