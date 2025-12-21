package com.skillbridge.service.sales;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.skillbridge.dto.sales.request.CreateSOWRequest;
import com.skillbridge.dto.sales.request.CreateChangeRequestRequest;
import com.skillbridge.dto.sales.response.SOWContractDTO;
import com.skillbridge.dto.sales.response.SOWContractDetailDTO;
import com.skillbridge.dto.sales.response.ChangeRequestListItemDTO;
import com.skillbridge.dto.sales.response.ChangeRequestsListResponseDTO;
import com.skillbridge.dto.sales.response.ChangeRequestResponseDTO;
import com.skillbridge.dto.sales.response.SalesChangeRequestDetailDTO;
import com.skillbridge.entity.auth.User;
import com.skillbridge.entity.contract.*;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.repository.contract.*;
import com.skillbridge.repository.document.DocumentMetadataRepository;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import com.skillbridge.entity.document.DocumentMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.skillbridge.service.common.S3Service;
import com.skillbridge.service.common.EmailService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import com.skillbridge.dto.common.AttachmentInfo;

/**
 * Sales SOW Contract Service
 * Handles business logic for creating SOW contracts
 */
@Service
@Transactional
public class SalesSOWContractService {
    
    private static final Logger logger = LoggerFactory.getLogger(SalesSOWContractService.class);
    
    @Autowired
    private SOWContractRepository sowContractRepository;
    
    @Autowired
    private ContractRepository contractRepository; // For MSA
    
    @Autowired
    private DeliveryItemRepository deliveryItemRepository;
    
    @Autowired
    private MilestoneDeliverableRepository milestoneDeliverableRepository;
    
    @Autowired
    private RetainerBillingDetailRepository retainerBillingDetailRepository;
    
    @Autowired
    private FixedPriceBillingDetailRepository fixedPriceBillingDetailRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired(required = false)
    private S3Service s3Service;
    
    @Value("${aws.s3.enabled:false}")
    private boolean s3Enabled;
    
    @Autowired
    private ContractHistoryRepository contractHistoryRepository;
    
    @Autowired
    private ContractInternalReviewRepository contractInternalReviewRepository;
    
    @Autowired
    private DocumentMetadataRepository documentMetadataRepository;
    
    @Autowired
    private ChangeRequestRepository changeRequestRepository;
    
    @Autowired
    private ChangeRequestEngagedEngineerRepository changeRequestEngagedEngineerRepository;
    
    @Autowired
    private SOWEngagedEngineerRepository sowEngagedEngineerRepository;
    
    @Autowired
    private ChangeRequestBillingDetailRepository changeRequestBillingDetailRepository;
    
    @Autowired
    private ChangeRequestAttachmentRepository changeRequestAttachmentRepository;
    
    @Autowired
    private ChangeRequestHistoryRepository changeRequestHistoryRepository;
    
    @Autowired
    private SOWBaselineService sowBaselineService;
    
    @Autowired
    private CREventService crEventService;
    
    @Autowired
    private ContractAppendixService contractAppendixService;
    
    @Autowired
    private CRBillingEventRepository crBillingEventRepository;
    
    @Autowired
    private EmailService emailService;
    
    private final Gson gson = new Gson();
    
    /**
     * Create SOW contract
     */
    public SOWContractDTO createSOWContract(CreateSOWRequest request, MultipartFile[] attachments, User currentUser) {
        // Find parent MSA contract by contractId string (format: MSA-YYYY-NN)
        Contract parentMSA = findMSAByContractId(request.getMsaId());
        if (parentMSA == null) {
            throw new RuntimeException("Parent MSA not found: " + request.getMsaId());
        }
        
        // Validate MSA status (must be Active or Client Under Review)
        String statusName = parentMSA.getStatus().name();
        if (!"Active".equals(statusName) && !"Under_Review".equals(statusName)) {
            // Map Under_Review to "Client Under Review" for display
            if ("Under_Review".equals(statusName)) {
                // Check if there's an approved review
                Optional<ContractInternalReview> reviewOpt = contractInternalReviewRepository
                    .findFirstByContractIdAndContractTypeOrderByReviewedAtDesc(parentMSA.getId(), "MSA");
                if (reviewOpt.isPresent() && "APPROVE".equals(reviewOpt.get().getReviewAction())) {
                    // This is "Client Under Review", allow it
                } else {
                    throw new RuntimeException("Parent MSA must be Active or Client Under Review. Current status: " + statusName);
                }
            } else {
                throw new RuntimeException("Parent MSA must be Active or Client Under Review. Current status: " + statusName);
            }
        }
        
        // Validate client
        if (request.getClientId() == null) {
            throw new RuntimeException("Client ID is required");
        }
        User client = userRepository.findById(request.getClientId())
            .orElseThrow(() -> new RuntimeException("Client not found"));
        
        // Validate assignee
        User assignee = userRepository.findById(request.getAssigneeUserId())
            .orElseThrow(() -> new RuntimeException("Assignee not found"));
        
        // Create SOW contract
        SOWContract contract = new SOWContract();
        contract.setClientId(request.getClientId());
        contract.setParentMsaId(parentMSA.getId());
        contract.setEngagementType(request.getEngagementType());
        contract.setProjectName(request.getProjectName() != null ? request.getProjectName() : "SOW Project");
        contract.setContractName("SOW Contract - " + contract.getProjectName());
        
        // Map status string to enum
        SOWContract.SOWContractStatus statusEnum = mapStatusToEnum(request.getStatus());
        contract.setStatus(statusEnum);
        
        contract.setPeriodStart(LocalDate.parse(request.getEffectiveStart()));
        contract.setPeriodEnd(LocalDate.parse(request.getEffectiveEnd()));
        contract.setAssigneeUserId(request.getAssigneeUserId());
        contract.setScopeSummary(request.getScopeSummary());
        // Note: SOWContract entity doesn't have a 'note' field, so we skip it
        contract.setReviewerId(request.getReviewerId()); // Save reviewer ID
        
        // Set commercial terms from parent MSA
        contract.setCurrency(parentMSA.getCurrency());
        contract.setPaymentTerms(parentMSA.getPaymentTerms());
        contract.setInvoicingCycle(parentMSA.getInvoicingCycle());
        contract.setBillingDay(parentMSA.getBillingDay());
        contract.setTaxWithholding(parentMSA.getTaxWithholding());
        contract.setIpOwnership(parentMSA.getIpOwnership());
        contract.setGoverningLaw(parentMSA.getGoverningLaw());
        
        // Set LandBridge contact (use assignee)
        contract.setLandbridgeContactName(assignee.getFullName());
        contract.setLandbridgeContactEmail(assignee.getEmail());
        
        contract = sowContractRepository.save(contract);
        
        // Generate contract ID
        String contractId = generateContractId(contract.getId(), contract.getCreatedAt());
        
        // Calculate total value from delivery items or contract value
        BigDecimal totalValue = BigDecimal.ZERO;
        
        // Create engaged engineers or milestone deliverables based on engagement type
        if ("Retainer".equals(request.getEngagementType())) {
            // Validate engaged engineers
            if (request.getEngagedEngineers() == null || request.getEngagedEngineers().isEmpty()) {
                throw new RuntimeException("At least one engaged engineer is required for Retainer SOW");
            }
            
            // Validate billing details
            if (request.getBillingDetails() == null || request.getBillingDetails().isEmpty()) {
                throw new RuntimeException("At least one billing detail is required for Retainer SOW");
            }
            
            // Calculate total value from billing details.
            // IMPORTANT: For SOW + CR we treat incoming amount as already-final
            // (MSA tax logic is applied on the frontend). Backend must NOT
            // re-apply tax here.
            for (CreateSOWRequest.BillingDetailDTO detail : request.getBillingDetails()) {
                if (detail.getAmount() != null) {
                    BigDecimal lineTotal = BigDecimal.valueOf(detail.getAmount());
                    totalValue = totalValue.add(lineTotal);
                }
            }
            
            // Create billing details (manual input, no longer auto-generated)
            createRetainerBillingDetails(contract.getId(), request.getBillingDetails(), parentMSA);
            
            // Create engaged engineers
            if (request.getEngagedEngineers() != null && !request.getEngagedEngineers().isEmpty()) {
                createSOWEngagedEngineers(contract.getId(), request.getEngagedEngineers());
            }
        } else if ("Fixed Price".equals(request.getEngagementType())) {
            if (request.getMilestoneDeliverables() != null && !request.getMilestoneDeliverables().isEmpty()) {
                createMilestoneDeliverables(contract.getId(), request.getMilestoneDeliverables());
            } else {
                throw new RuntimeException("At least one milestone deliverable is required for Fixed Price SOW");
            }
            
            // Use contractValue if provided, otherwise calculate from billing details (Total Amount including tax)
            // Note: For consistency, we prefer calculating from billing details sum to ensure contract value matches total billing
                if (request.getBillingDetails() != null && !request.getBillingDetails().isEmpty()) {
                // Calculate total value from billing details (all amounts already include tax from frontend)
                    for (CreateSOWRequest.BillingDetailDTO detail : request.getBillingDetails()) {
                        if (detail.getAmount() != null) {
                        BigDecimal lineTotal = BigDecimal.valueOf(detail.getAmount());
                        totalValue = totalValue.add(lineTotal);
                        }
                    }
            } else if (request.getContractValue() != null && request.getContractValue() > 0) {
                // Fallback to contractValue if billing details are not provided
                totalValue = BigDecimal.valueOf(request.getContractValue());
            }
            
            // Create billing details from milestone deliverables
            if (request.getBillingDetails() != null && !request.getBillingDetails().isEmpty()) {
                createFixedPriceBillingDetails(contract.getId(), request.getBillingDetails(), parentMSA);
            } else {
                // Auto-generate billing details from milestone deliverables and contract value
                if (request.getContractValue() != null && request.getContractValue() > 0) {
                    autoGenerateFixedPriceBillingDetails(contract.getId(), request.getMilestoneDeliverables(), request.getContractValue());
                } else {
                    throw new RuntimeException("Contract value or billing details are required for Fixed Price SOW");
                }
            }
        }
        
        // Set total value
        contract.setValue(totalValue);
        contract = sowContractRepository.save(contract);
        
        // Upload attachments and save to contract entity (similar to MSA)
        if (attachments != null && attachments.length > 0) {
            List<AttachmentInfo> fileInfos = uploadAttachments(contract.getId(), attachments, currentUser.getId());
            if (!fileInfos.isEmpty()) {
                contract.setLink(fileInfos.get(0).getS3Key()); // Store first file S3 key
                contract.setAttachmentsManifest(gson.toJson(fileInfos));
                contract = sowContractRepository.save(contract); // Update with file links
                sowContractRepository.flush(); // Force flush to database
            }
        }
        
        // Handle review if reviewer is assigned and review is submitted
        if (request.getReviewerId() != null && request.getReviewAction() != null) {
            submitReview(contract.getId(), request.getReviewNotes(), request.getReviewAction(), currentUser);
        }
        
        // Create initial history entry
        createHistoryEntry(contract.getId(), "CREATED", 
            "SOW Contract created by " + currentUser.getFullName(), null, null, currentUser.getId());
        
        // Convert to DTO
        SOWContractDTO dto = new SOWContractDTO();
        dto.setId(contract.getId());
        dto.setContractId(contractId);
        dto.setContractName(contract.getContractName());
        dto.setStatus(contract.getStatus().name().replace("_", " "));
        
        return dto;
    }
    
    /**
     * Submit review for SOW contract
     */
    public SOWContractDTO submitReview(Integer contractId, String reviewNotes, String action, User currentUser) {
        SOWContract contract = sowContractRepository.findById(contractId)
            .orElseThrow(() -> new RuntimeException("Contract not found"));
        
        // Verify current user is a Sales Manager (only Sales Managers can review)
        if (!"SALES_MANAGER".equals(currentUser.getRole())) {
            throw new RuntimeException("Only Sales Managers can submit reviews");
        }
        
        // Verify current user is the assigned reviewer
        if (contract.getReviewerId() == null || !contract.getReviewerId().equals(currentUser.getId())) {
            throw new RuntimeException("Only the assigned reviewer can submit review");
        }
        
        // Update contract status based on action
        if ("APPROVE".equalsIgnoreCase(action)) {
            // When approved, change status to "Client Under Review" (mapped from Under_Review enum)
            contract.setStatus(SOWContract.SOWContractStatus.Under_Review); // Maps to "Client Under Review" in display
        } else if ("REQUEST_REVISION".equalsIgnoreCase(action)) {
            // When requesting revision, move contract to Request for Change
            contract.setStatus(SOWContract.SOWContractStatus.Request_for_Change);
        }
        
        contract = sowContractRepository.save(contract);
        
        // Save review to contract_internal_review table (not visible to clients)
        ContractInternalReview review = new ContractInternalReview();
        review.setSowContractId(contractId);
        review.setContractId(null); // SOW contract only
        review.setContractType("SOW");
        review.setReviewerId(currentUser.getId());
        review.setReviewAction(action);
        review.setReviewNotes(reviewNotes);
        contractInternalReviewRepository.save(review);
        
        // DO NOT create history entry for internal review (to keep it hidden from clients)
        
        // Convert to DTO
        SOWContractDTO dto = new SOWContractDTO();
        dto.setId(contract.getId());
        String contractIdForDto = generateContractId(contract.getId(), contract.getCreatedAt());
        dto.setContractId(contractIdForDto);
        dto.setContractName(contract.getContractName());
        // Map status for display: Under_Review -> "Client Under Review" when approved
        String statusDisplay = contract.getStatus().name().replace("_", " ");
        if ("Under Review".equals(statusDisplay) && "APPROVE".equalsIgnoreCase(action)) {
            statusDisplay = "Client Under Review";
        } else if ("Under Review".equals(statusDisplay)) {
            statusDisplay = "Internal Review";
        }
        dto.setStatus(statusDisplay);
        
        // Send email notification to client when Sales Manager approves contract
        if ("APPROVE".equalsIgnoreCase(action)) {
            try {
                // Get client user
                if (contract.getClientId() != null) {
                    Optional<User> clientUserOpt = userRepository.findById(contract.getClientId());
                    if (clientUserOpt.isPresent()) {
                        User clientUser = clientUserOpt.get();
                        // Send email notification
                        emailService.sendContractPendingApprovalNotificationEmail(
                            clientUser,
                            "SOW",
                            contract.getContractName() != null ? contract.getContractName() : "SOW Contract",
                            contractIdForDto
                        );
                    }
                }
            } catch (Exception e) {
                // Log error but don't fail the contract approval
                System.err.println("Failed to send contract pending approval notification email: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        return dto;
    }
    
    /**
     * Helper to compare nullable objects with equals
     */
    private boolean equalsNullable(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    /**
     * Helper: parse engineer level string into level/role
     */
    private java.util.Map<String, String> parseLevelRole(String engineerLevel) {
        java.util.Map<String, String> result = new java.util.HashMap<>();
        if (engineerLevel == null || engineerLevel.trim().isEmpty()) {
            result.put("level", "");
            result.put("role", "");
            return result;
        }
        String[] parts = engineerLevel.trim().split("\\s+", 2);
        if (parts.length >= 2) {
            result.put("level", parts[0]);
            result.put("role", parts[1]);
        } else {
            result.put("level", engineerLevel.trim());
            result.put("role", "");
        }
        return result;
    }

    /**
     * Check if CR engineer matches existing baseline/legacy (unchanged auto-fill)
     */
    private boolean isSameAsExistingEng(
        CreateChangeRequestRequest.EngagedEngineerDTO dto,
        List<SOWEngagedEngineerBase> baselineEngs,
        List<SOWEngagedEngineer> legacyEngs,
        LocalDate effectiveStart
    ) {
        // Prefer level and role from DTO if available, otherwise parse from engineerLevel
        String crLevel = (dto.getLevel() != null && !dto.getLevel().trim().isEmpty()) 
            ? dto.getLevel().trim() 
            : parseLevelRole(dto.getEngineerLevel()).get("level");
        String crRole = (dto.getRole() != null && !dto.getRole().trim().isEmpty()) 
            ? dto.getRole().trim() 
            : parseLevelRole(dto.getEngineerLevel()).get("role");

        // Normalize dto values; if missing, allow fallback to existing values to detect unchanged auto-fill
        LocalDate dtoStart = null;
        LocalDate dtoEnd = null;
        if (dto.getStartDate() != null && !dto.getStartDate().trim().isEmpty()) {
            dtoStart = LocalDate.parse(dto.getStartDate());
        } else {
            dtoStart = effectiveStart; // fallback
        }
        if (dto.getEndDate() != null && !dto.getEndDate().trim().isEmpty()) {
            dtoEnd = LocalDate.parse(dto.getEndDate());
        }

        BigDecimal dtoRating = dto.getRating() != null ? BigDecimal.valueOf(dto.getRating()) : null;
        BigDecimal dtoSalary = dto.getSalary() != null ? BigDecimal.valueOf(dto.getSalary()) : null;

        // If baseEngineerId is provided, prioritize matching with that specific baseline engineer
        if (dto.getBaseEngineerId() != null) {
            for (SOWEngagedEngineerBase base : baselineEngs) {
                if (base.getId() != null && base.getId().equals(dto.getBaseEngineerId())) {
                    // Found the matching baseline engineer, compare all fields directly
                    // Use DTO values directly, don't fallback to baseline values for comparison
                    boolean levelSame = equalsNullable(base.getLevel(), crLevel);
                    boolean roleSame = equalsNullable(base.getRole(), crRole);
                    boolean startSame = equalsNullable(base.getStartDate(), dtoStart);
                    boolean endSame = equalsNullable(base.getEndDate(), dtoEnd);
                    boolean ratingSame = equalsNullable(base.getRating(), dtoRating);
                    boolean salarySame = equalsNullable(base.getUnitRate(), dtoSalary);
                    if (levelSame && roleSame && startSame && endSame && ratingSame && salarySame) {
                        return true; // Identical to baseline, skip saving
                    }
                    // If baseEngineerId matches but fields differ, this is a modification - don't skip
                    break;
                }
            }
        }

        // Compare with all baseline engineers (fallback if baseEngineerId not provided or no match)
        for (SOWEngagedEngineerBase base : baselineEngs) {
            // Skip if we already checked this one via baseEngineerId
            if (dto.getBaseEngineerId() != null && base.getId() != null && base.getId().equals(dto.getBaseEngineerId())) {
                continue;
            }
            LocalDate cmpStart = dtoStart != null ? dtoStart : base.getStartDate();
            LocalDate cmpEnd = dtoEnd != null ? dtoEnd : base.getEndDate();
            BigDecimal cmpRating = dtoRating != null ? dtoRating : base.getRating();
            BigDecimal cmpSalary = dtoSalary != null ? dtoSalary : base.getUnitRate();
            String cmpLevel = !crLevel.isEmpty() ? crLevel : base.getLevel();
            String cmpRole = !crRole.isEmpty() ? crRole : base.getRole();

            boolean levelSame = equalsNullable(base.getLevel(), cmpLevel);
            boolean roleSame = equalsNullable(base.getRole(), cmpRole);
            boolean startSame = equalsNullable(base.getStartDate(), cmpStart);
            boolean endSame = equalsNullable(base.getEndDate(), cmpEnd);
            boolean ratingSame = equalsNullable(base.getRating(), cmpRating);
            boolean salarySame = equalsNullable(base.getUnitRate(), cmpSalary);
            if (levelSame && roleSame && startSame && endSame && ratingSame && salarySame) {
                return true;
            }
        }

        // Compare with legacy current engineers
        for (SOWEngagedEngineer eng : legacyEngs) {
            java.util.Map<String, String> parsedLegacy = parseLevelRole(eng.getEngineerLevel());
            LocalDate cmpStart = dtoStart != null ? dtoStart : eng.getStartDate();
            LocalDate cmpEnd = dtoEnd != null ? dtoEnd : eng.getEndDate();
            BigDecimal cmpRating = dtoRating != null ? dtoRating : eng.getRating();
            BigDecimal cmpSalary = dtoSalary != null ? dtoSalary : eng.getSalary();
            String cmpLevel = !crLevel.isEmpty() ? crLevel : parsedLegacy.get("level");
            String cmpRole = !crRole.isEmpty() ? crRole : parsedLegacy.get("role");

            boolean levelSame = equalsNullable(parsedLegacy.get("level"), cmpLevel);
            boolean roleSame = equalsNullable(parsedLegacy.get("role"), cmpRole);
            boolean startSame = equalsNullable(eng.getStartDate(), cmpStart);
            boolean endSame = equalsNullable(eng.getEndDate(), cmpEnd);
            boolean ratingSame = equalsNullable(eng.getRating(), cmpRating);
            boolean salarySame = equalsNullable(eng.getSalary(), cmpSalary);
            if (levelSame && roleSame && startSame && endSame && ratingSame && salarySame) {
                return true;
            }
        }

        return false;
    }
    
    /**
     * Update SOW contract (Draft or Request_for_Change)
     * - Retainer: supports RFC edits to engineers & billing (existing behavior)
     * - Fixed Price: now supports RFC edits to milestone deliverables & billing
     * - Role-based transitions in RFC:
     *     Sales Rep -> Internal Review (Under_Review)
     *     Sales Manager -> Client Under Review (Under_Review + internal review APPROVE record)
     */
    @Transactional
    public SOWContractDTO updateSOWContract(Integer contractId, CreateSOWRequest request, MultipartFile[] attachments, User currentUser) {
        // Find existing contract
        SOWContract contract = sowContractRepository.findById(contractId)
            .orElseThrow(() -> new RuntimeException("SOW Contract not found"));
        
        // Check access permission (Sales Manager sees all, Sales Rep sees only assigned)
        if (!"SALES_MANAGER".equals(currentUser.getRole())) {
            if (contract.getAssigneeUserId() == null || !contract.getAssigneeUserId().equals(currentUser.getId())) {
                throw new RuntimeException("Access denied: You can only update contracts assigned to you");
            }
        }
        
        // Only allow update when status is Draft or Request_for_Change
        if (contract.getStatus() != SOWContract.SOWContractStatus.Draft && 
            contract.getStatus() != SOWContract.SOWContractStatus.Request_for_Change) {
            throw new RuntimeException("SOW Contract can only be updated when status is Draft or Request for Change. Current status: " + contract.getStatus().name());
        }
        
        boolean isRequestForChange = contract.getStatus() == SOWContract.SOWContractStatus.Request_for_Change;
        String engagementType = contract.getEngagementType() != null ? contract.getEngagementType() : "";
        boolean isRetainer = "Retainer".equals(engagementType);
        boolean isFixedPrice = "Fixed Price".equals(engagementType) || "Fixed_Price".equals(engagementType);
        
        // === Basic fields (Draft or RFC) ===
            if (request.getProjectName() != null) {
                contract.setProjectName(request.getProjectName());
                contract.setContractName("SOW Contract - " + contract.getProjectName());
            }
            if (request.getEffectiveStart() != null && !request.getEffectiveStart().trim().isEmpty()) {
                contract.setPeriodStart(LocalDate.parse(request.getEffectiveStart()));
            }
            if (request.getEffectiveEnd() != null && !request.getEffectiveEnd().trim().isEmpty()) {
                contract.setPeriodEnd(LocalDate.parse(request.getEffectiveEnd()));
            }
            if (request.getScopeSummary() != null) {
                contract.setScopeSummary(request.getScopeSummary());
            }
            if (request.getAssigneeUserId() != null) {
                User assignee = userRepository.findById(request.getAssigneeUserId())
                    .orElseThrow(() -> new RuntimeException("Assignee not found"));
                contract.setAssigneeUserId(request.getAssigneeUserId());
                contract.setLandbridgeContactName(assignee.getFullName());
                contract.setLandbridgeContactEmail(assignee.getEmail());
            }
            
        // Update contract value (Fixed Price)
        if (request.getContractValue() != null) {
            contract.setValue(BigDecimal.valueOf(request.getContractValue()));
        }
        
        // === Retainer: engineers & billing (Draft or RFC) ===
        if (isRetainer && request.getEngagedEngineers() != null && !request.getEngagedEngineers().isEmpty()) {
                List<SOWEngagedEngineer> existingEngineers = sowEngagedEngineerRepository.findBySowContractId(contractId);
            if (isRequestForChange) {
                LocalDate today = LocalDate.now();
                for (SOWEngagedEngineer existing : existingEngineers) {
                    if (existing.getStartDate() != null && existing.getStartDate().isAfter(today)) {
                    sowEngagedEngineerRepository.delete(existing);
                }
                }
            } else {
                for (SOWEngagedEngineer existing : existingEngineers) {
                    sowEngagedEngineerRepository.delete(existing);
                }
            }
                createSOWEngagedEngineers(contractId, request.getEngagedEngineers());
            }
            
        if (isRetainer && request.getBillingDetails() != null && !request.getBillingDetails().isEmpty()) {
            // Load parent MSA for tax info
            Contract parentMSA = null;
            if (contract.getParentMsaId() != null) {
                parentMSA = contractRepository.findById(contract.getParentMsaId()).orElse(null);
            }
                List<RetainerBillingDetail> existingBilling = retainerBillingDetailRepository.findBySowContractIdOrderByPaymentDateDesc(contractId);
            if (isRequestForChange) {
                LocalDate today = LocalDate.now();
                for (RetainerBillingDetail existing : existingBilling) {
                    if (existing.getPaymentDate() != null && existing.getPaymentDate().isAfter(today)) {
                    retainerBillingDetailRepository.delete(existing);
                }
                }
            } else {
                for (RetainerBillingDetail existing : existingBilling) {
                    retainerBillingDetailRepository.delete(existing);
                }
            }
            createRetainerBillingDetails(contractId, request.getBillingDetails(), parentMSA);
        }
        
        // === Fixed Price: milestones & billing (Draft or RFC) ===
        if (isFixedPrice && request.getMilestoneDeliverables() != null && !request.getMilestoneDeliverables().isEmpty()) {
            List<MilestoneDeliverable> existingMilestones = milestoneDeliverableRepository.findBySowContractIdOrderByPlannedEndAsc(contractId);
            for (MilestoneDeliverable existing : existingMilestones) {
                milestoneDeliverableRepository.delete(existing);
            }
            createMilestoneDeliverables(contractId, request.getMilestoneDeliverables());
        }
        
        if (isFixedPrice && request.getBillingDetails() != null && !request.getBillingDetails().isEmpty()) {
            // Load parent MSA for tax info
            Contract parentMSA = null;
            if (contract.getParentMsaId() != null) {
                parentMSA = contractRepository.findById(contract.getParentMsaId()).orElse(null);
            }
            List<FixedPriceBillingDetail> existingBilling = fixedPriceBillingDetailRepository.findBySowContractIdOrderByInvoiceDateDesc(contractId);
            for (FixedPriceBillingDetail existing : existingBilling) {
                fixedPriceBillingDetailRepository.delete(existing);
            }
            createFixedPriceBillingDetails(contractId, request.getBillingDetails(), parentMSA);
        }
        
        // Delivery Items (Retainer legacy)
        if (request.getDeliveryItems() != null && !request.getDeliveryItems().isEmpty()) {
            List<DeliveryItem> existingItems = deliveryItemRepository.findBySowContractIdOrderByPaymentDateDesc(contractId);
            for (DeliveryItem existing : existingItems) {
                deliveryItemRepository.delete(existing);
            }
            createDeliveryItems(contractId, request.getDeliveryItems());
            }
            
            contract = sowContractRepository.save(contract);
            
        // Status transitions for RFC
        if (isRequestForChange) {
            if ("SALES_REP".equals(currentUser.getRole())) {
                // Sales Rep: move to Internal Review only
                contract.setStatus(SOWContract.SOWContractStatus.Under_Review);
            } else if ("SALES_MANAGER".equals(currentUser.getRole())) {
                // Sales Manager: move to Client Under Review (resolve as Under_Review + APPROVE review)
                contract.setStatus(SOWContract.SOWContractStatus.Under_Review);
                ContractInternalReview review = new ContractInternalReview();
                review.setSowContractId(contract.getId());
                review.setContractType("SOW");
                review.setReviewerId(currentUser.getId());
                review.setReviewAction("APPROVE");
                review.setReviewNotes("Auto-approved by Sales Manager on update");
                contractInternalReviewRepository.save(review);
            }
            contract = sowContractRepository.save(contract);
            
            createHistoryEntry(contract.getId(), "UPDATED", 
                "SOW Contract updated by " + currentUser.getFullName() + " (Request for Change)", 
                null, null, currentUser.getId());
        } else {
            createHistoryEntry(contract.getId(), "UPDATED", 
                "SOW Contract updated by " + currentUser.getFullName(), 
                null, null, currentUser.getId());
        }
        
        // Upload new attachments if any
        if (attachments != null && attachments.length > 0) {
            List<AttachmentInfo> fileInfos = uploadAttachments(contractId, attachments, currentUser.getId());
            if (!fileInfos.isEmpty()) {
                contract = sowContractRepository.findById(contractId)
                    .orElseThrow(() -> new RuntimeException("Contract not found after save"));
                
                List<AttachmentInfo> existingInfos = new ArrayList<>();
                if (contract.getAttachmentsManifest() != null && !contract.getAttachmentsManifest().trim().isEmpty()) {
                    try {
                        // Try to parse as List<AttachmentInfo> (new format)
                        Type attachmentInfoListType = new TypeToken<List<AttachmentInfo>>(){}.getType();
                        existingInfos = gson.fromJson(contract.getAttachmentsManifest(), attachmentInfoListType);
                        if (existingInfos == null) {
                            // Fallback: try to parse as List<String> (old format)
                            Type stringListType = new TypeToken<List<String>>(){}.getType();
                            List<String> existingLinks = gson.fromJson(contract.getAttachmentsManifest(), stringListType);
                            if (existingLinks != null) {
                                for (String s3Key : existingLinks) {
                                    String fileName = s3Key;
                                    if (fileName.contains("/")) {
                                        fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                                    }
                                    existingInfos.add(new AttachmentInfo(s3Key, fileName));
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Error parsing attachments manifest: " + e.getMessage());
                        existingInfos = new ArrayList<>();
                    }
                }
                if (existingInfos == null) {
                    existingInfos = new ArrayList<>();
                }
                existingInfos.addAll(fileInfos);
                contract.setAttachmentsManifest(gson.toJson(existingInfos));
                contract = sowContractRepository.save(contract);
            }
        }
        
        // Reload contract to get latest state
        contract = sowContractRepository.findById(contractId)
            .orElseThrow(() -> new RuntimeException("Contract not found after update"));
        
        // Convert to DTO
        SOWContractDTO dto = new SOWContractDTO();
        dto.setId(contract.getId());
        dto.setContractId(generateContractId(contract.getId(), contract.getCreatedAt()));
        dto.setContractName(contract.getContractName());
        dto.setStatus(contract.getStatus().name().replace("_", " "));
        
        return dto;
    }
    
    /**
     * Get SOW contract detail
     */
    /**
     * Get all versions of a SOW contract
     */
    public List<SOWContractDetailDTO> getSOWContractVersions(Integer contractId, User currentUser) {
        SOWContract contract = sowContractRepository.findById(contractId)
            .orElseThrow(() -> new RuntimeException("SOW Contract not found"));
        
        // Check access permission (Sales Manager sees all, Sales Rep sees only assigned)
        if (!"SALES_MANAGER".equals(currentUser.getRole())) {
            if (contract.getAssigneeUserId() == null || !contract.getAssigneeUserId().equals(currentUser.getId())) {
                throw new RuntimeException("Access denied: You can only view contracts assigned to you");
            }
        }
        
        // Find the original contract ID (if this is a version, find the original)
        Integer originalContractId = contract.getParentVersionId() != null ? contract.getParentVersionId() : contractId;
        
        // Get all versions
        List<SOWContract> versions = sowContractRepository.findAllVersionsByParentVersionId(originalContractId);
        
        // If no versions found, return just the current contract
        if (versions.isEmpty()) {
            versions = java.util.Collections.singletonList(contract);
        }
        
        // Convert to DTOs
        List<SOWContractDetailDTO> versionDTOs = new ArrayList<>();
        for (SOWContract version : versions) {
            SOWContractDetailDTO dto = getSOWContractDetail(version.getId(), currentUser);
            versionDTOs.add(dto);
        }
        
        return versionDTOs;
    }
    
    public SOWContractDetailDTO getSOWContractDetail(Integer contractId, User currentUser) {
        SOWContract contract = sowContractRepository.findById(contractId)
            .orElseThrow(() -> new RuntimeException("SOW Contract not found"));
        
        // Check access permission (Sales Manager sees all, Sales Rep sees only assigned)
        if (!"SALES_MANAGER".equals(currentUser.getRole())) {
            if (contract.getAssigneeUserId() == null || !contract.getAssigneeUserId().equals(currentUser.getId())) {
                throw new RuntimeException("Access denied: You can only view contracts assigned to you");
            }
        }
        
        // Load client information
        User client = userRepository.findById(contract.getClientId())
            .orElseThrow(() -> new RuntimeException("Client not found"));
        
        // Load assignee
        User assignee = null;
        if (contract.getAssigneeUserId() != null) {
            assignee = userRepository.findById(contract.getAssigneeUserId()).orElse(null);
        }
        
        // Load parent MSA
        Contract parentMSA = null;
        String msaId = null;
        if (contract.getParentMsaId() != null) {
            parentMSA = contractRepository.findById(contract.getParentMsaId()).orElse(null);
            if (parentMSA != null) {
                // Generate MSA ID (format: MSA-YYYY-NN)
                int year = parentMSA.getCreatedAt() != null ? parentMSA.getCreatedAt().getYear() : 2025;
                int sequenceNumber = parentMSA.getId() % 100;
                msaId = String.format("MSA-%d-%02d", year, sequenceNumber);
            }
        }
        
        // Load client contact (default to client)
        User clientContact = client;
        
        // Load landbridge contact
        User landbridgeContact = null;
        if (contract.getLandbridgeContactEmail() != null) {
            landbridgeContact = userRepository.findByEmail(contract.getLandbridgeContactEmail()).orElse(null);
        }
        if (landbridgeContact == null && assignee != null) {
            landbridgeContact = assignee;
        }
        
        // Load attachments from attachments_manifest
        List<SOWContractDetailDTO.AttachmentDTO> attachments = new ArrayList<>();
        if (contract.getAttachmentsManifest() != null && !contract.getAttachmentsManifest().trim().isEmpty()) {
            try {
                // Try to parse as List<AttachmentInfo> (new format with fileName)
                Type attachmentInfoListType = new TypeToken<List<AttachmentInfo>>(){}.getType();
                List<AttachmentInfo> attachmentInfos = gson.fromJson(contract.getAttachmentsManifest(), attachmentInfoListType);
                if (attachmentInfos != null && !attachmentInfos.isEmpty()) {
                    for (AttachmentInfo info : attachmentInfos) {
                        if (info.getS3Key() != null && !info.getS3Key().trim().isEmpty()) {
                            attachments.add(new SOWContractDetailDTO.AttachmentDTO(info.getS3Key(), info.getFileName(), null));
                        }
                    }
                } else {
                    // Fallback: try to parse as List<String> (old format)
                    Type stringListType = new TypeToken<List<String>>(){}.getType();
                    List<String> attachmentLinks = gson.fromJson(contract.getAttachmentsManifest(), stringListType);
                    if (attachmentLinks != null && !attachmentLinks.isEmpty()) {
                        for (String s3Key : attachmentLinks) {
                            if (s3Key != null && !s3Key.trim().isEmpty()) {
                                String fileName = s3Key;
                                if (fileName.contains("/")) {
                                    fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                                }
                                attachments.add(new SOWContractDetailDTO.AttachmentDTO(s3Key, fileName, null));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // If parsing as AttachmentInfo fails, try List<String> (old format)
                try {
                    Type stringListType = new TypeToken<List<String>>(){}.getType();
                    List<String> attachmentLinks = gson.fromJson(contract.getAttachmentsManifest(), stringListType);
                    if (attachmentLinks != null && !attachmentLinks.isEmpty()) {
                        for (String s3Key : attachmentLinks) {
                            if (s3Key != null && !s3Key.trim().isEmpty()) {
                                String fileName = s3Key;
                                if (fileName.contains("/")) {
                                    fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                                }
                                attachments.add(new SOWContractDetailDTO.AttachmentDTO(s3Key, fileName, null));
                            }
                        }
                    }
                } catch (Exception e2) {
                    System.err.println("Error parsing attachments_manifest for SOW contract " + contractId + ": " + e2.getMessage());
                }
            }
        }
        
        // Fallback to DocumentMetadata if no attachments from manifest
        if (attachments.isEmpty()) {
            List<DocumentMetadata> documents = documentMetadataRepository.findByEntityIdAndEntityType(
                contractId, "sow_contract");
            attachments = documents.stream()
                .map(doc -> {
                    String fileName = doc.getS3Key();
                    if (fileName != null && fileName.contains("/")) {
                        fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                    }
                    return new SOWContractDetailDTO.AttachmentDTO(
                        doc.getS3Key(),
                        fileName,
                        null
                    );
                })
                .collect(java.util.stream.Collectors.toList());
        }
        
        // Get reviewer info
        Integer reviewerId = contract.getReviewerId();
        String reviewerName = null;
        String reviewNotes = null;
        String reviewAction = null;
        
        if (reviewerId != null) {
            User reviewer = userRepository.findById(reviewerId).orElse(null);
            if (reviewer != null) {
                reviewerName = reviewer.getFullName();
            }
        }
        
        // Get latest review info from contract_internal_review table
        Optional<ContractInternalReview> reviewOpt = contractInternalReviewRepository
            .findFirstBySowContractIdAndContractTypeOrderByReviewedAtDesc(contractId, "SOW");
        
        if (reviewOpt.isPresent()) {
            ContractInternalReview review = reviewOpt.get();
            reviewAction = review.getReviewAction();
            reviewNotes = review.getReviewNotes();
        }
        
        // Load SOW-specific data based on engagement type
        List<SOWContractDetailDTO.MilestoneDeliverableDTO> milestoneDeliverables = new ArrayList<>();
        List<SOWContractDetailDTO.DeliveryItemDTO> deliveryItems = new ArrayList<>();
        List<SOWContractDetailDTO.EngagedEngineerDTO> engagedEngineers = new ArrayList<>();
        List<SOWContractDetailDTO.BillingDetailDTO> billingDetails = new ArrayList<>();
        
        String engagementType = contract.getEngagementType() != null ? contract.getEngagementType() : "";
        if ("Fixed_Price".equals(engagementType) || "Fixed Price".equals(engagementType)) {
            // Load milestone deliverables
            List<MilestoneDeliverable> milestones = milestoneDeliverableRepository.findBySowContractIdOrderByPlannedEndAsc(contractId);
            for (MilestoneDeliverable milestone : milestones) {
                SOWContractDetailDTO.MilestoneDeliverableDTO dto = new SOWContractDetailDTO.MilestoneDeliverableDTO();
                dto.setId(milestone.getId());
                dto.setMilestone(milestone.getMilestone());
                dto.setDeliveryNote(milestone.getDeliveryNote());
                dto.setAcceptanceCriteria(milestone.getAcceptanceCriteria());
                dto.setPlannedEnd(milestone.getPlannedEnd() != null ? milestone.getPlannedEnd().toString() : null);
                dto.setPaymentPercentage(milestone.getPaymentPercentage() != null ? milestone.getPaymentPercentage().doubleValue() : null);
                milestoneDeliverables.add(dto);
            }
            
            // Load billing details
            List<FixedPriceBillingDetail> fixedPriceBilling = fixedPriceBillingDetailRepository.findBySowContractIdOrderByInvoiceDateDesc(contractId);
            // Reverse to get ascending order
            java.util.Collections.reverse(fixedPriceBilling);
            for (FixedPriceBillingDetail billing : fixedPriceBilling) {
                SOWContractDetailDTO.BillingDetailDTO dto = new SOWContractDetailDTO.BillingDetailDTO();
                dto.setId(billing.getId());
                dto.setBillingName(billing.getBillingName());
                dto.setMilestone(billing.getMilestone());
                dto.setAmount(billing.getAmount() != null ? billing.getAmount().doubleValue() : null);
                dto.setPercentage(billing.getPercentage() != null ? billing.getPercentage().doubleValue() : null);
                dto.setInvoiceDate(billing.getInvoiceDate() != null ? billing.getInvoiceDate().toString() : null);
                // FixedPriceBillingDetail doesn't have deliveryNote field
                // Try to get from milestone deliverable if milestoneDeliverableId is set
                String deliveryNote = null;
                if (billing.getMilestoneDeliverableId() != null) {
                    Optional<MilestoneDeliverable> milestoneOpt = milestoneDeliverableRepository.findById(billing.getMilestoneDeliverableId());
                    if (milestoneOpt.isPresent()) {
                        deliveryNote = milestoneOpt.get().getDeliveryNote();
                    }
                }
                dto.setDeliveryNote(deliveryNote);
                dto.setIsPaid(billing.getIsPaid() != null ? billing.getIsPaid() : false);
                billingDetails.add(dto);
            }
        } else if ("Retainer".equals(engagementType)) {
            // Load delivery items (deprecated, kept for backward compatibility)
            List<DeliveryItem> items = deliveryItemRepository.findBySowContractIdOrderByPaymentDateDesc(contractId);
            // Reverse to get ascending order
            java.util.Collections.reverse(items);
            for (DeliveryItem item : items) {
                SOWContractDetailDTO.DeliveryItemDTO dto = new SOWContractDetailDTO.DeliveryItemDTO();
                dto.setId(item.getId());
                dto.setMilestone(item.getMilestone());
                dto.setDeliveryNote(item.getDeliveryNote());
                dto.setAmount(item.getAmount() != null ? item.getAmount().doubleValue() : null);
                dto.setPaymentDate(item.getPaymentDate() != null ? item.getPaymentDate().toString() : null);
                deliveryItems.add(dto);
            }
            
            // EVENT-BASED APPROACH: Calculate current state from baseline + events
            LocalDate currentDate = LocalDate.now();
            
            // Try to load baseline first (for event-based contracts)
            List<SOWEngagedEngineerBase> baselineEngineers = sowBaselineService.getBaselineResources(contractId);
            
            if (!baselineEngineers.isEmpty()) {
                // Event-based: Calculate current engineers from baseline + events
                List<CREventService.CurrentEngineerState> currentEngineerStates = 
                    crEventService.calculateCurrentResources(contractId, currentDate);
                
                // Convert to DTOs
                for (CREventService.CurrentEngineerState state : currentEngineerStates) {
                    SOWContractDetailDTO.EngagedEngineerDTO dto = new SOWContractDetailDTO.EngagedEngineerDTO();
                    dto.setId(state.getEngineerId());
                    String level = state.getLevel() != null ? state.getLevel().trim() : "";
                    String role = state.getRole() != null ? state.getRole().trim() : "";
                    // Avoid duplicates like "Middle Frontend Middle Frontend"
                    String engineerLevel;
                    if (!level.isEmpty() && !role.isEmpty()) {
                        String roleLower = role.toLowerCase();
                        String levelLower = level.toLowerCase();
                        engineerLevel = roleLower.contains(levelLower) ? role : (level + " " + role);
                    } else if (!role.isEmpty()) {
                        engineerLevel = role;
                    } else {
                        engineerLevel = level;
                    }
                    dto.setEngineerLevel(engineerLevel.trim());
                    dto.setStartDate(state.getStartDate() != null ? state.getStartDate().toString() : null);
                    dto.setEndDate(state.getEndDate() != null ? state.getEndDate().toString() : null);
                    // Event-based system uses baseline which is Monthly by default
                    dto.setBillingType("Monthly");
                    dto.setHourlyRate(null);
                    dto.setHours(null);
                    dto.setSubtotal(null);
                    dto.setRating(state.getRating() != null ? state.getRating().doubleValue() : null);
                    dto.setSalary(state.getUnitRate() != null ? state.getUnitRate().doubleValue() : null);
                    engagedEngineers.add(dto);
                }
                
                // Calculate current billing from baseline + events
                List<RetainerBillingBase> baselineBillingList = sowBaselineService.getBaselineBilling(contractId);
                
                // Get all unique billing months
                java.util.Set<LocalDate> billingMonths = new java.util.HashSet<>();
                for (RetainerBillingBase base : baselineBillingList) {
                    if (base.getBillingMonth() != null) {
                        billingMonths.add(base.getBillingMonth());
                    }
                }
                
                // Get all billing events to find additional months
                List<CRBillingEvent> allBillingEvents = crBillingEventRepository.findApprovedEventsBySowContractId(contractId);
                for (CRBillingEvent event : allBillingEvents) {
                    if (event.getBillingMonth() != null) {
                        billingMonths.add(event.getBillingMonth());
                    }
                }
                
                // Calculate billing for each month (baseline + events)
                for (LocalDate month : billingMonths.stream().sorted().collect(java.util.stream.Collectors.toList())) {
                    BigDecimal totalAmount = crEventService.calculateCurrentBilling(contractId, month);
                    
                    SOWContractDetailDTO.BillingDetailDTO dto = new SOWContractDetailDTO.BillingDetailDTO();
                    dto.setId(null); // No specific ID for calculated billing
                    dto.setAmount(totalAmount != null ? totalAmount.doubleValue() : null);
                    dto.setPercentage(null);
                    dto.setInvoiceDate(month != null ? month.toString() : null);
                    
                    // Get description from baseline or events
                    String description = "";
                    var baselineOpt = baselineBillingList.stream()
                        .filter(b -> b.getBillingMonth() != null && b.getBillingMonth().equals(month))
                        .findFirst();
                    if (baselineOpt.isPresent() && baselineOpt.get().getDescription() != null) {
                        description = baselineOpt.get().getDescription();
                    }
                    
                    // Add event descriptions
                    List<CRBillingEvent> monthEvents = allBillingEvents.stream()
                        .filter(e -> e.getBillingMonth() != null && e.getBillingMonth().equals(month))
                        .collect(java.util.stream.Collectors.toList());
                    if (!monthEvents.isEmpty()) {
                        if (!description.isEmpty()) description += "; ";
                        description += monthEvents.stream()
                            .map(e -> e.getDescription() != null ? e.getDescription() : "")
                            .collect(java.util.stream.Collectors.joining("; "));
                    }
                    
                    dto.setDeliveryNote(description);
                    dto.setIsPaid(false); // Event-based billing details don't have payment status
                    billingDetails.add(dto);
                }

                // Fallback: if for any reason current resources list is empty, use legacy table
                if (engagedEngineers.isEmpty()) {
                    List<SOWEngagedEngineer> engineers = sowEngagedEngineerRepository.findBySowContractId(contractId);
                    for (SOWEngagedEngineer engineer : engineers) {
                        SOWContractDetailDTO.EngagedEngineerDTO dto = new SOWContractDetailDTO.EngagedEngineerDTO();
                        dto.setId(engineer.getId());
                        dto.setEngineerLevel(engineer.getEngineerLevel());
                        dto.setStartDate(engineer.getStartDate() != null ? engineer.getStartDate().toString() : null);
                        dto.setEndDate(engineer.getEndDate() != null ? engineer.getEndDate().toString() : null);
                        dto.setBillingType(engineer.getBillingType() != null ? engineer.getBillingType() : "Monthly");
                        dto.setHourlyRate(engineer.getHourlyRate() != null ? engineer.getHourlyRate().doubleValue() : null);
                        dto.setHours(engineer.getHours() != null ? engineer.getHours().doubleValue() : null);
                        dto.setSubtotal(engineer.getSubtotal() != null ? engineer.getSubtotal().doubleValue() : null);
                        dto.setRating(engineer.getRating() != null ? engineer.getRating().doubleValue() : null);
                        dto.setSalary(engineer.getSalary() != null ? engineer.getSalary().doubleValue() : null);
                        engagedEngineers.add(dto);
                    }
                }
            } else {
                // Fallback to old approach (for contracts that haven't been migrated to event-based)
                // Load engaged engineers from old table
                List<SOWEngagedEngineer> engineers = sowEngagedEngineerRepository.findBySowContractId(contractId);
                for (SOWEngagedEngineer engineer : engineers) {
                    SOWContractDetailDTO.EngagedEngineerDTO dto = new SOWContractDetailDTO.EngagedEngineerDTO();
                    dto.setId(engineer.getId());
                    dto.setEngineerLevel(engineer.getEngineerLevel());
                    dto.setStartDate(engineer.getStartDate() != null ? engineer.getStartDate().toString() : null);
                    dto.setEndDate(engineer.getEndDate() != null ? engineer.getEndDate().toString() : null);
                    dto.setBillingType(engineer.getBillingType() != null ? engineer.getBillingType() : "Monthly");
                    dto.setHourlyRate(engineer.getHourlyRate() != null ? engineer.getHourlyRate().doubleValue() : null);
                    dto.setHours(engineer.getHours() != null ? engineer.getHours().doubleValue() : null);
                    dto.setSubtotal(engineer.getSubtotal() != null ? engineer.getSubtotal().doubleValue() : null);
                    dto.setRating(engineer.getRating() != null ? engineer.getRating().doubleValue() : null);
                    dto.setSalary(engineer.getSalary() != null ? engineer.getSalary().doubleValue() : null);
                    engagedEngineers.add(dto);
                }
                
                // Load billing details from old table
                List<RetainerBillingDetail> retainerBilling = retainerBillingDetailRepository.findBySowContractIdOrderByPaymentDateDesc(contractId);
                // Reverse to get ascending order
                java.util.Collections.reverse(retainerBilling);
                for (RetainerBillingDetail billing : retainerBilling) {
                    SOWContractDetailDTO.BillingDetailDTO dto = new SOWContractDetailDTO.BillingDetailDTO();
                    dto.setId(billing.getId());
                    // RetainerBillingDetail doesn't have billingName and milestone fields
                    // Try to get from delivery item if deliveryItemId is set
                    String billingName = null;
                    String milestone = null;
                    if (billing.getDeliveryItemId() != null) {
                        Optional<DeliveryItem> deliveryItemOpt = deliveryItemRepository.findById(billing.getDeliveryItemId());
                        if (deliveryItemOpt.isPresent()) {
                            DeliveryItem item = deliveryItemOpt.get();
                            milestone = item.getMilestone();
                            billingName = item.getMilestone() + " Payment"; // Generate billing name from milestone
                        }
                    }
                    dto.setBillingName(billingName);
                    dto.setMilestone(milestone);
                    dto.setAmount(billing.getAmount() != null ? billing.getAmount().doubleValue() : null);
                    dto.setPercentage(null); // Retainer doesn't have percentage
                    dto.setInvoiceDate(billing.getPaymentDate() != null ? billing.getPaymentDate().toString() : null);
                    dto.setDeliveryNote(billing.getDeliveryNote());
                    dto.setIsPaid(billing.getIsPaid() != null ? billing.getIsPaid() : false);
                    billingDetails.add(dto);
                }
            }
        }
        
        // Load history
        List<ContractHistory> allHistory = contractHistoryRepository.findBySowContractIdOrderByEntryDateDesc(contractId);
        List<SOWContractDetailDTO.HistoryItemDTO> history = new ArrayList<>();
        for (ContractHistory hist : allHistory) {
            SOWContractDetailDTO.HistoryItemDTO dto = new SOWContractDetailDTO.HistoryItemDTO();
            dto.setId(hist.getId());
            dto.setDate(hist.getEntryDate() != null ? hist.getEntryDate().toString() : null);
            dto.setDescription(hist.getDescription());
            dto.setDocumentLink(hist.getDocumentLink());
            dto.setDocumentName(hist.getDocumentName());
            history.add(dto);
        }
        
        // Build DTO
        SOWContractDetailDTO dto = new SOWContractDetailDTO();
        dto.setId(contract.getId());
        dto.setContractId(generateContractId(contract.getId(), contract.getCreatedAt()));
        dto.setContractName(contract.getContractName());
        
        // Map status for display
        String statusDisplay = contract.getStatus().name().replace("_", " ");
        if ("Under Review".equals(statusDisplay)) {
            if (reviewOpt.isPresent() && "APPROVE".equals(reviewOpt.get().getReviewAction())) {
                statusDisplay = "Client Under Review";
            } else {
                statusDisplay = "Internal Review";
            }
        }
        dto.setStatus(statusDisplay);
        
        dto.setMsaId(msaId);
        dto.setClientId(contract.getClientId());
        dto.setClientName(client.getFullName());
        dto.setClientEmail(client.getEmail());
        dto.setEffectiveStart(contract.getPeriodStart() != null ? contract.getPeriodStart().toString() : null);
        dto.setEffectiveEnd(contract.getPeriodEnd() != null ? contract.getPeriodEnd().toString() : null);
        dto.setAssigneeUserId(contract.getAssigneeUserId());
        dto.setAssigneeName(assignee != null ? assignee.getFullName() : null);
        dto.setProjectName(contract.getProjectName());
        dto.setScopeSummary(contract.getScopeSummary());
        dto.setEngagementType(contract.getEngagementType() != null ? contract.getEngagementType().replace("_", " ") : null);
        dto.setValue(contract.getValue() != null ? contract.getValue().doubleValue() : null);
        
        dto.setCurrency(contract.getCurrency());
        dto.setPaymentTerms(contract.getPaymentTerms());
        dto.setInvoicingCycle(contract.getInvoicingCycle());
        dto.setBillingDay(contract.getBillingDay());
        dto.setTaxWithholding(contract.getTaxWithholding());
        // Set taxType from parent MSA (SOW inherits tax settings from MSA)
        if (parentMSA != null) {
            dto.setTaxType(parentMSA.getTaxType());
        } else {
            dto.setTaxType(null); // Default to null if no parent MSA
        }
        dto.setIpOwnership(contract.getIpOwnership());
        dto.setGoverningLaw(contract.getGoverningLaw());
        
        dto.setClientContactId(clientContact.getId());
        dto.setClientContactName(clientContact.getFullName());
        dto.setClientContactEmail(clientContact.getEmail());
        dto.setLandbridgeContactId(landbridgeContact != null ? landbridgeContact.getId() : null);
        dto.setLandbridgeContactName(landbridgeContact != null ? landbridgeContact.getFullName() : null);
        dto.setLandbridgeContactEmail(landbridgeContact != null ? landbridgeContact.getEmail() : null);
        
        dto.setMilestoneDeliverables(milestoneDeliverables);
        dto.setDeliveryItems(deliveryItems);
        dto.setEngagedEngineers(engagedEngineers);
        dto.setBillingDetails(billingDetails);
        dto.setAttachments(attachments);
        dto.setReviewerId(reviewerId);
        dto.setReviewerName(reviewerName);
        dto.setReviewNotes(reviewNotes);
        dto.setReviewAction(reviewAction);
        dto.setHistory(history);
        dto.setVersion(contract.getVersion()); // Set version number
        
        return dto;
    }
    
    /**
     * Find MSA contract by contractId string (format: MSA-YYYY-NN)
     */
    private Contract findMSAByContractId(String contractId) {
        if (contractId == null || contractId.trim().isEmpty()) {
            return null;
        }
        
        // Parse contractId: MSA-YYYY-NN
        if (!contractId.startsWith("MSA-")) {
            return null;
        }
        
        try {
            String[] parts = contractId.substring(4).split("-");
            if (parts.length != 2) {
                return null;
            }
            
            int year = Integer.parseInt(parts[0]);
            int sequenceNumber = Integer.parseInt(parts[1]);
            
            // Find all contracts and match by generated contractId
            List<Contract> allContracts = contractRepository.findAll();
            for (Contract contract : allContracts) {
                String generatedId = generateMSAContractId(contract.getId(), contract.getCreatedAt());
                if (contractId.equals(generatedId)) {
                    return contract;
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing contractId: " + contractId + " - " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Generate MSA contract ID in format: MSA-YYYY-NN
     */
    private String generateMSAContractId(Integer id, LocalDateTime createdAt) {
        int year = createdAt != null ? createdAt.getYear() : 2025;
        // Use last 2 digits of ID as sequence number (simplified)
        int sequenceNumber = id % 100;
        return String.format("MSA-%d-%02d", year, sequenceNumber);
    }
    
    /**
     * Generate SOW contract ID in format: SOW-YYYY-MM-DD-NN
     */
    private String generateContractId(Integer id, LocalDateTime createdAt) {
        int year = createdAt != null ? createdAt.getYear() : 2025;
        int month = createdAt != null ? createdAt.getMonthValue() : 1;
        int day = createdAt != null ? createdAt.getDayOfMonth() : 1;
        // Use last 2 digits of ID as sequence number (simplified)
        int sequenceNumber = id % 100;
        return String.format("SOW-%d-%02d-%02d-%02d", year, month, day, sequenceNumber);
    }
    
    /**
     * Map status string to SOWContractStatus enum
     */
    private SOWContract.SOWContractStatus mapStatusToEnum(String status) {
        if (status == null || status.trim().isEmpty()) {
            return SOWContract.SOWContractStatus.Draft;
        }
        
        String statusUpper = status.toUpperCase().replace(" ", "_");
        try {
            return SOWContract.SOWContractStatus.valueOf(statusUpper);
        } catch (IllegalArgumentException e) {
            // Map common status strings
            if (statusUpper.contains("INTERNAL_REVIEW") || statusUpper.contains("INTERNALREVIEW")) {
                return SOWContract.SOWContractStatus.Under_Review;
            } else if (statusUpper.contains("CLIENT_UNDER_REVIEW") || statusUpper.contains("CLIENTUNDERREVIEW")) {
                return SOWContract.SOWContractStatus.Under_Review;
            } else {
                return SOWContract.SOWContractStatus.Draft;
            }
        }
    }
    
    /**
     * Create delivery items for Retainer SOW
     */
    private void createDeliveryItems(Integer sowContractId, List<CreateSOWRequest.DeliveryItemDTO> deliveryItems) {
        for (CreateSOWRequest.DeliveryItemDTO item : deliveryItems) {
            DeliveryItem deliveryItem = new DeliveryItem();
            deliveryItem.setSowContractId(sowContractId);
            deliveryItem.setMilestone(item.getMilestone());
            deliveryItem.setDeliveryNote(item.getDeliveryNote());
            deliveryItem.setAmount(BigDecimal.valueOf(item.getAmount()));
            deliveryItem.setPaymentDate(LocalDate.parse(item.getPaymentDate()));
            deliveryItemRepository.save(deliveryItem);
        }
    }
    
    /**
     * Create milestone deliverables for Fixed Price SOW
     */
    private void createMilestoneDeliverables(Integer sowContractId, List<CreateSOWRequest.MilestoneDeliverableDTO> milestones) {
        for (CreateSOWRequest.MilestoneDeliverableDTO milestone : milestones) {
            MilestoneDeliverable deliverable = new MilestoneDeliverable();
            deliverable.setSowContractId(sowContractId);
            deliverable.setMilestone(milestone.getMilestone());
            deliverable.setDeliveryNote(milestone.getDeliveryNote());
            deliverable.setAcceptanceCriteria(milestone.getAcceptanceCriteria());
            deliverable.setPlannedEnd(LocalDate.parse(milestone.getPlannedEnd()));
            deliverable.setPaymentPercentage(BigDecimal.valueOf(milestone.getPaymentPercentage()));
            milestoneDeliverableRepository.save(deliverable);
        }
    }
    
    /**
     * Create retainer billing details
     * Stores Total Amount (including tax) based on parent MSA tax settings
     */
    private void createRetainerBillingDetails(Integer sowContractId, List<CreateSOWRequest.BillingDetailDTO> billingDetails, Contract parentMSA) {
        for (CreateSOWRequest.BillingDetailDTO detail : billingDetails) {
            if (detail.getAmount() == null) {
                continue;
            }
            RetainerBillingDetail billingDetail = new RetainerBillingDetail();
            billingDetail.setSowContractId(sowContractId);
            billingDetail.setPaymentDate(LocalDate.parse(detail.getPaymentDate()));
            billingDetail.setDeliveryNote(detail.getDeliveryNote());
            // For SOW Retainer billing, frontend already sends the final amount
            // (including tax logic according to parent MSA). Do not re-apply tax here.
            BigDecimal finalAmount = BigDecimal.valueOf(detail.getAmount());
            billingDetail.setAmount(finalAmount);
            retainerBillingDetailRepository.save(billingDetail);
        }
    }
    
    /**
     * Create SOW engaged engineers for Retainer SOW
     */
    private void createSOWEngagedEngineers(Integer sowContractId, List<CreateSOWRequest.EngagedEngineerDTO> engagedEngineers) {
        for (CreateSOWRequest.EngagedEngineerDTO engineerDTO : engagedEngineers) {
            SOWEngagedEngineer engineer = new SOWEngagedEngineer();
            engineer.setSowContractId(sowContractId);
            engineer.setEngineerLevel(engineerDTO.getEngineerLevel());
            if (engineerDTO.getStartDate() != null && !engineerDTO.getStartDate().trim().isEmpty()) {
                engineer.setStartDate(LocalDate.parse(engineerDTO.getStartDate()));
            }
            if (engineerDTO.getEndDate() != null && !engineerDTO.getEndDate().trim().isEmpty()) {
                engineer.setEndDate(LocalDate.parse(engineerDTO.getEndDate()));
            }
            // Set billing type (default to "Monthly" if not provided)
            String billingType = (engineerDTO.getBillingType() != null && !engineerDTO.getBillingType().trim().isEmpty()) 
                ? engineerDTO.getBillingType() : "Monthly";
            engineer.setBillingType(billingType);
            
            if ("Hourly".equals(billingType)) {
                // For hourly billing
                if (engineerDTO.getHourlyRate() != null) {
                    engineer.setHourlyRate(BigDecimal.valueOf(engineerDTO.getHourlyRate()));
                }
                if (engineerDTO.getHours() != null) {
                    engineer.setHours(BigDecimal.valueOf(engineerDTO.getHours()));
                }
                if (engineerDTO.getSubtotal() != null) {
                    engineer.setSubtotal(BigDecimal.valueOf(engineerDTO.getSubtotal()));
                } else if (engineerDTO.getHourlyRate() != null && engineerDTO.getHours() != null) {
                    // Auto-calculate subtotal if not provided
                    engineer.setSubtotal(BigDecimal.valueOf(engineerDTO.getHourlyRate() * engineerDTO.getHours()));
                }
                // For hourly, rating represents hourly rate, so set salary to subtotal
                if (engineerDTO.getSubtotal() != null) {
                    engineer.setSalary(BigDecimal.valueOf(engineerDTO.getSubtotal()));
                } else if (engineerDTO.getSalary() != null) {
                    engineer.setSalary(BigDecimal.valueOf(engineerDTO.getSalary()));
                }
                // Rating is not used for hourly, but keep it for backward compatibility
                if (engineerDTO.getRating() != null) {
                    engineer.setRating(BigDecimal.valueOf(engineerDTO.getRating()));
                }
            } else {
                // For monthly billing
                engineer.setHourlyRate(null);
                engineer.setHours(null);
                engineer.setSubtotal(null);
                if (engineerDTO.getRating() != null) {
                    engineer.setRating(BigDecimal.valueOf(engineerDTO.getRating()));
                }
                if (engineerDTO.getSalary() != null) {
                    engineer.setSalary(BigDecimal.valueOf(engineerDTO.getSalary()));
                }
            }
            sowEngagedEngineerRepository.save(engineer);
        }
    }
    
    /**
     * Auto-generate retainer billing details from delivery items
     * (Legacy path - keep as-is, amounts are assumed to be total amounts)
     */
    private void autoGenerateRetainerBillingDetails(Integer sowContractId, List<CreateSOWRequest.DeliveryItemDTO> deliveryItems) {
        for (CreateSOWRequest.DeliveryItemDTO item : deliveryItems) {
            RetainerBillingDetail billingDetail = new RetainerBillingDetail();
            billingDetail.setSowContractId(sowContractId);
            billingDetail.setPaymentDate(LocalDate.parse(item.getPaymentDate()));
            billingDetail.setDeliveryNote(item.getDeliveryNote());
            billingDetail.setAmount(BigDecimal.valueOf(item.getAmount()));
            retainerBillingDetailRepository.save(billingDetail);
        }
    }
    
    /**
     * Create fixed price billing details
     * Stores Total Amount (including tax) based on parent MSA tax settings
     */
    private void createFixedPriceBillingDetails(Integer sowContractId, List<CreateSOWRequest.BillingDetailDTO> billingDetails, Contract parentMSA) {
        for (CreateSOWRequest.BillingDetailDTO detail : billingDetails) {
            if (detail.getAmount() == null) {
                continue;
            }
            FixedPriceBillingDetail billingDetail = new FixedPriceBillingDetail();
            billingDetail.setSowContractId(sowContractId);
            billingDetail.setBillingName(detail.getBillingName() != null ? detail.getBillingName() : "Payment");
            billingDetail.setMilestone(detail.getMilestone());
            // For SOW Fixed Price billing, frontend already sends the final amount
            // (including tax logic according to parent MSA). Do not re-apply tax here.
            BigDecimal finalAmount = BigDecimal.valueOf(detail.getAmount());
            billingDetail.setAmount(finalAmount);
            billingDetail.setPercentage(detail.getPercentage() != null ? BigDecimal.valueOf(detail.getPercentage()) : null);
            billingDetail.setInvoiceDate(LocalDate.parse(detail.getPaymentDate())); // Using paymentDate as invoiceDate
            fixedPriceBillingDetailRepository.save(billingDetail);
        }
    }
    
    /**
     * Auto-generate fixed price billing details from milestone deliverables and contract value
     */
    private void autoGenerateFixedPriceBillingDetails(Integer sowContractId, List<CreateSOWRequest.MilestoneDeliverableDTO> milestones, Double contractValue) {
        BigDecimal totalValue = BigDecimal.valueOf(contractValue);
        for (CreateSOWRequest.MilestoneDeliverableDTO milestone : milestones) {
            FixedPriceBillingDetail billingDetail = new FixedPriceBillingDetail();
            billingDetail.setSowContractId(sowContractId);
            billingDetail.setBillingName(milestone.getMilestone() != null && !milestone.getMilestone().isEmpty() ? 
                milestone.getMilestone() + " Payment" : "Payment");
            billingDetail.setMilestone(milestone.getMilestone());
            // Calculate amount from contract value and payment percentage
            BigDecimal percentage = milestone.getPaymentPercentage() != null ? 
                BigDecimal.valueOf(milestone.getPaymentPercentage()) : BigDecimal.ZERO;
            BigDecimal amount = totalValue.multiply(percentage).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            billingDetail.setAmount(amount);
            billingDetail.setPercentage(percentage);
            if (milestone.getPlannedEnd() != null && !milestone.getPlannedEnd().isEmpty()) {
                billingDetail.setInvoiceDate(LocalDate.parse(milestone.getPlannedEnd()));
            }
            fixedPriceBillingDetailRepository.save(billingDetail);
        }
    }
    
    /**
     * Parse tax rate from taxWithholding string (e.g., "10%" or "10")
     */
    private BigDecimal parseTaxRate(String taxWithholding) {
        if (taxWithholding == null) {
            return BigDecimal.ZERO;
        }
        String trimmed = taxWithholding.trim();
        if (trimmed.isEmpty()) {
            return BigDecimal.ZERO;
        }
        if (trimmed.endsWith("%")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        try {
            return new BigDecimal(trimmed);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Apply tax to input amount based on parent MSA tax settings.
     * - If taxRate = 0 or parentMSA is null → return inputAmount
     * - If taxType = EXCLUDED → total = input + tax
     * - If taxType = INCLUDED or null → total = input
     */
    private BigDecimal applyTax(BigDecimal inputAmount, Contract parentMSA) {
        if (inputAmount == null || parentMSA == null) {
            return inputAmount;
        }
        BigDecimal rate = parseTaxRate(parentMSA.getTaxWithholding());
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            return inputAmount;
        }
        String taxType = parentMSA.getTaxType();
        if (taxType != null && taxType.equalsIgnoreCase("Excluded")) {
            BigDecimal taxAmount = inputAmount
                .multiply(rate)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            return inputAmount.add(taxAmount);
        }
        // Included or unspecified → treat input as total
        return inputAmount;
    }
    
    /**
     * Upload attachments to S3
     * Returns list of S3 keys (similar to MSA)
     */
    private List<AttachmentInfo> uploadAttachments(Integer contractId, MultipartFile[] attachments, Integer ownerId) {
        List<AttachmentInfo> fileInfos = new ArrayList<>();
        System.out.println("uploadAttachments called: contractId=" + contractId + ", attachments.length=" + (attachments != null ? attachments.length : 0) + ", s3Enabled=" + s3Enabled + ", s3Service=" + (s3Service != null ? "not null" : "null"));
        
        // Skip upload if S3 is not configured or enabled
        if (!s3Enabled || s3Service == null) {
            System.out.println("S3 is not configured or enabled. Skipping file upload for contract: " + contractId);
            return fileInfos;
        }
        
        for (MultipartFile file : attachments) {
            if (file.isEmpty()) {
                System.out.println("Skipping empty file: " + file.getOriginalFilename());
                continue;
            }
            
            // Validate file type (PDF only)
            String contentType = file.getContentType();
            String originalFileName = file.getOriginalFilename();
            System.out.println("File: " + originalFileName + ", contentType: " + contentType + ", size: " + file.getSize());
            if (contentType == null || !contentType.equals("application/pdf")) {
                System.out.println("Skipping non-PDF file: " + originalFileName + " (contentType: " + contentType + ")");
                continue; // Skip non-PDF files
            }
            
            try {
                // Upload to S3 (returns S3 key)
                String s3Key = s3Service.uploadFile(file, "contracts/sow/" + contractId);
                System.out.println("File uploaded successfully. S3 key: " + s3Key);
                fileInfos.add(new AttachmentInfo(s3Key, originalFileName));
                
                // Save document metadata
                DocumentMetadata metadata = new DocumentMetadata();
                metadata.setS3Key(s3Key);
                metadata.setOwnerId(ownerId);
                metadata.setDocumentType("contract");
                metadata.setEntityId(contractId);
                metadata.setEntityType("sow_contract");
                // Allow SALES_MANAGER and SALES_REP roles to access
                metadata.setAllowedRoles(gson.toJson(Arrays.asList("SALES_MANAGER", "SALES_REP")));
                documentMetadataRepository.save(metadata);
                System.out.println("DocumentMetadata saved for S3 key: " + s3Key);
            } catch (IOException e) {
                System.err.println("IOException uploading file: " + originalFileName + " - " + e.getMessage());
                throw new RuntimeException("Failed to upload file: " + originalFileName, e);
            } catch (RuntimeException e) {
                // If S3 upload fails, log error but don't fail the entire contract creation
                System.err.println("RuntimeException uploading file to S3: " + originalFileName + " - " + e.getMessage());
                e.printStackTrace();
                // Continue with other files
            }
        }
        
        System.out.println("uploadAttachments returning " + fileInfos.size() + " file(s)");
        return fileInfos;
    }
    
    /**
     * Create history entry for SOW contract
     */
    private void createHistoryEntry(Integer contractId, String activityType, String description, 
                                   String fileLink, String fileUrl, Integer createdBy) {
        ContractHistory history = new ContractHistory();
        history.setContractId(null); // SOW contracts use sow_contract_id
        history.setSowContractId(contractId);
        history.setHistoryType("SOW");
        history.setEntryDate(LocalDate.now());
        history.setDescription(description);
        history.setDocumentLink(fileLink);
        history.setDocumentName(fileUrl);
        history.setCreatedBy(createdBy);
        contractHistoryRepository.save(history);
    }
    
    /**
     * Get change requests list for SOW contract with pagination
     */
    public ChangeRequestsListResponseDTO getChangeRequestsForSOW(Integer sowContractId, int page, int size, User currentUser) {
        // Verify SOW contract exists and user has access
        SOWContract sowContract = sowContractRepository.findById(sowContractId)
            .orElseThrow(() -> new RuntimeException("SOW Contract not found"));
        
        // Check access permission (Sales Manager sees all, Sales Rep sees only assigned)
        if (!"SALES_MANAGER".equals(currentUser.getRole())) {
            if (sowContract.getAssigneeUserId() == null || !sowContract.getAssigneeUserId().equals(currentUser.getId())) {
                throw new RuntimeException("Access denied: You can only view change requests for contracts assigned to you");
            }
        }
        
        // Create pageable
        Pageable pageable = PageRequest.of(page, size);
        
        // Fetch change requests with pagination
        Page<ChangeRequest> changeRequestPage = changeRequestRepository
            .findBySowContractIdAndContractTypeOrderByCreatedAtDesc(sowContractId, pageable);
        
        // Convert to DTOs
        List<ChangeRequestListItemDTO> content = new ArrayList<>();
        for (ChangeRequest cr : changeRequestPage.getContent()) {
            ChangeRequestListItemDTO dto = new ChangeRequestListItemDTO();
            dto.setId(cr.getId());
            dto.setChangeRequestId(cr.getChangeRequestId());
            dto.setType(cr.getType());
            dto.setSummary(cr.getSummary());
            dto.setEffectiveFrom(cr.getEffectiveFrom() != null ? cr.getEffectiveFrom().toString() : null);
            dto.setEffectiveUntil(cr.getEffectiveUntil() != null ? cr.getEffectiveUntil().toString() : null);
            dto.setExpectedExtraCost(cr.getExpectedExtraCost() != null ? cr.getExpectedExtraCost().doubleValue() : null);
            dto.setCostEstimatedByLandbridge(cr.getCostEstimatedByLandbridge() != null ? cr.getCostEstimatedByLandbridge().doubleValue() : null);
            dto.setStatus(cr.getStatus());
            content.add(dto);
        }
        
        // Build response
        ChangeRequestsListResponseDTO response = new ChangeRequestsListResponseDTO();
        response.setContent(content);
        response.setTotalElements(changeRequestPage.getTotalElements());
        response.setTotalPages(changeRequestPage.getTotalPages());
        response.setCurrentPage(page);
        response.setPageSize(size);
        
        return response;
    }
    
    /**
     * Create change request for Retainer SOW contract
     */
    public ChangeRequestResponseDTO createChangeRequestForSOW(
        Integer sowContractId,
        CreateChangeRequestRequest request,
        MultipartFile[] attachments,
        User currentUser
    ) {
        // Verify SOW contract exists and is Retainer type
        SOWContract sowContract = sowContractRepository.findById(sowContractId)
            .orElseThrow(() -> new RuntimeException("SOW Contract not found"));
        
        // Check access permission (Sales Manager sees all, Sales Rep sees only assigned)
        if (!"SALES_MANAGER".equals(currentUser.getRole())) {
            if (sowContract.getAssigneeUserId() == null || !sowContract.getAssigneeUserId().equals(currentUser.getId())) {
                throw new RuntimeException("Access denied: You can only create change requests for contracts assigned to you");
            }
        }
        
        // Validate SOW contract engagement type
        String engagementType = sowContract.getEngagementType();
        if (engagementType == null) {
            throw new RuntimeException("SOW contract engagement type is not set");
        }
        
        // Normalize engagement type
        boolean isRetainer = engagementType.equals("Retainer") || engagementType.equals("Retainer_");
        boolean isFixedPrice = engagementType.equals("Fixed Price") || engagementType.equals("Fixed_Price");
        
        if (!isRetainer && !isFixedPrice) {
            throw new RuntimeException("Invalid engagement type: " + engagementType);
        }
        
        // Validate CR type based on engagement type
        if (request.getType() != null) {
            if (isRetainer) {
                List<String> validTypes = List.of("RESOURCE_CHANGE", "SCHEDULE_CHANGE", "SCOPE_ADJUSTMENT");
                if (!validTypes.contains(request.getType())) {
                    throw new RuntimeException("CR Type '" + request.getType() + "' is not valid for Retainer contract");
                }
            } else if (isFixedPrice) {
                List<String> validTypes = List.of("Add Scope", "Remove Scope", "Other");
                if (!validTypes.contains(request.getType())) {
                    throw new RuntimeException("CR Type '" + request.getType() + "' is not valid for Fixed Price contract");
                }
            }
        }
        
        // Create change request entity
        ChangeRequest changeRequest = new ChangeRequest();
        // For Retainer SOW with versioning:
        // - If contract has no parent_version_id, use contract's id (V1)
        // - If contract has parent_version_id, use parent_version_id to reference V1
        Integer sowContractIdForCR = sowContract.getParentVersionId() != null 
            ? sowContract.getParentVersionId() 
            : sowContract.getId();
        changeRequest.setSowContractId(sowContractIdForCR);
        changeRequest.setContractType("SOW");
        changeRequest.setTitle(request.getTitle());
        changeRequest.setType(request.getType());
        changeRequest.setSummary(request.getSummary());
        changeRequest.setDescription(request.getSummary()); // Use summary as description
        changeRequest.setReason(request.getComment()); // Use comment as reason
        
        // Set effective dates (for both Retainer and Fixed Price)
            if (request.getEffectiveFrom() != null && !request.getEffectiveFrom().trim().isEmpty()) {
            try {
                changeRequest.setEffectiveFrom(LocalDate.parse(request.getEffectiveFrom()));
            } catch (Exception e) {
                throw new RuntimeException("Invalid effectiveFrom date format. Expected: YYYY-MM-DD");
            }
            }
            if (request.getEffectiveUntil() != null && !request.getEffectiveUntil().trim().isEmpty()) {
            try {
                changeRequest.setEffectiveUntil(LocalDate.parse(request.getEffectiveUntil()));
            } catch (Exception e) {
                throw new RuntimeException("Invalid effectiveUntil date format. Expected: YYYY-MM-DD");
            }
        }
        
        // Set references (evidence)
        if (request.getReferences() != null && !request.getReferences().trim().isEmpty()) {
            List<String> evidenceList = new ArrayList<>();
            evidenceList.add(request.getReferences());
            changeRequest.setEvidence(gson.toJson(evidenceList));
        }
        
        // Calculate total amount based on engagement type
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (isRetainer) {
            // For Retainer: calculate from billing adjustments (billingDetails)
            if (request.getBillingDetails() != null && !request.getBillingDetails().isEmpty()) {
                for (CreateChangeRequestRequest.BillingDetailDTO billing : request.getBillingDetails()) {
                    if (billing.getAmount() != null) {
                        totalAmount = totalAmount.add(BigDecimal.valueOf(billing.getAmount()));
                    }
                }
            }
            // cost_estimated_by_landbridge = tổng Billing Adjustments
            changeRequest.setCostEstimatedByLandbridge(totalAmount);
        } else if (isFixedPrice) {
            // For Fixed Price: use additional cost from impact analysis
            if (request.getImpactAnalysis() != null && request.getImpactAnalysis().getAdditionalCost() != null) {
                totalAmount = BigDecimal.valueOf(request.getImpactAnalysis().getAdditionalCost());
            }
        }
        changeRequest.setAmount(totalAmount);
        // ExpectedExtraCost chỉ set khi client tạo CR; Sales Rep không set trường này
        boolean isClientUser = "CLIENT".equalsIgnoreCase(currentUser.getRole()) || "CLIENT_USER".equalsIgnoreCase(currentUser.getRole());
        if (isClientUser) {
        changeRequest.setExpectedExtraCost(totalAmount);
        }
        // For Fixed Price, costEstimatedByLandbridge is already covered by totalAmount above; for Retainer we set explicitly.
        if (!isRetainer) {
        changeRequest.setCostEstimatedByLandbridge(totalAmount);
        }
        
        // Set impact analysis fields for Fixed Price
        if (isFixedPrice && request.getImpactAnalysis() != null) {
            CreateChangeRequestRequest.ImpactAnalysisDTO impact = request.getImpactAnalysis();
            if (impact.getDevHours() != null) {
                changeRequest.setDevHours(impact.getDevHours());
            }
            if (impact.getTestHours() != null) {
                changeRequest.setTestHours(impact.getTestHours());
            }
            if (impact.getNewEndDate() != null && !impact.getNewEndDate().trim().isEmpty()) {
                changeRequest.setNewEndDate(LocalDate.parse(impact.getNewEndDate()));
            }
            if (impact.getDelayDuration() != null) {
                changeRequest.setDelayDuration(impact.getDelayDuration());
            }
        }
        
        // Set status based on action and reviewAction
        // If reviewAction is "APPROVE" and action is "submit", set status to "Client Under Review"
        if ("submit".equalsIgnoreCase(request.getAction()) && "APPROVE".equalsIgnoreCase(request.getReviewAction())) {
            changeRequest.setStatus("Client Under Review");
        } else if ("submit".equalsIgnoreCase(request.getAction())) {
            changeRequest.setStatus("Under Internal Review");
        } else {
            changeRequest.setStatus("Draft");
        }
        
        // Set created by
        changeRequest.setCreatedBy(currentUser.getId());
        
        // Set internal reviewer if provided
        if (request.getInternalReviewerId() != null) {
            changeRequest.setInternalReviewerId(request.getInternalReviewerId());
        }
        
        // Generate change request ID
        changeRequest.setChangeRequestId(generateChangeRequestId());
        
        // Save change request
        changeRequest = changeRequestRepository.save(changeRequest);
        
        // Prepare baseline/current engineers for comparison (skip saving unchanged auto-fill)
        List<SOWEngagedEngineerBase> baselineEngs = sowBaselineService.getBaselineResources(sowContractIdForCR);
        List<SOWEngagedEngineer> legacyEngs = sowEngagedEngineerRepository.findBySowContractId(sowContractIdForCR);
        LocalDate effectiveStart = changeRequest.getEffectiveFrom();

        // Save engaged engineers (skip if identical to baseline/current)
        if (request.getEngagedEngineers() != null && !request.getEngagedEngineers().isEmpty()) {
            for (CreateChangeRequestRequest.EngagedEngineerDTO engineerDTO : request.getEngagedEngineers()) {
                if (isSameAsExistingEng(engineerDTO, baselineEngs, legacyEngs, effectiveStart)) {
                    continue; // skip unchanged auto-fill
                }
                ChangeRequestEngagedEngineer engineer = new ChangeRequestEngagedEngineer();
                engineer.setChangeRequestId(changeRequest.getId());
                engineer.setEngineerLevel(engineerDTO.getEngineerLevel());
                if (engineerDTO.getStartDate() != null && !engineerDTO.getStartDate().trim().isEmpty()) {
                    engineer.setStartDate(LocalDate.parse(engineerDTO.getStartDate()));
                }
                if (engineerDTO.getEndDate() != null && !engineerDTO.getEndDate().trim().isEmpty()) {
                    engineer.setEndDate(LocalDate.parse(engineerDTO.getEndDate()));
                }
                // Set billing type (default to "Monthly" if not provided)
                String billingType = (engineerDTO.getBillingType() != null && !engineerDTO.getBillingType().trim().isEmpty()) 
                    ? engineerDTO.getBillingType() : "Monthly";
                engineer.setBillingType(billingType);
                
                if ("Hourly".equals(billingType)) {
                    if (engineerDTO.getHourlyRate() != null) {
                        engineer.setHourlyRate(BigDecimal.valueOf(engineerDTO.getHourlyRate()));
                    }
                    if (engineerDTO.getHours() != null) {
                        engineer.setHours(BigDecimal.valueOf(engineerDTO.getHours()));
                    }
                    if (engineerDTO.getSubtotal() != null) {
                        engineer.setSubtotal(BigDecimal.valueOf(engineerDTO.getSubtotal()));
                    } else if (engineerDTO.getHourlyRate() != null && engineerDTO.getHours() != null) {
                        engineer.setSubtotal(BigDecimal.valueOf(engineerDTO.getHourlyRate() * engineerDTO.getHours()));
                    }
                    if (engineerDTO.getSubtotal() != null) {
                        engineer.setSalary(BigDecimal.valueOf(engineerDTO.getSubtotal()));
                    } else if (engineerDTO.getSalary() != null) {
                        engineer.setSalary(BigDecimal.valueOf(engineerDTO.getSalary()));
                    }
                    if (engineerDTO.getRating() != null) {
                        engineer.setRating(BigDecimal.valueOf(engineerDTO.getRating()));
                    }
                } else {
                    engineer.setHourlyRate(null);
                    engineer.setHours(null);
                    engineer.setSubtotal(null);
                    if (engineerDTO.getRating() != null) {
                        engineer.setRating(BigDecimal.valueOf(engineerDTO.getRating()));
                    }
                    if (engineerDTO.getSalary() != null) {
                        engineer.setSalary(BigDecimal.valueOf(engineerDTO.getSalary()));
                    }
                }
                changeRequestEngagedEngineerRepository.save(engineer);
            }
        }
        
        // Save billing details
        if (request.getBillingDetails() != null && !request.getBillingDetails().isEmpty()) {
            for (CreateChangeRequestRequest.BillingDetailDTO billingDTO : request.getBillingDetails()) {
                ChangeRequestBillingDetail billing = new ChangeRequestBillingDetail();
                billing.setChangeRequestId(changeRequest.getId());
                if (billingDTO.getPaymentDate() != null && !billingDTO.getPaymentDate().trim().isEmpty()) {
                    billing.setPaymentDate(LocalDate.parse(billingDTO.getPaymentDate()));
                }
                billing.setDeliveryNote(billingDTO.getDeliveryNote());
                if (billingDTO.getAmount() != null) {
                    billing.setAmount(BigDecimal.valueOf(billingDTO.getAmount()));
                }
                changeRequestBillingDetailRepository.save(billing);
            }
        }
        
        // Upload attachments
        if (attachments != null && attachments.length > 0) {
            uploadChangeRequestAttachments(changeRequest.getId(), attachments, currentUser.getId());
        }
        
        // Create history entry
        createChangeRequestHistoryEntry(changeRequest.getId(), 
            "CREATED", 
            "Change request created by " + currentUser.getFullName(), 
            currentUser.getId());
        
        // Build response
        ChangeRequestResponseDTO response = new ChangeRequestResponseDTO();
        response.setId(changeRequest.getId());
        response.setChangeRequestId(changeRequest.getChangeRequestId());
        response.setSuccess(true);
        response.setMessage("Change request created successfully");
        
        return response;
    }
    
    /**
     * Generate change request display ID in format CR-YYYY-NN
     */
    private String generateChangeRequestId() {
        int year = LocalDateTime.now().getYear();
        
        // Count change requests in the same year
        long countInYear = changeRequestRepository.findAll().stream()
            .filter(cr -> cr.getCreatedAt() != null && cr.getCreatedAt().getYear() == year)
            .count();
        
        // Format: CR-YYYY-NN (NN is 2 digits, zero-padded)
        return String.format("CR-%d-%02d", year, countInYear + 1);
    }
    
    /**
     * Upload attachments for change request
     */
    private void uploadChangeRequestAttachments(Integer changeRequestId, MultipartFile[] files, Integer uploadedBy) {
        if (files == null || files.length == 0) {
            return;
        }
        
        List<String> fileLinks = new ArrayList<>();
        
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }
            
            // Validate file type (PDF only)
            String contentType = file.getContentType();
            if (contentType == null || !contentType.equals("application/pdf")) {
                throw new RuntimeException("Only PDF files are allowed. File: " + file.getOriginalFilename());
            }
            
            // Validate file size (10MB limit)
            long maxSize = 10 * 1024 * 1024; // 10MB
            if (file.getSize() > maxSize) {
                throw new RuntimeException("File size exceeds 10MB limit. File: " + file.getOriginalFilename());
            }
            
            try {
                String s3Key = null;
                
                // Upload to S3 if enabled
                if (s3Enabled && s3Service != null) {
                    String fileName = file.getOriginalFilename();
                    String uniqueFileName = "change-requests/" + changeRequestId + "/" + System.currentTimeMillis() + "_" + fileName;
                    s3Key = s3Service.uploadFile(file, uniqueFileName);
                    fileLinks.add(s3Key);
                } else {
                    // Save to local storage
                    String fileName = file.getOriginalFilename();
                    String uniqueFileName = System.currentTimeMillis() + "_" + changeRequestId + "_" + fileName;
                    java.nio.file.Path uploadPath = java.nio.file.Paths.get("uploads/change-requests");
                    if (!java.nio.file.Files.exists(uploadPath)) {
                        java.nio.file.Files.createDirectories(uploadPath);
                    }
                    java.nio.file.Path filePath = uploadPath.resolve(uniqueFileName);
                    file.transferTo(filePath.toFile());
                    s3Key = filePath.toString();
                    fileLinks.add(s3Key);
                }
                
                // Save attachment metadata
                ChangeRequestAttachment attachment = new ChangeRequestAttachment();
                attachment.setChangeRequestId(changeRequestId);
                attachment.setFileName(file.getOriginalFilename());
                attachment.setFilePath(s3Key);
                attachment.setFileSize(file.getSize());
                attachment.setFileType(file.getContentType());
                attachment.setUploadedBy(uploadedBy);
                changeRequestAttachmentRepository.save(attachment);
                
            } catch (IOException e) {
                System.err.println("IOException uploading file: " + file.getOriginalFilename() + " - " + e.getMessage());
                throw new RuntimeException("Failed to upload file: " + file.getOriginalFilename(), e);
            } catch (RuntimeException e) {
                System.err.println("RuntimeException uploading file to S3: " + file.getOriginalFilename() + " - " + e.getMessage());
                throw e;
            }
        }
    }
    
    /**
     * Create history entry for change request
     */
    private void createChangeRequestHistoryEntry(Integer changeRequestId, String action, String description, Integer createdBy) {
        try {
            ChangeRequestHistory history = new ChangeRequestHistory();
            history.setChangeRequestId(changeRequestId);
            history.setAction(action);
            history.setUserId(createdBy);
            if (createdBy != null) {
                Optional<User> user = userRepository.findById(createdBy);
                history.setUserName(user.isPresent() ? user.get().getFullName() : "Unknown");
            } else {
                history.setUserName("Unknown");
            }
            history.setTimestamp(LocalDateTime.now());
            changeRequestHistoryRepository.save(history);
        } catch (Exception e) {
            System.err.println("Failed to create change request history entry: " + e.getMessage());
        }
    }
    
    /**
     * Get change request detail for SOW
     */
    public SalesChangeRequestDetailDTO getChangeRequestDetailForSOW(
        Integer sowContractId,
        Integer changeRequestId,
        User currentUser
    ) {
        // Verify SOW contract exists
        SOWContract sowContract = sowContractRepository.findById(sowContractId)
            .orElseThrow(() -> new RuntimeException("SOW Contract not found"));
        
        // Check access permission
        if (!"SALES_MANAGER".equals(currentUser.getRole())) {
            if (sowContract.getAssigneeUserId() == null || !sowContract.getAssigneeUserId().equals(currentUser.getId())) {
                throw new RuntimeException("Access denied: You can only view change requests for contracts assigned to you");
            }
        }
        
        // Find change request
        ChangeRequest changeRequest = changeRequestRepository.findById(changeRequestId)
            .orElseThrow(() -> new RuntimeException("Change request not found"));
        
        // Validate change request belongs to SOW contract
        if (!changeRequest.getSowContractId().equals(sowContractId) || !"SOW".equals(changeRequest.getContractType())) {
            throw new RuntimeException("Change request does not belong to this SOW contract");
        }
        
        // Get creator name
        String createdBy = "Unknown";
        if (changeRequest.getCreatedBy() != null) {
            Optional<User> creator = userRepository.findById(changeRequest.getCreatedBy());
            if (creator.isPresent()) {
                createdBy = creator.get().getFullName();
            }
        }
        
        // Get reviewer name
        String reviewerName = null;
        Integer reviewerId = changeRequest.getInternalReviewerId();
        if (reviewerId != null) {
            Optional<User> reviewer = userRepository.findById(reviewerId);
            if (reviewer.isPresent()) {
                reviewerName = reviewer.get().getFullName();
            }
        }
        
        // Get engaged engineers, billing details, attachments, history (same as MSA)
        List<ChangeRequestEngagedEngineer> engineers = changeRequestEngagedEngineerRepository.findByChangeRequestId(changeRequestId);
        List<EngagedEngineerDTO> engineerDTOs = engineers.stream()
            .map(e -> {
                EngagedEngineerDTO dto = new EngagedEngineerDTO();
                dto.setId(e.getId());
                dto.setEngineerLevel(e.getEngineerLevel());
                dto.setStartDate(e.getStartDate() != null ? e.getStartDate().toString() : null);
                dto.setEndDate(e.getEndDate() != null ? e.getEndDate().toString() : null);
                dto.setBillingType(e.getBillingType() != null ? e.getBillingType() : "Monthly");
                dto.setHourlyRate(e.getHourlyRate() != null ? e.getHourlyRate().doubleValue() : null);
                dto.setHours(e.getHours() != null ? e.getHours().doubleValue() : null);
                dto.setSubtotal(e.getSubtotal() != null ? e.getSubtotal().doubleValue() : null);
                dto.setRating(e.getRating() != null ? e.getRating().doubleValue() : null);
                dto.setSalary(e.getSalary() != null ? e.getSalary().doubleValue() : null);
                return dto;
            })
            .collect(java.util.stream.Collectors.toList());
        
        List<ChangeRequestBillingDetail> billingDetails = changeRequestBillingDetailRepository.findByChangeRequestId(changeRequestId);
        List<BillingDetailDTO> billingDTOs = billingDetails.stream()
            .map(b -> {
                BillingDetailDTO dto = new BillingDetailDTO();
                dto.setId(b.getId());
                dto.setPaymentDate(b.getPaymentDate() != null ? b.getPaymentDate().toString() : null);
                dto.setDeliveryNote(b.getDeliveryNote());
                dto.setAmount(b.getAmount() != null ? b.getAmount().doubleValue() : null);
                return dto;
            })
            .collect(java.util.stream.Collectors.toList());
        
        List<ChangeRequestAttachment> attachments = changeRequestAttachmentRepository.findByChangeRequestId(changeRequestId);
        List<SalesChangeRequestDetailDTO.AttachmentDTO> attachmentDTOs = attachments.stream()
            .map(a -> {
                SalesChangeRequestDetailDTO.AttachmentDTO dto = new SalesChangeRequestDetailDTO.AttachmentDTO();
                dto.setId(a.getId());
                dto.setFileName(a.getFileName());
                dto.setFilePath(a.getFilePath());
                dto.setFileSize(a.getFileSize());
                return dto;
            })
            .collect(java.util.stream.Collectors.toList());
        
        List<ChangeRequestHistory> history = changeRequestHistoryRepository.findByChangeRequestIdOrderByTimestampDesc(changeRequestId);
        List<HistoryItemDTO> historyDTOs = history.stream()
            .map(h -> {
                HistoryItemDTO dto = new HistoryItemDTO();
                dto.setId(h.getId());
                dto.setDate(h.getTimestamp() != null ? h.getTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "");
                String description = h.getAction();
                if (h.getUserName() != null) {
                    description += " by " + h.getUserName();
                }
                dto.setDescription(description);
                dto.setUser(h.getUserName() != null ? h.getUserName() : "Unknown");
                dto.setDocumentLink(null);
                return dto;
            })
            .collect(java.util.stream.Collectors.toList());
        
        // Parse references
        String references = null;
        if (changeRequest.getEvidence() != null && !changeRequest.getEvidence().trim().isEmpty()) {
            try {
                List<String> evidenceList = gson.fromJson(changeRequest.getEvidence(), new TypeToken<List<String>>(){}.getType());
                if (evidenceList != null && !evidenceList.isEmpty()) {
                    references = String.join(", ", evidenceList);
                }
            } catch (Exception e) {
                references = changeRequest.getEvidence();
            }
        }
        
        // Build DTO
        SalesChangeRequestDetailDTO dto = new SalesChangeRequestDetailDTO();
        dto.setId(changeRequest.getId());
        dto.setChangeRequestId(changeRequest.getChangeRequestId());
        dto.setTitle(changeRequest.getTitle());
        dto.setType(changeRequest.getType());
        dto.setSummary(changeRequest.getSummary());
        dto.setEffectiveFrom(changeRequest.getEffectiveFrom() != null ? changeRequest.getEffectiveFrom().toString() : null);
        dto.setEffectiveUntil(changeRequest.getEffectiveUntil() != null ? changeRequest.getEffectiveUntil().toString() : null);
        dto.setDesiredStartDate(changeRequest.getDesiredStartDate() != null ? changeRequest.getDesiredStartDate().toString() : null);
        dto.setDesiredEndDate(changeRequest.getDesiredEndDate() != null ? changeRequest.getDesiredEndDate().toString() : null);
        dto.setExpectedExtraCost(changeRequest.getExpectedExtraCost() != null ? changeRequest.getExpectedExtraCost().doubleValue() : null);
        dto.setReferences(references);
        dto.setStatus(changeRequest.getStatus());
        dto.setCreatedBy(createdBy);
        dto.setCreatedById(changeRequest.getCreatedBy());
        dto.setCreatedDate(changeRequest.getCreatedAt() != null ? changeRequest.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "");
        dto.setEngagedEngineers(engineerDTOs);
        dto.setBillingDetails(billingDTOs);
        dto.setAttachments(attachmentDTOs);
        dto.setHistory(historyDTOs);
        dto.setInternalReviewerId(reviewerId);
        dto.setInternalReviewerName(reviewerName);
        dto.setComment(changeRequest.getReason());
        
        // Set impact analysis fields (for Fixed Price SOW)
        dto.setDevHours(changeRequest.getDevHours());
        dto.setTestHours(changeRequest.getTestHours());
        dto.setNewEndDate(changeRequest.getNewEndDate() != null ? changeRequest.getNewEndDate().toString() : null);
        dto.setDelayDuration(changeRequest.getDelayDuration());
        dto.setAdditionalCost(changeRequest.getCostEstimatedByLandbridge() != null ? changeRequest.getCostEstimatedByLandbridge().doubleValue() : null);
        
        return dto;
    }
    
    /**
     * Update change request for SOW (Draft only) - Similar to MSA
     */
    @Transactional
    public void updateChangeRequestForSOW(
        Integer sowContractId,
        Integer changeRequestId,
        CreateChangeRequestRequest request,
        MultipartFile[] attachments,
        User currentUser
    ) {
        // Similar implementation to MSA but for SOW
        SOWContract sowContract = sowContractRepository.findById(sowContractId)
            .orElseThrow(() -> new RuntimeException("SOW Contract not found"));
        
        ChangeRequest changeRequest = changeRequestRepository.findById(changeRequestId)
            .orElseThrow(() -> new RuntimeException("Change request not found"));
        
        if (!changeRequest.getSowContractId().equals(sowContractId) || !"SOW".equals(changeRequest.getContractType())) {
            throw new RuntimeException("Change request does not belong to this SOW contract");
        }
        
        // Allow update for Draft or Processing status
        if (!"Draft".equals(changeRequest.getStatus()) && !"Processing".equals(changeRequest.getStatus())) {
            throw new RuntimeException("Only Draft or Processing change requests can be updated");
        }
        
        // For Draft status, only creator can update. For Processing status, any Sales user can update (to add impact analysis)
        if ("Draft".equals(changeRequest.getStatus())) {
            if (changeRequest.getCreatedBy() == null || !changeRequest.getCreatedBy().equals(currentUser.getId())) {
                throw new RuntimeException("Only the creator can update Draft change requests");
            }
        }
        
        // Get SOW contract to determine engagement type
        String engagementType = sowContract.getEngagementType();
        boolean isRetainer = engagementType != null && (engagementType.equals("Retainer") || engagementType.equals("Retainer_"));
        boolean isFixedPrice = engagementType != null && (engagementType.equals("Fixed Price") || engagementType.equals("Fixed_Price"));
        
        // Update fields (same as MSA)
        if (request.getTitle() != null) changeRequest.setTitle(request.getTitle());
        if (request.getType() != null) changeRequest.setType(request.getType());
        if (request.getSummary() != null) {
            changeRequest.setSummary(request.getSummary());
            changeRequest.setDescription(request.getSummary());
        }
        // Update effectiveFrom and effectiveUntil for both Retainer and Fixed Price when status is Processing
            if (request.getEffectiveFrom() != null && !request.getEffectiveFrom().trim().isEmpty()) {
                changeRequest.setEffectiveFrom(LocalDate.parse(request.getEffectiveFrom()));
            }
            if (request.getEffectiveUntil() != null && !request.getEffectiveUntil().trim().isEmpty()) {
                changeRequest.setEffectiveUntil(LocalDate.parse(request.getEffectiveUntil()));
        }
        if (request.getReferences() != null) {
            List<String> evidenceList = new ArrayList<>();
            evidenceList.add(request.getReferences());
            changeRequest.setEvidence(gson.toJson(evidenceList));
        }
        if (request.getComment() != null) {
            changeRequest.setReason(request.getComment());
        }
        
        // Update internal reviewer if provided
        if (request.getInternalReviewerId() != null) {
            changeRequest.setInternalReviewerId(request.getInternalReviewerId());
        }
        
        // Calculate total amount based on engagement type
        boolean isClientUser = "CLIENT".equalsIgnoreCase(currentUser.getRole()) || "CLIENT_USER".equalsIgnoreCase(currentUser.getRole());
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (isRetainer) {
            if (request.getBillingDetails() != null && !request.getBillingDetails().isEmpty()) {
                for (CreateChangeRequestRequest.BillingDetailDTO billing : request.getBillingDetails()) {
                    if (billing.getAmount() != null) {
                        totalAmount = totalAmount.add(BigDecimal.valueOf(billing.getAmount()));
                    }
                }
            }
            changeRequest.setCostEstimatedByLandbridge(totalAmount); // Billing Adjustments total
        } else if (isFixedPrice) {
            if (request.getImpactAnalysis() != null && request.getImpactAnalysis().getAdditionalCost() != null) {
                totalAmount = BigDecimal.valueOf(request.getImpactAnalysis().getAdditionalCost());
            }
        }
        changeRequest.setAmount(totalAmount);
        // ExpectedExtraCost chỉ set khi client tạo CR; Sales Rep không cập nhật trường này
        if (isClientUser) {
        changeRequest.setExpectedExtraCost(totalAmount);
        }
        if (!isRetainer) {
        changeRequest.setCostEstimatedByLandbridge(totalAmount);
        }
        
        // Update impact analysis fields for Fixed Price
        if (isFixedPrice && request.getImpactAnalysis() != null) {
            CreateChangeRequestRequest.ImpactAnalysisDTO impact = request.getImpactAnalysis();
            if (impact.getDevHours() != null) {
                changeRequest.setDevHours(impact.getDevHours());
            }
            if (impact.getTestHours() != null) {
                changeRequest.setTestHours(impact.getTestHours());
            }
            if (impact.getNewEndDate() != null && !impact.getNewEndDate().trim().isEmpty()) {
                changeRequest.setNewEndDate(LocalDate.parse(impact.getNewEndDate()));
            }
            if (impact.getDelayDuration() != null) {
                changeRequest.setDelayDuration(impact.getDelayDuration());
            }
        }
        
        changeRequestRepository.save(changeRequest);
        
        // Update engineers and billing (only for Retainer)
        changeRequestEngagedEngineerRepository.deleteByChangeRequestId(changeRequestId);
        if (isRetainer && request.getEngagedEngineers() != null && !request.getEngagedEngineers().isEmpty()) {
            // Prepare baseline/current for comparison
            List<SOWEngagedEngineerBase> baselineEngs = sowBaselineService.getBaselineResources(sowContractId);
            List<SOWEngagedEngineer> legacyEngs = sowEngagedEngineerRepository.findBySowContractId(sowContractId);
            LocalDate effectiveStart = changeRequest.getEffectiveFrom();

            for (CreateChangeRequestRequest.EngagedEngineerDTO engineerDTO : request.getEngagedEngineers()) {
                if (isSameAsExistingEng(engineerDTO, baselineEngs, legacyEngs, effectiveStart)) {
                    continue; // skip unchanged auto-fill
                }
                ChangeRequestEngagedEngineer engineer = new ChangeRequestEngagedEngineer();
                engineer.setChangeRequestId(changeRequestId);
                engineer.setEngineerLevel(engineerDTO.getEngineerLevel());
                if (engineerDTO.getStartDate() != null && !engineerDTO.getStartDate().trim().isEmpty()) {
                    engineer.setStartDate(LocalDate.parse(engineerDTO.getStartDate()));
                }
                if (engineerDTO.getEndDate() != null && !engineerDTO.getEndDate().trim().isEmpty()) {
                    engineer.setEndDate(LocalDate.parse(engineerDTO.getEndDate()));
                }
                // Set billing type (default to "Monthly" if not provided)
                String billingType = (engineerDTO.getBillingType() != null && !engineerDTO.getBillingType().trim().isEmpty()) 
                    ? engineerDTO.getBillingType() : "Monthly";
                engineer.setBillingType(billingType);
                
                if ("Hourly".equals(billingType)) {
                    if (engineerDTO.getHourlyRate() != null) {
                        engineer.setHourlyRate(BigDecimal.valueOf(engineerDTO.getHourlyRate()));
                    }
                    if (engineerDTO.getHours() != null) {
                        engineer.setHours(BigDecimal.valueOf(engineerDTO.getHours()));
                    }
                    if (engineerDTO.getSubtotal() != null) {
                        engineer.setSubtotal(BigDecimal.valueOf(engineerDTO.getSubtotal()));
                    } else if (engineerDTO.getHourlyRate() != null && engineerDTO.getHours() != null) {
                        engineer.setSubtotal(BigDecimal.valueOf(engineerDTO.getHourlyRate() * engineerDTO.getHours()));
                    }
                    if (engineerDTO.getSubtotal() != null) {
                        engineer.setSalary(BigDecimal.valueOf(engineerDTO.getSubtotal()));
                    } else if (engineerDTO.getSalary() != null) {
                        engineer.setSalary(BigDecimal.valueOf(engineerDTO.getSalary()));
                    }
                    if (engineerDTO.getRating() != null) {
                        engineer.setRating(BigDecimal.valueOf(engineerDTO.getRating()));
                    }
                } else {
                    engineer.setHourlyRate(null);
                    engineer.setHours(null);
                    engineer.setSubtotal(null);
                    if (engineerDTO.getRating() != null) {
                        engineer.setRating(BigDecimal.valueOf(engineerDTO.getRating()));
                    }
                    if (engineerDTO.getSalary() != null) {
                        engineer.setSalary(BigDecimal.valueOf(engineerDTO.getSalary()));
                    }
                }
                changeRequestEngagedEngineerRepository.save(engineer);
            }
        }
        
        changeRequestBillingDetailRepository.deleteByChangeRequestId(changeRequestId);
        if (isRetainer && request.getBillingDetails() != null && !request.getBillingDetails().isEmpty()) {
            for (CreateChangeRequestRequest.BillingDetailDTO billingDTO : request.getBillingDetails()) {
                ChangeRequestBillingDetail billing = new ChangeRequestBillingDetail();
                billing.setChangeRequestId(changeRequestId);
                if (billingDTO.getPaymentDate() != null && !billingDTO.getPaymentDate().trim().isEmpty()) {
                    billing.setPaymentDate(LocalDate.parse(billingDTO.getPaymentDate()));
                }
                billing.setDeliveryNote(billingDTO.getDeliveryNote());
                if (billingDTO.getAmount() != null) {
                    billing.setAmount(BigDecimal.valueOf(billingDTO.getAmount()));
                }
                changeRequestBillingDetailRepository.save(billing);
            }
        }
        
        if (attachments != null && attachments.length > 0) {
            uploadChangeRequestAttachments(changeRequestId, attachments, currentUser.getId());
        }
        
        createChangeRequestHistoryEntry(changeRequestId, "UPDATED", 
            "Change request updated by " + currentUser.getFullName(), currentUser.getId());
    }
    
    /**
     * Submit change request for SOW
     */
    @Transactional
    public void submitChangeRequestForSOW(
        Integer sowContractId,
        Integer changeRequestId,
        Integer internalReviewerId,
        User currentUser
    ) {
        SOWContract sowContract = sowContractRepository.findById(sowContractId)
            .orElseThrow(() -> new RuntimeException("SOW Contract not found"));
        
        ChangeRequest changeRequest = changeRequestRepository.findById(changeRequestId)
            .orElseThrow(() -> new RuntimeException("Change request not found"));
        
        if (!changeRequest.getSowContractId().equals(sowContractId) || !"SOW".equals(changeRequest.getContractType())) {
            throw new RuntimeException("Change request does not belong to this SOW contract");
        }
        
        if (!"Draft".equals(changeRequest.getStatus()) && !"Processing".equals(changeRequest.getStatus())) {
            throw new RuntimeException("Only Draft or Processing change requests can be submitted");
        }
        
        // Allow submission if:
        // 1. User is the creator, OR
        // 2. User is a Sales Rep and is the assignee of the SOW contract
        boolean isCreator = changeRequest.getCreatedBy() != null && changeRequest.getCreatedBy().equals(currentUser.getId());
        boolean isAssignee = "SALES_REP".equals(currentUser.getRole()) && 
                            sowContract.getAssigneeUserId() != null && 
                            sowContract.getAssigneeUserId().equals(currentUser.getId());
        
        if (!isCreator && !isAssignee) {
            throw new RuntimeException("Only the creator or assigned Sales Rep can submit this change request");
        }
        
        if (internalReviewerId == null) {
            throw new RuntimeException("Internal reviewer is required");
        }
        
        // Set internal reviewer
        changeRequest.setInternalReviewerId(internalReviewerId);
        
        changeRequest.setStatus("Under Internal Review");
        changeRequestRepository.save(changeRequest);
        
        createChangeRequestHistoryEntry(changeRequestId, "SUBMITTED", 
            "Change request submitted for internal review by " + currentUser.getFullName(), currentUser.getId());
    }
    
    /**
     * Submit review for SOW change request
     */
    @Transactional
    public void submitChangeRequestReviewForSOW(
        Integer sowContractId,
        Integer changeRequestId,
        String reviewAction,
        String reviewNotes,
        User currentUser
    ) {
        SOWContract sowContract = sowContractRepository.findById(sowContractId)
            .orElseThrow(() -> new RuntimeException("SOW Contract not found"));
        
        ChangeRequest changeRequest = changeRequestRepository.findById(changeRequestId)
            .orElseThrow(() -> new RuntimeException("Change request not found"));
        
        if (!changeRequest.getSowContractId().equals(sowContractId) || !"SOW".equals(changeRequest.getContractType())) {
            throw new RuntimeException("Change request does not belong to this SOW contract");
        }
        
        // Allow review for "Under Internal Review" or "Processing" status
        if (!"Under Internal Review".equals(changeRequest.getStatus()) && !"Processing".equals(changeRequest.getStatus())) {
            throw new RuntimeException("Change request is not under internal review or processing");
        }
        
        if (!"APPROVE".equals(reviewAction) && !"REQUEST_REVISION".equals(reviewAction)) {
            throw new RuntimeException("Invalid review action. Must be APPROVE or REQUEST_REVISION");
        }
        
        if (!"SALES_MANAGER".equals(currentUser.getRole())) {
            throw new RuntimeException("Only Sales Managers can review change requests");
        }
        
        if ("APPROVE".equals(reviewAction)) {
            changeRequest.setStatus("Client Under Review");
        } else if ("REQUEST_REVISION".equals(reviewAction)) {
            changeRequest.setStatus("Draft");
        }
        
        changeRequestRepository.save(changeRequest);
        
        String actionDescription = "APPROVE".equals(reviewAction) ? "approved" : "requested revision for";
        createChangeRequestHistoryEntry(changeRequestId, "REVIEWED", 
            "Change request " + actionDescription + " by " + currentUser.getFullName() + 
            (reviewNotes != null && !reviewNotes.trim().isEmpty() ? ". Notes: " + reviewNotes : ""), 
            currentUser.getId());
    }
    
    /**
     * Approve change request and apply changes to contract (for Retainer SOW) - EVENT-BASED
     * This applies changes based on CR type: RESOURCE_CHANGE, SCHEDULE_CHANGE, SCOPE_ADJUSTMENT
     * Creates events instead of new contract version
     */
    @Transactional
    public void approveChangeRequestForSOW(
        Integer sowContractId,
        Integer changeRequestId,
        String reviewNotes,
        User currentUser
    ) {
        SOWContract sowContract = sowContractRepository.findById(sowContractId)
            .orElseThrow(() -> new RuntimeException("SOW Contract not found"));
        
        ChangeRequest changeRequest = changeRequestRepository.findById(changeRequestId)
            .orElseThrow(() -> new RuntimeException("Change request not found"));
        
        if (!changeRequest.getSowContractId().equals(sowContractId) || !"SOW".equals(changeRequest.getContractType())) {
            throw new RuntimeException("Change request does not belong to this SOW contract");
        }
        
        // Verify engagement type is Retainer
        String engagementType = sowContract.getEngagementType();
        if (engagementType == null || (!engagementType.equals("Retainer") && !engagementType.equals("Retainer_"))) {
            throw new RuntimeException("This operation is only for Retainer SOW contracts");
        }
        
        // Allow approve for "Processing" status (CRs created by client and reviewed by Sales)
        // Also allow "Under Review" and "Client Under Review" (for client approval flow)
        if (!"Processing".equals(changeRequest.getStatus()) 
            && !"Under Review".equals(changeRequest.getStatus())
            && !"Client Under Review".equals(changeRequest.getStatus())) {
            throw new RuntimeException("Only Processing, Under Review, or Client Under Review change requests can be approved");
        }
        
        LocalDate effectiveStart = changeRequest.getEffectiveFrom();
        if (effectiveStart == null) {
            throw new RuntimeException("Effective start date is required");
        }
        
        // Ensure baseline exists (create if not exists)
        sowBaselineService.createBaseline(sowContractId);
        
        String crType = changeRequest.getType();
        
        // Create events based on CR type (EVENT-BASED APPROACH)
        if ("RESOURCE_CHANGE".equals(crType)) {
            applyResourceChangeEventBased(sowContractId, changeRequest, effectiveStart, currentUser);
        } else if ("SCHEDULE_CHANGE".equals(crType)) {
            applyScheduleChangeEventBased(sowContract, changeRequest, effectiveStart, currentUser);
        } else if ("SCOPE_ADJUSTMENT".equals(crType)) {
            applyScopeAdjustmentEventBased(sowContractId, changeRequest, effectiveStart, currentUser);
        } else if ("RATE_ADJUSTMENT".equals(crType)) {
            applyRateAdjustmentEventBased(sowContractId, changeRequest, effectiveStart, currentUser);
        } else {
            throw new RuntimeException("Unsupported CR type: " + crType);
        }
        
        // Generate appendix
        ContractAppendix appendix = contractAppendixService.generateAppendix(changeRequest);
        
        // Update CR status to APPROVED
        changeRequest.setStatus("Approved");
        changeRequest.setApprovedBy(currentUser.getId());
        changeRequest.setApprovedAt(LocalDateTime.now());
        if (reviewNotes != null && !reviewNotes.trim().isEmpty()) {
            changeRequest.setSalesInternalNote(reviewNotes);
        }
        changeRequestRepository.save(changeRequest);
        
        // Create history entry
        createChangeRequestHistoryEntry(changeRequestId, "APPROVED",
            "Change request approved by " + currentUser.getFullName() + 
            (reviewNotes != null && !reviewNotes.trim().isEmpty() ? ". Notes: " + reviewNotes : "") +
            ". Appendix " + appendix.getAppendixNumber() + " created.", 
            currentUser.getId());
    }
    
    /**
     * Clone SOW contract to create a new version
     */
    private SOWContract cloneSOWContractForNewVersion(SOWContract original) {
        // Get the latest version number for this contract family
        Integer maxVersion = sowContractRepository.findMaxVersionByParentVersionId(original.getParentVersionId() != null ? 
            original.getParentVersionId() : original.getId());
        Integer newVersionNumber = maxVersion != null ? maxVersion + 1 : original.getVersion() + 1;
        
        // Create new version
        SOWContract newVersion = new SOWContract();
        newVersion.setClientId(original.getClientId());
        newVersion.setContractName(original.getContractName());
        newVersion.setStatus(original.getStatus());
        newVersion.setEngagementType(original.getEngagementType());
        newVersion.setParentMsaId(original.getParentMsaId());
        newVersion.setProjectName(original.getProjectName());
        newVersion.setScopeSummary(original.getScopeSummary());
        newVersion.setPeriodStart(original.getPeriodStart());
        newVersion.setPeriodEnd(original.getPeriodEnd());
        newVersion.setValue(original.getValue());
        newVersion.setAssigneeId(original.getAssigneeId());
        newVersion.setAssigneeUserId(original.getAssigneeUserId());
        newVersion.setReviewerId(original.getReviewerId());
        newVersion.setCurrency(original.getCurrency());
        newVersion.setPaymentTerms(original.getPaymentTerms());
        newVersion.setInvoicingCycle(original.getInvoicingCycle());
        newVersion.setBillingDay(original.getBillingDay());
        newVersion.setTaxWithholding(original.getTaxWithholding());
        newVersion.setIpOwnership(original.getIpOwnership());
        newVersion.setGoverningLaw(original.getGoverningLaw());
        newVersion.setLandbridgeContactName(original.getLandbridgeContactName());
        newVersion.setLandbridgeContactEmail(original.getLandbridgeContactEmail());
        newVersion.setLink(original.getLink());
        newVersion.setAttachmentsManifest(original.getAttachmentsManifest());
        newVersion.setVersion(newVersionNumber);
        newVersion.setParentVersionId(original.getParentVersionId() != null ? original.getParentVersionId() : original.getId());
        
        newVersion = sowContractRepository.save(newVersion);
        
        // Note: Engaged engineers and billing details will be populated from the approved CR
        // They are NOT cloned from the original version
        
        return newVersion;
    }
    
    /**
     * Reject change request without applying changes (for Retainer SOW)
     */
    @Transactional
    public void rejectChangeRequestForSOW(
        Integer sowContractId,
        Integer changeRequestId,
        String reason,
        User currentUser
    ) {
        SOWContract sowContract = sowContractRepository.findById(sowContractId)
            .orElseThrow(() -> new RuntimeException("SOW Contract not found"));
        
        ChangeRequest changeRequest = changeRequestRepository.findById(changeRequestId)
            .orElseThrow(() -> new RuntimeException("Change request not found"));
        
        if (!changeRequest.getSowContractId().equals(sowContractId) || !"SOW".equals(changeRequest.getContractType())) {
            throw new RuntimeException("Change request does not belong to this SOW contract");
        }
        
        // Allow reject for "Processing" status
        if (!"Processing".equals(changeRequest.getStatus())) {
            throw new RuntimeException("Only Processing change requests can be rejected");
        }
        
        // Update CR status to REJECTED
        changeRequest.setStatus("Rejected");
        if (reason != null && !reason.trim().isEmpty()) {
            changeRequest.setReason((changeRequest.getReason() != null ? changeRequest.getReason() + "\n\n" : "") + 
                "Rejection reason: " + reason);
        }
        changeRequestRepository.save(changeRequest);
        
        // Create history entry
        createChangeRequestHistoryEntry(changeRequestId, "REJECTED", 
            "Change request rejected by " + currentUser.getFullName() + 
            (reason != null && !reason.trim().isEmpty() ? ". Reason: " + reason : ""), 
            currentUser.getId());
    }
    
    /**
     * Get preview of changes (Before/After comparison) for change request
     * Returns current state vs proposed state
     */
    public java.util.Map<String, Object> getChangeRequestPreviewForSOW(
        Integer sowContractId,
        Integer changeRequestId,
        User currentUser
    ) {
        SOWContract sowContract = sowContractRepository.findById(sowContractId)
            .orElseThrow(() -> new RuntimeException("SOW Contract not found"));
        
        ChangeRequest changeRequest = changeRequestRepository.findById(changeRequestId)
            .orElseThrow(() -> new RuntimeException("Change request not found"));
        
        if (!changeRequest.getSowContractId().equals(sowContractId) || !"SOW".equals(changeRequest.getContractType())) {
            throw new RuntimeException("Change request does not belong to this SOW contract");
        }
        
        LocalDate effectiveStart = changeRequest.getEffectiveFrom();
        if (effectiveStart == null) {
            throw new RuntimeException("Effective start date is required");
        }
        
        // Get current state (Before)
        List<SOWEngagedEngineer> currentEngineers = sowEngagedEngineerRepository.findBySowContractId(sowContractId);
        List<RetainerBillingDetail> currentBilling = retainerBillingDetailRepository.findBySowContractIdOrderByPaymentDateDesc(sowContractId);
        
        // Get proposed changes from CR
        List<ChangeRequestEngagedEngineer> crEngineers = changeRequestEngagedEngineerRepository.findByChangeRequestId(changeRequestId);
        List<ChangeRequestBillingDetail> crBilling = changeRequestBillingDetailRepository.findByChangeRequestId(changeRequestId);
        
        // Build preview response
        java.util.Map<String, Object> preview = new java.util.HashMap<>();
        
        // Resources Before/After
        java.util.List<java.util.Map<String, Object>> resourcesBefore = new ArrayList<>();
        for (SOWEngagedEngineer eng : currentEngineers) {
            if (eng.getEndDate() == null || !eng.getEndDate().isBefore(effectiveStart)) {
                java.util.Map<String, Object> engMap = new java.util.HashMap<>();
                engMap.put("id", eng.getId());
                engMap.put("engineerLevel", eng.getEngineerLevel());
                engMap.put("startDate", eng.getStartDate() != null ? eng.getStartDate().toString() : null);
                engMap.put("endDate", eng.getEndDate() != null ? eng.getEndDate().toString() : null);
                engMap.put("rating", eng.getRating() != null ? eng.getRating().doubleValue() : null);
                engMap.put("salary", eng.getSalary() != null ? eng.getSalary().doubleValue() : null);
                resourcesBefore.add(engMap);
            }
        }
        
        java.util.List<java.util.Map<String, Object>> resourcesAfter = new ArrayList<>();
        // Apply proposed changes to build "After" state
        // This is a simplified version - in production, you'd need more sophisticated logic
        resourcesAfter.addAll(resourcesBefore);
        for (ChangeRequestEngagedEngineer crEng : crEngineers) {
            java.util.Map<String, Object> engMap = new java.util.HashMap<>();
            engMap.put("id", crEng.getId());
            engMap.put("engineerLevel", crEng.getEngineerLevel());
            engMap.put("startDate", crEng.getStartDate() != null ? crEng.getStartDate().toString() : null);
            engMap.put("endDate", crEng.getEndDate() != null ? crEng.getEndDate().toString() : null);
            engMap.put("rating", crEng.getRating() != null ? crEng.getRating().doubleValue() : null);
            engMap.put("salary", crEng.getSalary() != null ? crEng.getSalary().doubleValue() : null);
            resourcesAfter.add(engMap);
        }
        
        // Billing Before/After
        java.util.List<java.util.Map<String, Object>> billingBefore = new ArrayList<>();
        for (RetainerBillingDetail billing : currentBilling) {
            if (billing.getPaymentDate() == null || !billing.getPaymentDate().isBefore(effectiveStart)) {
                java.util.Map<String, Object> billingMap = new java.util.HashMap<>();
                billingMap.put("id", billing.getId());
                billingMap.put("paymentDate", billing.getPaymentDate() != null ? billing.getPaymentDate().toString() : null);
                billingMap.put("deliveryNote", billing.getDeliveryNote());
                billingMap.put("amount", billing.getAmount() != null ? billing.getAmount().doubleValue() : null);
                billingBefore.add(billingMap);
            }
        }
        
        java.util.List<java.util.Map<String, Object>> billingAfter = new ArrayList<>();
        billingAfter.addAll(billingBefore);
        for (ChangeRequestBillingDetail crBillingDetail : crBilling) {
            java.util.Map<String, Object> billingMap = new java.util.HashMap<>();
            billingMap.put("id", crBillingDetail.getId());
            billingMap.put("paymentDate", crBillingDetail.getPaymentDate() != null ? crBillingDetail.getPaymentDate().toString() : null);
            billingMap.put("deliveryNote", crBillingDetail.getDeliveryNote());
            billingMap.put("amount", crBillingDetail.getAmount() != null ? crBillingDetail.getAmount().doubleValue() : null);
            billingAfter.add(billingMap);
        }
        
        preview.put("resources", java.util.Map.of(
            "before", resourcesBefore,
            "after", resourcesAfter
        ));
        preview.put("billing", java.util.Map.of(
            "before", billingBefore,
            "after", billingAfter
        ));
        
        return preview;
    }
    
    /**
     * Apply RESOURCE_CHANGE: Update resources and billing from effective_start
     */
    private void applyResourceChange(Integer sowContractId, Integer changeRequestId, LocalDate effectiveStart, User currentUser) {
        // Get proposed changes from CR
        List<ChangeRequestEngagedEngineer> crEngineers = changeRequestEngagedEngineerRepository.findByChangeRequestId(changeRequestId);
        List<ChangeRequestBillingDetail> crBilling = changeRequestBillingDetailRepository.findByChangeRequestId(changeRequestId);
        
        // Apply engineers from CR to new version
        // Start Date of engineer = Start date from CR (not Effective from)
        for (ChangeRequestEngagedEngineer crEng : crEngineers) {
            SOWEngagedEngineer newEng = new SOWEngagedEngineer();
            newEng.setSowContractId(sowContractId);
            newEng.setEngineerLevel(crEng.getEngineerLevel());
            // Start Date = Start date from CR engineer (user input)
            newEng.setStartDate(crEng.getStartDate() != null ? crEng.getStartDate() : effectiveStart);
            newEng.setEndDate(crEng.getEndDate());
            newEng.setRating(crEng.getRating());
            newEng.setSalary(crEng.getSalary());
            sowEngagedEngineerRepository.save(newEng);
        }
        
        // Apply billing details from CR to new version
        for (ChangeRequestBillingDetail crBillingDetail : crBilling) {
            RetainerBillingDetail newBilling = new RetainerBillingDetail();
            newBilling.setSowContractId(sowContractId);
            newBilling.setPaymentDate(crBillingDetail.getPaymentDate());
            newBilling.setDeliveryNote(crBillingDetail.getDeliveryNote());
            newBilling.setAmount(crBillingDetail.getAmount());
            // Note: ChangeRequestBillingDetail doesn't have deliveryItemId field
            // deliveryItemId will remain null for billing details created from CR
            newBilling.setChangeRequestId(changeRequestId);
            retainerBillingDetailRepository.save(newBilling);
        }
    }
    
    /**
     * Apply SCHEDULE_CHANGE: Update contract end date and billing
     */
    private void applyScheduleChange(SOWContract sowContract, Integer changeRequestId, LocalDate effectiveStart, User currentUser) {
        ChangeRequest changeRequest = changeRequestRepository.findById(changeRequestId)
            .orElseThrow(() -> new RuntimeException("Change request not found"));
        
        LocalDate newEndDate = changeRequest.getEffectiveUntil();
        if (newEndDate == null) {
            throw new RuntimeException("New end date is required for SCHEDULE_CHANGE");
        }
        
        // Update contract end date
        sowContract.setPeriodEnd(newEndDate);
        sowContractRepository.save(sowContract);
        
        // Update engineer end dates if they extend beyond new end date
        List<SOWEngagedEngineer> engineers = sowEngagedEngineerRepository.findBySowContractId(sowContract.getId());
        for (SOWEngagedEngineer eng : engineers) {
            if (eng.getEndDate() != null && eng.getEndDate().isAfter(newEndDate)) {
                eng.setEndDate(newEndDate);
                sowEngagedEngineerRepository.save(eng);
            }
        }
        
        // Cancel billing entries after new end date
        List<RetainerBillingDetail> billingDetails = retainerBillingDetailRepository.findBySowContractIdOrderByPaymentDateDesc(sowContract.getId());
        for (RetainerBillingDetail billing : billingDetails) {
            if (billing.getPaymentDate() != null && billing.getPaymentDate().isAfter(newEndDate)) {
                // Mark as cancelled (you may need to add a cancelled field or status field)
                // For now, we'll just update the change_request_id to track it
                billing.setChangeRequestId(changeRequestId);
                retainerBillingDetailRepository.save(billing);
            }
        }
        
        // Apply billing changes from CR
        List<ChangeRequestBillingDetail> crBilling = changeRequestBillingDetailRepository.findByChangeRequestId(changeRequestId);
        for (ChangeRequestBillingDetail crBillingDetail : crBilling) {
            if (crBillingDetail.getPaymentDate() != null && 
                !crBillingDetail.getPaymentDate().isBefore(effectiveStart) &&
                !crBillingDetail.getPaymentDate().isAfter(newEndDate)) {
                if (crBillingDetail.getId() == null) {
                    // Add new billing
                    RetainerBillingDetail newBilling = new RetainerBillingDetail();
                    newBilling.setSowContractId(sowContract.getId());
                    newBilling.setPaymentDate(crBillingDetail.getPaymentDate());
                    newBilling.setDeliveryNote(crBillingDetail.getDeliveryNote());
                    newBilling.setAmount(crBillingDetail.getAmount());
                    newBilling.setChangeRequestId(changeRequestId);
                    retainerBillingDetailRepository.save(newBilling);
                }
            }
        }
    }
    
    /**
     * Apply SCOPE_ADJUSTMENT: Create new one-time billing entry
     */
    private void applyScopeAdjustment(Integer sowContractId, Integer changeRequestId, LocalDate effectiveStart, User currentUser) {
        ChangeRequest changeRequest = changeRequestRepository.findById(changeRequestId)
            .orElseThrow(() -> new RuntimeException("Change request not found"));
        
        // Get amount and billing date from CR billing details
        List<ChangeRequestBillingDetail> crBilling = changeRequestBillingDetailRepository.findByChangeRequestId(changeRequestId);
        if (crBilling.isEmpty()) {
            throw new RuntimeException("Billing detail is required for SCOPE_ADJUSTMENT");
        }
        
        ChangeRequestBillingDetail scopeBilling = crBilling.get(0);
        if (scopeBilling.getPaymentDate() == null || scopeBilling.getAmount() == null) {
            throw new RuntimeException("Payment date and amount are required for SCOPE_ADJUSTMENT");
        }
        
        // Create new billing entry
        RetainerBillingDetail newBilling = new RetainerBillingDetail();
        newBilling.setSowContractId(sowContractId);
        newBilling.setPaymentDate(scopeBilling.getPaymentDate());
        newBilling.setDeliveryNote(scopeBilling.getDeliveryNote() != null ? scopeBilling.getDeliveryNote() : 
            "Scope adjustment – " + changeRequest.getChangeRequestId());
        newBilling.setAmount(scopeBilling.getAmount());
        newBilling.setChangeRequestId(changeRequestId);
        newBilling.setDeliveryItemId(null); // Not from delivery item
        retainerBillingDetailRepository.save(newBilling);
    }
    
    /**
     * Apply RATE_ADJUSTMENT: Update engineer rating and recalculate billing
     */
    private void applyRateAdjustment(Integer sowContractId, Integer changeRequestId, LocalDate effectiveStart, User currentUser) {
        // Get proposed changes from CR
        List<ChangeRequestEngagedEngineer> crEngineers = changeRequestEngagedEngineerRepository.findByChangeRequestId(changeRequestId);
        
        // Update engineer ratings
        for (ChangeRequestEngagedEngineer crEng : crEngineers) {
            if (crEng.getId() != null && crEng.getStartDate() != null && !crEng.getStartDate().isBefore(effectiveStart)) {
                Optional<SOWEngagedEngineer> existingOpt = sowEngagedEngineerRepository.findById(crEng.getId());
                if (existingOpt.isPresent()) {
                    SOWEngagedEngineer existing = existingOpt.get();
                    existing.setRating(crEng.getRating());
                    sowEngagedEngineerRepository.save(existing);
                }
            }
        }
        
        // Update billing based on new ratings (simplified - you may need more sophisticated calculation)
        List<ChangeRequestBillingDetail> crBilling = changeRequestBillingDetailRepository.findByChangeRequestId(changeRequestId);
        for (ChangeRequestBillingDetail crBillingDetail : crBilling) {
            if (crBillingDetail.getPaymentDate() != null && !crBillingDetail.getPaymentDate().isBefore(effectiveStart)) {
                if (crBillingDetail.getId() != null) {
                    Optional<RetainerBillingDetail> existingOpt = retainerBillingDetailRepository.findById(crBillingDetail.getId());
                    if (existingOpt.isPresent()) {
                        RetainerBillingDetail existing = existingOpt.get();
                        existing.setAmount(crBillingDetail.getAmount());
                        existing.setChangeRequestId(changeRequestId);
                        retainerBillingDetailRepository.save(existing);
                    }
                }
            }
        }
    }
    
    // ============================================
    // EVENT-BASED METHODS (New Implementation)
    // ============================================
    
    /**
     * Apply RESOURCE_CHANGE using event-based approach
     * Creates resource events instead of directly updating engineers
     */
    private void applyResourceChangeEventBased(Integer sowContractId, ChangeRequest changeRequest, LocalDate effectiveStart, User currentUser) {
        List<ChangeRequestEngagedEngineer> crEngineers = changeRequestEngagedEngineerRepository.findByChangeRequestId(changeRequest.getId());
        List<ChangeRequestBillingDetail> crBilling = changeRequestBillingDetailRepository.findByChangeRequestId(changeRequest.getId());
        
        // Get baseline engineers to compare
        List<SOWEngagedEngineerBase> baselineEngineers = sowBaselineService.getBaselineResources(sowContractId);
        
        // Parse engineerLevel to extract level and role
        // Format: "Middle Backend Engineer" -> level="Middle", role="Backend Engineer"
        // Or: "Senior Frontend" -> level="Senior", role="Frontend"
        java.util.function.Function<String, java.util.Map<String, String>> parseEngineerLevel = (engineerLevel) -> {
            java.util.Map<String, String> result = new java.util.HashMap<>();
            if (engineerLevel == null || engineerLevel.trim().isEmpty()) {
                result.put("level", "");
                result.put("role", "");
                return result;
            }
            
            // Normalize to avoid duplicates like "Middle Frontend Middle Frontend"
            String cleaned = engineerLevel.trim();
            String[] parts = cleaned.split("\\s+", 2);
            if (parts.length >= 2) {
                String level = parts[0];
                String role = parts[1];
                // If role already contains level (case-insensitive), don't duplicate
                if (role.toLowerCase().contains(level.toLowerCase())) {
                    result.put("level", level);
                    result.put("role", role);
            } else {
                    result.put("level", level);
                    result.put("role", role);
                }
            } else {
                result.put("level", cleaned);
                result.put("role", "");
            }
            return result;
        };
        
        // Create resource events only when there is an actual change compared to baseline
        for (ChangeRequestEngagedEngineer crEng : crEngineers) {
            // Try to match baseline by ID first (most reliable if CR carries baseline ID)
            SOWEngagedEngineerBase matchingBaseEng = null;
            boolean matchedById = false;
            if (crEng.getId() != null) {
                matchingBaseEng = baselineEngineers.stream()
                    .filter(b -> b.getId() != null && b.getId().equals(crEng.getId()))
                    .findFirst()
                    .orElse(null);
                matchedById = matchingBaseEng != null;
            }
            // Parse engineer level to get role and level
            // Format: "Middle Backend Engineer" -> level="Middle", role="Backend Engineer"
            java.util.Map<String, String> parsed = parseEngineerLevel.apply(crEng.getEngineerLevel());
            String crLevel = parsed.get("level");
            String crRole = parsed.get("role");
            
            // If no match by ID, try fuzzy match by role/level
            if (!matchedById && !baselineEngineers.isEmpty() && !crLevel.isEmpty() && !crRole.isEmpty()) {
                matchingBaseEng = baselineEngineers.stream()
                    .filter(base -> {
                        boolean levelMatch = base.getLevel() != null && 
                            base.getLevel().equalsIgnoreCase(crLevel);
                        boolean roleMatch = base.getRole() != null && 
                            (base.getRole().equalsIgnoreCase(crRole) || 
                             base.getRole().toLowerCase().contains(crRole.toLowerCase()) ||
                             crRole.toLowerCase().contains(base.getRole().toLowerCase()));
                        return levelMatch && roleMatch;
                    })
                    .findFirst()
                    .orElse(null);
            }
            
            if (matchingBaseEng != null) {
                // Check if anything changed compared to baseline
                boolean changed =
                    !equalsNullable(matchingBaseEng.getStartDate(), crEng.getStartDate() != null ? crEng.getStartDate() : effectiveStart) ||
                    !equalsNullable(matchingBaseEng.getEndDate(), crEng.getEndDate()) ||
                    !equalsNullable(matchingBaseEng.getRating(), crEng.getRating()) ||
                    !equalsNullable(matchingBaseEng.getUnitRate(), crEng.getSalary()) ||
                    !equalsNullable(matchingBaseEng.getRole(), crRole) ||
                    !equalsNullable(matchingBaseEng.getLevel(), crLevel);
                
                if (changed) {
                crEventService.createResourceEvent(
                    changeRequest,
                    CRResourceEvent.ResourceAction.MODIFY,
                    matchingBaseEng.getId(),
                    matchingBaseEng.getRole(),
                    matchingBaseEng.getLevel(),
                    matchingBaseEng.getRating(),
                    crEng.getRating(),
                    matchingBaseEng.getUnitRate(),
                    crEng.getSalary(),
                    matchingBaseEng.getStartDate(),
                    crEng.getStartDate() != null ? crEng.getStartDate() : effectiveStart,
                    matchingBaseEng.getEndDate(),
                    crEng.getEndDate(),
                    effectiveStart
                );
                }
            } else {
                // ADD event - new engineer not in baseline (or baseline is empty)
                // Only create ADD nếu thực sự khác baseline (không match hoàn toàn với bất kỳ bản ghi baseline)
                boolean hasData = (crEng.getRating() != null || crEng.getSalary() != null
                    || crEng.getStartDate() != null || crEng.getEndDate() != null
                    || (crEng.getEngineerLevel() != null && !crEng.getEngineerLevel().trim().isEmpty()));
                
                // Nếu có baseline, kiểm tra xem có bản ghi nào trùng hoàn toàn (level/role/start/end/rating/salary)
                boolean sameAsBaseline = false;
                if (!baselineEngineers.isEmpty()) {
                    for (SOWEngagedEngineerBase base : baselineEngineers) {
                        boolean levelSame = equalsNullable(base.getLevel(), crLevel);
                        boolean roleSame = equalsNullable(base.getRole(), crRole);
                        boolean startSame = equalsNullable(base.getStartDate(), crEng.getStartDate() != null ? crEng.getStartDate() : effectiveStart);
                        boolean endSame = equalsNullable(base.getEndDate(), crEng.getEndDate());
                        boolean ratingSame = equalsNullable(base.getRating(), crEng.getRating());
                        boolean salarySame = equalsNullable(base.getUnitRate(), crEng.getSalary());
                        if (levelSame && roleSame && startSame && endSame && ratingSame && salarySame) {
                            sameAsBaseline = true;
                            break;
                        }
                    }
                }
                
                if (hasData && !sameAsBaseline) {
                String eventRole = !crRole.isEmpty() ? crRole : crEng.getEngineerLevel();
                String eventLevel = !crLevel.isEmpty() ? crLevel : "";
                
                if (eventRole.isEmpty() && eventLevel.isEmpty()) {
                    eventRole = crEng.getEngineerLevel();
                    eventLevel = "";
                }
                
                crEventService.createResourceEvent(
                    changeRequest,
                    CRResourceEvent.ResourceAction.ADD,
                    null,
                    eventRole,
                    eventLevel,
                    null,
                    crEng.getRating(),
                    null,
                    crEng.getSalary(),
                    null,
                    crEng.getStartDate() != null ? crEng.getStartDate() : effectiveStart,
                    null,
                    crEng.getEndDate(),
                    effectiveStart
                );
                }
            }
        }
        
        // Log if no engineers found (for debugging)
        if (crEngineers.isEmpty()) {
            logger.warn("No engineers found in Change Request {} for RESOURCE_CHANGE", changeRequest.getId());
        }
        
        // Create billing events
        for (ChangeRequestBillingDetail crBillingDetail : crBilling) {
            if (crBillingDetail.getPaymentDate() != null && !crBillingDetail.getPaymentDate().isBefore(effectiveStart)) {
                LocalDate billingMonth = crBillingDetail.getPaymentDate();
                
                // Calculate delta (simplified - in production, compare with baseline)
                BigDecimal deltaAmount = crBillingDetail.getAmount();
                
                crEventService.createBillingEvent(
                    changeRequest,
                    billingMonth,
                    deltaAmount,
                    crBillingDetail.getDeliveryNote(),
                    CRBillingEvent.BillingEventType.RETAINER_ADJUST
                );
            }
        }
    }
    
    /**
     * Apply SCHEDULE_CHANGE using event-based approach
     */
    private void applyScheduleChangeEventBased(SOWContract sowContract, ChangeRequest changeRequest, LocalDate effectiveStart, User currentUser) {
        LocalDate newEndDate = changeRequest.getEffectiveUntil();
        if (newEndDate == null) {
            throw new RuntimeException("New end date is required for SCHEDULE_CHANGE");
        }
        
        // Update contract end date directly (this is a contract-level change)
        sowContract.setPeriodEnd(newEndDate);
        sowContractRepository.save(sowContract);
        
        // Create resource events for engineers that need end date adjustment
        List<SOWEngagedEngineerBase> baselineEngineers = sowBaselineService.getBaselineResources(sowContract.getId());
        for (SOWEngagedEngineerBase baseEng : baselineEngineers) {
            if (baseEng.getEndDate() == null || baseEng.getEndDate().isAfter(newEndDate)) {
                crEventService.createResourceEvent(
                    changeRequest,
                    CRResourceEvent.ResourceAction.MODIFY,
                    baseEng.getId(),
                    baseEng.getRole(),
                    baseEng.getLevel(),
                    baseEng.getRating(),
                    baseEng.getRating(),
                    baseEng.getUnitRate(),
                    baseEng.getUnitRate(),
                    baseEng.getStartDate(),
                    baseEng.getStartDate(),
                    baseEng.getEndDate(),
                    newEndDate,
                    effectiveStart
                );
            }
        }
        
        // Create billing events for new months (if extending) or cancellation (if shortening)
        // This is simplified - in production, you'd need more sophisticated logic
    }
    
    /**
     * Apply SCOPE_ADJUSTMENT using event-based approach
     */
    private void applyScopeAdjustmentEventBased(Integer sowContractId, ChangeRequest changeRequest, LocalDate effectiveStart, User currentUser) {
        // SCOPE_ADJUSTMENT creates a one-time billing event
        LocalDate billingDate = changeRequest.getEffectiveFrom() != null 
            ? changeRequest.getEffectiveFrom().withDayOfMonth(1)
            : effectiveStart.withDayOfMonth(1);
        
        crEventService.createBillingEvent(
            changeRequest,
            billingDate,
            changeRequest.getAmount(),
            "Scope adjustment: " + (changeRequest.getDescription() != null ? changeRequest.getDescription() : ""),
            CRBillingEvent.BillingEventType.SCOPE_ADJUSTMENT
        );
    }
    
    /**
     * Apply RATE_ADJUSTMENT using event-based approach
     */
    private void applyRateAdjustmentEventBased(Integer sowContractId, ChangeRequest changeRequest, LocalDate effectiveStart, User currentUser) {
        List<ChangeRequestEngagedEngineer> crEngineers = changeRequestEngagedEngineerRepository.findByChangeRequestId(changeRequest.getId());
        List<SOWEngagedEngineerBase> baselineEngineers = sowBaselineService.getBaselineResources(sowContractId);
        
        for (ChangeRequestEngagedEngineer crEng : crEngineers) {
            if (crEng.getId() != null) {
                SOWEngagedEngineerBase baseEng = baselineEngineers.stream()
                    .filter(e -> e.getId().equals(crEng.getId()))
                    .findFirst()
                    .orElse(null);
                
                if (baseEng != null) {
                    // MODIFY event for rating change
                    crEventService.createResourceEvent(
                        changeRequest,
                        CRResourceEvent.ResourceAction.MODIFY,
                        baseEng.getId(),
                        baseEng.getRole(),
                        baseEng.getLevel(),
                        baseEng.getRating(),
                        crEng.getRating(),
                        baseEng.getUnitRate(),
                        crEng.getSalary(),
                        baseEng.getStartDate(),
                        baseEng.getStartDate(),
                        baseEng.getEndDate(),
                        baseEng.getEndDate(),
                        effectiveStart
                    );
                }
            }
        }
        
        // Create billing events for affected months
        List<ChangeRequestBillingDetail> crBilling = changeRequestBillingDetailRepository.findByChangeRequestId(changeRequest.getId());
        for (ChangeRequestBillingDetail crBillingDetail : crBilling) {
            if (crBillingDetail.getPaymentDate() != null && !crBillingDetail.getPaymentDate().isBefore(effectiveStart)) {
                LocalDate billingMonth = crBillingDetail.getPaymentDate();
                
                // Calculate delta
                BigDecimal deltaAmount = crBillingDetail.getAmount();
                
                crEventService.createBillingEvent(
                    changeRequest,
                    billingMonth,
                    deltaAmount,
                    "Rate adjustment: " + crBillingDetail.getDeliveryNote(),
                    CRBillingEvent.BillingEventType.RETAINER_ADJUST
                );
            }
        }
    }
    
    /**
     * Get current resources at a specific date for CR creation/editing
     * Returns resources calculated from baseline + approved events up to asOfDate
     * @param sowContractId SOW contract ID
     * @param asOfDate Date to calculate resources for (usually CR.effectiveFrom)
     * @param currentUser Current user (for access control)
     * @return CurrentResourcesDTO with resources list
     */
    public com.skillbridge.dto.sales.response.CurrentResourcesDTO getCurrentResources(
            Integer sowContractId, 
            LocalDate asOfDate, 
            User currentUser) {
        
        // 1. Validate contract exists and is Retainer type
        SOWContract contract = sowContractRepository.findById(sowContractId)
            .orElseThrow(() -> new RuntimeException("SOW Contract not found"));
        
        // Check access permission (Sales Manager sees all, Sales Rep sees only assigned)
        if (!"SALES_MANAGER".equals(currentUser.getRole())) {
            if (contract.getAssigneeUserId() == null || !contract.getAssigneeUserId().equals(currentUser.getId())) {
                throw new RuntimeException("Access denied: You can only view contracts assigned to you");
            }
        }
        
        String engagementType = contract.getEngagementType();
        if (engagementType == null || (!engagementType.equals("Retainer") && !engagementType.equals("Retainer_"))) {
            throw new RuntimeException("This operation is only for Retainer SOW contracts");
        }
        
        // 2. Ensure baseline exists (create if not)
        sowBaselineService.createBaseline(sowContractId);
        
        // 3. Call CREventService.calculateCurrentResources(sowContractId, asOfDate)
        List<CREventService.CurrentEngineerState> currentEngineerStates = 
            crEventService.calculateCurrentResources(sowContractId, asOfDate);
        
        // 4. Convert to DTO format
        List<com.skillbridge.dto.sales.response.CurrentResourcesDTO.ResourceDTO> resourceDTOs = new ArrayList<>();
        
        // Get baseline engineers to map baseEngineerId
        List<SOWEngagedEngineerBase> baselineEngineers = sowBaselineService.getBaselineResources(sowContractId);
        java.util.Map<Integer, Integer> engineerIdToBaseIdMap = new java.util.HashMap<>();
        for (SOWEngagedEngineerBase base : baselineEngineers) {
            engineerIdToBaseIdMap.put(base.getId(), base.getId());
        }
        
        for (CREventService.CurrentEngineerState state : currentEngineerStates) {
            com.skillbridge.dto.sales.response.CurrentResourcesDTO.ResourceDTO dto = 
                new com.skillbridge.dto.sales.response.CurrentResourcesDTO.ResourceDTO();
            
            dto.setEngineerId(state.getEngineerId());
            dto.setBaseEngineerId(engineerIdToBaseIdMap.get(state.getEngineerId())); // Map to baseline ID if exists
            dto.setLevel(state.getLevel());
            dto.setRole(state.getRole());
            
            // Build engineerLevelLabel: "Level Role" (e.g., "Middle Backend Engineer")
            String levelStr = state.getLevel() != null ? state.getLevel().trim() : "";
            String roleStr = state.getRole() != null ? state.getRole().trim() : "";
            String engineerLevelLabel;
            if (!levelStr.isEmpty() && !roleStr.isEmpty()) {
                // If role already contains level (case-insensitive), use role only to avoid duplicates
                if (roleStr.toLowerCase().contains(levelStr.toLowerCase())) {
                    engineerLevelLabel = roleStr;
                } else {
                    engineerLevelLabel = (levelStr + " " + roleStr).trim();
                }
            } else if (!roleStr.isEmpty()) {
                engineerLevelLabel = roleStr;
            } else {
                engineerLevelLabel = levelStr;
            }
            dto.setEngineerLevelLabel(engineerLevelLabel);
            
            dto.setStartDate(state.getStartDate());
            dto.setEndDate(state.getEndDate());
            dto.setRating(state.getRating());
            dto.setUnitRate(state.getUnitRate());
            
            resourceDTOs.add(dto);
        }
        
        // 5. Return
        com.skillbridge.dto.sales.response.CurrentResourcesDTO response = 
            new com.skillbridge.dto.sales.response.CurrentResourcesDTO();
        response.setSowContractId(sowContractId);
        response.setAsOfDate(asOfDate);
        response.setResources(resourceDTOs);
        
        return response;
    }
    
    /**
     * Update payment status for billing detail
     * @param contractId SOW contract ID
     * @param billingDetailId Billing detail ID
     * @param isPaid Payment status (true = paid, false = unpaid)
     * @param engagementType "Fixed Price" or "Retainer"
     * @param currentUser Current user
     */
    @Transactional
    public void updateBillingDetailPaymentStatus(Integer contractId, Integer billingDetailId, Boolean isPaid, String engagementType, User currentUser) {
        // Verify user has access
        SOWContract contract = sowContractRepository.findById(contractId)
            .orElseThrow(() -> new RuntimeException("SOW Contract not found"));
        
        // Check access permission
        if (!"SALES_MANAGER".equals(currentUser.getRole())) {
            if (contract.getAssigneeUserId() == null || !contract.getAssigneeUserId().equals(currentUser.getId())) {
                throw new RuntimeException("Access denied: You can only update contracts assigned to you");
            }
        }
        
        if ("Fixed Price".equals(engagementType)) {
            FixedPriceBillingDetail billing = fixedPriceBillingDetailRepository.findById(billingDetailId)
                .orElseThrow(() -> new RuntimeException("Billing detail not found"));
            
            // Verify billing belongs to this contract
            if (!billing.getSowContractId().equals(contractId)) {
                throw new RuntimeException("Billing detail does not belong to this contract");
            }
            
            billing.setIsPaid(isPaid != null ? isPaid : false);
            fixedPriceBillingDetailRepository.save(billing);
        } else if ("Retainer".equals(engagementType)) {
            RetainerBillingDetail billing = retainerBillingDetailRepository.findById(billingDetailId)
                .orElseThrow(() -> new RuntimeException("Billing detail not found"));
            
            // Verify billing belongs to this contract
            if (!billing.getSowContractId().equals(contractId)) {
                throw new RuntimeException("Billing detail does not belong to this contract");
            }
            
            billing.setIsPaid(isPaid != null ? isPaid : false);
            retainerBillingDetailRepository.save(billing);
        } else {
            throw new RuntimeException("Invalid engagement type: " + engagementType);
        }
    }
}

