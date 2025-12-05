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
    private FixedPriceBillingDetailRepository fixedPriceBillingDetailRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private com.skillbridge.service.sales.SalesSOWContractService salesSOWContractService;
    
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
        
        // Get internal reviewer name
        String internalReviewerName = null;
        if (changeRequest.getInternalReviewerId() != null) {
            Optional<User> reviewer = userRepository.findById(changeRequest.getInternalReviewerId());
            if (reviewer.isPresent()) {
                internalReviewerName = reviewer.get().getFullName();
            }
        }
        dto.setInternalReviewerName(internalReviewerName);
        
        // Get review notes from history (look for "REVIEWED" action with notes)
        // Review notes are appended to the action string in format: "... Notes: <notes>"
        String reviewNotes = null;
        for (ChangeRequestHistory hist : history) {
            if (hist.getAction() != null && hist.getAction().contains("REVIEWED")) {
                String action = hist.getAction();
                if (action != null && action.contains("Notes:")) {
                    int notesIndex = action.indexOf("Notes:");
                    if (notesIndex >= 0) {
                        reviewNotes = action.substring(notesIndex + 6).trim();
                    }
                }
            }
        }
        dto.setReviewNotes(reviewNotes);
        
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
     * Approve change request (Under Review or Client Under Review -> Active)
     */
    @Transactional
    public void approveChangeRequest(
        Integer contractId,
        Integer changeRequestId,
        Integer clientUserId
    ) {
        ChangeRequest changeRequest = validateAndGetChangeRequest(contractId, changeRequestId, clientUserId);
        
        // Validate status is "Under Review" or "Client Under Review"
        if (!"Under Review".equals(changeRequest.getStatus()) && !"Client Under Review".equals(changeRequest.getStatus())) {
            throw new IllegalStateException("Only Under Review or Client Under Review change requests can be approved");
        }
        
        // For SOW contracts, handle based on engagement type
        // For Retainer SOW, we need to create version BEFORE changing status
        boolean isRetainerSOW = false;
        if ("SOW".equals(changeRequest.getContractType()) && changeRequest.getSowContractId() != null) {
            SOWContract sowContract = sowContractRepository.findById(changeRequest.getSowContractId())
                .orElse(null);
            
            if (sowContract != null) {
                String engagementType = sowContract.getEngagementType();
                
                // For Retainer SOW, create new version and apply changes BEFORE changing status
                if ("Retainer".equals(engagementType) || "Retainer_".equals(engagementType)) {
                    isRetainerSOW = true;
                    // Get client user for approval
                    User clientUser = userRepository.findById(clientUserId)
                        .orElseThrow(() -> new IllegalStateException("Client user not found"));
                    
                    // Use SalesSOWContractService to approve and create version
                    // This will create a new version and apply changes
                    // Note: approveChangeRequestForSOW now accepts "Under Review" and "Client Under Review" status
                    // IMPORTANT: Call this BEFORE changing status to "Active"
                    salesSOWContractService.approveChangeRequestForSOW(
                        changeRequest.getSowContractId(),
                        changeRequestId,
                        null, // No review notes from client approval
                        clientUser
                    );
                    
                    logger.info("Retainer SOW change request approved by client, new version created: CR-{}, SOW-{}", 
                        changeRequest.getChangeRequestId(), sowContract.getId());
                }
            }
        }
        
        // Change status to "Active" (after version creation for Retainer SOW)
        changeRequest.setStatus("Active");
        changeRequestRepository.save(changeRequest);
        
        // Log history
        logHistory(changeRequestId, "Approved", clientUserId);
        
        // For SOW contracts, handle based on engagement type (for non-Retainer)
        if ("SOW".equals(changeRequest.getContractType()) && changeRequest.getSowContractId() != null && !isRetainerSOW) {
            SOWContract sowContract = sowContractRepository.findById(changeRequest.getSowContractId())
                .orElse(null);
            
            if (sowContract != null) {
                String engagementType = sowContract.getEngagementType(); 
                // For Fixed Price SOW, create billing detail when approved
                if ("Fixed Price".equals(engagementType)) {
                    // Check if impact analysis data exists (newEndDate and cost)
                    // Use costEstimatedByLandbridge if available, otherwise fallback to expectedExtraCost or amount
                    BigDecimal costToUse = changeRequest.getCostEstimatedByLandbridge();
                    if (costToUse == null) {
                        costToUse = changeRequest.getExpectedExtraCost();
                    }
                    if (costToUse == null) {
                        costToUse = changeRequest.getAmount();
                    }
                    
                    if (changeRequest.getNewEndDate() != null && costToUse != null) {
                        // Ensure costEstimatedByLandbridge is set for billing detail creation
                        if (changeRequest.getCostEstimatedByLandbridge() == null) {
                            changeRequest.setCostEstimatedByLandbridge(costToUse);
                            changeRequestRepository.save(changeRequest);
                        }
                        createBillingDetailForFixedPriceSOW(changeRequest, sowContract);
                    }
                }
            }
        }
    }
    
    /**
     * Create billing detail for Fixed Price SOW change request when approved
     */
    private void createBillingDetailForFixedPriceSOW(ChangeRequest changeRequest, SOWContract sowContract) {
        try {
            // Get MSA contract to get billing day
            Contract msaContract = contractRepository.findById(sowContract.getParentMsaId())
                .orElse(null);
            
            String billingDay = null;
            if (msaContract != null) {
                billingDay = msaContract.getBillingDay();
            } else {
                // Fallback to SOW contract's billing day if MSA not found
                billingDay = sowContract.getBillingDay();
            }
            
            // Calculate invoice date based on newEndDate and billing day
            LocalDate invoiceDate = calculateInvoiceDate(changeRequest.getNewEndDate(), billingDay);
            
            // Create FixedPriceBillingDetail
            FixedPriceBillingDetail billingDetail = new FixedPriceBillingDetail();
            billingDetail.setSowContractId(sowContract.getId());
            billingDetail.setBillingName(changeRequest.getTitle() != null ? changeRequest.getTitle() : "Change Request Payment");
            billingDetail.setMilestone(changeRequest.getNewEndDate() != null ? 
                changeRequest.getNewEndDate().format(DATE_FORMATTER) : null);
            billingDetail.setAmount(changeRequest.getCostEstimatedByLandbridge());
            billingDetail.setPercentage(null); // Percentage is null for change requests
            billingDetail.setInvoiceDate(invoiceDate);
            billingDetail.setChangeRequestId(changeRequest.getId());
            
            fixedPriceBillingDetailRepository.save(billingDetail);
            
            logger.info("Created billing detail for Fixed Price SOW change request: CR-{}, SOW-{}", 
                changeRequest.getChangeRequestId(), sowContract.getId());
        } catch (Exception e) {
            logger.error("Error creating billing detail for Fixed Price SOW change request: {}", e.getMessage(), e);
            // Don't throw exception to avoid rolling back the approval
        }
    }
    
    /**
     * Calculate invoice date based on planned end date and billing day
     * Logic: If planned day > billing day, use billing day of next month, else use billing day of current month
     */
    private LocalDate calculateInvoiceDate(LocalDate plannedEndDate, String billingDay) {
        if (plannedEndDate == null) {
            return LocalDate.now();
        }
        
        if (billingDay == null || billingDay.trim().isEmpty()) {
            return plannedEndDate;
        }
        
        int plannedYear = plannedEndDate.getYear();
        int plannedMonth = plannedEndDate.getMonthValue();
        int plannedDay = plannedEndDate.getDayOfMonth();
        
        // Parse billing day
        int billingDayNumber;
        String billingDayLower = billingDay.toLowerCase();
        if (billingDayLower.contains("last business day") || billingDayLower.contains("last")) {
            // For "Last business day", use the last day of the month
            LocalDate lastDayOfMonth = LocalDate.of(plannedYear, plannedMonth, 1)
                .withDayOfMonth(LocalDate.of(plannedYear, plannedMonth, 1).lengthOfMonth());
            billingDayNumber = lastDayOfMonth.getDayOfMonth();
        } else {
            // Try to extract number from billing day (e.g., "15", "15th", "Day 15")
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\d+");
            java.util.regex.Matcher matcher = pattern.matcher(billingDay);
            if (matcher.find()) {
                billingDayNumber = Integer.parseInt(matcher.group());
            } else {
                billingDayNumber = plannedDay;
            }
        }
        
        // Determine invoice date
        LocalDate invoiceDate;
        if (plannedDay > billingDayNumber) {
            // Planned End > Billing Day: use Billing Day of next month
            invoiceDate = LocalDate.of(plannedYear, plannedMonth, 1)
                .plusMonths(1)
                .withDayOfMonth(Math.min(billingDayNumber, 
                    LocalDate.of(plannedYear, plannedMonth, 1).plusMonths(1).lengthOfMonth()));
        } else {
            // Planned End <= Billing Day: use Billing Day of current month
            invoiceDate = LocalDate.of(plannedYear, plannedMonth, 
                Math.min(billingDayNumber, LocalDate.of(plannedYear, plannedMonth, 1).lengthOfMonth()));
        }
        
        return invoiceDate;
    }
    
    /**
     * Request for change (Under Review or Client Under Review -> Request for Change)
     */
    @Transactional
    public void requestForChange(
        Integer contractId,
        Integer changeRequestId,
        Integer clientUserId,
        String message
    ) {
        ChangeRequest changeRequest = validateAndGetChangeRequest(contractId, changeRequestId, clientUserId);
        
        // Validate status is "Under Review" or "Client Under Review"
        if (!"Under Review".equals(changeRequest.getStatus()) && !"Client Under Review".equals(changeRequest.getStatus())) {
            throw new IllegalStateException("Only Under Review or Client Under Review change requests can be requested for change");
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

