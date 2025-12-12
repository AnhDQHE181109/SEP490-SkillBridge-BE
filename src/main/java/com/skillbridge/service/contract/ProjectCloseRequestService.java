package com.skillbridge.service.contract;

import com.skillbridge.dto.contract.request.*;
import com.skillbridge.dto.contract.response.ProjectCloseRequestDetailDTO;
import com.skillbridge.dto.contract.response.ProjectCloseRequestResponse;
import com.skillbridge.entity.auth.User;
import com.skillbridge.entity.contract.*;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.repository.contract.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Project Close Request Service
 * Handles business logic for project close requests
 * Story-41: Project Close Request for SOW Contract
 */
@Service
@Transactional
public class ProjectCloseRequestService {
    
    @Autowired
    private ProjectCloseRequestRepository projectCloseRequestRepository;
    
    @Autowired
    private SOWContractRepository sowContractRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ContractHistoryRepository contractHistoryRepository;
    
    /**
     * Validate if a Close Request can be created for a SOW
     */
    public void validateCanCreateCloseRequest(Integer sowId, User currentUser) {
        // Check user role
        if (!"SALES_REP".equals(currentUser.getRole()) && !"SALES_MANAGER".equals(currentUser.getRole())) {
            throw new RuntimeException("Only Sales Representatives and Sales Managers can create close requests.");
        }
        
        // Check SOW exists
        SOWContract sow = sowContractRepository.findById(sowId)
            .orElseThrow(() -> new RuntimeException("SOW Contract not found"));
        
        // Check SOW status is Active
        if (sow.getStatus() != SOWContract.SOWContractStatus.Active) {
            throw new RuntimeException("Close request can only be created for Active SOW contracts.");
        }
        
        // Check SOW end_date <= today
        if (sow.getPeriodEnd() != null && sow.getPeriodEnd().isAfter(LocalDate.now())) {
            throw new RuntimeException("Close request can only be created when the contract end date has passed.");
        }
        
        // Check no existing Pending Close Request exists
        boolean hasPending = projectCloseRequestRepository.existsBySowIdAndStatus(
            sowId, 
            ProjectCloseRequest.ProjectCloseRequestStatus.Pending
        );
        if (hasPending) {
            throw new RuntimeException("A pending close request already exists for this SOW. Please wait for client response or resubmit the existing request.");
        }
    }
    
    /**
     * Create a new Project Close Request
     */
    public ProjectCloseRequestResponse createCloseRequest(
        Integer sowId, 
        CreateProjectCloseRequestRequest request, 
        User currentUser
    ) {
        // Validate
        validateCanCreateCloseRequest(sowId, currentUser);
        
        // Validate message and links length
        if (request.getMessage() != null && request.getMessage().length() > 5000) {
            throw new RuntimeException("Message must not exceed 5000 characters.");
        }
        if (request.getLinks() != null && request.getLinks().length() > 2000) {
            throw new RuntimeException("Links must not exceed 2000 characters.");
        }
        
        // Create entity
        ProjectCloseRequest closeRequest = new ProjectCloseRequest();
        closeRequest.setSowId(sowId);
        closeRequest.setRequestedByUserId(currentUser.getId());
        closeRequest.setStatus(ProjectCloseRequest.ProjectCloseRequestStatus.Pending);
        closeRequest.setMessage(request.getMessage());
        closeRequest.setLinks(request.getLinks());
        
        // Save
        closeRequest = projectCloseRequestRepository.save(closeRequest);
        
        // Create audit log
        createHistoryEntry(sowId, "CloseRequestCreated", 
            "Project close request created by " + currentUser.getFullName() + ".", 
            null, null, currentUser.getId());
        
        // Convert to DTO
        ProjectCloseRequestDetailDTO dto = convertToDetailDTO(closeRequest);
        
        return new ProjectCloseRequestResponse(true, "Project close request created successfully", dto);
    }
    
    /**
     * Get latest Close Request for a SOW
     */
    public ProjectCloseRequestDetailDTO getLatestCloseRequest(Integer sowId, User currentUser) {
        // Check SOW exists
        SOWContract sow = sowContractRepository.findById(sowId)
            .orElseThrow(() -> new RuntimeException("SOW Contract not found"));
        
        // Check access permission
        if ("CLIENT".equals(currentUser.getRole())) {
            // Client can only see their own SOWs
            if (!sow.getClientId().equals(currentUser.getId())) {
                throw new RuntimeException("Access denied: You do not have permission to view this close request.");
            }
        } else if ("SALES_REP".equals(currentUser.getRole()) || "SALES_MANAGER".equals(currentUser.getRole())) {
            // Sales Rep can only see assigned SOWs, Sales Manager can see all
            if ("SALES_REP".equals(currentUser.getRole())) {
                if (sow.getAssigneeUserId() == null || !sow.getAssigneeUserId().equals(currentUser.getId())) {
                    throw new RuntimeException("Access denied: You can only view close requests for contracts assigned to you");
                }
            }
        } else {
            throw new RuntimeException("Access denied: Invalid user role.");
        }
        
        // Get latest close request
        Optional<ProjectCloseRequest> closeRequestOpt = projectCloseRequestRepository
            .findFirstBySowIdOrderByCreatedAtDesc(sowId);
        
        if (closeRequestOpt.isEmpty()) {
            // Return null DTO instead of throwing exception - this is a valid state
            // The frontend will handle the case when no close request exists
            return null;
        }
        
        return convertToDetailDTO(closeRequestOpt.get());
    }
    
    /**
     * Resubmit a rejected Close Request (SalesRep action)
     */
    public ProjectCloseRequestResponse resubmitCloseRequest(
        Integer closeRequestId, 
        ResubmitProjectCloseRequestRequest request, 
        User currentUser
    ) {
        // Validate user role
        if (!"SALES_REP".equals(currentUser.getRole()) && !"SALES_MANAGER".equals(currentUser.getRole())) {
            throw new RuntimeException("Only Sales Representatives and Sales Managers can resubmit close requests.");
        }
        
        // Get close request
        ProjectCloseRequest closeRequest = projectCloseRequestRepository.findById(closeRequestId)
            .orElseThrow(() -> new RuntimeException("Close request not found"));
        
        // Validate status
        if (closeRequest.getStatus() != ProjectCloseRequest.ProjectCloseRequestStatus.Rejected) {
            throw new RuntimeException("Only rejected close requests can be resubmitted.");
        }
        
        // Validate ownership (original requester or SalesManager)
        if (!"SALES_MANAGER".equals(currentUser.getRole()) && 
            !closeRequest.getRequestedByUserId().equals(currentUser.getId())) {
            throw new RuntimeException("You do not have permission to resubmit this close request.");
        }
        
        // Validate message and links length
        if (request.getMessage() != null && request.getMessage().length() > 5000) {
            throw new RuntimeException("Message must not exceed 5000 characters.");
        }
        if (request.getLinks() != null && request.getLinks().length() > 2000) {
            throw new RuntimeException("Links must not exceed 2000 characters.");
        }
        
        // Update close request
        if (request.getMessage() != null) {
            closeRequest.setMessage(request.getMessage());
        }
        if (request.getLinks() != null) {
            closeRequest.setLinks(request.getLinks());
        }
        closeRequest.setStatus(ProjectCloseRequest.ProjectCloseRequestStatus.Pending);
        // Keep client_reject_reason for history (optional)
        closeRequest = projectCloseRequestRepository.save(closeRequest);
        
        // Create audit log
        createHistoryEntry(closeRequest.getSowId(), "CloseRequestResubmitted", 
            "Project close request resubmitted after rejection by " + currentUser.getFullName() + ".", 
            null, null, currentUser.getId());
        
        // Convert to DTO
        ProjectCloseRequestDetailDTO dto = convertToDetailDTO(closeRequest);
        
        return new ProjectCloseRequestResponse(true, 
            "Project close request resubmitted successfully", dto);
    }
    
    /**
     * Convert ProjectCloseRequest entity to DetailDTO
     */
    private ProjectCloseRequestDetailDTO convertToDetailDTO(ProjectCloseRequest closeRequest) {
        ProjectCloseRequestDetailDTO dto = new ProjectCloseRequestDetailDTO();
        dto.setId(closeRequest.getId());
        dto.setSowId(closeRequest.getSowId());
        dto.setStatus(closeRequest.getStatus().name());
        dto.setMessage(closeRequest.getMessage());
        dto.setLinks(closeRequest.getLinks());
        dto.setClientRejectReason(closeRequest.getClientRejectReason());
        dto.setCreatedAt(closeRequest.getCreatedAt());
        dto.setUpdatedAt(closeRequest.getUpdatedAt());
        
        // Load SOW information
        SOWContract sow = sowContractRepository.findById(closeRequest.getSowId())
            .orElse(null);
        if (sow != null) {
            dto.setSowContractName(sow.getContractName());
            dto.setSowStatus(sow.getStatus().name());
            // Format period
            if (sow.getPeriodStart() != null && sow.getPeriodEnd() != null) {
                dto.setSowPeriod(String.format("%s-%s", 
                    sow.getPeriodStart().toString().replace("-", "/"),
                    sow.getPeriodEnd().toString().replace("-", "/")));
            }
        }
        
        // Load requester information
        User requester = userRepository.findById(closeRequest.getRequestedByUserId())
            .orElse(null);
        if (requester != null) {
            ProjectCloseRequestDetailDTO.UserInfoDTO userInfo = 
                new ProjectCloseRequestDetailDTO.UserInfoDTO(
                    requester.getId(),
                    requester.getFullName(),
                    requester.getEmail()
                );
            dto.setRequestedBy(userInfo);
        }
        
        return dto;
    }
    
    /**
     * Create history entry for SOW contract
     */
    private void createHistoryEntry(Integer sowId, String activityType, String description, 
                                   String fileLink, String fileUrl, Integer createdBy) {
        ContractHistory history = new ContractHistory();
        history.setContractId(null); // SOW contracts use sow_contract_id
        history.setSowContractId(sowId);
        history.setHistoryType("SOW");
        history.setEntryDate(LocalDate.now());
        history.setDescription(description);
        history.setDocumentLink(fileLink);
        history.setDocumentName(fileUrl);
        history.setCreatedBy(createdBy);
        contractHistoryRepository.save(history);
    }
}

