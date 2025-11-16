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
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Sales SOW Contract Service
 * Handles business logic for creating SOW contracts
 */
@Service
@Transactional
public class SalesSOWContractService {
    
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
            
            // Calculate total value from billing details
            for (CreateSOWRequest.BillingDetailDTO detail : request.getBillingDetails()) {
                if (detail.getAmount() != null) {
                    totalValue = totalValue.add(BigDecimal.valueOf(detail.getAmount()));
                }
            }
            
            // Create billing details (manual input, no longer auto-generated)
            createRetainerBillingDetails(contract.getId(), request.getBillingDetails());
            
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
            
            // Use contractValue if provided, otherwise calculate from billing details
            if (request.getContractValue() != null && request.getContractValue() > 0) {
                totalValue = BigDecimal.valueOf(request.getContractValue());
            } else {
                // Calculate total value from billing details
                if (request.getBillingDetails() != null && !request.getBillingDetails().isEmpty()) {
                    for (CreateSOWRequest.BillingDetailDTO detail : request.getBillingDetails()) {
                        if (detail.getAmount() != null) {
                            totalValue = totalValue.add(BigDecimal.valueOf(detail.getAmount()));
                        }
                    }
                }
            }
            
            // Create billing details from milestone deliverables
            if (request.getBillingDetails() != null && !request.getBillingDetails().isEmpty()) {
                createFixedPriceBillingDetails(contract.getId(), request.getBillingDetails());
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
            List<String> fileLinks = uploadAttachments(contract.getId(), attachments, currentUser.getId());
            if (!fileLinks.isEmpty()) {
                contract.setLink(fileLinks.get(0)); // Store first file S3 key
                contract.setAttachmentsManifest(gson.toJson(fileLinks));
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
            // When request revision, change status back to Draft to allow editing
            contract.setStatus(SOWContract.SOWContractStatus.Draft);
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
        dto.setContractId(generateContractId(contract.getId(), contract.getCreatedAt()));
        dto.setContractName(contract.getContractName());
        // Map status for display: Under_Review -> "Client Under Review" when approved
        String statusDisplay = contract.getStatus().name().replace("_", " ");
        if ("Under Review".equals(statusDisplay) && "APPROVE".equalsIgnoreCase(action)) {
            statusDisplay = "Client Under Review";
        } else if ("Under Review".equals(statusDisplay)) {
            statusDisplay = "Internal Review";
        }
        dto.setStatus(statusDisplay);
        
        return dto;
    }
    
    /**
     * Get SOW contract detail
     */
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
                Type listType = new TypeToken<List<String>>(){}.getType();
                List<String> attachmentLinks = gson.fromJson(contract.getAttachmentsManifest(), listType);
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
            } catch (Exception e) {
                System.err.println("Error parsing attachments_manifest for SOW contract " + contractId + ": " + e.getMessage());
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
            
            // Load engaged engineers
            List<SOWEngagedEngineer> engineers = sowEngagedEngineerRepository.findBySowContractId(contractId);
            for (SOWEngagedEngineer engineer : engineers) {
                SOWContractDetailDTO.EngagedEngineerDTO dto = new SOWContractDetailDTO.EngagedEngineerDTO();
                dto.setId(engineer.getId());
                dto.setEngineerLevel(engineer.getEngineerLevel());
                dto.setStartDate(engineer.getStartDate() != null ? engineer.getStartDate().toString() : null);
                dto.setEndDate(engineer.getEndDate() != null ? engineer.getEndDate().toString() : null);
                dto.setRating(engineer.getRating() != null ? engineer.getRating().doubleValue() : null);
                dto.setSalary(engineer.getSalary() != null ? engineer.getSalary().doubleValue() : null);
                engagedEngineers.add(dto);
            }
            
            // Load billing details
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
                billingDetails.add(dto);
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
     */
    private void createRetainerBillingDetails(Integer sowContractId, List<CreateSOWRequest.BillingDetailDTO> billingDetails) {
        for (CreateSOWRequest.BillingDetailDTO detail : billingDetails) {
            RetainerBillingDetail billingDetail = new RetainerBillingDetail();
            billingDetail.setSowContractId(sowContractId);
            billingDetail.setPaymentDate(LocalDate.parse(detail.getPaymentDate()));
            billingDetail.setDeliveryNote(detail.getDeliveryNote());
            billingDetail.setAmount(BigDecimal.valueOf(detail.getAmount()));
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
            if (engineerDTO.getRating() != null) {
                engineer.setRating(BigDecimal.valueOf(engineerDTO.getRating()));
            }
            if (engineerDTO.getSalary() != null) {
                engineer.setSalary(BigDecimal.valueOf(engineerDTO.getSalary()));
            }
            sowEngagedEngineerRepository.save(engineer);
        }
    }
    
    /**
     * Auto-generate retainer billing details from delivery items
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
     */
    private void createFixedPriceBillingDetails(Integer sowContractId, List<CreateSOWRequest.BillingDetailDTO> billingDetails) {
        for (CreateSOWRequest.BillingDetailDTO detail : billingDetails) {
            FixedPriceBillingDetail billingDetail = new FixedPriceBillingDetail();
            billingDetail.setSowContractId(sowContractId);
            billingDetail.setBillingName(detail.getBillingName() != null ? detail.getBillingName() : "Payment");
            billingDetail.setMilestone(detail.getMilestone());
            billingDetail.setAmount(BigDecimal.valueOf(detail.getAmount()));
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
     * Upload attachments to S3
     * Returns list of S3 keys (similar to MSA)
     */
    private List<String> uploadAttachments(Integer contractId, MultipartFile[] attachments, Integer ownerId) {
        List<String> fileLinks = new ArrayList<>();
        System.out.println("uploadAttachments called: contractId=" + contractId + ", attachments.length=" + (attachments != null ? attachments.length : 0) + ", s3Enabled=" + s3Enabled + ", s3Service=" + (s3Service != null ? "not null" : "null"));
        
        // Skip upload if S3 is not configured or enabled
        if (!s3Enabled || s3Service == null) {
            System.out.println("S3 is not configured or enabled. Skipping file upload for contract: " + contractId);
            return fileLinks;
        }
        
        for (MultipartFile file : attachments) {
            if (file.isEmpty()) {
                System.out.println("Skipping empty file: " + file.getOriginalFilename());
                continue;
            }
            
            // Validate file type (PDF only)
            String contentType = file.getContentType();
            System.out.println("File: " + file.getOriginalFilename() + ", contentType: " + contentType + ", size: " + file.getSize());
            if (contentType == null || !contentType.equals("application/pdf")) {
                System.out.println("Skipping non-PDF file: " + file.getOriginalFilename() + " (contentType: " + contentType + ")");
                continue; // Skip non-PDF files
            }
            
            try {
                // Upload to S3 (returns S3 key)
                String s3Key = s3Service.uploadFile(file, "contracts/sow/" + contractId);
                System.out.println("File uploaded successfully. S3 key: " + s3Key);
                fileLinks.add(s3Key);
                
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
                System.err.println("IOException uploading file: " + file.getOriginalFilename() + " - " + e.getMessage());
                throw new RuntimeException("Failed to upload file: " + file.getOriginalFilename(), e);
            } catch (RuntimeException e) {
                // If S3 upload fails, log error but don't fail the entire contract creation
                System.err.println("RuntimeException uploading file to S3: " + file.getOriginalFilename() + " - " + e.getMessage());
                e.printStackTrace();
                // Continue with other files
            }
        }
        
        System.out.println("uploadAttachments returning " + fileLinks.size() + " file(s): " + fileLinks);
        return fileLinks;
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
                List<String> validTypes = List.of("Extend Schedule", "Rate Change", "Increase Resource", "Other");
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
        changeRequest.setSowContractId(sowContractId);
        changeRequest.setContractType("SOW");
        changeRequest.setTitle(request.getTitle());
        changeRequest.setType(request.getType());
        changeRequest.setSummary(request.getSummary());
        changeRequest.setDescription(request.getSummary()); // Use summary as description
        changeRequest.setReason(request.getComment()); // Use comment as reason
        
        // Set effective dates for Retainer
        if (isRetainer) {
            if (request.getEffectiveFrom() != null && !request.getEffectiveFrom().trim().isEmpty()) {
                changeRequest.setEffectiveFrom(LocalDate.parse(request.getEffectiveFrom()));
            }
            if (request.getEffectiveUntil() != null && !request.getEffectiveUntil().trim().isEmpty()) {
                changeRequest.setEffectiveUntil(LocalDate.parse(request.getEffectiveUntil()));
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
            // For Retainer: calculate from billing details
            if (request.getBillingDetails() != null && !request.getBillingDetails().isEmpty()) {
                for (CreateChangeRequestRequest.BillingDetailDTO billing : request.getBillingDetails()) {
                    if (billing.getAmount() != null) {
                        totalAmount = totalAmount.add(BigDecimal.valueOf(billing.getAmount()));
                    }
                }
            }
        } else if (isFixedPrice) {
            // For Fixed Price: use additional cost from impact analysis
            if (request.getImpactAnalysis() != null && request.getImpactAnalysis().getAdditionalCost() != null) {
                totalAmount = BigDecimal.valueOf(request.getImpactAnalysis().getAdditionalCost());
            }
        }
        changeRequest.setAmount(totalAmount);
        changeRequest.setExpectedExtraCost(totalAmount);
        changeRequest.setCostEstimatedByLandbridge(totalAmount);
        
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
        
        // Set status based on action
        if ("submit".equalsIgnoreCase(request.getAction())) {
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
        
        // Save engaged engineers (only for Retainer)
        if (isRetainer && request.getEngagedEngineers() != null && !request.getEngagedEngineers().isEmpty()) {
            for (CreateChangeRequestRequest.EngagedEngineerDTO engineerDTO : request.getEngagedEngineers()) {
                ChangeRequestEngagedEngineer engineer = new ChangeRequestEngagedEngineer();
                engineer.setChangeRequestId(changeRequest.getId());
                engineer.setEngineerLevel(engineerDTO.getEngineerLevel());
                if (engineerDTO.getStartDate() != null && !engineerDTO.getStartDate().trim().isEmpty()) {
                    engineer.setStartDate(LocalDate.parse(engineerDTO.getStartDate()));
                }
                if (engineerDTO.getEndDate() != null && !engineerDTO.getEndDate().trim().isEmpty()) {
                    engineer.setEndDate(LocalDate.parse(engineerDTO.getEndDate()));
                }
                if (engineerDTO.getRating() != null) {
                    engineer.setRating(BigDecimal.valueOf(engineerDTO.getRating()));
                }
                if (engineerDTO.getSalary() != null) {
                    engineer.setSalary(BigDecimal.valueOf(engineerDTO.getSalary()));
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
        List<SalesChangeRequestDetailDTO.EngagedEngineerDTO> engineerDTOs = engineers.stream()
            .map(e -> {
                SalesChangeRequestDetailDTO.EngagedEngineerDTO dto = new SalesChangeRequestDetailDTO.EngagedEngineerDTO();
                dto.setId(e.getId());
                dto.setEngineerLevel(e.getEngineerLevel());
                dto.setStartDate(e.getStartDate() != null ? e.getStartDate().toString() : null);
                dto.setEndDate(e.getEndDate() != null ? e.getEndDate().toString() : null);
                dto.setRating(e.getRating() != null ? e.getRating().doubleValue() : null);
                dto.setSalary(e.getSalary() != null ? e.getSalary().doubleValue() : null);
                return dto;
            })
            .collect(java.util.stream.Collectors.toList());
        
        List<ChangeRequestBillingDetail> billingDetails = changeRequestBillingDetailRepository.findByChangeRequestId(changeRequestId);
        List<SalesChangeRequestDetailDTO.BillingDetailDTO> billingDTOs = billingDetails.stream()
            .map(b -> {
                SalesChangeRequestDetailDTO.BillingDetailDTO dto = new SalesChangeRequestDetailDTO.BillingDetailDTO();
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
        List<SalesChangeRequestDetailDTO.HistoryItemDTO> historyDTOs = history.stream()
            .map(h -> {
                SalesChangeRequestDetailDTO.HistoryItemDTO dto = new SalesChangeRequestDetailDTO.HistoryItemDTO();
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
        if (isRetainer) {
            if (request.getEffectiveFrom() != null && !request.getEffectiveFrom().trim().isEmpty()) {
                changeRequest.setEffectiveFrom(LocalDate.parse(request.getEffectiveFrom()));
            }
            if (request.getEffectiveUntil() != null && !request.getEffectiveUntil().trim().isEmpty()) {
                changeRequest.setEffectiveUntil(LocalDate.parse(request.getEffectiveUntil()));
            }
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
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (isRetainer) {
            if (request.getBillingDetails() != null && !request.getBillingDetails().isEmpty()) {
                for (CreateChangeRequestRequest.BillingDetailDTO billing : request.getBillingDetails()) {
                    if (billing.getAmount() != null) {
                        totalAmount = totalAmount.add(BigDecimal.valueOf(billing.getAmount()));
                    }
                }
            }
        } else if (isFixedPrice) {
            if (request.getImpactAnalysis() != null && request.getImpactAnalysis().getAdditionalCost() != null) {
                totalAmount = BigDecimal.valueOf(request.getImpactAnalysis().getAdditionalCost());
            }
        }
        changeRequest.setAmount(totalAmount);
        changeRequest.setExpectedExtraCost(totalAmount);
        changeRequest.setCostEstimatedByLandbridge(totalAmount);
        
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
            for (CreateChangeRequestRequest.EngagedEngineerDTO engineerDTO : request.getEngagedEngineers()) {
                ChangeRequestEngagedEngineer engineer = new ChangeRequestEngagedEngineer();
                engineer.setChangeRequestId(changeRequestId);
                engineer.setEngineerLevel(engineerDTO.getEngineerLevel());
                if (engineerDTO.getStartDate() != null && !engineerDTO.getStartDate().trim().isEmpty()) {
                    engineer.setStartDate(LocalDate.parse(engineerDTO.getStartDate()));
                }
                if (engineerDTO.getEndDate() != null && !engineerDTO.getEndDate().trim().isEmpty()) {
                    engineer.setEndDate(LocalDate.parse(engineerDTO.getEndDate()));
                }
                if (engineerDTO.getRating() != null) {
                    engineer.setRating(BigDecimal.valueOf(engineerDTO.getRating()));
                }
                if (engineerDTO.getSalary() != null) {
                    engineer.setSalary(BigDecimal.valueOf(engineerDTO.getSalary()));
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
        
        if (!"Draft".equals(changeRequest.getStatus())) {
            throw new RuntimeException("Only Draft change requests can be submitted");
        }
        
        if (changeRequest.getCreatedBy() == null || !changeRequest.getCreatedBy().equals(currentUser.getId())) {
            throw new RuntimeException("Only the creator can submit this change request");
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
}

