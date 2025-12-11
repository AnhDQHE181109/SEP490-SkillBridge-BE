package com.skillbridge.controller.api.sales;

import com.skillbridge.dto.sales.request.CreateMSARequest;
import com.skillbridge.dto.sales.request.SubmitReviewRequest;
import com.skillbridge.dto.sales.request.CreateChangeRequestRequest;
import com.skillbridge.dto.sales.response.MSAContractDTO;
import com.skillbridge.dto.sales.response.MSAContractDetailDTO;
import com.skillbridge.dto.sales.response.ChangeRequestResponseDTO;
import com.skillbridge.dto.sales.response.SalesChangeRequestDetailDTO;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import com.skillbridge.entity.auth.User;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.service.sales.SalesMSAContractService;
import com.skillbridge.util.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/**
 * Sales MSA Contract Controller
 * Handles MSA contract creation endpoints for Sales Portal
 * Note: context-path is /api, so full path will be /api/sales/contracts/msa
 */
@RestController
@RequestMapping("/sales/contracts/msa")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:4200"}, 
             allowCredentials = "true",
             maxAge = 3600)
public class SalesMSAContractController {
    
    @Autowired
    private SalesMSAContractService contractService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    private final Gson gson = new Gson();
    
    /**
     * Create MSA contract
     * POST /sales/contracts/msa
     */
    @PostMapping
    public ResponseEntity<?> createMSAContract(
        @RequestParam(required = false) String opportunityId,
        @RequestParam Integer clientId,
        @RequestParam String effectiveStart,
        @RequestParam String effectiveEnd,
        @RequestParam(defaultValue = "Draft") String status,
        @RequestParam Integer assigneeUserId,
        @RequestParam(required = false) String note,
        @RequestParam String currency,
        @RequestParam String paymentTerms,
        @RequestParam String invoicingCycle,
        @RequestParam String billingDay,
        @RequestParam String taxWithholding,
        @RequestParam String ipOwnership,
        @RequestParam String governingLaw,
        @RequestParam Integer clientContactId,
        @RequestParam Integer landbridgeContactId,
        @RequestParam(required = false) Integer reviewerId,
        @RequestParam(required = false) String reviewNotes,
        @RequestParam(required = false) String reviewAction,
        @RequestParam(required = false) MultipartFile[] attachments,
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
            // Validate Effective End >= Effective Start
            try {
                java.time.LocalDate startDate = java.time.LocalDate.parse(effectiveStart);
                java.time.LocalDate endDate = java.time.LocalDate.parse(effectiveEnd);
                if (endDate.isBefore(startDate)) {
                    return ResponseEntity.status(400).body(new ErrorResponse("Effective End date must be on or after Effective Start date"));
                }
            } catch (java.time.format.DateTimeParseException e) {
                return ResponseEntity.status(400).body(new ErrorResponse("Invalid date format for Effective Start or Effective End"));
            }
            
            // Validate note length
            if (note != null && note.length() > 500) {
                return ResponseEntity.status(400).body(new ErrorResponse("Note must not exceed 500 characters"));
            }
            
            CreateMSARequest createRequest = new CreateMSARequest();
            createRequest.setOpportunityId(opportunityId);
            createRequest.setClientId(clientId);
            createRequest.setEffectiveStart(effectiveStart);
            createRequest.setEffectiveEnd(effectiveEnd);
            createRequest.setStatus(status);
            createRequest.setAssigneeUserId(assigneeUserId);
            createRequest.setNote(note);
            createRequest.setCurrency(currency);
            createRequest.setPaymentTerms(paymentTerms);
            createRequest.setInvoicingCycle(invoicingCycle);
            createRequest.setBillingDay(billingDay);
            createRequest.setTaxWithholding(taxWithholding);
            createRequest.setIpOwnership(ipOwnership);
            createRequest.setGoverningLaw(governingLaw);
            createRequest.setClientContactId(clientContactId);
            createRequest.setLandbridgeContactId(landbridgeContactId);
            createRequest.setReviewerId(reviewerId);
            createRequest.setReviewNotes(reviewNotes);
            createRequest.setReviewAction(reviewAction);
            
            MSAContractDTO contract = contractService.createMSAContract(createRequest, attachments, currentUser);
            return ResponseEntity.ok(contract);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to create MSA contract: " + e.getMessage()));
        }
    }
    
    /**
     * Get active MSA contracts (for SOW creation)
     * GET /sales/contracts/msa/active
     */
    @GetMapping("/active")
    public ResponseEntity<?> getActiveMSAContracts(
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
            List<MSAContractDTO> contracts = contractService.getActiveMSAContracts(currentUser);
            return ResponseEntity.ok(contracts);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch active MSA contracts: " + e.getMessage()));
        }
    }
    
    /**
     * Get MSA contract detail
     * GET /sales/contracts/msa/{contractId}
     */
    @GetMapping("/{contractId}")
    public ResponseEntity<?> getMSAContractDetail(
        @PathVariable Integer contractId,
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
            MSAContractDetailDTO contract = contractService.getMSAContractDetail(contractId, currentUser);
            return ResponseEntity.ok(contract);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch contract detail: " + e.getMessage()));
        }
    }
    
    /**
     * Update MSA contract
     * PUT /sales/contracts/msa/{contractId}
     */
    @PutMapping("/{contractId}")
    public ResponseEntity<?> updateMSAContract(
        @PathVariable Integer contractId,
        @RequestParam(required = false) String opportunityId,
        @RequestParam Integer clientId,
        @RequestParam String effectiveStart,
        @RequestParam String effectiveEnd,
        @RequestParam(defaultValue = "Draft") String status,
        @RequestParam Integer assigneeUserId,
        @RequestParam(required = false) String note,
        @RequestParam String currency,
        @RequestParam String paymentTerms,
        @RequestParam String invoicingCycle,
        @RequestParam String billingDay,
        @RequestParam String taxWithholding,
        @RequestParam String ipOwnership,
        @RequestParam String governingLaw,
        @RequestParam Integer clientContactId,
        @RequestParam Integer landbridgeContactId,
        @RequestParam(required = false) Integer reviewerId,
        @RequestParam(required = false) String reviewNotes,
        @RequestParam(required = false) String reviewAction,
        @RequestParam(required = false) MultipartFile[] attachments,
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
            // Validate Effective End >= Effective Start
            try {
                java.time.LocalDate startDate = java.time.LocalDate.parse(effectiveStart);
                java.time.LocalDate endDate = java.time.LocalDate.parse(effectiveEnd);
                if (endDate.isBefore(startDate)) {
                    return ResponseEntity.status(400).body(new ErrorResponse("Effective End date must be on or after Effective Start date"));
                }
            } catch (java.time.format.DateTimeParseException e) {
                return ResponseEntity.status(400).body(new ErrorResponse("Invalid date format for Effective Start or Effective End"));
            }
            
            // Validate note length
            if (note != null && note.length() > 500) {
                return ResponseEntity.status(400).body(new ErrorResponse("Note must not exceed 500 characters"));
            }
            
            CreateMSARequest updateRequest = new CreateMSARequest();
            updateRequest.setOpportunityId(opportunityId);
            updateRequest.setClientId(clientId);
            updateRequest.setEffectiveStart(effectiveStart);
            updateRequest.setEffectiveEnd(effectiveEnd);
            updateRequest.setStatus(status);
            updateRequest.setAssigneeUserId(assigneeUserId);
            updateRequest.setNote(note);
            updateRequest.setCurrency(currency);
            updateRequest.setPaymentTerms(paymentTerms);
            updateRequest.setInvoicingCycle(invoicingCycle);
            updateRequest.setBillingDay(billingDay);
            updateRequest.setTaxWithholding(taxWithholding);
            updateRequest.setIpOwnership(ipOwnership);
            updateRequest.setGoverningLaw(governingLaw);
            updateRequest.setClientContactId(clientContactId);
            updateRequest.setLandbridgeContactId(landbridgeContactId);
            updateRequest.setReviewerId(reviewerId);
            updateRequest.setReviewNotes(reviewNotes);
            updateRequest.setReviewAction(reviewAction);
            
            MSAContractDTO contract = contractService.updateMSAContract(contractId, updateRequest, attachments, currentUser);
            return ResponseEntity.ok(contract);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to update MSA contract: " + e.getMessage()));
        }
    }
    
    /**
     * Delete attachment from MSA contract
     * DELETE /sales/contracts/msa/{contractId}/attachments
     */
    @DeleteMapping("/{contractId}/attachments")
    public ResponseEntity<?> deleteAttachment(
        @PathVariable Integer contractId,
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
        
        try {
            contractService.deleteAttachment(contractId, s3Key, currentUser);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to delete attachment: " + e.getMessage()));
        }
    }
    
    /**
     * Submit review for contract
     * POST /sales/contracts/msa/{contractId}/review
     */
    @PostMapping("/{contractId}/review")
    public ResponseEntity<?> submitReview(
        @PathVariable Integer contractId,
        @RequestBody SubmitReviewRequest reviewRequest,
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
            MSAContractDTO contract = contractService.submitReview(
                contractId, 
                reviewRequest.getReviewNotes(), 
                reviewRequest.getAction(), 
                currentUser
            );
            return ResponseEntity.ok(contract);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to submit review: " + e.getMessage()));
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
    
    /**
     * Create change request for Fixed Price MSA contract
     * POST /sales/contracts/msa/{msaContractId}/change-requests
     */
    @PostMapping("/{msaContractId}/change-requests")
    public ResponseEntity<?> createChangeRequestForMSA(
        @PathVariable Integer msaContractId,
        @RequestParam String title,
        @RequestParam String type,
        @RequestParam String summary,
        @RequestParam String effectiveFrom,
        @RequestParam String effectiveUntil,
        @RequestParam(required = false) String references,
        @RequestParam(required = false) String engagedEngineers, // JSON string
        @RequestParam(required = false) String billingDetails, // JSON string
        @RequestParam Integer internalReviewerId,
        @RequestParam(required = false) String comment,
        @RequestParam String action, // "save" or "submit"
        @RequestParam(required = false) MultipartFile[] attachments,
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
            // Build request DTO
            CreateChangeRequestRequest createRequest = new CreateChangeRequestRequest();
            createRequest.setTitle(title);
            createRequest.setType(type);
            createRequest.setSummary(summary);
            createRequest.setEffectiveFrom(effectiveFrom);
            createRequest.setEffectiveUntil(effectiveUntil);
            createRequest.setReferences(references);
            createRequest.setInternalReviewerId(internalReviewerId);
            createRequest.setComment(comment);
            createRequest.setAction(action);
            
            // Parse JSON strings to objects
            if (engagedEngineers != null && !engagedEngineers.trim().isEmpty()) {
                try {
                    Type engineerListType = new TypeToken<List<CreateChangeRequestRequest.EngagedEngineerDTO>>(){}.getType();
                    List<CreateChangeRequestRequest.EngagedEngineerDTO> engineersList = gson.fromJson(engagedEngineers, engineerListType);
                    createRequest.setEngagedEngineers(engineersList != null ? engineersList : new ArrayList<>());
                } catch (Exception e) {
                    return ResponseEntity.status(400).body(new ErrorResponse("Invalid engagedEngineers JSON: " + e.getMessage()));
                }
            } else {
                createRequest.setEngagedEngineers(new ArrayList<>());
            }
            
            if (billingDetails != null && !billingDetails.trim().isEmpty()) {
                try {
                    Type billingDetailListType = new TypeToken<List<CreateChangeRequestRequest.BillingDetailDTO>>(){}.getType();
                    List<CreateChangeRequestRequest.BillingDetailDTO> billingDetailsList = gson.fromJson(billingDetails, billingDetailListType);
                    createRequest.setBillingDetails(billingDetailsList != null ? billingDetailsList : new ArrayList<>());
                } catch (Exception e) {
                    return ResponseEntity.status(400).body(new ErrorResponse("Invalid billingDetails JSON: " + e.getMessage()));
                }
            } else {
                createRequest.setBillingDetails(new ArrayList<>());
            }
            
            // Create change request
            ChangeRequestResponseDTO response = contractService.createChangeRequestForMSA(
                msaContractId, createRequest, attachments, currentUser);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            System.err.println("RuntimeException when creating change request: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(400).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            System.err.println("Exception when creating change request: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to create change request: " + e.getMessage()));
        }
    }
    
    /**
     * Get change request detail for MSA
     * GET /sales/contracts/msa/{msaContractId}/change-requests/{changeRequestId}
     */
    @GetMapping("/{msaContractId}/change-requests/{changeRequestId}")
    public ResponseEntity<?> getChangeRequestDetailForMSA(
        @PathVariable Integer msaContractId,
        @PathVariable Integer changeRequestId,
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
            SalesChangeRequestDetailDTO detail = contractService.getChangeRequestDetailForMSA(
                msaContractId, changeRequestId, currentUser);
            return ResponseEntity.ok(detail);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to get change request detail: " + e.getMessage()));
        }
    }
    
    /**
     * Update change request for MSA (Draft only)
     * PUT /sales/contracts/msa/{msaContractId}/change-requests/{changeRequestId}
     */
    @PutMapping("/{msaContractId}/change-requests/{changeRequestId}")
    public ResponseEntity<?> updateChangeRequestForMSA(
        @PathVariable Integer msaContractId,
        @PathVariable Integer changeRequestId,
        @RequestParam String title,
        @RequestParam String type,
        @RequestParam String summary,
        @RequestParam String effectiveFrom,
        @RequestParam String effectiveUntil,
        @RequestParam(required = false) String references,
        @RequestParam(required = false) String engagedEngineers,
        @RequestParam(required = false) String billingDetails,
        @RequestParam(required = false) Integer internalReviewerId,
        @RequestParam(required = false) String comment,
        @RequestParam(required = false) MultipartFile[] attachments,
        Authentication authentication,
        HttpServletRequest request
    ) {
        User currentUser = getCurrentUser(authentication, request);
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            // Build request DTO
            CreateChangeRequestRequest updateRequest = new CreateChangeRequestRequest();
            updateRequest.setTitle(title);
            updateRequest.setType(type);
            updateRequest.setSummary(summary);
            updateRequest.setEffectiveFrom(effectiveFrom);
            updateRequest.setEffectiveUntil(effectiveUntil);
            updateRequest.setReferences(references);
            updateRequest.setInternalReviewerId(internalReviewerId);
            updateRequest.setComment(comment);
            
            // Parse JSON strings
            if (engagedEngineers != null && !engagedEngineers.trim().isEmpty()) {
                Type engineerListType = new TypeToken<List<CreateChangeRequestRequest.EngagedEngineerDTO>>(){}.getType();
                List<CreateChangeRequestRequest.EngagedEngineerDTO> engineersList = gson.fromJson(engagedEngineers, engineerListType);
                updateRequest.setEngagedEngineers(engineersList != null ? engineersList : new ArrayList<>());
            } else {
                updateRequest.setEngagedEngineers(new ArrayList<>());
            }
            
            if (billingDetails != null && !billingDetails.trim().isEmpty()) {
                Type billingDetailListType = new TypeToken<List<CreateChangeRequestRequest.BillingDetailDTO>>(){}.getType();
                List<CreateChangeRequestRequest.BillingDetailDTO> billingDetailsList = gson.fromJson(billingDetails, billingDetailListType);
                updateRequest.setBillingDetails(billingDetailsList != null ? billingDetailsList : new ArrayList<>());
            } else {
                updateRequest.setBillingDetails(new ArrayList<>());
            }
            
            contractService.updateChangeRequestForMSA(msaContractId, changeRequestId, updateRequest, attachments, currentUser);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to update change request: " + e.getMessage()));
        }
    }
    
    /**
     * Submit change request for MSA
     * POST /sales/contracts/msa/{msaContractId}/change-requests/{changeRequestId}/submit
     */
    @PostMapping("/{msaContractId}/change-requests/{changeRequestId}/submit")
    public ResponseEntity<?> submitChangeRequestForMSA(
        @PathVariable Integer msaContractId,
        @PathVariable Integer changeRequestId,
        @RequestParam Integer internalReviewerId,
        Authentication authentication,
        HttpServletRequest request
    ) {
        User currentUser = getCurrentUser(authentication, request);
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            contractService.submitChangeRequestForMSA(msaContractId, changeRequestId, internalReviewerId, currentUser);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to submit change request: " + e.getMessage()));
        }
    }
    
    /**
     * Submit review for change request
     * POST /sales/contracts/msa/{msaContractId}/change-requests/{changeRequestId}/review
     */
    @PostMapping("/{msaContractId}/change-requests/{changeRequestId}/review")
    public ResponseEntity<?> submitChangeRequestReviewForMSA(
        @PathVariable Integer msaContractId,
        @PathVariable Integer changeRequestId,
        @RequestParam String reviewAction,
        @RequestParam(required = false) String reviewNotes,
        Authentication authentication,
        HttpServletRequest request
    ) {
        User currentUser = getCurrentUser(authentication, request);
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            contractService.submitChangeRequestReviewForMSA(msaContractId, changeRequestId, reviewAction, reviewNotes, currentUser);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to submit review: " + e.getMessage()));
        }
    }
}

