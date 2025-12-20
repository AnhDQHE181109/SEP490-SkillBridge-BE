package com.skillbridge.service.contract;

import com.skillbridge.dto.contract.response.*;
import com.skillbridge.entity.auth.User;
import com.skillbridge.entity.contract.*;
import com.skillbridge.entity.document.DocumentMetadata;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.repository.contract.*;
import com.skillbridge.repository.contract.ContractInternalReviewRepository;
import com.skillbridge.repository.document.DocumentMetadataRepository;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.skillbridge.dto.common.AttachmentInfo;

/**
 * Contract Detail Service
 * Handles business logic for contract detail operations
 */
@Service
public class ContractDetailService {
    
    @Autowired
    private ContractRepository contractRepository;
    
    @Autowired
    private SOWContractRepository sowContractRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ContractHistoryRepository contractHistoryRepository;
    
    @Autowired
    private ChangeRequestRepository changeRequestRepository;
    
    @Autowired
    private MilestoneDeliverableRepository milestoneDeliverableRepository;
    
    @Autowired
    private FixedPriceBillingDetailRepository fixedPriceBillingDetailRepository;
    
    @Autowired
    private DeliveryItemRepository deliveryItemRepository;
    
    @Autowired
    private RetainerBillingDetailRepository retainerBillingDetailRepository;
    
    @Autowired
    private SOWEngagedEngineerRepository sowEngagedEngineerRepository;
    
    @Autowired
    private DocumentMetadataRepository documentMetadataRepository;
    
    @Autowired
    private ContractInternalReviewRepository contractInternalReviewRepository;
    
    @Autowired(required = false)
    private com.skillbridge.service.sales.SOWBaselineService sowBaselineService;
    
    @Autowired(required = false)
    private com.skillbridge.service.sales.CREventService crEventService;
    
    @Autowired(required = false)
    private com.skillbridge.repository.contract.CRBillingEventRepository crBillingEventRepository;
    
    private final Gson gson = new Gson();
    
    /**
     * Get contract detail for client
     * Tries MSA first, then SOW
     */
    public ContractDetailDTO getContractDetail(Integer contractId, Integer clientUserId) {
        // Try to find as MSA contract first
        Contract msaContract = contractRepository.findByIdAndClientId(contractId, clientUserId).orElse(null);
        if (msaContract != null) {
            return getMSADetail(msaContract, clientUserId);
        }
        
        // Try to find as SOW contract
        SOWContract sowContract = sowContractRepository.findByIdAndClientId(contractId, clientUserId)
            .orElseThrow(() -> new EntityNotFoundException("Contract not found"));
        
        return getSOWDetail(sowContract, clientUserId);
    }
    
    /**
     * Get MSA contract detail
     */
    private ContractDetailDTO getMSADetail(Contract contract, Integer clientUserId) {
        // Load client contact information
        User client = userRepository.findById(contract.getClientId())
            .orElseThrow(() -> new EntityNotFoundException("Client not found"));
        
        // Get LandBridge contact (from contract entity if available, otherwise assignee or default)
        String landbridgeContactName = contract.getLandbridgeContactName() != null 
            ? contract.getLandbridgeContactName()
            : (contract.getAssigneeId() != null ? contract.getAssigneeId() : "LandBridge Support");
        String landbridgeContactEmail = contract.getLandbridgeContactEmail() != null
            ? contract.getLandbridgeContactEmail()
            : "support@landbridge.co.jp"; // Default
        
        // Load contract history (exclude internal review entries)
        List<ContractHistory> allHistory = contractHistoryRepository.findByContractIdOrderByEntryDateDesc(contract.getId());
        // Filter out internal review entries (REVIEWED type or description containing "Review submitted")
        List<ContractHistory> history = allHistory.stream()
            .filter(h -> !"REVIEWED".equals(h.getHistoryType()) 
                && (h.getDescription() == null || !h.getDescription().contains("Review submitted")))
            .collect(Collectors.toList());
        
        // Convert to DTO
        ContractDetailDTO dto = new ContractDetailDTO();
        dto.setInternalId(contract.getId());
        dto.setId(generateMSAId(contract));
        dto.setContractType("MSA");
        dto.setContractName(contract.getContractName());
        dto.setEffectiveStart(formatDate(contract.getPeriodStart()));
        dto.setEffectiveEnd(formatDate(contract.getPeriodEnd()));
        dto.setStatus(contract.getStatus().name().replace("_", " "));
        
        // Commercial Terms (from contract entity if available, otherwise defaults)
        dto.setCurrency(contract.getCurrency() != null ? contract.getCurrency() : "JPY");
        dto.setPaymentTerms(contract.getPaymentTerms() != null ? contract.getPaymentTerms() : "Net");
        dto.setInvoicingCycle(contract.getInvoicingCycle() != null ? contract.getInvoicingCycle() : "Monthly");
        dto.setBillingDay(contract.getBillingDay() != null ? contract.getBillingDay() : "Last business day");
        dto.setTaxWithholding(contract.getTaxWithholding() != null ? contract.getTaxWithholding() : "10%");
        dto.setTaxType(contract.getTaxType() != null ? contract.getTaxType() : "Included");
        
        // Legal / Compliance
        dto.setIpOwnership(contract.getIpOwnership() != null ? contract.getIpOwnership() : "Client");
        dto.setGoverningLaw(contract.getGoverningLaw() != null ? contract.getGoverningLaw() : "JP");
        
        // Contacts
        ContractDetailDTO.ContactInfo clientContact = new ContractDetailDTO.ContactInfo();
        clientContact.setName(client.getFullName() != null ? client.getFullName() : client.getEmail());
        clientContact.setEmail(client.getEmail());
        dto.setClientContact(clientContact);
        
        ContractDetailDTO.ContactInfo landbridgeContact = new ContractDetailDTO.ContactInfo();
        landbridgeContact.setName(landbridgeContactName);
        landbridgeContact.setEmail(landbridgeContactEmail);
        dto.setLandbridgeContact(landbridgeContact);
        
        // Load attachments from attachments_manifest
        List<ContractDetailDTO.AttachmentDTO> attachments = new ArrayList<>();
        
        // First, try to load from attachments_manifest
        if (contract.getAttachmentsManifest() != null && !contract.getAttachmentsManifest().trim().isEmpty()) {
            try {
                // Try to parse as List<AttachmentInfo> (new format with fileName)
                Type attachmentInfoListType = new TypeToken<List<AttachmentInfo>>(){}.getType();
                List<AttachmentInfo> attachmentInfos = gson.fromJson(contract.getAttachmentsManifest(), attachmentInfoListType);
                if (attachmentInfos != null && !attachmentInfos.isEmpty()) {
                    for (AttachmentInfo info : attachmentInfos) {
                        if (info.getS3Key() != null && !info.getS3Key().trim().isEmpty()) {
                            attachments.add(new ContractDetailDTO.AttachmentDTO(info.getS3Key(), info.getFileName(), null));
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
                                attachments.add(new ContractDetailDTO.AttachmentDTO(s3Key, fileName, null));
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
                                attachments.add(new ContractDetailDTO.AttachmentDTO(s3Key, fileName, null));
                            }
                        }
                    }
                } catch (Exception e2) {
                    System.err.println("Error parsing attachments_manifest for contract " + contract.getId() + ": " + e2.getMessage());
                }
            }
        }
        
        // If no attachments from manifest, try link
        if (attachments.isEmpty() && contract.getLink() != null && !contract.getLink().trim().isEmpty()) {
            String fileName = contract.getLink();
            if (fileName.contains("/")) {
                fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
            }
            attachments.add(new ContractDetailDTO.AttachmentDTO(contract.getLink(), fileName, null));
        }
        
        // Fallback: Check DocumentMetadata
        if (attachments.isEmpty()) {
            List<DocumentMetadata> documents = documentMetadataRepository.findByEntityIdAndEntityType(
                contract.getId(), "msa_contract");
            attachments = documents.stream()
                .map(doc -> {
                    String fileName = doc.getS3Key();
                    if (fileName != null && fileName.contains("/")) {
                        fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                    }
                    return new ContractDetailDTO.AttachmentDTO(
                        doc.getS3Key(),
                        fileName,
                        null
                    );
                })
                .collect(Collectors.toList());
        }
        
        dto.setAttachments(attachments);
        
        // History
        List<ContractHistoryItemDTO> historyDTOs = history.stream()
            .map(this::convertToHistoryDTO)
            .collect(Collectors.toList());
        dto.setHistory(historyDTOs);
        
        return dto;
    }
    
    /**
     * Get all versions of a SOW contract for client
     */
    public List<ContractDetailDTO> getSOWContractVersions(Integer contractId, Integer clientUserId) {
        SOWContract contract = sowContractRepository.findByIdAndClientId(contractId, clientUserId)
            .orElseThrow(() -> new EntityNotFoundException("SOW Contract not found"));
        
        // Find the original contract ID (if this is a version, find the original)
        Integer originalContractId = contract.getParentVersionId() != null ? contract.getParentVersionId() : contractId;
        
        // Get all versions
        List<SOWContract> versions = sowContractRepository.findAllVersionsByParentVersionId(originalContractId);
        
        // Filter to only include contracts that belong to this client
        versions = versions.stream()
            .filter(v -> v.getClientId().equals(clientUserId))
            .collect(Collectors.toList());
        
        // If no versions found, return just the current contract
        if (versions.isEmpty()) {
            versions = java.util.Collections.singletonList(contract);
        }
        
        // Convert to DTOs
        List<ContractDetailDTO> versionDTOs = new ArrayList<>();
        for (SOWContract version : versions) {
            ContractDetailDTO dto = getSOWDetail(version, clientUserId);
            versionDTOs.add(dto);
        }
        
        return versionDTOs;
    }
    
    /**
     * Get SOW contract detail (Fixed Price or Retainer)
     */
    private ContractDetailDTO getSOWDetail(SOWContract sow, Integer clientUserId) {
        // Load client contact information
        User client = userRepository.findById(sow.getClientId())
            .orElseThrow(() -> new EntityNotFoundException("Client not found"));
        
        // Get LandBridge contact
        String landbridgeContactName = sow.getLandbridgeContactName() != null 
            ? sow.getLandbridgeContactName()
            : (sow.getAssigneeId() != null ? sow.getAssigneeId() : "LandBridge Support");
        String landbridgeContactEmail = sow.getLandbridgeContactEmail() != null
            ? sow.getLandbridgeContactEmail()
            : "support@landbridge.co.jp";
        
        // Load contract history (for SOW contracts, use sow_contract_id)
        // Exclude internal review entries (REVIEWED type or description containing "Review submitted")
        List<ContractHistory> allHistory = contractHistoryRepository.findBySowContractIdOrderByEntryDateDesc(sow.getId());
        List<ContractHistory> history = allHistory.stream()
            .filter(h -> !"REVIEWED".equals(h.getHistoryType()) 
                && (h.getDescription() == null || !h.getDescription().contains("Review submitted")))
            .collect(Collectors.toList());
        
        // Load change requests (for SOW contracts, use sow_contract_id)
        List<ChangeRequest> changeRequests = changeRequestRepository.findBySowContractIdOrderByCreatedAtDesc(sow.getId());
        
        // Convert to DTO
        ContractDetailDTO dto = new ContractDetailDTO();
        dto.setInternalId(sow.getId());
        dto.setId(generateSOWId(sow));
        dto.setContractType("SOW");
        dto.setContractName(sow.getContractName());
        dto.setEffectiveStart(formatDate(sow.getPeriodStart()));
        dto.setEffectiveEnd(formatDate(sow.getPeriodEnd()));
        dto.setStatus(sow.getStatus().name().replace("_", " "));
        
        // SOW specific fields
        String engagementType = sow.getEngagementType() != null ? sow.getEngagementType().replace("_", " ") : "Fixed Price";
        dto.setEngagementType(engagementType);
        dto.setProjectName(sow.getProjectName());
        dto.setScopeSummary(sow.getScopeSummary());
        
        // Parent MSA info
        if (sow.getParentMsaId() != null) {
            Contract parentMSA = contractRepository.findById(sow.getParentMsaId()).orElse(null);
            if (parentMSA != null) {
                ContractDetailDTO.ParentMSAInfo parentMSAInfo = new ContractDetailDTO.ParentMSAInfo();
                parentMSAInfo.setId(generateMSAId(parentMSA));
                parentMSAInfo.setStatus(parentMSA.getStatus().name().replace("_", " "));
                dto.setParentMSA(parentMSAInfo);
            }
        }
        
        // Commercial Terms
        dto.setCurrency(sow.getCurrency() != null ? sow.getCurrency() : "JPY");
        dto.setPaymentTerms(sow.getPaymentTerms() != null ? sow.getPaymentTerms() : "Net");
        dto.setInvoicingCycle(sow.getInvoicingCycle() != null ? sow.getInvoicingCycle() : "Monthly");
        dto.setBillingDay(sow.getBillingDay() != null ? sow.getBillingDay() : "Last business day");
        dto.setTaxWithholding(sow.getTaxWithholding() != null ? sow.getTaxWithholding() : "10%");
        
        // Legal / Compliance
        dto.setIpOwnership(sow.getIpOwnership() != null ? sow.getIpOwnership() : "Client");
        dto.setGoverningLaw(sow.getGoverningLaw() != null ? sow.getGoverningLaw() : "JP");
        
        // Contacts
        ContractDetailDTO.ContactInfo clientContact = new ContractDetailDTO.ContactInfo();
        clientContact.setName(client.getFullName() != null ? client.getFullName() : client.getEmail());
        clientContact.setEmail(client.getEmail());
        dto.setClientContact(clientContact);
        
        ContractDetailDTO.ContactInfo landbridgeContact = new ContractDetailDTO.ContactInfo();
        landbridgeContact.setName(landbridgeContactName);
        landbridgeContact.setEmail(landbridgeContactEmail);
        dto.setLandbridgeContact(landbridgeContact);
        
        // SOW type specific data
        // Load from milestone_deliverables, fixed_price_billing_details, delivery_items, retainer_billing_details
        // These tables now reference sow_contracts directly (after V19 migration)
        String engagementTypeStr = sow.getEngagementType() != null ? sow.getEngagementType() : "";
        if (engagementTypeStr.equals("Fixed_Price") || engagementTypeStr.equals("Fixed Price")) {
            // Load milestone deliverables
            List<MilestoneDeliverable> milestones = milestoneDeliverableRepository.findBySowContractIdOrderByPlannedEndAsc(sow.getId());
            List<MilestoneDeliverableDTO> milestoneDTOs = milestones.stream()
                .map(this::convertToMilestoneDTO)
                .collect(Collectors.toList());
            dto.setMilestones(milestoneDTOs);
            
            // Load billing details
            List<FixedPriceBillingDetail> billingDetails = fixedPriceBillingDetailRepository.findBySowContractIdOrderByInvoiceDateDesc(sow.getId());
            List<FixedPriceBillingDetailDTO> billingDetailDTOs = billingDetails.stream()
                .map(this::convertToFixedPriceBillingDTO)
                .collect(Collectors.toList());
            dto.setBillingDetails(billingDetailDTOs);
        } else if (engagementTypeStr.equals("Retainer")) {
            // Load engaged engineers
            List<SOWEngagedEngineer> engagedEngineers = sowEngagedEngineerRepository.findBySowContractIdOrderByStartDateAsc(sow.getId());
            List<ContractDetailDTO.EngagedEngineerDTO> engagedEngineerDTOs = engagedEngineers.stream()
                .map(this::convertToEngagedEngineerDTO)
                .collect(Collectors.toList());
            dto.setEngagedEngineers(engagedEngineerDTOs);
            
            // Load delivery items
            List<DeliveryItem> deliveryItems = deliveryItemRepository.findBySowContractIdOrderByPaymentDateDesc(sow.getId());
            List<DeliveryItemDTO> deliveryItemDTOs = deliveryItems.stream()
                .map(this::convertToDeliveryItemDTO)
                .collect(Collectors.toList());
            dto.setDeliveryItems(deliveryItemDTOs);
            
            // Load billing details - Use event-based calculation if baseline exists, otherwise fallback to legacy table
            List<RetainerBillingDetailDTO> billingDetailDTOs = new ArrayList<>();
            
            // Try event-based approach first (if baseline exists)
            if (sowBaselineService != null && crEventService != null && crBillingEventRepository != null) {
                try {
                    List<com.skillbridge.entity.contract.SOWEngagedEngineerBase> baselineEngineers = 
                        sowBaselineService.getBaselineResources(sow.getId());
                    
                    if (!baselineEngineers.isEmpty()) {
                        // Event-based: Calculate current billing from baseline + events
                        List<com.skillbridge.entity.contract.RetainerBillingBase> baselineBillingList = 
                            sowBaselineService.getBaselineBilling(sow.getId());
                        
                        // Get all unique billing months
                        java.util.Set<LocalDate> billingMonths = new java.util.HashSet<>();
                        for (com.skillbridge.entity.contract.RetainerBillingBase base : baselineBillingList) {
                            if (base.getBillingMonth() != null) {
                                billingMonths.add(base.getBillingMonth());
                            }
                        }
                        
                        // Get all billing events to find additional months
                        List<com.skillbridge.entity.contract.CRBillingEvent> allBillingEvents = 
                            crBillingEventRepository.findApprovedEventsBySowContractId(sow.getId());
                        for (com.skillbridge.entity.contract.CRBillingEvent event : allBillingEvents) {
                            if (event.getBillingMonth() != null) {
                                billingMonths.add(event.getBillingMonth());
                            }
                        }
                        
                        // Calculate billing for each month (baseline + events)
                        for (LocalDate month : billingMonths.stream().sorted().collect(Collectors.toList())) {
                            BigDecimal totalAmount = crEventService.calculateCurrentBilling(sow.getId(), month);
                            
                            RetainerBillingDetailDTO billingDTO = new RetainerBillingDetailDTO();
                            billingDTO.setId(null); // No specific ID for calculated billing
                            billingDTO.setPaymentDate(month != null ? formatDate(month) : null);
                            billingDTO.setAmount(formatCurrency(totalAmount != null ? totalAmount : BigDecimal.ZERO));
                            
                            // Get description from baseline or events
                            String description = "";
                            var baselineOpt = baselineBillingList.stream()
                                .filter(b -> b.getBillingMonth() != null && b.getBillingMonth().equals(month))
                                .findFirst();
                            if (baselineOpt.isPresent() && baselineOpt.get().getDescription() != null) {
                                description = baselineOpt.get().getDescription();
                            }
                            
                            // Add event descriptions
                            List<com.skillbridge.entity.contract.CRBillingEvent> monthEvents = allBillingEvents.stream()
                                .filter(e -> e.getBillingMonth() != null && e.getBillingMonth().equals(month))
                                .collect(Collectors.toList());
                            if (!monthEvents.isEmpty()) {
                                if (!description.isEmpty()) description += "; ";
                                description += monthEvents.stream()
                                    .map(e -> e.getDescription() != null ? e.getDescription() : "")
                                    .collect(Collectors.joining("; "));
                            }
                            
                            billingDTO.setDeliveryNote(description);
                            billingDetailDTOs.add(billingDTO);
                        }
                    }
                } catch (Exception e) {
                    // If event-based calculation fails, fallback to legacy table
                    System.err.println("Error calculating event-based billing for contract " + sow.getId() + ": " + e.getMessage());
                }
            }
            
            // Fallback to legacy table if event-based calculation didn't produce results
            if (billingDetailDTOs.isEmpty()) {
            List<RetainerBillingDetail> billingDetails = retainerBillingDetailRepository.findBySowContractIdOrderByPaymentDateDesc(sow.getId());
                billingDetailDTOs = billingDetails.stream()
                .map(this::convertToRetainerBillingDTO)
                .collect(Collectors.toList());
            }
            
            dto.setRetainerBillingDetails(billingDetailDTOs);
        }
        
        // Change Requests
        List<ChangeRequestDTO> changeRequestDTOs = changeRequests.stream()
            .map(this::convertToChangeRequestDTO)
            .collect(Collectors.toList());
        dto.setChangeRequests(changeRequestDTOs);
        
        // Load attachments from attachments_manifest
        List<ContractDetailDTO.AttachmentDTO> attachments = new ArrayList<>();
        
        // First, try to load from attachments_manifest
        if (sow.getAttachmentsManifest() != null && !sow.getAttachmentsManifest().trim().isEmpty()) {
            try {
                // Try to parse as List<AttachmentInfo> (new format with fileName)
                Type attachmentInfoListType = new TypeToken<List<AttachmentInfo>>(){}.getType();
                List<AttachmentInfo> attachmentInfos = gson.fromJson(sow.getAttachmentsManifest(), attachmentInfoListType);
                if (attachmentInfos != null && !attachmentInfos.isEmpty()) {
                    for (AttachmentInfo info : attachmentInfos) {
                        if (info.getS3Key() != null && !info.getS3Key().trim().isEmpty()) {
                            attachments.add(new ContractDetailDTO.AttachmentDTO(info.getS3Key(), info.getFileName(), null));
                        }
                    }
                } else {
                    // Fallback: try to parse as List<String> (old format)
                    Type stringListType = new TypeToken<List<String>>(){}.getType();
                    List<String> attachmentLinks = gson.fromJson(sow.getAttachmentsManifest(), stringListType);
                    if (attachmentLinks != null && !attachmentLinks.isEmpty()) {
                        for (String s3Key : attachmentLinks) {
                            if (s3Key != null && !s3Key.trim().isEmpty()) {
                                String fileName = s3Key;
                                if (fileName.contains("/")) {
                                    fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                                }
                                attachments.add(new ContractDetailDTO.AttachmentDTO(s3Key, fileName, null));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // If parsing as AttachmentInfo fails, try List<String> (old format)
                try {
                    Type stringListType = new TypeToken<List<String>>(){}.getType();
                    List<String> attachmentLinks = gson.fromJson(sow.getAttachmentsManifest(), stringListType);
                    if (attachmentLinks != null && !attachmentLinks.isEmpty()) {
                        for (String s3Key : attachmentLinks) {
                            if (s3Key != null && !s3Key.trim().isEmpty()) {
                                String fileName = s3Key;
                                if (fileName.contains("/")) {
                                    fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                                }
                                attachments.add(new ContractDetailDTO.AttachmentDTO(s3Key, fileName, null));
                            }
                        }
                    }
                } catch (Exception e2) {
                    System.err.println("Error parsing attachments_manifest for SOW contract " + sow.getId() + ": " + e2.getMessage());
                }
            }
        }
        
        // If no attachments from manifest, try link
        if (attachments.isEmpty() && sow.getLink() != null && !sow.getLink().trim().isEmpty()) {
            String fileName = sow.getLink();
            if (fileName.contains("/")) {
                fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
            }
            attachments.add(new ContractDetailDTO.AttachmentDTO(sow.getLink(), fileName, null));
        }
        
        // Fallback: Check DocumentMetadata
        if (attachments.isEmpty()) {
            List<DocumentMetadata> documents = documentMetadataRepository.findByEntityIdAndEntityType(
                sow.getId(), "sow_contract");
            attachments = documents.stream()
                .map(doc -> {
                    String fileName = doc.getS3Key();
                    if (fileName != null && fileName.contains("/")) {
                        fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                    }
                    return new ContractDetailDTO.AttachmentDTO(
                        doc.getS3Key(),
                        fileName,
                        null
                    );
                })
                .collect(Collectors.toList());
        }
        
        dto.setAttachments(attachments);
        
        // History
        List<ContractHistoryItemDTO> historyDTOs = history.stream()
            .map(this::convertToHistoryDTO)
            .collect(Collectors.toList());
        dto.setHistory(historyDTOs);
        
        return dto;
    }
    
    /**
     * Approve contract (MSA or SOW)
     */
    @Transactional
    public void approveContract(Integer contractId, Integer clientUserId) {
        // Try MSA first
        Contract msaContract = contractRepository.findByIdAndClientId(contractId, clientUserId).orElse(null);
        if (msaContract != null) {
            approveMSAContract(msaContract, contractId, clientUserId);
            return;
        }
        
        // Try SOW
        SOWContract sowContract = sowContractRepository.findByIdAndClientId(contractId, clientUserId)
            .orElseThrow(() -> new EntityNotFoundException("Contract not found"));
        approveSOWContract(sowContract, contractId, clientUserId);
    }
    
    private void approveMSAContract(Contract contract, Integer contractId, Integer clientUserId) {
        // Validate contract can be approved
        if (contract.getStatus() == Contract.ContractStatus.Active) {
            throw new IllegalStateException("Contract is already approved");
        }
        if (contract.getStatus() == Contract.ContractStatus.Terminated || 
            contract.getStatus() == Contract.ContractStatus.Completed) {
            throw new IllegalStateException("Contract cannot be approved in current status");
        }
        
        // Get old status for history
        Contract.ContractStatus oldStatus = contract.getStatus();
        
        // Update contract status to Active when client approves
        contract.setStatus(Contract.ContractStatus.Active);
        contractRepository.save(contract);
        
        // Create history entry with status change
        ContractHistory history = new ContractHistory();
        history.setContractId(contractId);
        history.setEntryDate(LocalDate.now());
        history.setDescription(String.format("Status changed from %s to Active by client", oldStatus.name().replace("_", " ")));
        history.setCreatedBy(clientUserId);
        history.setHistoryType("MSA");
        contractHistoryRepository.save(history);
    }
    
    private void approveSOWContract(SOWContract contract, Integer contractId, Integer clientUserId) {
        // Validate contract can be approved
        if (contract.getStatus() == SOWContract.SOWContractStatus.Active) {
            throw new IllegalStateException("Contract is already approved");
        }
        if (contract.getStatus() == SOWContract.SOWContractStatus.Terminated || 
            contract.getStatus() == SOWContract.SOWContractStatus.Completed) {
            throw new IllegalStateException("Contract cannot be approved in current status");
        }
        
        // Get old status for history
        SOWContract.SOWContractStatus oldStatus = contract.getStatus();
        
        // Update contract status to Active when client approves
        contract.setStatus(SOWContract.SOWContractStatus.Active);
        sowContractRepository.save(contract);
        
        // Create history entry with status change
        ContractHistory history = new ContractHistory();
        history.setSowContractId(contractId);
        history.setEntryDate(LocalDate.now());
        history.setDescription(String.format("Status changed from %s to Active by client", oldStatus.name().replace("_", " ")));
        history.setCreatedBy(clientUserId);
        history.setHistoryType("SOW");
        contractHistoryRepository.save(history);
    }
    
    /**
     * Add comment to contract (Request for Change) - MSA or SOW
     */
    @Transactional
    public void addComment(Integer contractId, Integer clientUserId, String comment) {
        // Try MSA first
        Contract msaContract = contractRepository.findByIdAndClientId(contractId, clientUserId).orElse(null);
        if (msaContract != null) {
            addCommentToMSA(msaContract, contractId, clientUserId, comment);
            return;
        }
        
        // Try SOW
        SOWContract sowContract = sowContractRepository.findByIdAndClientId(contractId, clientUserId)
            .orElseThrow(() -> new EntityNotFoundException("Contract not found"));
        addCommentToSOW(sowContract, contractId, clientUserId, comment);
    }
    
    private void addCommentToMSA(Contract contract, Integer contractId, Integer clientUserId, String comment) {
        // Get old status for history
        Contract.ContractStatus oldStatus = contract.getStatus();
        
        // Clear internal review records so UI does not show Client Under Review after RFC
        contractInternalReviewRepository.deleteByContractIdAndContractType(contractId, "MSA");
        
        // Update contract status to Request_for_Change
        contract.setStatus(Contract.ContractStatus.Request_for_Change);
        contractRepository.save(contract);
        
        // Create history entry with comment and status change
        ContractHistory history = new ContractHistory();
        history.setContractId(contractId);
        history.setEntryDate(LocalDate.now());
        history.setDescription(String.format("Status changed from %s to Request for Change. Comment: %s", 
            oldStatus.name().replace("_", " "), comment));
        history.setCreatedBy(clientUserId);
        history.setHistoryType("MSA");
        contractHistoryRepository.save(history);
    }
    
    private void addCommentToSOW(SOWContract contract, Integer contractId, Integer clientUserId, String comment) {
        // Get old status for history
        SOWContract.SOWContractStatus oldStatus = contract.getStatus();
        
        // Clear internal review records so UI does not show Client Under Review after RFC
        contractInternalReviewRepository.deleteBySowContractIdAndContractType(contractId, "SOW");
        
        // Update contract status to Request_for_Change
        contract.setStatus(SOWContract.SOWContractStatus.Request_for_Change);
        sowContractRepository.save(contract);
        
        // Create history entry with comment and status change
        ContractHistory history = new ContractHistory();
        history.setSowContractId(contractId);
        history.setEntryDate(LocalDate.now());
        history.setDescription(String.format("Status changed from %s to Request for Change. Comment: %s", 
            oldStatus.name().replace("_", " "), comment));
        history.setCreatedBy(clientUserId);
        history.setHistoryType("SOW");
        contractHistoryRepository.save(history);
    }
    
    /**
     * Cancel contract (MSA or SOW)
     */
    @Transactional
    public void cancelContract(Integer contractId, Integer clientUserId, String reason) {
        // Try MSA first
        Contract msaContract = contractRepository.findByIdAndClientId(contractId, clientUserId).orElse(null);
        if (msaContract != null) {
            cancelMSAContract(msaContract, contractId, clientUserId, reason);
            return;
        }
        
        // Try SOW
        SOWContract sowContract = sowContractRepository.findByIdAndClientId(contractId, clientUserId)
            .orElseThrow(() -> new EntityNotFoundException("Contract not found"));
        cancelSOWContract(sowContract, contractId, clientUserId, reason);
    }
    
    private void cancelMSAContract(Contract contract, Integer contractId, Integer clientUserId, String reason) {
        // Validate contract can be cancelled
        if (contract.getStatus() == Contract.ContractStatus.Terminated || 
            contract.getStatus() == Contract.ContractStatus.Cancelled) {
            throw new IllegalStateException("Contract is already cancelled/terminated");
        }
        if (contract.getStatus() == Contract.ContractStatus.Completed) {
            throw new IllegalStateException("Completed contract cannot be cancelled");
        }
        
        // Get old status for history
        Contract.ContractStatus oldStatus = contract.getStatus();
        
        // Update contract status based on current status
        Contract.ContractStatus newStatus;
        if (contract.getStatus() == Contract.ContractStatus.Active) {
            newStatus = Contract.ContractStatus.Pending;
        } else if (contract.getStatus() == Contract.ContractStatus.Under_Review) {
            newStatus = Contract.ContractStatus.Cancelled;
        } else {
            newStatus = Contract.ContractStatus.Cancelled; // Default
        }
        
        contract.setStatus(newStatus);
        contractRepository.save(contract);
        
        // Create history entry with cancellation reason and status change
        ContractHistory history = new ContractHistory();
        history.setContractId(contractId);
        history.setEntryDate(LocalDate.now());
        history.setDescription(String.format("Status changed from %s to %s. Reason: %s", 
            oldStatus.name().replace("_", " "), newStatus.name().replace("_", " "), reason));
        history.setCreatedBy(clientUserId);
        history.setHistoryType("MSA");
        contractHistoryRepository.save(history);
    }
    
    private void cancelSOWContract(SOWContract contract, Integer contractId, Integer clientUserId, String reason) {
        // Validate contract can be cancelled
        if (contract.getStatus() == SOWContract.SOWContractStatus.Terminated || 
            contract.getStatus() == SOWContract.SOWContractStatus.Cancelled) {
            throw new IllegalStateException("Contract is already cancelled/terminated");
        }
        if (contract.getStatus() == SOWContract.SOWContractStatus.Completed) {
            throw new IllegalStateException("Completed contract cannot be cancelled");
        }
        
        // Get old status for history
        SOWContract.SOWContractStatus oldStatus = contract.getStatus();
        
        // Update contract status based on current status
        SOWContract.SOWContractStatus newStatus;
        if (contract.getStatus() == SOWContract.SOWContractStatus.Active) {
            newStatus = SOWContract.SOWContractStatus.Pending;
        } else if (contract.getStatus() == SOWContract.SOWContractStatus.Under_Review) {
            newStatus = SOWContract.SOWContractStatus.Cancelled;
        } else {
            newStatus = SOWContract.SOWContractStatus.Cancelled; // Default
        }
        
        contract.setStatus(newStatus);
        sowContractRepository.save(contract);
        
        // Create history entry with cancellation reason and status change
        ContractHistory history = new ContractHistory();
        history.setSowContractId(contractId);
        history.setEntryDate(LocalDate.now());
        history.setDescription(String.format("Status changed from %s to %s. Reason: %s", 
            oldStatus.name().replace("_", " "), newStatus.name().replace("_", " "), reason));
        history.setCreatedBy(clientUserId);
        history.setHistoryType("SOW");
        contractHistoryRepository.save(history);
    }
    
    /**
     * Generate MSA contract display ID
     */
    private String generateMSAId(Contract contract) {
        int year = contract.getCreatedAt() != null ? contract.getCreatedAt().getYear() : 2025;
        return String.format("MSA-%d-%03d", year, contract.getId());
    }
    
    /**
     * Generate SOW contract display ID
     */
    private String generateSOWId(SOWContract sow) {
        int year = sow.getCreatedAt() != null ? sow.getCreatedAt().getYear() : 2025;
        return String.format("SOW-%d-%03d", year, sow.getId());
    }
    
    /**
     * Format date to YYYY/MM/DD
     */
    private String formatDate(LocalDate date) {
        if (date == null) return null;
        return String.format("%d/%d/%d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }
    
    /**
     * Convert ContractHistory to DTO
     */
    private ContractHistoryItemDTO convertToHistoryDTO(ContractHistory history) {
        ContractHistoryItemDTO dto = new ContractHistoryItemDTO();
        dto.setId(history.getId());
        dto.setDate(formatDate(history.getEntryDate()));
        dto.setDescription(history.getDescription());
        dto.setDocumentLink(history.getDocumentLink());
        dto.setDocumentName(history.getDocumentName());
        if (history.getCreatedBy() != null) {
            User user = userRepository.findById(history.getCreatedBy()).orElse(null);
            dto.setCreatedBy(user != null ? user.getFullName() : null);
        }
        return dto;
    }
    
    /**
     * Convert ChangeRequest to DTO
     */
    private ChangeRequestDTO convertToChangeRequestDTO(ChangeRequest changeRequest) {
        ChangeRequestDTO dto = new ChangeRequestDTO();
        dto.setId(changeRequest.getId());
        dto.setChangeRequestId(changeRequest.getChangeRequestId());
        dto.setType(changeRequest.getType());
        dto.setSummary(changeRequest.getSummary());
        dto.setPlannedEnd(formatDate(changeRequest.getPlannedEnd()));
        dto.setEffectiveFrom(formatDate(changeRequest.getEffectiveFrom()));
        dto.setEffectiveUntil(formatDate(changeRequest.getEffectiveUntil()));
        dto.setAmount(formatCurrency(changeRequest.getAmount()));
        dto.setStatus(changeRequest.getStatus());
        // Map cost fields (fallback to amount formatting)
        dto.setExpectedExtraCost(formatCurrency(changeRequest.getExpectedExtraCost()));
        dto.setCostEstimatedByLandbridge(formatCurrency(changeRequest.getCostEstimatedByLandbridge()));
        return dto;
    }
    
    /**
     * Format currency amount
     */
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return null;
        return String.format("¥%,d", amount.intValue());
    }
    
    /**
     * Convert MilestoneDeliverable to DTO
     */
    private MilestoneDeliverableDTO convertToMilestoneDTO(MilestoneDeliverable milestone) {
        MilestoneDeliverableDTO dto = new MilestoneDeliverableDTO();
        dto.setId(milestone.getId());
        dto.setMilestone(milestone.getMilestone());
        dto.setDeliveryNote(milestone.getDeliveryNote());
        dto.setAcceptanceCriteria(milestone.getAcceptanceCriteria());
        dto.setPlannedEnd(formatDate(milestone.getPlannedEnd()));
        if (milestone.getPaymentPercentage() != null) {
            dto.setPaymentPercentage(String.format("%.2f%%", milestone.getPaymentPercentage()));
        }
        return dto;
    }
    
    /**
     * Convert FixedPriceBillingDetail to DTO
     */
    private FixedPriceBillingDetailDTO convertToFixedPriceBillingDTO(FixedPriceBillingDetail billing) {
        FixedPriceBillingDetailDTO dto = new FixedPriceBillingDetailDTO();
        dto.setId(billing.getId());
        dto.setBillingName(billing.getBillingName());
        dto.setMilestone(billing.getMilestone());
        dto.setAmount(formatCurrency(billing.getAmount()));
        if (billing.getPercentage() != null) {
            dto.setPercentage(String.format("%.2f%%", billing.getPercentage()));
        }
        dto.setInvoiceDate(formatDate(billing.getInvoiceDate()));
        return dto;
    }
    
    /**
     * Convert DeliveryItem to DTO
     */
    private DeliveryItemDTO convertToDeliveryItemDTO(DeliveryItem item) {
        DeliveryItemDTO dto = new DeliveryItemDTO();
        dto.setId(item.getId());
        dto.setMilestone(item.getMilestone());
        dto.setDeliveryNote(item.getDeliveryNote());
        dto.setAmount(formatCurrency(item.getAmount()));
        dto.setPaymentDate(formatDate(item.getPaymentDate()));
        return dto;
    }
    
    /**
     * Convert RetainerBillingDetail to DTO
     */
    private ContractDetailDTO.EngagedEngineerDTO convertToEngagedEngineerDTO(SOWEngagedEngineer engineer) {
        ContractDetailDTO.EngagedEngineerDTO dto = new ContractDetailDTO.EngagedEngineerDTO();
        dto.setId(engineer.getId());
        dto.setEngineerLevel(engineer.getEngineerLevel());
        dto.setStartDate(formatDate(engineer.getStartDate()));
        dto.setEndDate(formatDate(engineer.getEndDate()));
        dto.setBillingType(engineer.getBillingType() != null ? engineer.getBillingType() : "Monthly");
        dto.setHourlyRate(engineer.getHourlyRate() != null ? engineer.getHourlyRate().doubleValue() : null);
        dto.setHours(engineer.getHours() != null ? engineer.getHours().doubleValue() : null);
        dto.setSubtotal(engineer.getSubtotal() != null ? engineer.getSubtotal().doubleValue() : null);
        dto.setRating(engineer.getRating() != null ? engineer.getRating().doubleValue() : null);
        dto.setSalary(engineer.getSalary() != null ? engineer.getSalary().doubleValue() : null);
        return dto;
    }
    
    private RetainerBillingDetailDTO convertToRetainerBillingDTO(RetainerBillingDetail billing) {
        RetainerBillingDetailDTO dto = new RetainerBillingDetailDTO();
        dto.setId(billing.getId());
        dto.setPaymentDate(formatDate(billing.getPaymentDate()));
        dto.setDeliveryNote(billing.getDeliveryNote());
        dto.setAmount(formatCurrency(billing.getAmount()));
        return dto;
    }
}

