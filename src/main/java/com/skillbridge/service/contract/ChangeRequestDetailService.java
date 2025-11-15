package com.skillbridge.service.contract;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillbridge.dto.contract.request.UpdateChangeRequestRequest;
import com.skillbridge.dto.contract.response.ChangeRequestDetailDTO;
import com.skillbridge.entity.auth.User;
import com.skillbridge.entity.contract.*;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.repository.contract.*;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Change Request Detail Service
 * Handles business logic for change request detail operations
 */
@Service
public class ChangeRequestDetailService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChangeRequestDetailService.class);
    
    @Autowired
    private ChangeRequestRepository changeRequestRepository;
    
    @Autowired
    private ChangeRequestAttachmentRepository changeRequestAttachmentRepository;
    
    @Autowired
    private ChangeRequestHistoryRepository changeRequestHistoryRepository;
    
    @Autowired
    private ChangeRequestEngagedEngineerRepository engagedEngineerRepository;
    
    @Autowired
    private ChangeRequestBillingDetailRepository billingDetailRepository;
    
    @Autowired
    private ContractRepository contractRepository;
    
    @Autowired
    private SOWContractRepository sowContractRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Get change request detail
     */
    public ChangeRequestDetailDTO getChangeRequestDetail(
        Integer contractId,
        Integer changeRequestId,
        Integer clientUserId
    ) {
        // Validate contract belongs to user
        Contract msaContract = contractRepository.findByIdAndClientId(contractId, clientUserId).orElse(null);
        SOWContract sowContract = null;
        
        if (msaContract == null) {
            sowContract = sowContractRepository.findByIdAndClientId(contractId, clientUserId)
                .orElseThrow(() -> new EntityNotFoundException("Contract not found or access denied"));
        }
        
        // Find change request
        ChangeRequest changeRequest = changeRequestRepository.findById(changeRequestId)
            .orElseThrow(() -> new EntityNotFoundException("Change request not found"));
        
        // Validate change request belongs to contract
        if (msaContract != null) {
            if (!changeRequest.getContractId().equals(contractId) || !"MSA".equals(changeRequest.getContractType())) {
                throw new EntityNotFoundException("Change request does not belong to this contract");
            }
        } else {
            if (!changeRequest.getSowContractId().equals(contractId) || !"SOW".equals(changeRequest.getContractType())) {
                throw new EntityNotFoundException("Change request does not belong to this contract");
            }
        }
        
        // Get creator name
        String createdBy = "Unknown";
        if (changeRequest.getCreatedBy() != null) {
            Optional<User> creator = userRepository.findById(changeRequest.getCreatedBy());
            if (creator.isPresent()) {
                createdBy = creator.get().getFullName();
            }
        }
        
        List<ChangeRequestHistory> history = changeRequestHistoryRepository.findByChangeRequestIdOrderByTimestampDesc(changeRequestId);
        
        // Build DTO
        ChangeRequestDetailDTO dto = new ChangeRequestDetailDTO();
        dto.setId(changeRequest.getId());
        dto.setChangeRequestId(changeRequest.getChangeRequestId());
        dto.setTitle(changeRequest.getTitle());
        dto.setType(changeRequest.getType());
        dto.setDescription(changeRequest.getDescription());
        dto.setReason(changeRequest.getReason());
        dto.setStatus(changeRequest.getStatus());
        dto.setCreatedBy(createdBy);
        dto.setCreatedDate(changeRequest.getCreatedAt() != null 
            ? changeRequest.getCreatedAt().format(DATE_FORMATTER) 
            : "");
        dto.setDesiredStartDate(changeRequest.getDesiredStartDate() != null 
            ? changeRequest.getDesiredStartDate().format(DATE_FORMATTER) 
            : "");
        dto.setDesiredEndDate(changeRequest.getDesiredEndDate() != null 
            ? changeRequest.getDesiredEndDate().format(DATE_FORMATTER) 
            : "");
        dto.setExpectedExtraCost(formatCurrency(changeRequest.getExpectedExtraCost()));
        
        // Parse evidence (JSON array)
        dto.setEvidence(parseEvidence(changeRequest.getEvidence()));
        
        // Get attachments
        List<ChangeRequestAttachment> attachments = changeRequestAttachmentRepository.findByChangeRequestId(changeRequestId);
        dto.setAttachments(attachments.stream()
            .map(this::convertToAttachmentDTO)
            .collect(Collectors.toList()));
        
        // Get history
        dto.setHistory(history.stream()
            .map(this::convertToHistoryDTO)
            .collect(Collectors.toList()));
        
        // Get impact analysis
        String engagementType = sowContract != null ? sowContract.getEngagementType() : null;
        dto.setImpactAnalysis(getImpactAnalysis(changeRequest, engagementType));
        
        return dto;
    }
    
    /**
     * Update change request (Draft status only)
     */
    @Transactional
    public void updateChangeRequest(
        Integer contractId,
        Integer changeRequestId,
        Integer clientUserId,
        UpdateChangeRequestRequest request
    ) {
        // Validate contract belongs to user
        Contract msaContract = contractRepository.findByIdAndClientId(contractId, clientUserId).orElse(null);
        SOWContract sowContract = null;
        
        if (msaContract == null) {
            sowContract = sowContractRepository.findByIdAndClientId(contractId, clientUserId)
                .orElseThrow(() -> new EntityNotFoundException("Contract not found or access denied"));
        }
        
        // Find change request
        ChangeRequest changeRequest = changeRequestRepository.findById(changeRequestId)
            .orElseThrow(() -> new EntityNotFoundException("Change request not found"));
        
        // Validate change request belongs to contract
        if (msaContract != null) {
            if (!changeRequest.getContractId().equals(contractId) || !"MSA".equals(changeRequest.getContractType())) {
                throw new EntityNotFoundException("Change request does not belong to this contract");
            }
        } else {
            if (!changeRequest.getSowContractId().equals(contractId) || !"SOW".equals(changeRequest.getContractType())) {
                throw new EntityNotFoundException("Change request does not belong to this contract");
            }
        }
        
        // Validate status is Draft
        if (!"Draft".equals(changeRequest.getStatus())) {
            throw new IllegalStateException("Only Draft change requests can be updated");
        }
        
        // Update fields
        if (request.getTitle() != null) {
            changeRequest.setTitle(request.getTitle());
        }
        if (request.getType() != null) {
            changeRequest.setType(request.getType());
        }
        if (request.getDescription() != null) {
            changeRequest.setDescription(request.getDescription());
        }
        if (request.getReason() != null) {
            changeRequest.setReason(request.getReason());
        }
        if (request.getDesiredStartDate() != null) {
            changeRequest.setDesiredStartDate(request.getDesiredStartDate());
        }
        if (request.getDesiredEndDate() != null) {
            changeRequest.setDesiredEndDate(request.getDesiredEndDate());
        }
        if (request.getExpectedExtraCost() != null) {
            changeRequest.setExpectedExtraCost(request.getExpectedExtraCost());
        }
        
        // Update summary
        if (request.getTitle() != null) {
            changeRequest.setSummary(request.getTitle());
        }
        
        // Save
        changeRequestRepository.save(changeRequest);
    }
    
    /**
     * Submit change request (Draft -> Under Review)
     */
    @Transactional
    public void submitChangeRequest(
        Integer contractId,
        Integer changeRequestId,
        Integer clientUserId
    ) {
        ChangeRequest changeRequest = validateAndGetChangeRequest(contractId, changeRequestId, clientUserId);
        
        // Validate status is Draft
        if (!"Draft".equals(changeRequest.getStatus())) {
            throw new IllegalStateException("Only Draft change requests can be submitted");
        }
        
        // Change status to "Under Review"
        changeRequest.setStatus("Under Review");
        changeRequestRepository.save(changeRequest);
        
        // Log history
        logHistory(changeRequestId, "Submitted", clientUserId);
    }
    
    /**
     * Approve change request (Under Review -> Active)
     */
    @Transactional
    public void approveChangeRequest(
        Integer contractId,
        Integer changeRequestId,
        Integer clientUserId
    ) {
        ChangeRequest changeRequest = validateAndGetChangeRequest(contractId, changeRequestId, clientUserId);
        
        // Validate status is "Under Review"
        if (!"Under Review".equals(changeRequest.getStatus())) {
            throw new IllegalStateException("Only Under Review change requests can be approved");
        }
        
        // Change status to "Active"
        changeRequest.setStatus("Active");
        changeRequestRepository.save(changeRequest);
        
        // Log history
        logHistory(changeRequestId, "Approved", clientUserId);
    }
    
    /**
     * Request for change (Under Review -> Request for Change)
     */
    @Transactional
    public void requestForChange(
        Integer contractId,
        Integer changeRequestId,
        Integer clientUserId,
        String message
    ) {
        ChangeRequest changeRequest = validateAndGetChangeRequest(contractId, changeRequestId, clientUserId);
        
        // Validate status is "Under Review"
        if (!"Under Review".equals(changeRequest.getStatus())) {
            throw new IllegalStateException("Only Under Review change requests can be requested for change");
        }
        
        // Change status to "Request for Change"
        changeRequest.setStatus("Request for Change");
        changeRequestRepository.save(changeRequest);
        
        // Log history with message if provided
        String action = message != null && !message.trim().isEmpty()
            ? String.format("Request for Change: %s", message)
            : "Request for Change";
        logHistory(changeRequestId, action, clientUserId);
    }
    
    /**
     * Terminate change request
     */
    @Transactional
    public void terminateChangeRequest(
        Integer contractId,
        Integer changeRequestId,
        Integer clientUserId
    ) {
        ChangeRequest changeRequest = validateAndGetChangeRequest(contractId, changeRequestId, clientUserId);
        
        // Change status to "Terminated"
        changeRequest.setStatus("Terminated");
        changeRequestRepository.save(changeRequest);
        
        // Log history
        logHistory(changeRequestId, "Terminated", clientUserId);
    }
    
    /**
     * Validate and get change request
     */
    private ChangeRequest validateAndGetChangeRequest(Integer contractId, Integer changeRequestId, Integer clientUserId) {
        // Validate contract belongs to user
        Contract msaContract = contractRepository.findByIdAndClientId(contractId, clientUserId).orElse(null);
        SOWContract sowContract = null;
        
        if (msaContract == null) {
            sowContract = sowContractRepository.findByIdAndClientId(contractId, clientUserId)
                .orElseThrow(() -> new EntityNotFoundException("Contract not found or access denied"));
        }
        
        // Find change request
        ChangeRequest changeRequest = changeRequestRepository.findById(changeRequestId)
            .orElseThrow(() -> new EntityNotFoundException("Change request not found"));
        
        // Validate change request belongs to contract
        if (msaContract != null) {
            if (!changeRequest.getContractId().equals(contractId) || !"MSA".equals(changeRequest.getContractType())) {
                throw new EntityNotFoundException("Change request does not belong to this contract");
            }
        } else {
            if (!changeRequest.getSowContractId().equals(contractId) || !"SOW".equals(changeRequest.getContractType())) {
                throw new EntityNotFoundException("Change request does not belong to this contract");
            }
        }
        
        return changeRequest;
    }
    
    /**
     * Get impact analysis based on contract type
     */
    private ChangeRequestDetailDTO.ImpactAnalysisDTO getImpactAnalysis(ChangeRequest changeRequest, String engagementType) {
        ChangeRequestDetailDTO.ImpactAnalysisDTO impactAnalysis = new ChangeRequestDetailDTO.ImpactAnalysisDTO();
        
        if ("Fixed Price".equalsIgnoreCase(engagementType)) {
            // Fixed Price impact analysis
            impactAnalysis.setDevHours(changeRequest.getDevHours());
            impactAnalysis.setTestHours(changeRequest.getTestHours());
            impactAnalysis.setNewEndDate(changeRequest.getNewEndDate() != null 
                ? changeRequest.getNewEndDate().format(DATE_FORMATTER) 
                : "");
            impactAnalysis.setDelayDuration(changeRequest.getDelayDuration());
            impactAnalysis.setAdditionalCost(formatCurrency(changeRequest.getCostEstimatedByLandbridge()));
        } else if ("Retainer".equalsIgnoreCase(engagementType)) {
            // Retainer impact analysis
            List<ChangeRequestEngagedEngineer> engineers = engagedEngineerRepository.findByChangeRequestId(changeRequest.getId());
            impactAnalysis.setEngagedEngineers(engineers.stream()
                .map(this::convertToEngagedEngineerDTO)
                .collect(Collectors.toList()));
            
            List<ChangeRequestBillingDetail> billingDetails = billingDetailRepository.findByChangeRequestId(changeRequest.getId());
            impactAnalysis.setBillingDetails(billingDetails.stream()
                .map(this::convertToBillingDetailDTO)
                .collect(Collectors.toList()));
        }
        
        return impactAnalysis;
    }
    
    /**
     * Parse evidence JSON string to list
     */
    private List<ChangeRequestDetailDTO.EvidenceItemDTO> parseEvidence(String evidenceJson) {
        if (evidenceJson == null || evidenceJson.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            List<ChangeRequestDetailDTO.EvidenceItemDTO> evidence = objectMapper.readValue(
                evidenceJson, 
                new TypeReference<List<ChangeRequestDetailDTO.EvidenceItemDTO>>() {}
            );
            return evidence;
        } catch (Exception e) {
            logger.warn("Failed to parse evidence JSON: {}", evidenceJson, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Convert attachment to DTO
     */
    private ChangeRequestDetailDTO.AttachmentDTO convertToAttachmentDTO(ChangeRequestAttachment attachment) {
        ChangeRequestDetailDTO.AttachmentDTO dto = new ChangeRequestDetailDTO.AttachmentDTO();
        dto.setId(attachment.getId());
        dto.setFileName(attachment.getFileName());
        dto.setFilePath(attachment.getFilePath());
        dto.setFileSize(attachment.getFileSize());
        dto.setUploadedAt(attachment.getUploadedAt() != null 
            ? attachment.getUploadedAt().format(DATE_FORMATTER) 
            : "");
        return dto;
    }
    
    /**
     * Convert history to DTO
     */
    private ChangeRequestDetailDTO.HistoryItemDTO convertToHistoryDTO(ChangeRequestHistory history) {
        ChangeRequestDetailDTO.HistoryItemDTO dto = new ChangeRequestDetailDTO.HistoryItemDTO();
        dto.setId(history.getId());
        dto.setAction(history.getAction());
        dto.setUserName(history.getUserName());
        dto.setTimestamp(history.getTimestamp() != null 
            ? history.getTimestamp().format(DATE_TIME_FORMATTER) 
            : "");
        return dto;
    }
    
    /**
     * Convert engaged engineer to DTO
     */
    private ChangeRequestDetailDTO.ImpactAnalysisDTO.EngagedEngineerDTO convertToEngagedEngineerDTO(ChangeRequestEngagedEngineer engineer) {
        ChangeRequestDetailDTO.ImpactAnalysisDTO.EngagedEngineerDTO dto = 
            new ChangeRequestDetailDTO.ImpactAnalysisDTO.EngagedEngineerDTO();
        dto.setEngineerLevel(engineer.getEngineerLevel());
        dto.setStartDate(engineer.getStartDate() != null 
            ? engineer.getStartDate().format(DATE_FORMATTER) 
            : "");
        dto.setEndDate(engineer.getEndDate() != null 
            ? engineer.getEndDate().format(DATE_FORMATTER) 
            : "");
        dto.setRating(engineer.getRating() != null 
            ? engineer.getRating().toString() + "%" 
            : "");
        dto.setSalary(formatCurrency(engineer.getSalary()));
        return dto;
    }
    
    /**
     * Convert billing detail to DTO
     */
    private ChangeRequestDetailDTO.ImpactAnalysisDTO.BillingDetailDTO convertToBillingDetailDTO(ChangeRequestBillingDetail billing) {
        ChangeRequestDetailDTO.ImpactAnalysisDTO.BillingDetailDTO dto = 
            new ChangeRequestDetailDTO.ImpactAnalysisDTO.BillingDetailDTO();
        dto.setPaymentDate(billing.getPaymentDate() != null 
            ? billing.getPaymentDate().format(DATE_FORMATTER) 
            : "");
        dto.setDeliveryNote(billing.getDeliveryNote());
        dto.setAmount(formatCurrency(billing.getAmount()));
        return dto;
    }
    
    /**
     * Format currency
     */
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        return "¥" + amount.toPlainString();
    }
    
    /**
     * Log history entry
     */
    private void logHistory(Integer changeRequestId, String action, Integer userId) {
        // Get user name
        String userName = "Unknown";
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            userName = user.getFullName() != null ? user.getFullName() : user.getEmail();
        }
        
        // Create history entry
        ChangeRequestHistory history = new ChangeRequestHistory();
        history.setChangeRequestId(changeRequestId);
        history.setAction(action);
        history.setUserId(userId);
        history.setUserName(userName);
        history.setTimestamp(LocalDateTime.now());
        
        changeRequestHistoryRepository.save(history);
    }
}

