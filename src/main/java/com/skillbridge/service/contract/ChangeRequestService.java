package com.skillbridge.service.contract;

import com.skillbridge.dto.contract.request.CreateChangeRequestRequest;
import com.skillbridge.dto.contract.response.ChangeRequestResponse;
import com.skillbridge.entity.contract.ChangeRequest;
import com.skillbridge.entity.contract.ChangeRequestAttachment;
import com.skillbridge.entity.contract.Contract;
import com.skillbridge.entity.contract.SOWContract;
import com.skillbridge.repository.contract.ChangeRequestAttachmentRepository;
import com.skillbridge.repository.contract.ChangeRequestRepository;
import com.skillbridge.repository.contract.ContractRepository;
import com.skillbridge.repository.contract.SOWContractRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Change Request Service
 * Handles business logic for change request operations
 */
@Service
public class ChangeRequestService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChangeRequestService.class);
    
    @Autowired
    private ChangeRequestRepository changeRequestRepository;
    
    @Autowired
    private ChangeRequestAttachmentRepository changeRequestAttachmentRepository;
    
    @Autowired
    private ContractRepository contractRepository;
    
    @Autowired
    private SOWContractRepository sowContractRepository;
    
    @Value("${app.upload.dir:uploads/change-requests}")
    private String uploadDir;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    
    /**
     * Create change request
     */
    @Transactional
    public ChangeRequestResponse createChangeRequest(
        Integer contractId,
        Integer clientUserId,
        CreateChangeRequestRequest request,
        List<MultipartFile> attachments
    ) {
        // Validate contract belongs to user
        Contract msaContract = contractRepository.findByIdAndClientId(contractId, clientUserId).orElse(null);
        SOWContract sowContract = null;
        
        if (msaContract == null) {
            sowContract = sowContractRepository.findByIdAndClientId(contractId, clientUserId)
                .orElseThrow(() -> new EntityNotFoundException("Contract not found or access denied"));
        }
        
        // Validate CR type is valid for contract type
        if (sowContract != null) {
            String engagementType = sowContract.getEngagementType(); // "Fixed Price" or "Retainer"
            validateCRTypeForContract(request.getType(), engagementType);
        }
        
        // Create change request entity
        ChangeRequest changeRequest = new ChangeRequest();
        
        if (msaContract != null) {
            changeRequest.setContractId(contractId);
            changeRequest.setContractType("MSA");
        } else {
            // For SOW contracts with versioning:
            // - If contract has no parent_version_id, use contract's id (V1)
            // - If contract has parent_version_id, use parent_version_id to reference V1
            if (sowContract != null) {
                Integer sowContractIdForCR = sowContract.getParentVersionId() != null 
                    ? sowContract.getParentVersionId() 
                    : sowContract.getId();
                changeRequest.setSowContractId(sowContractIdForCR);
            } else {
                changeRequest.setSowContractId(contractId);
            }
            changeRequest.setContractType("SOW");
        }
        
        changeRequest.setTitle(request.getTitle());
        changeRequest.setType(request.getType());
        changeRequest.setDescription(request.getDescription());
        changeRequest.setReason(request.getReason());
        changeRequest.setDesiredStartDate(request.getDesiredStartDate());
        changeRequest.setDesiredEndDate(request.getDesiredEndDate());
        changeRequest.setExpectedExtraCost(request.getExpectedExtraCost());
        
        // Set summary (use title if summary is not provided)
        changeRequest.setSummary(request.getTitle() != null ? request.getTitle() : "");
        
        // Set amount (use expectedExtraCost if amount is not provided)
        changeRequest.setAmount(request.getExpectedExtraCost() != null 
            ? request.getExpectedExtraCost() 
            : BigDecimal.ZERO);
        
        // Set status to "Processing" for submitted change requests
        changeRequest.setStatus("Processing");
        
        // Set created by
        changeRequest.setCreatedBy(clientUserId);
        
        // Generate change request display ID
        changeRequest.setChangeRequestId(generateChangeRequestId());
        
        // Save change request
        changeRequest = changeRequestRepository.save(changeRequest);
        
        // Save attachments
        if (attachments != null && !attachments.isEmpty()) {
            saveAttachments(changeRequest.getId(), attachments, clientUserId);
        }
        
        // Return response
        ChangeRequestResponse response = new ChangeRequestResponse();
        response.setSuccess(true);
        response.setMessage("Change request created successfully");
        response.setChangeRequestId(changeRequest.getId());
        response.setChangeRequestDisplayId(changeRequest.getChangeRequestId());
        
        return response;
    }
    
    /**
     * Save change request as draft
     */
    @Transactional
    public ChangeRequestResponse saveChangeRequestDraft(
        Integer contractId,
        Integer clientUserId,
        CreateChangeRequestRequest request,
        List<MultipartFile> attachments
    ) {
        // Validate contract belongs to user
        Contract msaContract = contractRepository.findByIdAndClientId(contractId, clientUserId).orElse(null);
        SOWContract sowContract = null;
        
        if (msaContract == null) {
            sowContract = sowContractRepository.findByIdAndClientId(contractId, clientUserId)
                .orElseThrow(() -> new EntityNotFoundException("Contract not found or access denied"));
        }
        
        // Create change request entity (all fields optional for draft)
        ChangeRequest changeRequest = new ChangeRequest();
        
        if (msaContract != null) {
            changeRequest.setContractId(contractId);
            changeRequest.setContractType("MSA");
        } else {
            changeRequest.setSowContractId(contractId);
            changeRequest.setContractType("SOW");
        }
        
        changeRequest.setTitle(request.getTitle());
        changeRequest.setType(request.getType());
        changeRequest.setDescription(request.getDescription());
        changeRequest.setReason(request.getReason());
        changeRequest.setDesiredStartDate(request.getDesiredStartDate());
        changeRequest.setDesiredEndDate(request.getDesiredEndDate());
        changeRequest.setExpectedExtraCost(request.getExpectedExtraCost());
        
        // Set summary (use title if summary is not provided)
        changeRequest.setSummary(request.getTitle() != null ? request.getTitle() : "");
        
        // Set amount (use expectedExtraCost if amount is not provided)
        changeRequest.setAmount(request.getExpectedExtraCost() != null 
            ? request.getExpectedExtraCost() 
            : BigDecimal.ZERO);
        
        // Set status to "Draft" for draft change requests
        changeRequest.setStatus("Draft");
        
        // Set created by
        changeRequest.setCreatedBy(clientUserId);
        
        // Generate change request display ID
        changeRequest.setChangeRequestId(generateChangeRequestId());
        
        // Save change request
        changeRequest = changeRequestRepository.save(changeRequest);
        
        // Save attachments
        if (attachments != null && !attachments.isEmpty()) {
            saveAttachments(changeRequest.getId(), attachments, clientUserId);
        }
        
        // Return response
        ChangeRequestResponse response = new ChangeRequestResponse();
        response.setSuccess(true);
        response.setMessage("Change request saved as draft");
        response.setChangeRequestId(changeRequest.getId());
        response.setChangeRequestDisplayId(changeRequest.getChangeRequestId());
        
        return response;
    }
    
    /**
     * Validate CR type is valid for contract engagement type
     */
    private void validateCRTypeForContract(String crType, String engagementType) {
        if (crType == null || engagementType == null) {
            return; // Skip validation if null
        }
        
        List<String> validTypes;
        if ("Fixed Price".equalsIgnoreCase(engagementType)) {
            validTypes = List.of("Add Scope", "Remove Scope", "Other");
        } else if ("Retainer".equalsIgnoreCase(engagementType)) {
            validTypes = List.of("RESOURCE_CHANGE", "SCHEDULE_CHANGE", "SCOPE_ADJUSTMENT");
        } else {
            return; // Unknown engagement type, skip validation
        }
        
        if (!validTypes.contains(crType)) {
            throw new IllegalArgumentException(
                String.format("CR Type '%s' is not valid for %s contract", crType, engagementType)
            );
        }
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
     * Save attachments to file system and database
     */
    private void saveAttachments(Integer changeRequestId, List<MultipartFile> attachments, Integer uploadedBy) {
        try {
            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            for (MultipartFile file : attachments) {
                if (file.isEmpty()) {
                    continue;
                }
                
                // Generate unique filename
                String originalFilename = file.getOriginalFilename();
                String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
                String uniqueFilename = System.currentTimeMillis() + "_" + changeRequestId + extension;
                Path filePath = uploadPath.resolve(uniqueFilename);
                
                // Save file
                file.transferTo(filePath.toFile());
                
                // Save attachment metadata to database
                ChangeRequestAttachment attachment = new ChangeRequestAttachment();
                attachment.setChangeRequestId(changeRequestId);
                attachment.setFileName(originalFilename);
                attachment.setFilePath(filePath.toString());
                attachment.setFileSize(file.getSize());
                attachment.setFileType(file.getContentType());
                attachment.setUploadedBy(uploadedBy);
                
                changeRequestAttachmentRepository.save(attachment);
            }
        } catch (IOException e) {
            logger.error("Error saving attachments for change request {}", changeRequestId, e);
            throw new RuntimeException("Failed to save attachments", e);
        }
    }
}

