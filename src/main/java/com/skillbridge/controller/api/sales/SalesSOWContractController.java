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
            
            CreateSOWRequest createRequest = new CreateSOWRequest();
            createRequest.setMsaId(msaId);
            createRequest.setClientId(clientId);
            createRequest.setEngagementType(engagementType);
            createRequest.setEffectiveStart(effectiveStart);
            createRequest.setEffectiveEnd(effectiveEnd);
            createRequest.setStatus(status);
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
}

