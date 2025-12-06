package com.skillbridge.controller.api.sales;

import com.skillbridge.dto.contact.response.CommunicationLogDTO;
import com.skillbridge.dto.sales.request.CreateCommunicationLogRequest;
import com.skillbridge.dto.sales.request.UpdateContactRequest;
import com.skillbridge.dto.sales.response.SalesContactDetailDTO;
import com.skillbridge.dto.sales.response.SalesContactListResponse;
import com.skillbridge.dto.sales.response.SalesUserDTO;
import com.skillbridge.entity.auth.User;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.service.sales.SalesContactDetailService;
import com.skillbridge.service.sales.SalesContactService;
import com.skillbridge.util.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Sales Contact Controller
 * Handles contact list endpoints for Sales Portal
 * Note: context-path is /api, so full path will be /api/sales/contacts
 */
@RestController
@RequestMapping("/sales/contacts")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:4200"}, 
             allowCredentials = "true",
             maxAge = 3600)
// Note: @PreAuthorize disabled in dev mode, role check done manually in controller
// @PreAuthorize("hasAnyRole('SALES_MANAGER', 'SALES_REP')")
public class SalesContactController {

    @Autowired
    private SalesContactService salesContactService;

    @Autowired
    private SalesContactDetailService salesContactDetailService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * Get contacts list with search, filter, and pagination
     * GET /sales/contacts (full path: /api/sales/contacts due to context-path)
     * 
     * Role-based filtering:
     * - Sales Manager: sees all contacts
     * - Sales Man: sees only assigned contacts
     */
    @GetMapping
    public ResponseEntity<SalesContactListResponse> getContacts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) Integer assigneeUserId,
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

        // Get contacts with role-based filtering
        SalesContactListResponse response = salesContactService.getContacts(
                search,
                status,
                assigneeUserId,
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

        // Fallback: try to get from JWT token in Authorization header (for dev mode)
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
     * Get contact detail by ID
     * GET /sales/contacts/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getContactDetail(
            @PathVariable Integer id,
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
            SalesContactDetailDTO contact = salesContactDetailService.getContactDetail(id, currentUser);
            return ResponseEntity.ok(contact);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Update contact
     * PUT /sales/contacts/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateContact(
            @PathVariable Integer id,
            @RequestBody UpdateContactRequest request,
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
            SalesContactDetailDTO contact = salesContactDetailService.updateContact(id, request, currentUser);
            return ResponseEntity.ok(contact);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Convert contact to opportunity
     * POST /sales/contacts/{id}/convert-to-opportunity
     */
    @PostMapping("/{id}/convert-to-opportunity")
    public ResponseEntity<?> convertToOpportunity(
            @PathVariable Integer id,
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
            SalesContactDetailDTO contact = salesContactDetailService.convertToOpportunity(id, currentUser);
            return ResponseEntity.ok(contact);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Add communication log
     * POST /sales/contacts/{id}/communication-logs
     */
    @PostMapping("/{id}/communication-logs")
    public ResponseEntity<?> addCommunicationLog(
            @PathVariable Integer id,
            @Valid @RequestBody CreateCommunicationLogRequest request,
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
            CommunicationLogDTO log = salesContactDetailService.addCommunicationLog(id, request, currentUser);
            return ResponseEntity.ok(log);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Send meeting email
     * POST /sales/contacts/{id}/send-mtg-email
     */
    @PostMapping("/{id}/send-mtg-email")
    public ResponseEntity<?> sendMeetingEmail(
            @PathVariable Integer id,
            @RequestBody com.skillbridge.dto.sales.request.SendMeetingEmailRequest sendRequest,
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
            SalesContactDetailDTO updatedContact = salesContactDetailService.sendMeetingEmail(id, sendRequest, currentUser);
            return ResponseEntity.ok(updatedContact);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get list of Sales Users (SALES_MANAGER and SALES_REP)
     * GET /sales/users
     */
    @GetMapping("/users")
    public ResponseEntity<?> getSalesUsers(
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
            // Get all Sales Managers and Sales Reps
            List<User> salesManagers = userRepository.findByRole("SALES_MANAGER");
            List<User> salesReps = userRepository.findByRole("SALES_REP");
            
            // Combine and convert to DTOs
            List<SalesUserDTO> salesUsers = Stream.concat(
                salesManagers.stream(),
                salesReps.stream()
            )
            .map(user -> new SalesUserDTO(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole()
            ))
            .collect(Collectors.toList());

            return ResponseEntity.ok(salesUsers);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch sales users"));
        }
    }

    /**
     * Get list of Client Users (CLIENT_USER role)
     * GET /sales/clients
     */
    @GetMapping("/clients")
    public ResponseEntity<?> getClientUsers(
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
            // Get all Client Users - check both CLIENT and CLIENT_USER roles
            List<User> clientUsersCLIENT = userRepository.findByRole("CLIENT");
            List<User> clientUsersCLIENT_USER = userRepository.findByRole("CLIENT_USER");
            
            // Combine both lists, remove duplicates, and filter only active users
            List<User> allClientUsers = Stream.concat(
                clientUsersCLIENT.stream(),
                clientUsersCLIENT_USER.stream()
            )
            .distinct()
            .filter(user -> user.getIsActive() == null || user.getIsActive()) // Only active users
            .collect(Collectors.toList());
            
            // Convert to DTOs
            List<SalesUserDTO> clients = allClientUsers.stream()
                .map(user -> new SalesUserDTO(
                    user.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRole()
                ))
                .collect(Collectors.toList());

            return ResponseEntity.ok(clients);
        } catch (Exception e) {
            e.printStackTrace(); // Log error for debugging
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch client users: " + e.getMessage()));
        }
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

