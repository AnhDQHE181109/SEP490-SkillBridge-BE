package com.skillbridge.controller.api.sales;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.skillbridge.dto.sales.request.CreateSOWRequest;
import com.skillbridge.dto.sales.request.SubmitReviewRequest;
import com.skillbridge.dto.sales.request.CreateChangeRequestRequest;
import com.skillbridge.dto.sales.response.SOWContractDTO;
import com.skillbridge.dto.sales.response.SOWContractDetailDTO;
import com.skillbridge.dto.sales.response.ChangeRequestsListResponseDTO;
import com.skillbridge.dto.sales.response.ChangeRequestResponseDTO;
import com.skillbridge.dto.sales.response.SalesChangeRequestDetailDTO;
import com.skillbridge.entity.auth.User;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.service.sales.SalesSOWContractService;
import com.skillbridge.service.sales.SOWBaselineService;
import com.skillbridge.service.sales.CREventService;
import com.skillbridge.service.sales.ContractAppendixService;
import com.skillbridge.entity.contract.ContractAppendix;
import com.skillbridge.util.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Sales SOW Contract Controller
 * Handles SOW contract creation endpoints for Sales Portal
 * Note: context-path is /api, so full path will be /api/sales/contracts/sow
 */
@RestController
@RequestMapping("/sales/contracts/sow")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:4200"}, 
             allowCredentials = "true",
             maxAge = 3600)
public class SalesSOWContractController {
    
    @Autowired
    private SalesSOWContractService contractService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Autowired
    private SOWBaselineService sowBaselineService;
    
    @Autowired
    private CREventService crEventService;
    
    @Autowired
    private ContractAppendixService contractAppendixService;
    
    private final Gson gson = new Gson();
    
    /**
     * Create SOW contract
     * POST /sales/contracts/sow
     */
    @PostMapping
    public ResponseEntity<?> createSOWContract(
        @RequestParam String msaId,
        @RequestParam Integer clientId,
        @RequestParam String engagementType,
        @RequestParam String effectiveStart,
        @RequestParam String effectiveEnd,
        @RequestParam(defaultValue = "Draft") String status,
        @RequestParam(required = false) Integer assigneeUserId,
        @RequestParam(required = false) String note,
        @RequestParam(required = false) String scopeSummary,
        @RequestParam(required = false) String projectName,
        @RequestParam(required = false) Double contractValue, // Total contract value for Fixed Price
        @RequestParam(required = false) String deliveryItems, // JSON string (deprecated)
        @RequestParam(required = false) String engagedEngineers, // JSON string (for Retainer)
        @RequestParam(required = false) String milestoneDeliverables, // JSON string
        @RequestParam(required = false) String billingDetails, // JSON string
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
            // Validate required fields
            if (assigneeUserId == null) {
                return ResponseEntity.status(400).body(new ErrorResponse("Assignee User ID is required"));
            }
            
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
            
            // Validate note and scopeSummary length
            if (note != null && note.length() > 500) {
                return ResponseEntity.status(400).body(new ErrorResponse("Note must not exceed 500 characters"));
            }
            if (scopeSummary != null && scopeSummary.length() > 5000) {
                return ResponseEntity.status(400).body(new ErrorResponse("Scope summary must not exceed 5000 characters"));
            }
            if (projectName != null && projectName.length() > 255) {
                return ResponseEntity.status(400).body(new ErrorResponse("Project name must not exceed 255 characters"));
            }
            
            // If reviewer is assigned and caller sent Draft (e.g. Save Contract), force Internal Review
            String normalizedStatus = status;
            if (reviewerId != null && "Draft".equalsIgnoreCase(status)) {
                normalizedStatus = "Internal Review";
            }

            CreateSOWRequest createRequest = new CreateSOWRequest();
            createRequest.setMsaId(msaId);
            createRequest.setClientId(clientId);
            createRequest.setEngagementType(engagementType);
            createRequest.setEffectiveStart(effectiveStart);
            createRequest.setEffectiveEnd(effectiveEnd);
            createRequest.setStatus(normalizedStatus);
            createRequest.setAssigneeUserId(assigneeUserId);
            createRequest.setNote(note);
            createRequest.setScopeSummary(scopeSummary);
            createRequest.setProjectName(projectName);
            createRequest.setContractValue(contractValue);
            
            // Parse JSON strings to objects
            if (deliveryItems != null && !deliveryItems.trim().isEmpty()) {
                try {
                    Type deliveryItemListType = new TypeToken<List<CreateSOWRequest.DeliveryItemDTO>>(){}.getType();
                    List<CreateSOWRequest.DeliveryItemDTO> deliveryItemsList = gson.fromJson(deliveryItems, deliveryItemListType);
                    createRequest.setDeliveryItems(deliveryItemsList != null ? deliveryItemsList : new ArrayList<>());
                } catch (Exception e) {
                    return ResponseEntity.status(400).body(new ErrorResponse("Invalid deliveryItems JSON: " + e.getMessage()));
                }
            } else {
                createRequest.setDeliveryItems(new ArrayList<>());
            }
            
            // Parse engagedEngineers JSON string
            if (engagedEngineers != null && !engagedEngineers.trim().isEmpty()) {
                try {
                    Type engagedEngineerListType = new TypeToken<List<CreateSOWRequest.EngagedEngineerDTO>>(){}.getType();
                    List<CreateSOWRequest.EngagedEngineerDTO> engagedEngineersList = gson.fromJson(engagedEngineers, engagedEngineerListType);
                    
                    // Validate Engaged Engineer dates for Retainer type
                    if ("Retainer".equals(engagementType) && engagedEngineersList != null) {
                        for (int i = 0; i < engagedEngineersList.size(); i++) {
                            CreateSOWRequest.EngagedEngineerDTO engineer = engagedEngineersList.get(i);
                            if (engineer.getStartDate() != null && engineer.getEndDate() != null) {
                                try {
                                    java.time.LocalDate engineerStartDate = java.time.LocalDate.parse(engineer.getStartDate());
                                    java.time.LocalDate engineerEndDate = java.time.LocalDate.parse(engineer.getEndDate());
                                    if (engineerEndDate.isBefore(engineerStartDate)) {
                                        return ResponseEntity.status(400).body(
                                            new ErrorResponse("Engaged Engineer " + (i + 1) + ": End Date must be on or after Start Date")
                                        );
                                    }
                                } catch (java.time.format.DateTimeParseException e) {
                                    return ResponseEntity.status(400).body(
                                        new ErrorResponse("Engaged Engineer " + (i + 1) + ": Invalid date format for Start Date or End Date")
                                    );
                                }
                            }
                        }
                    }
                    
                    createRequest.setEngagedEngineers(engagedEngineersList != null ? engagedEngineersList : new ArrayList<>());
                } catch (Exception e) {
                    return ResponseEntity.status(400).body(new ErrorResponse("Invalid engagedEngineers JSON: " + e.getMessage()));
                }
            } else {
                createRequest.setEngagedEngineers(new ArrayList<>());
            }
            
            if (milestoneDeliverables != null && !milestoneDeliverables.trim().isEmpty()) {
                try {
                    Type milestoneListType = new TypeToken<List<CreateSOWRequest.MilestoneDeliverableDTO>>(){}.getType();
                    List<CreateSOWRequest.MilestoneDeliverableDTO> milestonesList = gson.fromJson(milestoneDeliverables, milestoneListType);
                    createRequest.setMilestoneDeliverables(milestonesList != null ? milestonesList : new ArrayList<>());
                } catch (Exception e) {
                    return ResponseEntity.status(400).body(new ErrorResponse("Invalid milestoneDeliverables JSON: " + e.getMessage()));
                }
            } else {
                createRequest.setMilestoneDeliverables(new ArrayList<>());
            }
            
            if (billingDetails != null && !billingDetails.trim().isEmpty()) {
                try {
                    Type billingDetailListType = new TypeToken<List<CreateSOWRequest.BillingDetailDTO>>(){}.getType();
                    List<CreateSOWRequest.BillingDetailDTO> billingDetailsList = gson.fromJson(billingDetails, billingDetailListType);
                    createRequest.setBillingDetails(billingDetailsList != null ? billingDetailsList : new ArrayList<>());
                } catch (Exception e) {
                    return ResponseEntity.status(400).body(new ErrorResponse("Invalid billingDetails JSON: " + e.getMessage()));
                }
            } else {
                createRequest.setBillingDetails(new ArrayList<>());
            }
            
            createRequest.setReviewerId(reviewerId);
            createRequest.setReviewNotes(reviewNotes);
            createRequest.setReviewAction(reviewAction);
            
            SOWContractDTO contract = contractService.createSOWContract(createRequest, attachments, currentUser);
            return ResponseEntity.ok(contract);
        } catch (RuntimeException e) {
            System.err.println("RuntimeException when creating SOW contract: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(400).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            System.err.println("Exception when creating SOW contract: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to create SOW contract: " + e.getMessage()));
        }
    }
    
    /**
     * Submit review for SOW contract
     * POST /sales/contracts/sow/{contractId}/review
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
            SOWContractDTO contract = contractService.submitReview(
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
     * Get SOW contract detail
     * GET /sales/contracts/sow/{contractId}
     */
    @GetMapping("/{contractId}")
    public ResponseEntity<?> getSOWContractDetail(
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
            SOWContractDetailDTO detail = contractService.getSOWContractDetail(contractId, currentUser);
            return ResponseEntity.ok(detail);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to get SOW contract detail: " + e.getMessage()));
        }
    }
    
    /**
     * Get all versions of a SOW contract
     * GET /sales/contracts/sow/{contractId}/versions
     */
    @GetMapping("/{contractId}/versions")
    public ResponseEntity<?> getSOWContractVersions(
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
            List<SOWContractDetailDTO> versions = contractService.getSOWContractVersions(contractId, currentUser);
            return ResponseEntity.ok(versions);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to get SOW contract versions: " + e.getMessage()));
        }
    }
    
    /**
     * Update SOW contract (for Request_for_Change status - allows updating Engaged Engineers and Billing Details)
     * PUT /sales/contracts/sow/{contractId}
     */
    @PutMapping("/{contractId}")
    public ResponseEntity<?> updateSOWContract(
        @PathVariable Integer contractId,
        @RequestParam(required = false) String projectName,
        @RequestParam(required = false) String effectiveStart,
        @RequestParam(required = false) String effectiveEnd,
        @RequestParam(required = false) String note,
        @RequestParam(required = false) String scopeSummary,
        @RequestParam(required = false) Double contractValue,
        @RequestParam(required = false) Integer assigneeUserId,
        @RequestParam(required = false) String engagedEngineers, // JSON string (for Retainer)
        @RequestParam(required = false) String milestoneDeliverables, // JSON string (for Fixed Price)
        @RequestParam(required = false) String billingDetails, // JSON string
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
            if (effectiveStart != null && effectiveEnd != null && !effectiveStart.trim().isEmpty() && !effectiveEnd.trim().isEmpty()) {
                try {
                    java.time.LocalDate startDate = java.time.LocalDate.parse(effectiveStart);
                    java.time.LocalDate endDate = java.time.LocalDate.parse(effectiveEnd);
                    if (endDate.isBefore(startDate)) {
                        return ResponseEntity.status(400).body(new ErrorResponse("Effective End date must be on or after Effective Start date"));
                    }
                } catch (java.time.format.DateTimeParseException e) {
                    return ResponseEntity.status(400).body(new ErrorResponse("Invalid date format for Effective Start or Effective End"));
                }
            }
            
            // Validate note length
            if (note != null && note.length() > 500) {
                return ResponseEntity.status(400).body(new ErrorResponse("Note must not exceed 500 characters"));
            }
            
            // Validate scopeSummary length
            if (scopeSummary != null && scopeSummary.length() > 5000) {
                return ResponseEntity.status(400).body(new ErrorResponse("Scope summary must not exceed 5000 characters"));
            }
            
            // Validate projectName length
            if (projectName != null && projectName.length() > 255) {
                return ResponseEntity.status(400).body(new ErrorResponse("Project name must not exceed 255 characters"));
            }
            
            CreateSOWRequest updateRequest = new CreateSOWRequest();
            updateRequest.setProjectName(projectName);
            updateRequest.setEffectiveStart(effectiveStart);
            updateRequest.setEffectiveEnd(effectiveEnd);
            updateRequest.setNote(note);
            updateRequest.setScopeSummary(scopeSummary);
            updateRequest.setContractValue(contractValue);
            updateRequest.setAssigneeUserId(assigneeUserId);
            
            // Parse engagedEngineers if provided
            if (engagedEngineers != null && !engagedEngineers.trim().isEmpty()) {
                Type engagedEngineerListType = new TypeToken<List<CreateSOWRequest.EngagedEngineerDTO>>(){}.getType();
                List<CreateSOWRequest.EngagedEngineerDTO> engagedEngineersList = gson.fromJson(engagedEngineers, engagedEngineerListType);
                updateRequest.setEngagedEngineers(engagedEngineersList != null ? engagedEngineersList : new ArrayList<>());
            } else {
                updateRequest.setEngagedEngineers(new ArrayList<>());
            }
            
            // Parse milestoneDeliverables if provided (for Fixed Price)
            if (milestoneDeliverables != null && !milestoneDeliverables.trim().isEmpty()) {
                try {
                    Type milestoneListType = new TypeToken<List<CreateSOWRequest.MilestoneDeliverableDTO>>(){}.getType();
                    List<CreateSOWRequest.MilestoneDeliverableDTO> milestonesList = gson.fromJson(milestoneDeliverables, milestoneListType);
                    updateRequest.setMilestoneDeliverables(milestonesList != null ? milestonesList : new ArrayList<>());
                } catch (Exception e) {
                    return ResponseEntity.status(400).body(new ErrorResponse("Invalid milestoneDeliverables JSON: " + e.getMessage()));
                }
            } else {
                updateRequest.setMilestoneDeliverables(new ArrayList<>());
            }
            
            // Parse billingDetails if provided
            if (billingDetails != null && !billingDetails.trim().isEmpty()) {
                Type billingDetailListType = new TypeToken<List<CreateSOWRequest.BillingDetailDTO>>(){}.getType();
                List<CreateSOWRequest.BillingDetailDTO> billingDetailsList = gson.fromJson(billingDetails, billingDetailListType);
                updateRequest.setBillingDetails(billingDetailsList != null ? billingDetailsList : new ArrayList<>());
            } else {
                updateRequest.setBillingDetails(new ArrayList<>());
            }
            
            SOWContractDTO contract = contractService.updateSOWContract(contractId, updateRequest, attachments, currentUser);
            return ResponseEntity.ok(contract);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to update SOW contract: " + e.getMessage()));
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
     * Get change requests list for SOW contract
     * GET /sales/contracts/sow/{sowContractId}/change-requests
     */
    @GetMapping("/{sowContractId}/change-requests")
    public ResponseEntity<?> getChangeRequestsForSOW(
        @PathVariable Integer sowContractId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
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
            ChangeRequestsListResponseDTO response = contractService.getChangeRequestsForSOW(
                sowContractId, page, size, currentUser);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to get change requests: " + e.getMessage()));
        }
    }
    
    /**
     * Create change request for Retainer SOW contract
     * POST /sales/contracts/sow/{sowContractId}/change-requests
     */
    @PostMapping("/{sowContractId}/change-requests")
    public ResponseEntity<?> createChangeRequestForSOW(
        @PathVariable Integer sowContractId,
        @RequestParam String title,
        @RequestParam String type,
        @RequestParam String summary,
        @RequestParam String effectiveFrom,
        @RequestParam String effectiveUntil,
        @RequestParam(required = false) String references,
        @RequestParam(required = false) String engagedEngineers, // JSON string
        @RequestParam(required = false) String billingDetails, // JSON string
        @RequestParam(required = false) String impactAnalysis, // JSON string for Fixed Price
        @RequestParam Integer internalReviewerId,
        @RequestParam(required = false) String comment,
        @RequestParam String action, // "save" or "submit"
        @RequestParam(required = false) String reviewAction, // "APPROVE" or "REQUEST_REVISION" (when internal reviewer is current user)
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
            createRequest.setReviewAction(reviewAction);
            
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
            
            // Parse impact analysis (for Fixed Price)
            if (impactAnalysis != null && !impactAnalysis.trim().isEmpty()) {
                try {
                    CreateChangeRequestRequest.ImpactAnalysisDTO impactAnalysisDTO = gson.fromJson(impactAnalysis, CreateChangeRequestRequest.ImpactAnalysisDTO.class);
                    createRequest.setImpactAnalysis(impactAnalysisDTO);
                } catch (Exception e) {
                    return ResponseEntity.status(400).body(new ErrorResponse("Invalid impactAnalysis JSON: " + e.getMessage()));
                }
            }
            
            // Create change request
            ChangeRequestResponseDTO response = contractService.createChangeRequestForSOW(
                sowContractId, createRequest, attachments, currentUser);
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
     * Get change request detail for SOW
     * GET /sales/contracts/sow/{sowContractId}/change-requests/{changeRequestId}
     */
    @GetMapping("/{sowContractId}/change-requests/{changeRequestId}")
    public ResponseEntity<?> getChangeRequestDetailForSOW(
        @PathVariable Integer sowContractId,
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
            SalesChangeRequestDetailDTO detail = contractService.getChangeRequestDetailForSOW(
                sowContractId, changeRequestId, currentUser);
            return ResponseEntity.ok(detail);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to get change request detail: " + e.getMessage()));
        }
    }
    
    /**
     * Update change request for SOW (Draft only)
     * PUT /sales/contracts/sow/{sowContractId}/change-requests/{changeRequestId}
     */
    @PutMapping("/{sowContractId}/change-requests/{changeRequestId}")
    public ResponseEntity<?> updateChangeRequestForSOW(
        @PathVariable Integer sowContractId,
        @PathVariable Integer changeRequestId,
        @RequestParam String title,
        @RequestParam String type,
        @RequestParam String summary,
        @RequestParam String effectiveFrom,
        @RequestParam String effectiveUntil,
        @RequestParam(required = false) String references,
        @RequestParam(required = false) String engagedEngineers,
        @RequestParam(required = false) String billingDetails,
        @RequestParam(required = false) String impactAnalysis, // JSON string for Fixed Price
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
            CreateChangeRequestRequest updateRequest = new CreateChangeRequestRequest();
            updateRequest.setTitle(title);
            updateRequest.setType(type);
            updateRequest.setSummary(summary);
            updateRequest.setEffectiveFrom(effectiveFrom);
            updateRequest.setEffectiveUntil(effectiveUntil);
            updateRequest.setReferences(references);
            updateRequest.setInternalReviewerId(internalReviewerId);
            updateRequest.setComment(comment);
            
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
            
            // Parse impact analysis (for Fixed Price)
            if (impactAnalysis != null && !impactAnalysis.trim().isEmpty()) {
                try {
                    CreateChangeRequestRequest.ImpactAnalysisDTO impactAnalysisDTO = gson.fromJson(impactAnalysis, CreateChangeRequestRequest.ImpactAnalysisDTO.class);
                    updateRequest.setImpactAnalysis(impactAnalysisDTO);
                } catch (Exception e) {
                    return ResponseEntity.status(400).body(new ErrorResponse("Invalid impactAnalysis JSON: " + e.getMessage()));
                }
            }
            
            contractService.updateChangeRequestForSOW(sowContractId, changeRequestId, updateRequest, attachments, currentUser);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to update change request: " + e.getMessage()));
        }
    }
    
    /**
     * Submit change request for SOW
     * POST /sales/contracts/sow/{sowContractId}/change-requests/{changeRequestId}/submit
     */
    @PostMapping("/{sowContractId}/change-requests/{changeRequestId}/submit")
    public ResponseEntity<?> submitChangeRequestForSOW(
        @PathVariable Integer sowContractId,
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
            contractService.submitChangeRequestForSOW(sowContractId, changeRequestId, internalReviewerId, currentUser);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to submit change request: " + e.getMessage()));
        }
    }
    
    /**
     * Submit review for change request
     * POST /sales/contracts/sow/{sowContractId}/change-requests/{changeRequestId}/review
     */
    @PostMapping("/{sowContractId}/change-requests/{changeRequestId}/review")
    public ResponseEntity<?> submitChangeRequestReviewForSOW(
        @PathVariable Integer sowContractId,
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
            contractService.submitChangeRequestReviewForSOW(sowContractId, changeRequestId, reviewAction, reviewNotes, currentUser);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to submit review: " + e.getMessage()));
        }
    }
    
    /**
     * Approve change request and apply changes to contract (for Retainer SOW)
     * POST /sales/contracts/sow/{sowContractId}/change-requests/{changeRequestId}/approve
     */
    @PostMapping("/{sowContractId}/change-requests/{changeRequestId}/approve")
    public ResponseEntity<?> approveChangeRequestForSOW(
        @PathVariable Integer sowContractId,
        @PathVariable Integer changeRequestId,
        @RequestParam(required = false) String reviewNotes,
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
            contractService.approveChangeRequestForSOW(sowContractId, changeRequestId, reviewNotes, currentUser);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to approve change request: " + e.getMessage()));
        }
    }
    
    /**
     * Reject change request without applying changes (for Retainer SOW)
     * POST /sales/contracts/sow/{sowContractId}/change-requests/{changeRequestId}/reject
     */
    @PostMapping("/{sowContractId}/change-requests/{changeRequestId}/reject")
    public ResponseEntity<?> rejectChangeRequestForSOW(
        @PathVariable Integer sowContractId,
        @PathVariable Integer changeRequestId,
        @RequestParam(required = false) String reason,
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
            contractService.rejectChangeRequestForSOW(sowContractId, changeRequestId, reason, currentUser);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to reject change request: " + e.getMessage()));
        }
    }
    
    /**
     * Get preview of changes (Before/After comparison) for change request
     * GET /sales/contracts/sow/{sowContractId}/change-requests/{changeRequestId}/preview
     */
    @GetMapping("/{sowContractId}/change-requests/{changeRequestId}/preview")
    public ResponseEntity<?> getChangeRequestPreviewForSOW(
        @PathVariable Integer sowContractId,
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
            // This will be implemented in service
            return ResponseEntity.ok(contractService.getChangeRequestPreviewForSOW(sowContractId, changeRequestId, currentUser));
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to get preview: " + e.getMessage()));
        }
    }
    
    // ============================================
    // EVENT-BASED API ENDPOINTS
    // ============================================
    
    /**
     * Get baseline data for SOW contract (original contract snapshot)
     * GET /sales/contracts/sow/{contractId}/baseline
     */
    @GetMapping("/{contractId}/baseline")
    public ResponseEntity<?> getSOWContractBaseline(
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
            java.util.Map<String, Object> baseline = new java.util.HashMap<>();
            baseline.put("engineers", sowBaselineService.getBaselineResources(contractId));
            baseline.put("billing", sowBaselineService.getBaselineBilling(contractId));
            return ResponseEntity.ok(baseline);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to get baseline: " + e.getMessage()));
        }
    }
    
    /**
     * Get current state for SOW contract (baseline + events)
     * GET /sales/contracts/sow/{contractId}/current-state?asOfDate={date}
     */
    @GetMapping("/{contractId}/current-state")
    public ResponseEntity<?> getSOWContractCurrentState(
        @PathVariable Integer contractId,
        @RequestParam(required = false) String asOfDate,
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
            java.time.LocalDate date = asOfDate != null ? java.time.LocalDate.parse(asOfDate) : java.time.LocalDate.now();
            
            java.util.Map<String, Object> currentState = new java.util.HashMap<>();
            currentState.put("engineers", crEventService.calculateCurrentResources(contractId, date));
            
            // Calculate billing for current month
            java.time.LocalDate currentMonth = date.withDayOfMonth(1);
            currentState.put("billing", java.util.Map.of(
                "month", currentMonth.toString(),
                "amount", crEventService.calculateCurrentBilling(contractId, currentMonth)
            ));
            
            return ResponseEntity.ok(currentState);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to get current state: " + e.getMessage()));
        }
    }
    
    /**
     * Get current resources at a specific date for CR creation/editing
     * GET /sales/contracts/sow/{contractId}/current-resources?asOfDate={date}
     */
    @GetMapping("/{contractId}/current-resources")
    public ResponseEntity<?> getSOWContractCurrentResources(
        @PathVariable Integer contractId,
        @RequestParam(required = true) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate asOfDate,
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
            com.skillbridge.dto.sales.response.CurrentResourcesDTO response = 
                contractService.getCurrentResources(contractId, asOfDate, currentUser);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to get current resources: " + e.getMessage()));
        }
    }
    
    /**
     * Get all appendices for SOW contract
     * GET /sales/contracts/sow/{contractId}/appendices
     */
    @GetMapping("/{contractId}/appendices")
    public ResponseEntity<?> getSOWContractAppendices(
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
            List<ContractAppendix> appendices = contractAppendixService.getAppendices(contractId);
            return ResponseEntity.ok(appendices);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to get appendices: " + e.getMessage()));
        }
    }
    
    /**
     * Get appendix detail
     * GET /sales/contracts/sow/{contractId}/appendices/{appendixId}
     */
    @GetMapping("/{contractId}/appendices/{appendixId}")
    public ResponseEntity<?> getSOWContractAppendix(
        @PathVariable Integer contractId,
        @PathVariable Integer appendixId,
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
            ContractAppendix appendix = contractAppendixService.getAppendix(appendixId);
            if (!appendix.getSowContractId().equals(contractId)) {
                return ResponseEntity.status(400).body(new ErrorResponse("Appendix does not belong to this contract"));
            }
            return ResponseEntity.ok(appendix);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to get appendix: " + e.getMessage()));
        }
    }
    
    /**
     * Sign appendix
     * POST /sales/contracts/sow/{contractId}/appendices/{appendixId}/sign
     */
    @PostMapping("/{contractId}/appendices/{appendixId}/sign")
    public ResponseEntity<?> signSOWContractAppendix(
        @PathVariable Integer contractId,
        @PathVariable Integer appendixId,
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
            contractAppendixService.signAppendix(appendixId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to sign appendix: " + e.getMessage()));
        }
    }
    
    /**
     * Get events for SOW contract
     * GET /sales/contracts/sow/{contractId}/events?type={resource|billing}&fromDate={date}&toDate={date}
     */
    @GetMapping("/{contractId}/events")
    
    /**
     * Update payment status for billing detail
     * PATCH /sales/contracts/sow/{contractId}/billing-details/{billingDetailId}/payment-status
     */
    @PatchMapping("/{contractId}/billing-details/{billingDetailId}/payment-status")
    public ResponseEntity<?> updateBillingDetailPaymentStatus(
        @PathVariable Integer contractId,
        @PathVariable Integer billingDetailId,
        @RequestParam Boolean isPaid,
        @RequestParam String engagementType,
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
            contractService.updateBillingDetailPaymentStatus(contractId, billingDetailId, isPaid, engagementType, currentUser);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to update payment status: " + e.getMessage()));
        }
    }
    
    /**
     * Get events for SOW contract
     * GET /sales/contracts/sow/{contractId}/events?type={resource|billing}&fromDate={date}&toDate={date}
     */
    @GetMapping("/{contractId}/events")
    public ResponseEntity<?> getSOWContractEvents(
        @PathVariable Integer contractId,
        @RequestParam(required = false) String type, // "resource" or "billing"
        @RequestParam(required = false) String fromDate,
        @RequestParam(required = false) String toDate,
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
            java.util.Map<String, Object> events = new java.util.HashMap<>();
            
            if (type == null || "resource".equals(type)) {
                // If toDate is provided, filter events up to that date
                // Otherwise, get all approved events (including future ones)
                if (toDate != null) {
                    java.time.LocalDate asOfDate = java.time.LocalDate.parse(toDate);
                events.put("resources", crEventService.getResourceEvents(contractId, asOfDate));
                } else {
                    // Get all approved events without date filter
                    events.put("resources", crEventService.getAllResourceEvents(contractId));
                }
            }
            
            if (type == null || "billing".equals(type)) {
                if (fromDate != null) {
                    java.time.LocalDate month = java.time.LocalDate.parse(fromDate).withDayOfMonth(1);
                    events.put("billing", crEventService.getBillingEvents(contractId, month));
                } else {
                    // Get all billing events
                    events.put("billing", crEventService.getBillingEvents(contractId, java.time.LocalDate.now().withDayOfMonth(1)));
                }
            }
            
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to get events: " + e.getMessage()));
        }
    }
    
    /**
     * Get monthly resource snapshot for SOW contract
     * GET /sales/contracts/sow/{contractId}/monthly-resources?yearMonth=YYYY-MM
     */
    @GetMapping("/{contractId}/monthly-resources")
    public ResponseEntity<?> getMonthlyResourceSnapshot(
        @PathVariable Integer contractId,
        @RequestParam String yearMonth, // Format: YYYY-MM
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
            List<CREventService.MonthlyEngineerSnapshot> snapshot = 
                crEventService.calculateMonthlyResourceSnapshot(contractId, yearMonth);
            return ResponseEntity.ok(snapshot);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to get monthly resources: " + e.getMessage()));
        }
    }
}

