package com.skillbridge.service.sales;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.skillbridge.dto.sales.request.CreateMSARequest;
import com.skillbridge.dto.sales.request.CreateChangeRequestRequest;
import com.skillbridge.dto.sales.response.MSAContractDTO;
import com.skillbridge.dto.sales.response.MSAContractDetailDTO;
import com.skillbridge.dto.sales.response.ChangeRequestResponseDTO;
import com.skillbridge.dto.sales.response.SalesChangeRequestDetailDTO;
import com.skillbridge.entity.auth.User;
import com.skillbridge.entity.contract.Contract;
import com.skillbridge.entity.contract.ContractHistory;
import com.skillbridge.entity.contract.ContractInternalReview;
import com.skillbridge.entity.contract.ChangeRequest;
import com.skillbridge.entity.contract.ChangeRequestEngagedEngineer;
import com.skillbridge.entity.contract.ChangeRequestBillingDetail;
import com.skillbridge.entity.contract.ChangeRequestAttachment;
import com.skillbridge.entity.document.DocumentMetadata;
import com.skillbridge.entity.opportunity.Opportunity;
import com.skillbridge.entity.proposal.Proposal;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.repository.contract.ContractHistoryRepository;
import com.skillbridge.repository.contract.ContractInternalReviewRepository;
import com.skillbridge.repository.contract.ContractRepository;
import com.skillbridge.repository.contract.ChangeRequestRepository;
import com.skillbridge.repository.contract.ChangeRequestEngagedEngineerRepository;
import com.skillbridge.repository.contract.ChangeRequestBillingDetailRepository;
import com.skillbridge.repository.contract.ChangeRequestAttachmentRepository;
import com.skillbridge.repository.contract.ChangeRequestHistoryRepository;
import com.skillbridge.entity.contract.ChangeRequestHistory;
import com.skillbridge.repository.document.DocumentMetadataRepository;
import com.skillbridge.repository.opportunity.OpportunityRepository;
import com.skillbridge.repository.proposal.ProposalRepository;
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
import com.skillbridge.dto.common.AttachmentInfo;

/**
 * Sales MSA Contract Service
 * Handles business logic for creating MSA contracts
 */
@Service
@Transactional
public class SalesMSAContractService {
    
    @Autowired
    private ContractRepository contractRepository;
    
    @Autowired
    private OpportunityRepository opportunityRepository;
    
    @Autowired
    private ProposalRepository proposalRepository;
    
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
    private ChangeRequestBillingDetailRepository changeRequestBillingDetailRepository;
    
    @Autowired
    private ChangeRequestAttachmentRepository changeRequestAttachmentRepository;
    
    @Autowired
    private ChangeRequestHistoryRepository changeRequestHistoryRepository;
    
    private final Gson gson = new Gson();
    
    private static final java.time.format.DateTimeFormatter DATE_FORMATTER = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * Create MSA contract
     */
    public MSAContractDTO createMSAContract(CreateMSARequest request, MultipartFile[] attachments, User currentUser) {
        // Validate opportunity if provided
        Opportunity opportunity = null;
        Integer actualClientId = request.getClientId();
        
        if (request.getOpportunityId() != null && !request.getOpportunityId().trim().isEmpty()) {
            opportunity = opportunityRepository.findByOpportunityId(request.getOpportunityId())
                .orElseThrow(() -> new RuntimeException("Opportunity not found: " + request.getOpportunityId()));
            
            // Verify opportunity has approved proposal
            List<Proposal> proposals = proposalRepository.findByOpportunityIdOrderByVersionDesc(opportunity.getId());
            boolean hasApprovedProposal = proposals.stream()
                .anyMatch(p -> "approved".equalsIgnoreCase(p.getStatus()) && 
                              "APPROVE".equalsIgnoreCase(p.getReviewAction()));
            
            if (!hasApprovedProposal) {
                throw new RuntimeException("Opportunity must have an approved proposal to create MSA");
            }
            
            // If clientId not provided, try to find client user from opportunity's clientEmail
            if (actualClientId == null && opportunity.getClientEmail() != null) {
                Optional<User> clientUserOpt = userRepository.findByEmail(opportunity.getClientEmail());
                if (clientUserOpt.isPresent()) {
                    actualClientId = clientUserOpt.get().getId();
                }
            }
        }
        
        // Validate client
        if (actualClientId == null) {
            throw new RuntimeException("Client ID is required");
        }
        User client = userRepository.findById(actualClientId)
            .orElseThrow(() -> new RuntimeException("Client not found"));
        
        // Validate assignee
        User assignee = userRepository.findById(request.getAssigneeUserId())
            .orElseThrow(() -> new RuntimeException("Assignee not found"));
        
        // Validate contacts
        User clientContact = userRepository.findById(request.getClientContactId())
            .orElseThrow(() -> new RuntimeException("Client contact not found"));
        User landbridgeContact = userRepository.findById(request.getLandbridgeContactId())
            .orElseThrow(() -> new RuntimeException("LandBridge contact not found"));
        
        // Create contract
        Contract contract = new Contract();
        contract.setClientId(actualClientId);
        
        // Generate contract name from opportunity or use default
        if (opportunity != null) {
            contract.setContractName("MSA Contract - " + opportunity.getClientName());
        } else {
            contract.setContractName("MSA Contract - " + client.getFullName());
        }
        
        // Map status string to enum
        Contract.ContractStatus statusEnum = mapStatusToEnum(request.getStatus());
        contract.setStatus(statusEnum);
        
        contract.setPeriodStart(LocalDate.parse(request.getEffectiveStart()));
        contract.setPeriodEnd(LocalDate.parse(request.getEffectiveEnd()));
        contract.setAssigneeUserId(request.getAssigneeUserId());
        contract.setReviewerId(request.getReviewerId()); // Save reviewer ID
        contract.setCurrency(request.getCurrency());
        contract.setPaymentTerms(request.getPaymentTerms());
        contract.setInvoicingCycle(request.getInvoicingCycle());
        contract.setBillingDay(request.getBillingDay());
        contract.setTaxWithholding(request.getTaxWithholding());
        contract.setIpOwnership(request.getIpOwnership());
        contract.setGoverningLaw(request.getGoverningLaw());
        contract.setLandbridgeContactName(landbridgeContact.getFullName());
        contract.setLandbridgeContactEmail(landbridgeContact.getEmail());
        
        contract = contractRepository.save(contract);
        
        // Generate contract ID
        String contractId = generateContractId(contract.getId(), contract.getCreatedAt());
        
        // Upload attachments and save to contract entity (similar to Proposal)
        if (attachments != null && attachments.length > 0) {
            List<AttachmentInfo> fileInfos = uploadAttachments(contract.getId(), attachments, currentUser.getId());
            if (!fileInfos.isEmpty()) {
                contract.setLink(fileInfos.get(0).getS3Key()); // Store first file S3 key
                contract.setAttachmentsManifest(gson.toJson(fileInfos));
                contract = contractRepository.save(contract); // Update with file links
            }
        }
        
        // Handle review only when a Sales Manager submits an action
        if (request.getReviewerId() != null && request.getReviewAction() != null) {
            if ("SALES_MANAGER".equals(currentUser.getRole())) {
            submitReview(contract.getId(), request.getReviewNotes(), request.getReviewAction(), currentUser);
            } else {
                // Sales Reps can update contract details but cannot submit review actions
                // Ignore reviewAction to prevent permission error
                System.out.println("Review action ignored for non-manager user: " + currentUser.getId());
            }
        }
        
        // Create initial history entry
        createHistoryEntry(contract.getId(), "CREATED", 
            "MSA Contract created by " + currentUser.getFullName(), null, null, currentUser.getId());
        
        // Convert to DTO
        MSAContractDTO dto = new MSAContractDTO();
        dto.setId(contract.getId());
        dto.setContractId(contractId);
        dto.setContractName(contract.getContractName());
        dto.setStatus(contract.getStatus().name().replace("_", " "));
        
        return dto;
    }
    
    /**
     * Get active MSA contracts (for SOW creation)
     */
    public List<MSAContractDTO> getActiveMSAContracts(User currentUser) {
        // Role-based filtering
        final Integer assigneeUserId;
        if ("SALES_REP".equals(currentUser.getRole())) {
            // Sales Rep: only contracts assigned to themselves
            assigneeUserId = currentUser.getId();
        } else {
            assigneeUserId = null;
        }
        
        // Find contracts with status Active or Under_Review (Client Under Review)
        List<Contract> contracts;
        if (assigneeUserId != null) {
            // Sales Rep: only assigned contracts
            contracts = contractRepository.findAll().stream()
                .filter(c -> assigneeUserId.equals(c.getAssigneeUserId()))
                .filter(c -> Contract.ContractStatus.Active.equals(c.getStatus()) || 
                            Contract.ContractStatus.Under_Review.equals(c.getStatus()))
                .collect(java.util.stream.Collectors.toList());
        } else {
            // Sales Manager: all contracts
            contracts = contractRepository.findAll().stream()
                .filter(c -> Contract.ContractStatus.Active.equals(c.getStatus()) || 
                            Contract.ContractStatus.Under_Review.equals(c.getStatus()))
                .collect(java.util.stream.Collectors.toList());
        }
        
        // Convert to DTOs
        List<MSAContractDTO> dtos = new ArrayList<>();
        for (Contract contract : contracts) {
            MSAContractDTO dto = new MSAContractDTO();
            dto.setId(contract.getId());
            dto.setContractId(generateContractId(contract.getId(), contract.getCreatedAt()));
            dto.setContractName(contract.getContractName());
            
            // Map status for display
            String statusDisplay = contract.getStatus().name().replace("_", " ");
            if ("Under Review".equals(statusDisplay)) {
                // Check if there's an approved review (Client Under Review)
                Optional<ContractInternalReview> reviewOpt = contractInternalReviewRepository
                    .findFirstByContractIdAndContractTypeOrderByReviewedAtDesc(contract.getId(), "MSA");
                if (reviewOpt.isPresent() && "APPROVE".equals(reviewOpt.get().getReviewAction())) {
                    statusDisplay = "Client Under Review";
                } else {
                    statusDisplay = "Internal Review";
                }
            }
            dto.setStatus(statusDisplay);
            
            // Only include Active or Client Under Review contracts
            if ("Active".equals(statusDisplay) || "Client Under Review".equals(statusDisplay)) {
                dtos.add(dto);
            }
        }
        
        return dtos;
    }
    
    /**
     * Get MSA contract detail
     */
    public MSAContractDetailDTO getMSAContractDetail(Integer contractId, User currentUser) {
        Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new RuntimeException("Contract not found"));
        
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
        
        // Load client contact (default to client, in real scenario might need separate mapping)
        User clientContact = client; // Default to client
        
        // Load landbridge contact
        User landbridgeContact = null;
        if (contract.getLandbridgeContactEmail() != null) {
            landbridgeContact = userRepository.findByEmail(contract.getLandbridgeContactEmail()).orElse(null);
        }
        if (landbridgeContact == null && assignee != null) {
            landbridgeContact = assignee;
        }
        
        // Try to find opportunity (simplified - in real scenario might need contract_opportunity mapping)
        String opportunityId = null;
        if (client.getEmail() != null) {
            List<Opportunity> opportunities = opportunityRepository.findAll().stream()
                .filter(opp -> client.getEmail().equals(opp.getClientEmail()))
                .filter(opp -> "WON".equals(opp.getStatus()))
                .limit(1)
                .collect(java.util.stream.Collectors.toList());
            if (!opportunities.isEmpty()) {
                opportunityId = opportunities.get(0).getOpportunityId();
            }
        }
        
        // Load attachments from attachments_manifest (similar to Proposal)
        // Priority: attachments_manifest > link > DocumentMetadata
        List<MSAContractDetailDTO.AttachmentDTO> attachments = new ArrayList<>();
        
        // First, try to load from attachments_manifest
        if (contract.getAttachmentsManifest() != null && !contract.getAttachmentsManifest().trim().isEmpty()) {
            try {
                // Try to parse as List<AttachmentInfo> (new format with fileName)
                Type attachmentInfoListType = new TypeToken<List<AttachmentInfo>>(){}.getType();
                List<AttachmentInfo> attachmentInfos = gson.fromJson(contract.getAttachmentsManifest(), attachmentInfoListType);
                if (attachmentInfos != null && !attachmentInfos.isEmpty()) {
                    for (AttachmentInfo info : attachmentInfos) {
                        if (info.getS3Key() != null && !info.getS3Key().trim().isEmpty()) {
                            attachments.add(new MSAContractDetailDTO.AttachmentDTO(info.getS3Key(), info.getFileName(), null));
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
                                attachments.add(new MSAContractDetailDTO.AttachmentDTO(s3Key, fileName, null));
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
                                attachments.add(new MSAContractDetailDTO.AttachmentDTO(s3Key, fileName, null));
                            }
                        }
                    }
                } catch (Exception e2) {
                    System.err.println("Error parsing attachments_manifest for contract " + contractId + ": " + e2.getMessage());
                    // Continue to fallback
                }
            }
        }
        
        // If no attachments from manifest, try link
        if (attachments.isEmpty() && contract.getLink() != null && !contract.getLink().trim().isEmpty()) {
            String fileName = contract.getLink();
            if (fileName.contains("/")) {
                fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
            }
            attachments.add(new MSAContractDetailDTO.AttachmentDTO(contract.getLink(), fileName, null));
        }
        
        // Fallback: Always check DocumentMetadata (in case attachments_manifest is not set but files exist)
        // This ensures backward compatibility and handles cases where files were uploaded but manifest wasn't updated
        List<DocumentMetadata> documents = documentMetadataRepository.findByEntityIdAndEntityType(
            contractId, "msa_contract");
        
        // If we have attachments from manifest/link, verify they exist in DocumentMetadata
        // If we don't have attachments, use DocumentMetadata as source
        if (attachments.isEmpty()) {
            // No attachments from manifest/link, use DocumentMetadata
            attachments = documents.stream()
                .map(doc -> {
                    String fileName = doc.getS3Key();
                    if (fileName != null && fileName.contains("/")) {
                        fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                    }
                    return new MSAContractDetailDTO.AttachmentDTO(
                        doc.getS3Key(),
                        fileName,
                        null
                    );
                })
                .collect(java.util.stream.Collectors.toList());
        } else if (!documents.isEmpty()) {
            // We have attachments from manifest, but also check DocumentMetadata
            // Merge any additional files from DocumentMetadata that aren't in manifest
            List<String> existingS3Keys = attachments.stream()
                .map(MSAContractDetailDTO.AttachmentDTO::getS3Key)
                .collect(java.util.stream.Collectors.toList());
            
            for (DocumentMetadata doc : documents) {
                if (doc.getS3Key() != null && !existingS3Keys.contains(doc.getS3Key())) {
                    String fileName = doc.getS3Key();
                    if (fileName.contains("/")) {
                        fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                    }
                    attachments.add(new MSAContractDetailDTO.AttachmentDTO(
                        doc.getS3Key(),
                        fileName,
                        null
                    ));
                }
            }
        }
        
        // Get reviewer info: first from contract.reviewerId (assigned reviewer), then from review if submitted
        Integer reviewerId = contract.getReviewerId(); // Get assigned reviewer from contract
        String reviewerName = null;
        String reviewNotes = null;
        String reviewAction = null;
        
        // Get reviewer name from contract.reviewerId
        if (reviewerId != null) {
            User reviewer = userRepository.findById(reviewerId).orElse(null);
            if (reviewer != null) {
                reviewerName = reviewer.getFullName();
            }
        }
        
        // Get latest review info from contract_internal_review table (if review was submitted)
        Optional<ContractInternalReview> reviewOpt = contractInternalReviewRepository
            .findFirstByContractIdAndContractTypeOrderByReviewedAtDesc(contractId, "MSA");
        
        if (reviewOpt.isPresent()) {
            ContractInternalReview review = reviewOpt.get();
            // If review was submitted, use review info (reviewerId should match contract.reviewerId)
            reviewAction = review.getReviewAction();
            reviewNotes = review.getReviewNotes();
            // Note: reviewerId and reviewerName already set from contract above
        }
        
        // Build DTO
        MSAContractDetailDTO dto = new MSAContractDetailDTO();
        dto.setId(contract.getId());
        dto.setContractId(generateContractId(contract.getId(), contract.getCreatedAt()));
        dto.setContractName(contract.getContractName());
        
        // Map status for display: Under_Review -> "Client Under Review" when approved, otherwise "Internal Review"
        String statusDisplay = contract.getStatus().name().replace("_", " ");
        if ("Under Review".equals(statusDisplay)) {
            // Check if there's an approved review
            if (reviewOpt.isPresent() && "APPROVE".equals(reviewOpt.get().getReviewAction())) {
                statusDisplay = "Client Under Review";
            } else {
                statusDisplay = "Internal Review";
            }
        }
        dto.setStatus(statusDisplay);
        
        dto.setOpportunityId(opportunityId);
        dto.setClientId(contract.getClientId());
        dto.setClientName(client.getFullName());
        dto.setClientEmail(client.getEmail());
        dto.setEffectiveStart(contract.getPeriodStart() != null ? contract.getPeriodStart().toString() : null);
        dto.setEffectiveEnd(contract.getPeriodEnd() != null ? contract.getPeriodEnd().toString() : null);
        dto.setAssigneeUserId(contract.getAssigneeUserId());
        dto.setAssigneeName(assignee != null ? assignee.getFullName() : null);
        // Note field not in Contract entity, might need to add or use history
        
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
        
        dto.setAttachments(attachments);
        dto.setReviewerId(reviewerId);
        dto.setReviewerName(reviewerName);
        dto.setReviewNotes(reviewNotes);
        dto.setReviewAction(reviewAction);
        
        // Load history
        List<ContractHistory> allHistory = contractHistoryRepository.findByContractIdOrderByEntryDateDesc(contractId);
        List<MSAContractDetailDTO.HistoryItemDTO> history = new ArrayList<>();
        for (ContractHistory hist : allHistory) {
            MSAContractDetailDTO.HistoryItemDTO historyDTO = new MSAContractDetailDTO.HistoryItemDTO();
            historyDTO.setId(hist.getId());
            historyDTO.setDate(hist.getEntryDate() != null ? hist.getEntryDate().toString() : null);
            historyDTO.setDescription(hist.getDescription());
            historyDTO.setDocumentLink(hist.getDocumentLink());
            historyDTO.setDocumentName(hist.getDocumentName());
            history.add(historyDTO);
        }
        dto.setHistory(history);
        
        return dto;
    }
    
    /**
     * Update MSA contract (only allowed when status is Draft)
     */
    public MSAContractDTO updateMSAContract(Integer contractId, CreateMSARequest request, MultipartFile[] attachments, User currentUser) {
        // Find existing contract
        Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new RuntimeException("Contract not found"));
        
        // Check access permission (Sales Manager sees all, Sales Rep sees only assigned)
        if (!"SALES_MANAGER".equals(currentUser.getRole())) {
            if (contract.getAssigneeUserId() == null || !contract.getAssigneeUserId().equals(currentUser.getId())) {
                throw new RuntimeException("Access denied: You can only update contracts assigned to you");
            }
        }
        
        // Only allow update when status is Draft or Request_for_Change
        if (contract.getStatus() != Contract.ContractStatus.Draft && 
            contract.getStatus() != Contract.ContractStatus.Request_for_Change) {
            throw new RuntimeException("Contract can only be updated when status is Draft or Request for Change. Current status: " + contract.getStatus().name());
        }
        
        // Validate opportunity if provided
        Opportunity opportunity = null;
        Integer actualClientId = request.getClientId();
        
        if (request.getOpportunityId() != null && !request.getOpportunityId().trim().isEmpty()) {
            opportunity = opportunityRepository.findByOpportunityId(request.getOpportunityId())
                .orElseThrow(() -> new RuntimeException("Opportunity not found: " + request.getOpportunityId()));
            
            // If clientId not provided, try to find client user from opportunity's clientEmail
            if (actualClientId == null && opportunity.getClientEmail() != null) {
                Optional<User> clientUserOpt = userRepository.findByEmail(opportunity.getClientEmail());
                if (clientUserOpt.isPresent()) {
                    actualClientId = clientUserOpt.get().getId();
                }
            }
        }
        
        // Validate client
        if (actualClientId == null) {
            throw new RuntimeException("Client ID is required");
        }
        User client = userRepository.findById(actualClientId)
            .orElseThrow(() -> new RuntimeException("Client not found"));
        
        // Validate assignee
        User assignee = userRepository.findById(request.getAssigneeUserId())
            .orElseThrow(() -> new RuntimeException("Assignee not found"));
        
        // Validate contacts
        User clientContact = userRepository.findById(request.getClientContactId())
            .orElseThrow(() -> new RuntimeException("Client contact not found"));
        User landbridgeContact = userRepository.findById(request.getLandbridgeContactId())
            .orElseThrow(() -> new RuntimeException("LandBridge contact not found"));
        
        // Update contract fields
        contract.setClientId(actualClientId);
        
        // Update contract name from opportunity or use default
        if (opportunity != null) {
            contract.setContractName("MSA Contract - " + opportunity.getClientName());
        } else {
            contract.setContractName("MSA Contract - " + client.getFullName());
        }
        
        // Map status string to enum
        Contract.ContractStatus statusEnum = mapStatusToEnum(request.getStatus());
        contract.setStatus(statusEnum);
        
        contract.setPeriodStart(LocalDate.parse(request.getEffectiveStart()));
        contract.setPeriodEnd(LocalDate.parse(request.getEffectiveEnd()));
        contract.setAssigneeUserId(request.getAssigneeUserId());
        contract.setReviewerId(request.getReviewerId()); // Save reviewer ID
        contract.setCurrency(request.getCurrency());
        contract.setPaymentTerms(request.getPaymentTerms());
        contract.setInvoicingCycle(request.getInvoicingCycle());
        contract.setBillingDay(request.getBillingDay());
        contract.setTaxWithholding(request.getTaxWithholding());
        contract.setIpOwnership(request.getIpOwnership());
        contract.setGoverningLaw(request.getGoverningLaw());
        contract.setLandbridgeContactName(landbridgeContact.getFullName());
        contract.setLandbridgeContactEmail(landbridgeContact.getEmail());
        
        contract = contractRepository.save(contract);
        
        // Upload new attachments if any and save to contract entity (similar to Proposal)
        if (attachments != null && attachments.length > 0) {
            System.out.println("Uploading " + attachments.length + " attachment(s) for contract " + contract.getId());
            List<AttachmentInfo> fileInfos = uploadAttachments(contract.getId(), attachments, currentUser.getId());
            System.out.println("Uploaded " + fileInfos.size() + " file(s)");
            
            if (!fileInfos.isEmpty()) {
                // Reload contract to get latest state
                contract = contractRepository.findById(contract.getId())
                    .orElseThrow(() -> new RuntimeException("Contract not found after save"));
                
                // If contract already has attachments, merge with new ones
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
                        System.out.println("Existing attachments from manifest: " + existingInfos.size());
                    } catch (Exception e) {
                        System.err.println("Error parsing existing attachments_manifest: " + e.getMessage());
                        // If parsing fails, start fresh
                        existingInfos = new ArrayList<>();
                    }
                }
                if (existingInfos == null) {
                    existingInfos = new ArrayList<>();
                }
                existingInfos.addAll(fileInfos);
                System.out.println("Merged attachments list: " + existingInfos.size() + " files");
                
                contract.setLink(existingInfos.get(0).getS3Key()); // Store first file S3 key
                contract.setAttachmentsManifest(gson.toJson(existingInfos));
                System.out.println("Saving contract with link: " + contract.getLink() + ", manifest: " + contract.getAttachmentsManifest());
                
                contract = contractRepository.save(contract); // Update with file links
                contractRepository.flush(); // Force flush to database
                System.out.println("Contract saved successfully. Contract ID: " + contract.getId() + ", Link: " + contract.getLink() + ", Manifest length: " + (contract.getAttachmentsManifest() != null ? contract.getAttachmentsManifest().length() : 0));
            } else {
                System.out.println("No files were uploaded (fileLinks is empty)");
            }
        } else {
            System.out.println("No attachments provided for update");
        }
        
        // Handle review if reviewer is assigned and review is submitted
        if (request.getReviewerId() != null && request.getReviewAction() != null) {
            submitReview(contract.getId(), request.getReviewNotes(), request.getReviewAction(), currentUser);
        }
        
        // Create history entry
        createHistoryEntry(contract.getId(), "UPDATED", 
            "MSA Contract updated by " + currentUser.getFullName(), null, null, currentUser.getId());
        
        // Convert to DTO
        MSAContractDTO dto = new MSAContractDTO();
        dto.setId(contract.getId());
        dto.setContractId(generateContractId(contract.getId(), contract.getCreatedAt()));
        dto.setContractName(contract.getContractName());
        dto.setStatus(contract.getStatus().name().replace("_", " "));
        
        return dto;
    }
    
    /**
     * Submit review for contract
     */
    public MSAContractDTO submitReview(Integer contractId, String reviewNotes, String action, User currentUser) {
        Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new RuntimeException("Contract not found"));
        
        // Verify current user is a Sales Manager (only Sales Managers can review)
        if (!"SALES_MANAGER".equals(currentUser.getRole())) {
            throw new RuntimeException("Only Sales Managers can submit reviews");
        }
        
        // Update contract status based on action
        if ("APPROVE".equalsIgnoreCase(action)) {
            // When approved, change status to "Client Under Review" (mapped from Under_Review enum)
            contract.setStatus(Contract.ContractStatus.Under_Review); // Maps to "Client Under Review" in display
        } else if ("REQUEST_REVISION".equalsIgnoreCase(action)) {
            // When requesting revision, move contract to Request for Change
            contract.setStatus(Contract.ContractStatus.Request_for_Change);
        }
        
        contract = contractRepository.save(contract);
        
        // Save review to contract_internal_review table (not visible to clients)
        ContractInternalReview review = new ContractInternalReview();
        review.setContractId(contractId);
        review.setSowContractId(null); // MSA contract only
        review.setContractType("MSA");
        review.setReviewerId(currentUser.getId());
        review.setReviewAction(action);
        review.setReviewNotes(reviewNotes);
        contractInternalReviewRepository.save(review);
        
        // DO NOT create history entry for internal review (to keep it hidden from clients)
        // Only create history entry for non-review actions
        
        // Convert to DTO
        MSAContractDTO dto = new MSAContractDTO();
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
     * Map status string to ContractStatus enum
     */
    private Contract.ContractStatus mapStatusToEnum(String status) {
        if (status == null || status.trim().isEmpty()) {
            return Contract.ContractStatus.Draft;
        }
        
        String statusUpper = status.toUpperCase().replace(" ", "_");
        try {
            return Contract.ContractStatus.valueOf(statusUpper);
        } catch (IllegalArgumentException e) {
            // Map common status strings
            if (statusUpper.contains("INTERNAL_REVIEW") || statusUpper.contains("INTERNALREVIEW")) {
                return Contract.ContractStatus.Under_Review;
            } else if (statusUpper.contains("CLIENT_UNDER_REVIEW") || statusUpper.contains("CLIENTUNDERREVIEW")) {
                return Contract.ContractStatus.Under_Review;
            } else {
                return Contract.ContractStatus.Draft;
            }
        }
    }
    
    /**
     * Generate contract ID in format: MSA-YYYY-NN
     */
    private String generateContractId(Integer id, LocalDateTime createdAt) {
        int year = createdAt != null ? createdAt.getYear() : 2025;
        // Use last 2 digits of ID as sequence number (simplified)
        int sequenceNumber = id % 100;
        return String.format("MSA-%d-%02d", year, sequenceNumber);
    }
    
    /**
     * Upload attachments to S3
     * Returns list of S3 keys (similar to Proposal)
     */
    private List<AttachmentInfo> uploadAttachments(Integer contractId, MultipartFile[] attachments, Integer ownerId) {
        List<AttachmentInfo> fileInfos = new ArrayList<>();
        System.out.println("uploadAttachments called: contractId=" + contractId + ", attachments.length=" + (attachments != null ? attachments.length : 0) + ", s3Enabled=" + s3Enabled + ", s3Service=" + (s3Service != null ? "not null" : "null"));
        
        // Skip upload if S3 is not configured or enabled
        if (!s3Enabled || s3Service == null) {
            // S3 not configured, skip upload but log warning
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
                String s3Key = s3Service.uploadFile(file, "contracts/msa/" + contractId);
                System.out.println("File uploaded successfully. S3 key: " + s3Key);
                fileInfos.add(new AttachmentInfo(s3Key, originalFileName));
                
                // Save document metadata
                DocumentMetadata metadata = new DocumentMetadata();
                metadata.setS3Key(s3Key);
                metadata.setOwnerId(ownerId);
                metadata.setDocumentType("contract");
                metadata.setEntityId(contractId);
                metadata.setEntityType("msa_contract");
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
     * Delete attachment from MSA contract
     */
    public void deleteAttachment(Integer contractId, String s3Key, User currentUser) {
        // Find existing contract
        Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new RuntimeException("Contract not found"));
        
        // Check access permission (Sales Manager sees all, Sales Rep sees only assigned)
        if (!"SALES_MANAGER".equals(currentUser.getRole())) {
            if (contract.getAssigneeUserId() == null || !contract.getAssigneeUserId().equals(currentUser.getId())) {
                throw new RuntimeException("Access denied: You can only delete attachments from contracts assigned to you");
            }
        }
        
        // Only allow delete when status is Draft
        if (contract.getStatus() != Contract.ContractStatus.Draft) {
            throw new RuntimeException("Attachments can only be deleted when contract status is Draft. Current status: " + contract.getStatus().name());
        }
        
        // Remove from attachments_manifest
        if (contract.getAttachmentsManifest() != null && !contract.getAttachmentsManifest().trim().isEmpty()) {
            try {
                // Try to parse as List<AttachmentInfo> (new format)
                Type attachmentInfoListType = new TypeToken<List<AttachmentInfo>>(){}.getType();
                List<AttachmentInfo> attachmentInfos = gson.fromJson(contract.getAttachmentsManifest(), attachmentInfoListType);
                if (attachmentInfos != null && !attachmentInfos.isEmpty()) {
                    boolean removed = attachmentInfos.removeIf(info -> s3Key.equals(info.getS3Key()));
                    if (!removed) {
                        throw new RuntimeException("Attachment not found in contract");
                    }
                    
                    // Update contract
                    if (attachmentInfos.isEmpty()) {
                        contract.setLink(null);
                        contract.setAttachmentsManifest(null);
                    } else {
                        contract.setLink(attachmentInfos.get(0).getS3Key());
                        contract.setAttachmentsManifest(gson.toJson(attachmentInfos));
                    }
                } else {
                    // Fallback: try to parse as List<String> (old format)
                    Type stringListType = new TypeToken<List<String>>(){}.getType();
                    List<String> attachmentLinks = gson.fromJson(contract.getAttachmentsManifest(), stringListType);
                    if (attachmentLinks != null) {
                        boolean removed = attachmentLinks.remove(s3Key);
                        if (!removed) {
                            throw new RuntimeException("Attachment not found in contract");
                        }
                        
                        // Update contract
                        if (attachmentLinks.isEmpty()) {
                            contract.setLink(null);
                            contract.setAttachmentsManifest(null);
                        } else {
                            contract.setLink(attachmentLinks.get(0));
                            contract.setAttachmentsManifest(gson.toJson(attachmentLinks));
                        }
                    }
                }
            } catch (Exception e) {
                // If parsing as AttachmentInfo fails, try List<String> (old format)
                try {
                    Type stringListType = new TypeToken<List<String>>(){}.getType();
                    List<String> attachmentLinks = gson.fromJson(contract.getAttachmentsManifest(), stringListType);
                    if (attachmentLinks != null) {
                        boolean removed = attachmentLinks.remove(s3Key);
                        if (!removed) {
                            throw new RuntimeException("Attachment not found in contract");
                        }
                        
                        // Update contract
                        if (attachmentLinks.isEmpty()) {
                            contract.setLink(null);
                            contract.setAttachmentsManifest(null);
                        } else {
                            contract.setLink(attachmentLinks.get(0));
                            contract.setAttachmentsManifest(gson.toJson(attachmentLinks));
                        }
                    }
                } catch (Exception e2) {
                    System.err.println("Error parsing attachments_manifest: " + e2.getMessage());
                    throw new RuntimeException("Failed to parse attachments manifest", e2);
                }
            }
        }
        contractRepository.save(contract);
        contractRepository.flush();
        
        // Delete from S3
        if (s3Enabled && s3Service != null) {
            try {
                s3Service.deleteFile(s3Key);
                System.out.println("File deleted from S3: " + s3Key);
            } catch (Exception e) {
                System.err.println("Failed to delete file from S3: " + s3Key + " - " + e.getMessage());
                // Continue even if S3 delete fails
            }
        }
        
        // Delete from DocumentMetadata
        Optional<DocumentMetadata> docMeta = documentMetadataRepository.findByS3Key(s3Key);
        if (docMeta.isPresent()) {
            documentMetadataRepository.delete(docMeta.get());
            System.out.println("DocumentMetadata deleted for S3 key: " + s3Key);
        }
        
        // Create history entry
        createHistoryEntry(contractId, "ATTACHMENT_DELETED", 
            "Attachment deleted by " + currentUser.getFullName() + ": " + s3Key, null, null, currentUser.getId());
    }
    
    /**
     * Create history entry
     */
    private void createHistoryEntry(Integer contractId, String activityType, String description, 
                                   String fileLink, String fileUrl, Integer createdBy) {
        ContractHistory history = new ContractHistory();
        history.setContractId(contractId);
        history.setHistoryType("MSA");
        history.setEntryDate(LocalDate.now());
        history.setDescription(description);
        history.setDocumentLink(fileLink);
        history.setDocumentName(fileUrl);
        history.setCreatedBy(createdBy);
        contractHistoryRepository.save(history);
    }
    
    /**
     * Create change request for Fixed Price MSA contract
     */
    public ChangeRequestResponseDTO createChangeRequestForMSA(
        Integer msaContractId,
        CreateChangeRequestRequest request,
        MultipartFile[] attachments,
        User currentUser
    ) {
        // Verify MSA contract exists
        Contract msaContract = contractRepository.findById(msaContractId)
            .orElseThrow(() -> new RuntimeException("MSA Contract not found"));
        
        // Check access permission (Sales Manager sees all, Sales Rep sees only assigned)
        if (!"SALES_MANAGER".equals(currentUser.getRole())) {
            if (msaContract.getAssigneeUserId() == null || !msaContract.getAssigneeUserId().equals(currentUser.getId())) {
                throw new RuntimeException("Access denied: You can only create change requests for contracts assigned to you");
            }
        }
        
        // Note: MSA contracts don't have engagement type like SOW, but we assume all MSAs are Fixed Price
        // Validate CR type is valid for Fixed Price MSA
        if (request.getType() != null) {
            List<String> validTypes = List.of("Add Scope", "Remove Scope", "Other");
            if (!validTypes.contains(request.getType())) {
                throw new RuntimeException("CR Type '" + request.getType() + "' is not valid for Fixed Price MSA contract");
            }
        }
        
        // Create change request entity
        ChangeRequest changeRequest = new ChangeRequest();
        changeRequest.setContractId(msaContractId);
        changeRequest.setContractType("MSA");
        changeRequest.setTitle(request.getTitle());
        changeRequest.setType(request.getType());
        changeRequest.setSummary(request.getSummary());
        changeRequest.setDescription(request.getSummary()); // Use summary as description
        changeRequest.setReason(request.getComment()); // Use comment as reason
        
        // Set effective dates
        if (request.getEffectiveFrom() != null && !request.getEffectiveFrom().trim().isEmpty()) {
            changeRequest.setEffectiveFrom(LocalDate.parse(request.getEffectiveFrom()));
        }
        if (request.getEffectiveUntil() != null && !request.getEffectiveUntil().trim().isEmpty()) {
            changeRequest.setEffectiveUntil(LocalDate.parse(request.getEffectiveUntil()));
        }
        
        // Set references (evidence)
        if (request.getReferences() != null && !request.getReferences().trim().isEmpty()) {
            List<String> evidenceList = new ArrayList<>();
            evidenceList.add(request.getReferences());
            changeRequest.setEvidence(gson.toJson(evidenceList));
        }
        
        // Calculate total amount from billing details
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (request.getBillingDetails() != null && !request.getBillingDetails().isEmpty()) {
            for (CreateChangeRequestRequest.BillingDetailDTO billing : request.getBillingDetails()) {
                if (billing.getAmount() != null) {
                    totalAmount = totalAmount.add(BigDecimal.valueOf(billing.getAmount()));
                }
            }
        }
        changeRequest.setAmount(totalAmount);
        changeRequest.setExpectedExtraCost(totalAmount);
        changeRequest.setCostEstimatedByLandbridge(totalAmount);
        
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
        
        // Save engaged engineers
        if (request.getEngagedEngineers() != null && !request.getEngagedEngineers().isEmpty()) {
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
            history.setTimestamp(java.time.LocalDateTime.now());
            changeRequestHistoryRepository.save(history);
        } catch (Exception e) {
            // Log error but don't fail the operation
            System.err.println("Failed to create change request history entry: " + e.getMessage());
        }
    }
    
    /**
     * Get change request detail for MSA
     */
    public SalesChangeRequestDetailDTO getChangeRequestDetailForMSA(
        Integer msaContractId,
        Integer changeRequestId,
        User currentUser
    ) {
        // Verify MSA contract exists
        Contract msaContract = contractRepository.findById(msaContractId)
            .orElseThrow(() -> new RuntimeException("MSA Contract not found"));
        
        // Check access permission (Sales Manager sees all, Sales Rep sees only assigned)
        if (!"SALES_MANAGER".equals(currentUser.getRole())) {
            if (msaContract.getAssigneeUserId() == null || !msaContract.getAssigneeUserId().equals(currentUser.getId())) {
                throw new RuntimeException("Access denied: You can only view change requests for contracts assigned to you");
            }
        }
        
        // Find change request
        ChangeRequest changeRequest = changeRequestRepository.findById(changeRequestId)
            .orElseThrow(() -> new RuntimeException("Change request not found"));
        
        // Validate change request belongs to MSA contract
        if (!changeRequest.getContractId().equals(msaContractId) || !"MSA".equals(changeRequest.getContractType())) {
            throw new RuntimeException("Change request does not belong to this MSA contract");
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
        Integer reviewerId = null;
        if (changeRequest.getCreatedBy() != null) {
            // Try to get reviewer from contract's reviewerId or change request's reviewerId
            // For now, we'll check if there's a reviewer assigned
            // Note: ChangeRequest entity may need a reviewerId field
        }
        
        // Get engaged engineers
        List<ChangeRequestEngagedEngineer> engineers = changeRequestEngagedEngineerRepository.findByChangeRequestId(changeRequestId);
        List<SalesChangeRequestDetailDTO.EngagedEngineerDTO> engineerDTOs = engineers.stream()
            .map(e -> {
                SalesChangeRequestDetailDTO.EngagedEngineerDTO dto = new SalesChangeRequestDetailDTO.EngagedEngineerDTO();
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
        
        // Get billing details
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
        
        // Get attachments
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
        
        // Get history
        List<ChangeRequestHistory> history = changeRequestHistoryRepository.findByChangeRequestIdOrderByTimestampDesc(changeRequestId);
        List<SalesChangeRequestDetailDTO.HistoryItemDTO> historyDTOs = history.stream()
            .map(h -> {
                SalesChangeRequestDetailDTO.HistoryItemDTO dto = new SalesChangeRequestDetailDTO.HistoryItemDTO();
                dto.setId(h.getId());
                dto.setDate(h.getTimestamp() != null ? h.getTimestamp().format(DATE_FORMATTER) : "");
                // Use action and userName for description
                String description = h.getAction();
                if (h.getUserName() != null) {
                    description += " by " + h.getUserName();
                }
                dto.setDescription(description);
                dto.setUser(h.getUserName() != null ? h.getUserName() : "Unknown");
                dto.setDocumentLink(null); // ChangeRequestHistory doesn't have documentLink
                return dto;
            })
            .collect(java.util.stream.Collectors.toList());
        
        // Parse references (evidence)
        String references = null;
        if (changeRequest.getEvidence() != null && !changeRequest.getEvidence().trim().isEmpty()) {
            try {
                List<String> evidenceList = gson.fromJson(changeRequest.getEvidence(), new TypeToken<List<String>>(){}.getType());
                if (evidenceList != null && !evidenceList.isEmpty()) {
                    references = String.join(", ", evidenceList);
                }
            } catch (Exception e) {
                // If parsing fails, use raw evidence string
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
        dto.setCreatedDate(changeRequest.getCreatedAt() != null ? changeRequest.getCreatedAt().format(DATE_FORMATTER) : "");
        dto.setEngagedEngineers(engineerDTOs);
        dto.setBillingDetails(billingDTOs);
        dto.setAttachments(attachmentDTOs);
        dto.setHistory(historyDTOs);
        dto.setInternalReviewerId(reviewerId);
        dto.setInternalReviewerName(reviewerName);
        dto.setComment(changeRequest.getReason()); // Use reason as comment
        
        return dto;
    }
    
    /**
     * Update change request for MSA (Draft only)
     */
    @Transactional
    public void updateChangeRequestForMSA(
        Integer msaContractId,
        Integer changeRequestId,
        CreateChangeRequestRequest request,
        MultipartFile[] attachments,
        User currentUser
    ) {
        // Verify MSA contract exists
        Contract msaContract = contractRepository.findById(msaContractId)
            .orElseThrow(() -> new RuntimeException("MSA Contract not found"));
        
        // Find change request
        ChangeRequest changeRequest = changeRequestRepository.findById(changeRequestId)
            .orElseThrow(() -> new RuntimeException("Change request not found"));
        
        // Validate change request belongs to MSA contract
        if (!changeRequest.getContractId().equals(msaContractId) || !"MSA".equals(changeRequest.getContractType())) {
            throw new RuntimeException("Change request does not belong to this MSA contract");
        }
        
        // Validate status is Draft
        if (!"Draft".equals(changeRequest.getStatus())) {
            throw new RuntimeException("Only Draft change requests can be updated");
        }
        
        // Validate user is the creator
        if (changeRequest.getCreatedBy() == null || !changeRequest.getCreatedBy().equals(currentUser.getId())) {
            throw new RuntimeException("Only the creator can update this change request");
        }
        
        // Update change request fields
        if (request.getTitle() != null) {
            changeRequest.setTitle(request.getTitle());
        }
        if (request.getType() != null) {
            changeRequest.setType(request.getType());
        }
        if (request.getSummary() != null) {
            changeRequest.setSummary(request.getSummary());
            changeRequest.setDescription(request.getSummary());
        }
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
        
        // Calculate total amount from billing details
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (request.getBillingDetails() != null && !request.getBillingDetails().isEmpty()) {
            for (CreateChangeRequestRequest.BillingDetailDTO billing : request.getBillingDetails()) {
                if (billing.getAmount() != null) {
                    totalAmount = totalAmount.add(BigDecimal.valueOf(billing.getAmount()));
                }
            }
        }
        changeRequest.setAmount(totalAmount);
        changeRequest.setExpectedExtraCost(totalAmount);
        changeRequest.setCostEstimatedByLandbridge(totalAmount);
        
        // Save change request
        changeRequestRepository.save(changeRequest);
        
        // Update engaged engineers (delete old, add new)
        changeRequestEngagedEngineerRepository.deleteByChangeRequestId(changeRequestId);
        if (request.getEngagedEngineers() != null && !request.getEngagedEngineers().isEmpty()) {
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
        
        // Update billing details (delete old, add new)
        changeRequestBillingDetailRepository.deleteByChangeRequestId(changeRequestId);
        if (request.getBillingDetails() != null && !request.getBillingDetails().isEmpty()) {
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
        
        // Upload new attachments if any
        if (attachments != null && attachments.length > 0) {
            uploadChangeRequestAttachments(changeRequestId, attachments, currentUser.getId());
        }
        
        // Create history entry
        createChangeRequestHistoryEntry(changeRequestId, 
            "UPDATED", 
            "Change request updated by " + currentUser.getFullName(), 
            currentUser.getId());
    }
    
    /**
     * Submit change request for MSA (Draft -> Under Internal Review)
     */
    @Transactional
    public void submitChangeRequestForMSA(
        Integer msaContractId,
        Integer changeRequestId,
        Integer internalReviewerId,
        User currentUser
    ) {
        // Verify MSA contract exists
        Contract msaContract = contractRepository.findById(msaContractId)
            .orElseThrow(() -> new RuntimeException("MSA Contract not found"));
        
        // Find change request
        ChangeRequest changeRequest = changeRequestRepository.findById(changeRequestId)
            .orElseThrow(() -> new RuntimeException("Change request not found"));
        
        // Validate change request belongs to MSA contract
        if (!changeRequest.getContractId().equals(msaContractId) || !"MSA".equals(changeRequest.getContractType())) {
            throw new RuntimeException("Change request does not belong to this MSA contract");
        }
        
        // Validate status is Draft
        if (!"Draft".equals(changeRequest.getStatus())) {
            throw new RuntimeException("Only Draft change requests can be submitted");
        }
        
        // Validate user is the creator
        if (changeRequest.getCreatedBy() == null || !changeRequest.getCreatedBy().equals(currentUser.getId())) {
            throw new RuntimeException("Only the creator can submit this change request");
        }
        
        // Validate internal reviewer is provided
        if (internalReviewerId == null) {
            throw new RuntimeException("Internal reviewer is required");
        }
        
        // Set internal reviewer
        changeRequest.setInternalReviewerId(internalReviewerId);
        
        // Update status
        changeRequest.setStatus("Under Internal Review");
        changeRequestRepository.save(changeRequest);
        
        // Create history entry
        createChangeRequestHistoryEntry(changeRequestId, 
            "SUBMITTED", 
            "Change request submitted for internal review by " + currentUser.getFullName(), 
            currentUser.getId());
    }
    
    /**
     * Submit review for change request (Under Internal Review -> Client Under Review or Draft)
     */
    @Transactional
    public void submitChangeRequestReviewForMSA(
        Integer msaContractId,
        Integer changeRequestId,
        String reviewAction, // "APPROVE" or "REQUEST_REVISION"
        String reviewNotes,
        User currentUser
    ) {
        // Verify MSA contract exists
        Contract msaContract = contractRepository.findById(msaContractId)
            .orElseThrow(() -> new RuntimeException("MSA Contract not found"));
        
        // Find change request
        ChangeRequest changeRequest = changeRequestRepository.findById(changeRequestId)
            .orElseThrow(() -> new RuntimeException("Change request not found"));
        
        // Validate change request belongs to MSA contract
        if (!changeRequest.getContractId().equals(msaContractId) || !"MSA".equals(changeRequest.getContractType())) {
            throw new RuntimeException("Change request does not belong to this MSA contract");
        }
        
        // Validate status is Under Internal Review
        if (!"Under Internal Review".equals(changeRequest.getStatus())) {
            throw new RuntimeException("Change request is not under internal review");
        }
        
        // Validate review action
        if (!"APPROVE".equals(reviewAction) && !"REQUEST_REVISION".equals(reviewAction)) {
            throw new RuntimeException("Invalid review action. Must be APPROVE or REQUEST_REVISION");
        }
        
        // Note: We need to check if current user is the assigned reviewer
        // For now, we'll allow any Sales Manager to review
        if (!"SALES_MANAGER".equals(currentUser.getRole())) {
            throw new RuntimeException("Only Sales Managers can review change requests");
        }
        
        // Update status based on review action
        if ("APPROVE".equals(reviewAction)) {
            changeRequest.setStatus("Client Under Review");
        } else if ("REQUEST_REVISION".equals(reviewAction)) {
            changeRequest.setStatus("Draft");
        }
        
        changeRequestRepository.save(changeRequest);
        
        // Create history entry
        String actionDescription = "APPROVE".equals(reviewAction) ? "approved" : "requested revision for";
        createChangeRequestHistoryEntry(changeRequestId, 
            "REVIEWED", 
            "Change request " + actionDescription + " by " + currentUser.getFullName() + 
            (reviewNotes != null && !reviewNotes.trim().isEmpty() ? ". Notes: " + reviewNotes : ""), 
            currentUser.getId());
    }
}

