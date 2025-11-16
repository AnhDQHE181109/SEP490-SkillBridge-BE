package com.skillbridge.service.sales;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.skillbridge.dto.contract.response.ContractListItemDTO;
import com.skillbridge.dto.contract.response.ContractListResponse;
import com.skillbridge.entity.auth.User;
import com.skillbridge.entity.contract.Contract;
import com.skillbridge.entity.contract.SOWContract;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.repository.contract.ContractRepository;
import com.skillbridge.repository.contract.SOWContractRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Sales Contract Service
 * Handles business logic for sales contract list operations with role-based filtering
 */
@Service
public class SalesContractService {
    
    @Autowired
    private ContractRepository contractRepository;
    
    @Autowired
    private SOWContractRepository sowContractRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Get contracts list with role-based filtering
     * - Sales Manager: sees all contracts
     * - Sales Man: sees only contracts assigned to themselves
     */
    public ContractListResponse getContracts(
        String search,
        String status,
        int page,
        int size,
        User currentUser
    ) {
        // Role-based filtering
        Integer assigneeUserId = null;
        if ("SALES_REP".equals(currentUser.getRole())) {
            // Sales Man: only contracts assigned to themselves
            assigneeUserId = currentUser.getId();
        }
        // Sales Manager: assigneeUserId remains null (sees all contracts)
        
        // Build specifications for both MSA and SOW contracts
        Specification<Contract> msaSpec = buildMSASpecification(search, status, assigneeUserId);
        Specification<SOWContract> sowSpec = buildSOWSpecification(search, status, assigneeUserId);
        
        // Query both MSA and SOW contracts (fetch all matching, then combine and paginate)
        // Note: We fetch all matching contracts because we need to combine and sort them
        Pageable pageable = PageRequest.of(0, 10000, Sort.by("createdAt").descending());
        
        Page<Contract> msaPage = contractRepository.findAll(msaSpec, pageable);
        Page<SOWContract> sowPage = sowContractRepository.findAll(sowSpec, pageable);
        
        // Convert to DTOs
        List<ContractListItemDTO> contracts = new ArrayList<>();
        
        // Convert MSA contracts
        for (Contract contract : msaPage.getContent()) {
            ContractListItemDTO dto = convertMSAToDTO(contract);
            contracts.add(dto);
        }
        
        // Convert SOW contracts
        for (SOWContract sowContract : sowPage.getContent()) {
            ContractListItemDTO dto = convertSOWToDTO(sowContract);
            contracts.add(dto);
        }
        
        // Apply search filter on client name/email and contract ID (if search is provided)
        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.toLowerCase();
            contracts = contracts.stream()
                .filter(dto -> {
                    // Check contract ID (generated format)
                    if (dto.getId() != null && dto.getId().toLowerCase().contains(searchLower)) {
                        return true;
                    }
                    // Check contract name
                    if (dto.getContractName() != null && dto.getContractName().toLowerCase().contains(searchLower)) {
                        return true;
                    }
                    // Check client name
                    if (dto.getClientName() != null && dto.getClientName().toLowerCase().contains(searchLower)) {
                        return true;
                    }
                    // Check client email
                    if (dto.getClientEmail() != null && dto.getClientEmail().toLowerCase().contains(searchLower)) {
                        return true;
                    }
                    return false;
                })
                .collect(java.util.stream.Collectors.toList());
        }
        
        // Sort combined list by creation date (newest first)
        contracts.sort(Comparator.comparing((ContractListItemDTO dto) -> {
            if (dto.getCreatedAt() == null) return "";
            return dto.getCreatedAt();
        }).reversed());
        
        // Apply pagination to combined list
        int totalElements = contracts.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int start = page * size;
        int end = Math.min(start + size, contracts.size());
        
        List<ContractListItemDTO> paginatedContracts = new ArrayList<>();
        int no = 1; // Sequential number starting from 1 for current page
        for (int i = start; i < end; i++) {
            ContractListItemDTO dto = contracts.get(i);
            dto.setNo(no++);
            paginatedContracts.add(dto);
        }
        
        // Build response
        ContractListResponse response = new ContractListResponse();
        response.setContracts(paginatedContracts);
        response.setCurrentPage(page);
        response.setTotalPages(totalPages);
        response.setTotalElements(totalElements);
        
        return response;
    }
    
    /**
     * Build JPA Specification for MSA contracts
     */
    private Specification<Contract> buildMSASpecification(String search, String status, Integer assigneeUserId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Role-based filtering: assignee_user_id
            if (assigneeUserId != null) {
                predicates.add(cb.equal(root.get("assigneeUserId"), assigneeUserId));
            }
            
            // Note: Search filter is applied in service layer after converting to DTO
            // to support searching by contract ID, contract name, client name, and client email
            
            // Status filter
            if (status != null && !status.trim().isEmpty() && !status.equals("All")) {
                try {
                    String statusStr = status.replace(" ", "_");
                    Contract.ContractStatus statusEnum = Contract.ContractStatus.valueOf(statusStr);
                    predicates.add(cb.equal(root.get("status"), statusEnum));
                } catch (IllegalArgumentException e) {
                    // Invalid status, ignore
                }
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    /**
     * Build JPA Specification for SOW contracts
     */
    private Specification<SOWContract> buildSOWSpecification(String search, String status, Integer assigneeUserId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Role-based filtering: assignee_user_id
            if (assigneeUserId != null) {
                predicates.add(cb.equal(root.get("assigneeUserId"), assigneeUserId));
            }
            
            // Note: Search filter is applied in service layer after converting to DTO
            // to support searching by contract ID, contract name, client name, and client email
            
            // Status filter
            if (status != null && !status.trim().isEmpty() && !status.equals("All")) {
                try {
                    String statusStr = status.replace(" ", "_");
                    SOWContract.SOWContractStatus statusEnum = SOWContract.SOWContractStatus.valueOf(statusStr);
                    predicates.add(cb.equal(root.get("status"), statusEnum));
                } catch (IllegalArgumentException e) {
                    // Invalid status, ignore
                }
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    /**
     * Convert MSA Contract to DTO
     */
    private ContractListItemDTO convertMSAToDTO(Contract contract) {
        ContractListItemDTO dto = new ContractListItemDTO();
        dto.setInternalId(contract.getId());
        dto.setId(generateContractId(contract.getId(), "MSA", contract.getCreatedAt()));
        dto.setContractName(contract.getContractName());
        dto.setType("MSA");
        dto.setPeriodStart(formatDateStart(contract.getPeriodStart()));
        dto.setPeriodEnd(formatDateEnd(contract.getPeriodEnd()));
        dto.setPeriod(formatPeriod(contract.getPeriodStart(), contract.getPeriodEnd()));
        dto.setValue(contract.getValue());
        dto.setFormattedValue(formatValue(contract.getValue()));
        dto.setStatus(contract.getStatus().name().replace("_", " "));
        dto.setAssignee(contract.getAssigneeId());
        
        // Load client name and email from users table
        if (contract.getClientId() != null) {
            userRepository.findById(contract.getClientId()).ifPresent(user -> {
                dto.setClientName(user.getFullName());
                dto.setClientEmail(user.getEmail());
            });
        }
        
        // Load assignee name from users table
        if (contract.getAssigneeUserId() != null) {
            userRepository.findById(contract.getAssigneeUserId()).ifPresent(user -> {
                dto.setAssigneeName(user.getFullName());
            });
        }
        
        // Parse attachments_manifest JSON (similar to Proposal)
        Gson gson = new Gson();
        if (contract.getAttachmentsManifest() != null && !contract.getAttachmentsManifest().isEmpty()) {
            try {
                Type listType = new TypeToken<List<String>>(){}.getType();
                List<String> attachments = gson.fromJson(contract.getAttachmentsManifest(), listType);
                dto.setAttachments(attachments);
            } catch (Exception e) {
                // If parsing fails, use link as single attachment
                List<String> attachments = new ArrayList<>();
                if (contract.getLink() != null) {
                    attachments.add(contract.getLink());
                }
                dto.setAttachments(attachments);
            }
        } else if (contract.getLink() != null) {
            // If no manifest but has link, use link as single attachment
            List<String> attachments = new ArrayList<>();
            attachments.add(contract.getLink());
            dto.setAttachments(attachments);
        } else {
            dto.setAttachments(new ArrayList<>());
        }
        
        dto.setCreatedAt(contract.getCreatedAt() != null ? contract.getCreatedAt().toString() : null);
        return dto;
    }
    
    /**
     * Convert SOW Contract to DTO
     */
    private ContractListItemDTO convertSOWToDTO(SOWContract sowContract) {
        ContractListItemDTO dto = new ContractListItemDTO();
        dto.setInternalId(sowContract.getId());
        dto.setId(generateContractId(sowContract.getId(), "SOW", sowContract.getCreatedAt()));
        dto.setContractName(sowContract.getContractName());
        dto.setType("SOW");
        dto.setPeriodStart(formatDateStart(sowContract.getPeriodStart()));
        dto.setPeriodEnd(formatDateEnd(sowContract.getPeriodEnd()));
        dto.setPeriod(formatPeriod(sowContract.getPeriodStart(), sowContract.getPeriodEnd()));
        dto.setValue(sowContract.getValue());
        dto.setFormattedValue(formatValue(sowContract.getValue()));
        dto.setStatus(sowContract.getStatus().name().replace("_", " "));
        dto.setAssignee(sowContract.getAssigneeId());
        
        // Load client name and email from users table
        if (sowContract.getClientId() != null) {
            userRepository.findById(sowContract.getClientId()).ifPresent(user -> {
                dto.setClientName(user.getFullName());
                dto.setClientEmail(user.getEmail());
            });
        }
        
        // Load assignee name from users table
        if (sowContract.getAssigneeUserId() != null) {
            userRepository.findById(sowContract.getAssigneeUserId()).ifPresent(user -> {
                dto.setAssigneeName(user.getFullName());
            });
        }
        
        dto.setCreatedAt(sowContract.getCreatedAt() != null ? sowContract.getCreatedAt().toString() : null);
        return dto;
    }
    
    /**
     * Generate contract ID in format: MSA-YYYY-NN or SOW-YYYY-NN
     * Note: This is a simplified version. In production, you might want to calculate sequence number
     * based on creation order in the year.
     */
    private String generateContractId(Integer id, String type, LocalDateTime createdAt) {
        int year = createdAt != null ? createdAt.getYear() : 2025;
        // Use last 2 digits of ID as sequence number (simplified)
        int sequenceNumber = id % 100;
        return String.format("%s-%d-%02d", type, year, sequenceNumber);
    }
    
    /**
     * Format period start date: YYYY/MM/D (single digit day without leading zero)
     */
    private String formatDateStart(LocalDate date) {
        if (date == null) return null;
        return String.format("%d/%02d/%d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }
    
    /**
     * Format period end date: YYYY/MM/DD (with leading zero for day)
     */
    private String formatDateEnd(LocalDate date) {
        if (date == null) return null;
        return String.format("%d/%02d/%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }
    
    /**
     * Format period: "YYYY/MM/D-YYYY/MM/DD" or "-"
     */
    private String formatPeriod(LocalDate start, LocalDate end) {
        if (start == null && end == null) {
            return "-";
        }
        if (start == null) {
            return "-" + formatDateEnd(end);
        }
        if (end == null) {
            return formatDateStart(start);
        }
        return formatDateStart(start) + "-" + formatDateEnd(end);
    }
    
    /**
     * Format value: "¥X,XXX,XXX" or "-"
     */
    private String formatValue(BigDecimal value) {
        if (value == null) {
            return "-";
        }
        DecimalFormat formatter = new DecimalFormat("¥#,###");
        return formatter.format(value);
    }
}

