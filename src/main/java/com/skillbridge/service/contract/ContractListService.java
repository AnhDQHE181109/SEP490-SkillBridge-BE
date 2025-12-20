package com.skillbridge.service.contract;

import com.skillbridge.dto.contract.response.ContractListItemDTO;
import com.skillbridge.dto.contract.response.ContractListResponse;
import com.skillbridge.entity.contract.Contract;
import com.skillbridge.entity.contract.ChangeRequest;
import com.skillbridge.entity.contract.SOWContract;
import com.skillbridge.entity.contract.ProjectCloseRequest;
import com.skillbridge.repository.contract.ContractRepository;
import com.skillbridge.repository.contract.SOWContractRepository;
import com.skillbridge.repository.contract.ProjectCloseRequestRepository;
import com.skillbridge.repository.contract.ChangeRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Contract List Service
 * Handles business logic for contract list operations
 */
@Service
public class ContractListService {
    
    @Autowired
    private ContractRepository contractRepository;
    
    @Autowired
    private SOWContractRepository sowContractRepository;
    
    @Autowired
    private ProjectCloseRequestRepository projectCloseRequestRepository;

    @Autowired
    private ChangeRequestRepository changeRequestRepository;
    
    public ContractListResponse getContracts(
        Integer clientUserId,
        String search,
        String status,
        String type,
        int page,
        int size
    ) {
        // Normalize search query
        String searchQuery = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        
        // Normalize type filter
        boolean filterMSA = (type == null || type.trim().isEmpty() || type.equals("All") || type.equals("MSA"));
        boolean filterSOW = (type == null || type.trim().isEmpty() || type.equals("All") || type.equals("SOW"));
        
        // Convert status string to enum
        Contract.ContractStatus msaStatusEnum = null;
        SOWContract.SOWContractStatus sowStatusEnum = null;
        if (status != null && !status.trim().isEmpty() && !status.equals("All")) {
            try {
                String statusStr = status.replace(" ", "_");
                msaStatusEnum = Contract.ContractStatus.valueOf(statusStr);
                sowStatusEnum = SOWContract.SOWContractStatus.valueOf(statusStr);
            } catch (IllegalArgumentException e) {
                // Invalid status, ignore
            }
        }
        
        // Define allowed statuses for client contract list
        List<Contract.ContractStatus> allowedMSAStatuses = List.of(
            Contract.ContractStatus.Active,
            Contract.ContractStatus.Pending,
            Contract.ContractStatus.Under_Review,
            Contract.ContractStatus.Request_for_Change,
            Contract.ContractStatus.Completed,
            Contract.ContractStatus.Terminated
        );
        
        List<SOWContract.SOWContractStatus> allowedSOWStatuses = List.of(
            SOWContract.SOWContractStatus.Active,
            SOWContract.SOWContractStatus.Pending,
            SOWContract.SOWContractStatus.Under_Review,
            SOWContract.SOWContractStatus.Request_for_Change,
            SOWContract.SOWContractStatus.Completed,
            SOWContract.SOWContractStatus.Terminated
        );
        
        // Query MSA contracts
        List<ContractListItemDTO> msaDTOs = new ArrayList<>();
        if (filterMSA) {
            Pageable pageable = PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Contract> msaPage = contractRepository.findByClientIdWithFilters(
                clientUserId, searchQuery, msaStatusEnum, pageable
            );
            msaDTOs = msaPage.getContent().stream()
                .filter(contract -> allowedMSAStatuses.contains(contract.getStatus()))
                .map(contract -> convertMSAToListItemDTO(contract, 0))
                .collect(Collectors.toList());
        }
        
        // Query SOW contracts
        List<ContractListItemDTO> sowDTOs = new ArrayList<>();
        if (filterSOW) {
            String engagementTypeFilter = null;
            if (type != null && !type.equals("All") && !type.equals("MSA") && !type.equals("SOW")) {
                // If type is "Fixed Price" or "Retainer", filter by engagement type
                engagementTypeFilter = type;
            }
            
            Pageable pageable = PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<SOWContract> sowPage = sowContractRepository.findByClientIdWithFilters(
                clientUserId, searchQuery, sowStatusEnum, engagementTypeFilter, pageable
            );
            sowDTOs = sowPage.getContent().stream()
                .filter(sow -> allowedSOWStatuses.contains(sow.getStatus()))
                .map(sow -> convertSOWToListItemDTO(sow, 0))
                .collect(Collectors.toList());
        }
        
        // Merge and sort by created date
        List<ContractListItemDTO> allDTOs = new ArrayList<>();
        allDTOs.addAll(msaDTOs);
        allDTOs.addAll(sowDTOs);
        allDTOs.sort(Comparator.comparing((ContractListItemDTO dto) -> {
            // Parse createdAt string to compare
            if (dto.getCreatedAt() == null) return "";
            return dto.getCreatedAt();
        }).reversed());
        
        // Apply pagination manually
        int totalElements = allDTOs.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, totalElements);
        
        List<ContractListItemDTO> paginatedDTOs = new ArrayList<>();
        for (int i = startIndex; i < endIndex; i++) {
            ContractListItemDTO dto = allDTOs.get(i);
            dto.setNo(i + 1);
            paginatedDTOs.add(dto);
        }
        
        // Build response
        ContractListResponse response = new ContractListResponse();
        response.setContracts(paginatedDTOs);
        response.setCurrentPage(page);
        response.setTotalPages(totalPages);
        response.setTotalElements(totalElements);
        
        return response;
    }
    
    private ContractListItemDTO convertMSAToListItemDTO(Contract contract, int no) {
        ContractListItemDTO dto = new ContractListItemDTO();
        dto.setInternalId(contract.getId()); // Primary key for navigation
        dto.setId(generateMSAId(contract)); // Display ID (unique: MSA-YYYY-NNN)
        dto.setNo(no);
        dto.setContractName(contract.getContractName());
        dto.setType("MSA");
        dto.setPeriodStart(formatDate(contract.getPeriodStart()));
        dto.setPeriodEnd(formatDate(contract.getPeriodEnd()));
        dto.setPeriod(formatPeriod(contract.getPeriodStart(), contract.getPeriodEnd()));
        dto.setValue(contract.getValue());
        dto.setFormattedValue(formatValue(contract.getValue()));
        dto.setStatus(contract.getStatus().name().replace("_", " "));
        dto.setAssignee(contract.getAssigneeId());
        dto.setCreatedAt(contract.getCreatedAt() != null ? contract.getCreatedAt().toString() : null);
        // MSA contracts do not have project close requests
        dto.setCloseRequestPending(false);
        
        return dto;
    }
    
    private ContractListItemDTO convertSOWToListItemDTO(SOWContract sow, int no) {
        ContractListItemDTO dto = new ContractListItemDTO();
        dto.setInternalId(sow.getId()); // Primary key for navigation
        dto.setId(generateSOWId(sow)); // Display ID
        dto.setNo(no);
        dto.setContractName(sow.getContractName());
        dto.setType("SOW");
        dto.setPeriodStart(formatDate(sow.getPeriodStart()));
        dto.setPeriodEnd(formatDate(sow.getPeriodEnd()));
        dto.setPeriod(formatPeriod(sow.getPeriodStart(), sow.getPeriodEnd()));
        BigDecimal totalValue = calculateSowTotalValue(sow.getId(), sow.getValue());
        dto.setValue(totalValue);
        dto.setFormattedValue(formatValue(totalValue));
        dto.setStatus(sow.getStatus().name().replace("_", " "));
        dto.setAssignee(sow.getAssigneeId());
        dto.setCreatedAt(sow.getCreatedAt() != null ? sow.getCreatedAt().toString() : null);
        
        // Project Close Request indicators (latest request for this SOW)
        dto.setCloseRequestPending(false);
        projectCloseRequestRepository.findFirstBySowIdOrderByCreatedAtDesc(sow.getId()).ifPresent(closeRequest -> {
            dto.setCloseRequestStatus(closeRequest.getStatus().name());
            dto.setCloseRequestPending(closeRequest.getStatus() == ProjectCloseRequest.ProjectCloseRequestStatus.Pending);
        });
        
        return dto;
    }
    
    private String generateMSAId(Contract contract) {
        int year = contract.getCreatedAt() != null ? contract.getCreatedAt().getYear() : 2025;
        return String.format("MSA-%d-%03d", year, contract.getId());
    }
    
    private String generateSOWId(SOWContract sow) {
        int year = sow.getCreatedAt() != null ? sow.getCreatedAt().getYear() : 2025;
        return String.format("SOW-%d-%03d", year, sow.getId());
    }
    
    private String formatDate(LocalDate date) {
        if (date == null) return null;
        int day = date.getDayOfMonth();
        if (day == 1) {
            return String.format("%d/%02d/%d", date.getYear(), date.getMonthValue(), day);
        }
        return String.format("%d/%02d/%02d", date.getYear(), date.getMonthValue(), day);
    }
    
    private String formatPeriod(LocalDate start, LocalDate end) {
        if (start == null && end == null) return "-";
        if (start == null) return "-" + formatDate(end);
        if (end == null) return formatDate(start);
        return formatDate(start) + "-" + formatDate(end);
    }
    
    /**
     * Calculate SOW total value = base value + approved/active CR amounts.
     * Note: Both baseValue and CR amounts are already total amounts including tax (calculated on frontend).
     * Backend only sums them up for display purposes.
     */
    private BigDecimal calculateSowTotalValue(Integer sowContractId, BigDecimal baseValue) {
        BigDecimal total = baseValue != null ? baseValue : BigDecimal.ZERO;

        if (sowContractId == null) {
            return total;
        }

        try {
            List<ChangeRequest> changeRequests = changeRequestRepository.findBySowContractIdOrderByCreatedAtDesc(sowContractId);
            for (ChangeRequest cr : changeRequests) {
                if (cr.getStatus() != null
                    && "Active".equalsIgnoreCase(cr.getStatus())
                    && cr.getAmount() != null) {
                    total = total.add(cr.getAmount());
                }
            }
        } catch (Exception ignored) {
            // Fall back to base value if CR lookup fails
        }

        return total;
    }
    
    /**
     * Format value: "¥X,XXX,XXX" or "-"
     * Note: The value parameter is already the total amount including tax (calculated on frontend).
     * Backend only formats the value for display purposes.
     */
    private String formatValue(BigDecimal value) {
        if (value == null) return "-";
        DecimalFormat formatter = new DecimalFormat("¥#,###");
        return formatter.format(value);
    }
}

