package com.skillbridge.service.sales;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.skillbridge.dto.sales.request.CreateOpportunityRequest;
import com.skillbridge.dto.sales.request.UpdateOpportunityRequest;
import com.skillbridge.dto.sales.response.OpportunityDetailDTO;
import com.skillbridge.dto.sales.response.ProposalDTO;
import com.skillbridge.dto.sales.response.HistoryEntryDTO;
import com.skillbridge.dto.sales.response.ProposalVersionDTO;
import com.skillbridge.entity.auth.User;
import com.skillbridge.entity.contact.Contact;
import com.skillbridge.entity.opportunity.Opportunity;
import com.skillbridge.entity.proposal.Proposal;
import com.skillbridge.entity.proposal.ProposalHistory;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.repository.contact.ContactRepository;
import com.skillbridge.repository.opportunity.OpportunityRepository;
import com.skillbridge.repository.proposal.ProposalHistoryRepository;
import com.skillbridge.repository.proposal.ProposalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Sales Opportunity Detail Service
 * Handles business logic for opportunity detail operations
 */
@Service
public class SalesOpportunityDetailService {

    @Autowired
    private OpportunityRepository opportunityRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private ProposalHistoryRepository proposalHistoryRepository;

    private final Gson gson = new Gson();

    /**
     * Create opportunity from contact
     */
    @Transactional
    public OpportunityDetailDTO createFromContact(Integer contactId, CreateOpportunityRequest request, User currentUser) {
        // Fetch contact data
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new RuntimeException("Contact not found"));

        // Authorization check: Only assigned Sales Man can convert contact to opportunity
        if ("SALES_REP".equals(currentUser.getRole())) {
            if (contact.getAssigneeUserId() == null ||
                    !contact.getAssigneeUserId().equals(currentUser.getId())) {
                throw new RuntimeException("Access denied. Only assigned Sales Man can convert contact to opportunity");
            }
        }

        // Generate opportunity ID
        String opportunityId = generateOpportunityId();

        // Create opportunity entity
        Opportunity opportunity = new Opportunity();
        opportunity.setOpportunityId(opportunityId);
        opportunity.setContactId(contactId);
        opportunity.setClientName(request.getClientName());
        opportunity.setClientCompany(request.getClientCompany());
        opportunity.setClientEmail(request.getClientEmail());
        opportunity.setAssigneeUserId(request.getAssigneeUserId());
        opportunity.setProbability(request.getProbability() != null ? request.getProbability() : 0);
        opportunity.setEstValue(request.getEstValue() != null ? request.getEstValue() : BigDecimal.ZERO);
        opportunity.setCurrency(request.getCurrency() != null ? request.getCurrency() : "JPY");
        opportunity.setStatus("NEW");
        opportunity.setCreatedBy(currentUser.getId());

        opportunity = opportunityRepository.save(opportunity);

        // Update contact status to "Converted to Opportunity"
        contact.setStatus("Converted to Opportunity");
        contactRepository.save(contact);

        return convertToDetailDTO(opportunity);
    }

    /**
     * Get opportunity by ID with proposal
     * Supports both numeric ID and opportunityId string format (e.g., "OP-2025-01")
     */
    public OpportunityDetailDTO getOpportunityById(String opportunityId, User currentUser) {
        Opportunity opportunity;

        // Check if opportunityId is numeric (ID) or string format (OP-YYYY-NN)
        if (opportunityId.matches("\\d+")) {
            // Numeric ID - find by ID
            Integer id = Integer.parseInt(opportunityId);
            opportunity = opportunityRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Opportunity not found"));
        } else {
            // String format - find by opportunityId
            opportunity = opportunityRepository.findByOpportunityId(opportunityId)
                    .orElseThrow(() -> new RuntimeException("Opportunity not found"));
        }

        // Authorization check
        if ("SALES_REP".equals(currentUser.getRole())) {
            if (opportunity.getCreatedBy() == null ||
                    !opportunity.getCreatedBy().equals(currentUser.getId())) {
                throw new RuntimeException("Access denied. You can only view opportunities created by you");
            }
        }

        OpportunityDetailDTO dto = convertToDetailDTO(opportunity);

        // Load current proposal if exists
        Optional<Proposal> currentProposalOpt = proposalRepository.findByOpportunityIdAndIsCurrent(opportunity.getId(), true);
        if (currentProposalOpt.isPresent()) {
            Proposal proposal = currentProposalOpt.get();
            ProposalDTO proposalDTO = convertProposalToDTO(proposal);

            // Calculate if proposal can be edited
            proposalDTO.setCanEdit(canEditProposal(proposal));

            dto.setProposal(proposalDTO);
        }

        // Load all proposal versions
        List<Proposal> allProposals = proposalRepository.findByOpportunityIdOrderByVersionDesc(opportunity.getId());
        List<ProposalVersionDTO> proposalVersions = allProposals.stream()
                .map(this::convertToProposalVersionDTO)
                .collect(java.util.stream.Collectors.toList());
        dto.setProposalVersions(proposalVersions);

        // Load history
        List<ProposalHistory> historyList = proposalHistoryRepository.findByOpportunityIdOrderByCreatedAtDesc(opportunity.getId());
        List<HistoryEntryDTO> history = historyList.stream()
                .map(this::convertToHistoryEntryDTO)
                .collect(java.util.stream.Collectors.toList());
        dto.setHistory(history);

        // Check if can convert to contract (if any proposal is approved)
        boolean canConvert = allProposals.stream()
                .anyMatch(p -> "approved".equals(p.getStatus()) && "APPROVE".equals(p.getReviewAction()));
        dto.setCanConvertToContract(canConvert);

        return dto;
    }

    /**
     * Update opportunity
     * Supports both numeric ID and opportunityId string format (e.g., "OP-2025-01")
     */
    @Transactional
    public OpportunityDetailDTO updateOpportunity(String opportunityId, UpdateOpportunityRequest request, User currentUser) {
        Opportunity opportunity;

        // Check if opportunityId is numeric (ID) or string format (OP-YYYY-NN)
        if (opportunityId.matches("\\d+")) {
            // Numeric ID - find by ID
            Integer id = Integer.parseInt(opportunityId);
            opportunity = opportunityRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Opportunity not found"));
        } else {
            // String format - find by opportunityId
            opportunity = opportunityRepository.findByOpportunityId(opportunityId)
                    .orElseThrow(() -> new RuntimeException("Opportunity not found"));
        }

        // Authorization check
        if ("SALES_REP".equals(currentUser.getRole())) {
            if (opportunity.getCreatedBy() == null ||
                    !opportunity.getCreatedBy().equals(currentUser.getId())) {
                throw new RuntimeException("Access denied. You can only update opportunities created by you");
            }
        }

        // Update fields
        if (request.getClientName() != null) opportunity.setClientName(request.getClientName());
        if (request.getClientCompany() != null) opportunity.setClientCompany(request.getClientCompany());
        if (request.getClientEmail() != null) opportunity.setClientEmail(request.getClientEmail());
        if (request.getAssigneeUserId() != null) opportunity.setAssigneeUserId(request.getAssigneeUserId());
        if (request.getProbability() != null) opportunity.setProbability(request.getProbability());
        if (request.getEstValue() != null) opportunity.setEstValue(request.getEstValue());
        if (request.getCurrency() != null) opportunity.setCurrency(request.getCurrency());

        // Recalculate status based on proposal state
        opportunity.setStatus(calculateOpportunityStatus(opportunity));

        opportunity = opportunityRepository.save(opportunity);

        return convertToDetailDTO(opportunity);
    }

    /**
     * Calculate opportunity status based on proposal state
     */
    private String calculateOpportunityStatus(Opportunity opportunity) {
        // Get the current proposal (is_current = true) for this opportunity
        Optional<Proposal> proposalOpt = proposalRepository.findByOpportunityIdAndIsCurrent(opportunity.getId(), true);

        if (!proposalOpt.isPresent()) {
            return "NEW";
        }

        Proposal proposal = proposalOpt.get();
        String proposalStatus = proposal.getStatus();

        if (proposalStatus.equals("sent_to_client")) {
            return "CLIENT_UNDER_REVIEW";
        } else if (proposalStatus.equals("revision_requested")) {
            return "REVISION";
        } else if (proposalStatus.equals("draft") || proposalStatus.equals("internal_review")) {
            return "PROPOSAL_DRAFTING";
        } else if (proposalStatus.equals("approved")) {
            // When client approves proposal, opportunity is WON
            return "WON";
        }

        return "NEW";
    }

    /**
     * Generate opportunity ID in format OP-YYYY-NN
     */
    private String generateOpportunityId() {
        int year = LocalDate.now().getYear();

        // Count opportunities in the same year
        long countInYear = opportunityRepository.findAll().stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().getYear() == year)
                .count();

        // Format: OP-YYYY-NN (NN is 2 digits, zero-padded)
        return String.format("OP-%d-%02d", year, countInYear + 1);
    }

    /**
     * Convert Opportunity entity to OpportunityDetailDTO
     */
    private OpportunityDetailDTO convertToDetailDTO(Opportunity opportunity) {
        OpportunityDetailDTO dto = new OpportunityDetailDTO();
        dto.setId(opportunity.getId());
        dto.setOpportunityId(opportunity.getOpportunityId());
        dto.setContactId(opportunity.getContactId());
        dto.setClientName(opportunity.getClientName());
        dto.setClientCompany(opportunity.getClientCompany());
        dto.setClientEmail(opportunity.getClientEmail());
        dto.setAssigneeUserId(opportunity.getAssigneeUserId());
        dto.setProbability(opportunity.getProbability());
        dto.setEstValue(opportunity.getEstValue());
        dto.setCurrency(opportunity.getCurrency());
        dto.setStatus(opportunity.getStatus());
        dto.setCreatedBy(opportunity.getCreatedBy());

        // Calculate stage
        String stage = calculateStage(opportunity);
        dto.setStage(stage);

        // Load assignee name
        if (opportunity.getAssigneeUserId() != null) {
            userRepository.findById(opportunity.getAssigneeUserId()).ifPresent(user -> {
                dto.setAssigneeName(user.getFullName());
            });
        }

        // Load creator name
        if (opportunity.getCreatedBy() != null) {
            userRepository.findById(opportunity.getCreatedBy()).ifPresent(user -> {
                dto.setCreatedByName(user.getFullName());
            });
        }

        return dto;
    }

    /**
     * Calculate stage based on opportunity status
     */
    private String calculateStage(Opportunity opportunity) {
        String status = opportunity.getStatus();
        if (status == null) {
            return "New";
        }

        switch (status) {
            case "NEW":
                return "New";
            case "PROPOSAL_DRAFTING":
            case "PROPOSAL_SENT":
            case "REVISION":
                return "Proposal";
            case "WON":
                return "Won";
            case "LOST":
                return "Lost";
            default:
                return "New";
        }
    }

    /**
     * Convert Proposal entity to ProposalDTO
     */
    private ProposalDTO convertProposalToDTO(Proposal proposal) {
        ProposalDTO dto = new ProposalDTO();
        dto.setId(proposal.getId());
        dto.setOpportunityId(proposal.getOpportunityId());
        dto.setTitle(proposal.getTitle());
        dto.setStatus(proposal.getStatus());
        dto.setReviewerId(proposal.getReviewerId());
        dto.setReviewNotes(proposal.getReviewNotes());
        dto.setReviewAction(proposal.getReviewAction());
        dto.setReviewSubmittedAt(proposal.getReviewSubmittedAt());
        dto.setLink(proposal.getLink());
        dto.setCreatedBy(proposal.getCreatedBy());
        dto.setCreatedAt(proposal.getCreatedAt());
        dto.setUpdatedAt(proposal.getUpdatedAt());

        // Parse attachments_manifest JSON
        if (proposal.getAttachmentsManifest() != null && !proposal.getAttachmentsManifest().isEmpty()) {
            try {
                Type listType = new TypeToken<List<String>>(){}.getType();
                List<String> attachments = gson.fromJson(proposal.getAttachmentsManifest(), listType);
                dto.setAttachments(attachments);
            } catch (Exception e) {
                // If parsing fails, use link as single attachment
                List<String> attachments = new ArrayList<>();
                if (proposal.getLink() != null) {
                    attachments.add(proposal.getLink());
                }
                dto.setAttachments(attachments);
            }
        } else if (proposal.getLink() != null) {
            // If no manifest but has link, use link as single attachment
            List<String> attachments = new ArrayList<>();
            attachments.add(proposal.getLink());
            dto.setAttachments(attachments);
        }

        // Load reviewer name
        if (proposal.getReviewerId() != null) {
            userRepository.findById(proposal.getReviewerId()).ifPresent(user -> {
                dto.setReviewerName(user.getFullName());
            });
        }

        // Load creator name
        if (proposal.getCreatedBy() != null) {
            userRepository.findById(proposal.getCreatedBy()).ifPresent(user -> {
                dto.setCreatedByName(user.getFullName());
            });
        }

        // Set client feedback
        dto.setClientFeedback(proposal.getClientFeedback());

        return dto;
    }

    /**
     * Check if proposal can be edited
     */
    private boolean canEditProposal(Proposal proposal) {
        // Can edit if no reviewer assigned, or reviewer assigned but not yet saved (draft state)
        return proposal.getReviewerId() == null ||
                (proposal.getStatus().equals("draft") && proposal.getReviewSubmittedAt() == null);
    }

    /**
     * Mark opportunity as lost
     * Supports both numeric ID and opportunityId string format (e.g., "OP-2025-01")
     */
    @Transactional
    public OpportunityDetailDTO markAsLost(String opportunityId, User currentUser) {
        Opportunity opportunity;

        // Check if opportunityId is numeric (ID) or string format (OP-YYYY-NN)
        if (opportunityId.matches("\\d+")) {
            // Numeric ID - find by ID
            Integer id = Integer.parseInt(opportunityId);
            opportunity = opportunityRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Opportunity not found"));
        } else {
            // String format - find by opportunityId
            opportunity = opportunityRepository.findByOpportunityId(opportunityId)
                    .orElseThrow(() -> new RuntimeException("Opportunity not found"));
        }

        // Authorization check
        if ("SALES_REP".equals(currentUser.getRole())) {
            if (opportunity.getCreatedBy() == null ||
                    !opportunity.getCreatedBy().equals(currentUser.getId())) {
                throw new RuntimeException("Access denied. You can only mark opportunities created by you as lost");
            }
        }

        opportunity.setStatus("LOST");
        opportunity = opportunityRepository.save(opportunity);

        return convertToDetailDTO(opportunity);
    }

    /**
     * Convert opportunity to contract
     * Only allowed when client has approved a proposal
     * Supports both numeric ID and opportunityId string format (e.g., "OP-2025-01")
     */
    @Transactional
    public OpportunityDetailDTO convertToContract(String opportunityId, User currentUser) {
        Opportunity opportunity;

        // Check if opportunityId is numeric (ID) or string format (OP-YYYY-NN)
        if (opportunityId.matches("\\d+")) {
            // Numeric ID - find by ID
            Integer id = Integer.parseInt(opportunityId);
            opportunity = opportunityRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Opportunity not found"));
        } else {
            // String format - find by opportunityId
            opportunity = opportunityRepository.findByOpportunityId(opportunityId)
                    .orElseThrow(() -> new RuntimeException("Opportunity not found"));
        }

        // Authorization check
        if ("SALES_REP".equals(currentUser.getRole())) {
            if (opportunity.getCreatedBy() == null ||
                    !opportunity.getCreatedBy().equals(currentUser.getId())) {
                throw new RuntimeException("Access denied. You can only convert opportunities created by you");
            }
        }

        // Check if client approved a proposal
        List<Proposal> proposals = proposalRepository.findByOpportunityIdOrderByVersionDesc(opportunity.getId());
        Proposal approvedProposal = proposals.stream()
                .filter(p -> "approved".equals(p.getStatus()) && "APPROVE".equals(p.getReviewAction()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No approved proposal found. Cannot convert to contract"));

        // Update opportunity status to "Won"
        opportunity.setStatus("WON");
        opportunity = opportunityRepository.save(opportunity);

        // Update proposal status
        approvedProposal.setStatus("converted_to_contract");
        proposalRepository.save(approvedProposal);

        return convertToDetailDTO(opportunity);
    }

    /**
     * Convert Proposal to ProposalVersionDTO
     */
    private ProposalVersionDTO convertToProposalVersionDTO(Proposal proposal) {
        ProposalVersionDTO dto = new ProposalVersionDTO();
        dto.setId(proposal.getId());
        dto.setVersion(proposal.getVersion());
        dto.setName("v" + proposal.getVersion());
        dto.setTitle(proposal.getTitle());
        dto.setCreatedBy(proposal.getCreatedBy());
        dto.setCreatedAt(proposal.getCreatedAt());
        dto.setStatus(proposal.getStatus());
        dto.setReviewerId(proposal.getReviewerId());
        dto.setInternalFeedback(proposal.getReviewNotes());
        dto.setClientFeedback(proposal.getClientFeedback());
        dto.setIsCurrent(proposal.getIsCurrent());

        // Load creator name
        if (proposal.getCreatedBy() != null) {
            userRepository.findById(proposal.getCreatedBy()).ifPresent(user -> {
                dto.setCreatedByName(user.getFullName());
            });
        }

        // Load reviewer name
        if (proposal.getReviewerId() != null) {
            userRepository.findById(proposal.getReviewerId()).ifPresent(user -> {
                dto.setReviewerName(user.getFullName());
            });
        }

        return dto;
    }

    /**
     * Convert ProposalHistory to HistoryEntryDTO
     */
    private HistoryEntryDTO convertToHistoryEntryDTO(ProposalHistory history) {
        HistoryEntryDTO dto = new HistoryEntryDTO();
        dto.setId(history.getId());
        dto.setActivity(history.getActivityDescription());
        dto.setFileLink(history.getFileLink());
        dto.setFileUrl(history.getFileUrl());
        dto.setCreatedAt(history.getCreatedAt());

        // Format date as YYYY/MM/DD
        if (history.getCreatedAt() != null) {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd");
            dto.setDate(history.getCreatedAt().format(formatter));
        }

        return dto;
    }
}

