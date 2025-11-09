package com.skillbridge.service.proposal;

import com.skillbridge.dto.proposal.response.ProposalListItemDTO;
import com.skillbridge.dto.proposal.response.ProposalListResponse;
import com.skillbridge.entity.contact.Contact;
import com.skillbridge.entity.proposal.Proposal;
import com.skillbridge.repository.contact.ContactRepository;
import com.skillbridge.repository.proposal.ProposalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Proposal List Service
 * Handles business logic for proposal list operations
 */
@Service
public class ProposalListService {

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private ContactRepository contactRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /**
     * Get proposals for client with search, filter, and pagination
     */
    public ProposalListResponse getProposalsForClient(
        Integer clientUserId,
        String search,
        String status,
        int page,
        int size
    ) {
        // Normalize search query
        String searchQuery = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        
        // Normalize status filter - map "All" to null, and map frontend status values to backend values
        String statusFilter = null;
        if (status != null && !status.trim().isEmpty() && !status.equals("All")) {
            // Map frontend status values to backend values
            switch (status) {
                case "Under review":
                    statusFilter = "under review";
                    break;
                case "Request for change":
                    statusFilter = "Reject"; // Request for change maps to Reject
                    break;
                case "Approved":
                    statusFilter = "Approved";
                    break;
                default:
                    statusFilter = status.trim();
            }
        }

        try {
            // Create pageable with sorting by created_at DESC
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

            // Execute query
            Page<Proposal> proposalPage = proposalRepository.findProposalsForClient(
                clientUserId,
                searchQuery,
                statusFilter,
                pageable
            );

            // Convert to DTOs
            List<Proposal> proposals = proposalPage.getContent();
            List<ProposalListItemDTO> proposalDTOs = IntStream.range(0, proposals.size())
                .mapToObj(index -> convertToListItemDTO(proposals.get(index), page * size + index + 1))
                .collect(Collectors.toList());

            // Build response
            ProposalListResponse response = new ProposalListResponse();
            response.setProposals(proposalDTOs);
            response.setCurrentPage(proposalPage.getNumber());
            response.setTotalPages(proposalPage.getTotalPages());
            response.setTotalElements(proposalPage.getTotalElements());

            return response;
        } catch (Exception e) {
            // Log error for debugging
            System.err.println("Error in getProposalsForClient: " + e.getMessage());
            e.printStackTrace();
            
            // Return empty response
            ProposalListResponse emptyResponse = new ProposalListResponse();
            emptyResponse.setProposals(java.util.Collections.emptyList());
            emptyResponse.setCurrentPage(0);
            emptyResponse.setTotalPages(0);
            emptyResponse.setTotalElements(0);
            
            return emptyResponse;
        }
    }

    /**
     * Convert Proposal entity to ProposalListItemDTO
     */
    private ProposalListItemDTO convertToListItemDTO(Proposal proposal, int no) {
        ProposalListItemDTO dto = new ProposalListItemDTO();
        dto.setNo(no);
        dto.setInternalId(proposal.getId()); // Primary key for navigation
        dto.setId(generateProposalId(proposal)); // Format: P-YYYY-NN
        dto.setTitle(proposal.getTitle() != null ? proposal.getTitle() : "");
        
        // Get contact information - always load from repository to avoid lazy loading issues
        Contact contact = contactRepository.findById(proposal.getContactId()).orElse(null);
        if (contact != null) {
            dto.setContactId(generateContactId(contact));
            dto.setContactInternalId(contact.getId()); // Contact primary key for navigation
            dto.setContactDescription(contact.getDescription() != null 
                ? contact.getDescription() 
                : "");
        } else {
            dto.setContactId("CT-" + proposal.getContactId());
            dto.setContactInternalId(proposal.getContactId()); // Fallback to contact ID from proposal
            dto.setContactDescription("");
        }
        
        dto.setCreatedOn(proposal.getCreatedAt() != null 
            ? proposal.getCreatedAt().format(DATE_FORMATTER) 
            : "");
        
        // Map backend status to frontend status
        String backendStatus = proposal.getStatus() != null ? proposal.getStatus() : "draft";
        String frontendStatus = mapStatusToFrontend(backendStatus);
        dto.setStatus(frontendStatus);
        
        dto.setLastUpdated(proposal.getUpdatedAt() != null 
            ? proposal.getUpdatedAt().format(DATE_FORMATTER) 
            : "");
        return dto;
    }

    /**
     * Map backend status to frontend status
     */
    private String mapStatusToFrontend(String backendStatus) {
        if (backendStatus == null) {
            return "Under review";
        }
        switch (backendStatus.toLowerCase()) {
            case "draft":
            case "under review":
                return "Under review";
            case "reject":
                return "Request for change";
            case "sent to client":
            case "approved":
                return "Approved";
            default:
                return "Under review";
        }
    }

    /**
     * Generate Proposal ID in format P-YYYY-NN
     * YYYY = Year (4 digits)
     * NN = Sequential number within that year (2 digits, zero-padded)
     */
    private String generateProposalId(Proposal proposal) {
        if (proposal.getCreatedAt() == null || proposal.getId() == null) {
            // Fallback format if missing data
            if (proposal.getId() != null) {
                return "P-0000-" + String.format("%02d", proposal.getId());
            }
            return "P-0000-00";
        }

        int year = proposal.getCreatedAt().getYear();
        
        // Count proposals in the same year with ID less than or equal to current
        // For simplicity, we'll use a sequential number based on ID
        // In production, you might want to use a more sophisticated approach
        long countInYear = proposalRepository.findAll().stream()
            .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().getYear() == year)
            .filter(p -> p.getId() != null && p.getId() <= proposal.getId())
            .count();
        
        // Format: P-YYYY-NN
        return String.format("P-%d-%02d", year, countInYear);
    }

    /**
     * Generate Contact ID in format CT-YYYY-MM-DD-{clientUserId}-{contactId} (similar to ContactListService)
     */
    private String generateContactId(Contact contact) {
        if (contact.getCreatedAt() == null || contact.getClientUserId() == null || contact.getId() == null) {
            if (contact.getId() != null) {
                return "CT-0000-00-00-0-" + contact.getId();
            }
            return "CT-0000-00-00-0-0";
        }

        java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String dateStr = contact.getCreatedAt().format(dateFormatter);
        
        return String.format("CT-%s-%d-%d", dateStr, contact.getClientUserId(), contact.getId());
    }
}

