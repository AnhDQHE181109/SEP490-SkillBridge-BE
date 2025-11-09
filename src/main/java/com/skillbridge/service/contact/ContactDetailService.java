package com.skillbridge.service.contact;

import com.skillbridge.dto.contact.request.CancelRequest;
import com.skillbridge.dto.contact.response.*;
import com.skillbridge.entity.auth.User;
import com.skillbridge.entity.contact.CommunicationLog;
import com.skillbridge.entity.contact.ConsultationCancellation;
import com.skillbridge.entity.contact.Contact;
import com.skillbridge.entity.proposal.Proposal;
import com.skillbridge.entity.proposal.ProposalComment;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.repository.contact.CommunicationLogRepository;
import com.skillbridge.repository.contact.ConsultationCancellationRepository;
import com.skillbridge.repository.contact.ContactRepository;
import com.skillbridge.repository.proposal.ProposalCommentRepository;
import com.skillbridge.repository.proposal.ProposalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Contact Detail Service
 * Handles business logic for contact detail operations
 */
@Service
public class ContactDetailService {

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private CommunicationLogRepository communicationLogRepository;

    @Autowired
    private ProposalCommentRepository proposalCommentRepository;

    @Autowired
    private ConsultationCancellationRepository cancellationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProposalRepository proposalRepository;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    /**
     * Get contact detail for client
     */
    public ContactDetailDTO getContactDetail(Integer contactId, Integer clientUserId) {
        // Validate contact belongs to client
        Contact contact = contactRepository.findByIdAndClientUserId(contactId, clientUserId)
            .orElseThrow(() -> new RuntimeException("Contact not found"));

        // Get client user information
        User clientUser = null;
        if (contact.getClientUserId() != null) {
            Optional<User> userOpt = userRepository.findById(contact.getClientUserId());
            clientUser = userOpt.orElse(null);
        }

        // Get communication logs
        List<CommunicationLog> logs = communicationLogRepository.findByContactIdOrderByCreatedAtDesc(contactId);

        // Get proposal for this contact (get the most recent one if multiple exist)
        List<Proposal> proposals = proposalRepository.findByContactId(contactId);
        Proposal proposal = null;
        if (proposals != null && !proposals.isEmpty()) {
            // Get the most recent proposal (sorted by createdAt DESC)
            proposal = proposals.stream()
                .sorted((p1, p2) -> {
                    if (p1.getCreatedAt() == null && p2.getCreatedAt() == null) return 0;
                    if (p1.getCreatedAt() == null) return 1;
                    if (p2.getCreatedAt() == null) return -1;
                    return p2.getCreatedAt().compareTo(p1.getCreatedAt());
                })
                .findFirst()
                .orElse(null);
        }

        // Get latest proposal comment if exists
        List<ProposalComment> comments = proposalCommentRepository.findByContactIdOrderByCreatedAtDesc(contactId);
        ProposalComment latestComment = null;
        if (comments != null && !comments.isEmpty()) {
            latestComment = comments.get(0); // First one is the latest (ordered DESC)
        }

        // Build DTO
        ContactDetailDTO dto = new ContactDetailDTO();
        dto.setContactId(contact.getId());
        dto.setId(generateContactId(contact)); // Format: CT-YYYY-NN
        dto.setClientName(clientUser != null ? (clientUser.getFullName() != null ? clientUser.getFullName() : "") : "");
        dto.setPhone(clientUser != null ? (clientUser.getPhone() != null ? clientUser.getPhone() : "") : "");
        dto.setEmail(clientUser != null ? (clientUser.getEmail() != null ? clientUser.getEmail() : "") : "");
        dto.setClientCompany(clientUser != null ? (clientUser.getCompanyName() != null ? clientUser.getCompanyName() : "-") : "-");
        dto.setDateReceived(contact.getCreatedAt() != null 
            ? contact.getCreatedAt().format(DATE_TIME_FORMATTER) + " JST"
            : "");
        dto.setConsultationRequest(contact.getDescription() != null ? contact.getDescription() : "");
        dto.setOnlineMtgDate(contact.getOnlineMtgDate() != null 
            ? contact.getOnlineMtgDate().format(DATE_TIME_FORMATTER) 
            : null);
        dto.setOnlineMtgLink(contact.getOnlineMtgLink());
        dto.setStatus(contact.getStatus() != null ? contact.getStatus() : "New");
        
        // Set proposal link and status from proposal entity (if exists)
        // Otherwise fallback to contact entity fields (for backward compatibility)
        if (proposal != null) {
            dto.setProposalLink(proposal.getLink());
            dto.setProposalStatus(mapProposalStatusToFrontend(proposal.getStatus()));
            
            // Set approved time if proposal is approved
            if ("Approved".equalsIgnoreCase(proposal.getStatus()) && proposal.getUpdatedAt() != null) {
                dto.setProposalApprovedAt(proposal.getUpdatedAt().format(DATE_TIME_FORMATTER));
            }
        } else {
            // Fallback to contact entity fields (may be null or old data)
            dto.setProposalLink(contact.getProposalLink());
            dto.setProposalStatus(contact.getProposalStatus() != null ? contact.getProposalStatus() : "Pending");
        }
        
        // Set latest proposal comment if exists
        if (latestComment != null) {
            ProposalCommentDTO commentDTO = new ProposalCommentDTO();
            commentDTO.setMessage(latestComment.getMessage());
            commentDTO.setCreatedAt(latestComment.getCreatedAt() != null 
                ? latestComment.getCreatedAt().format(DATE_TIME_FORMATTER) 
                : "");
            dto.setProposalComment(commentDTO);
        }
        
        dto.setCommunicationLogs(convertToLogDTOs(logs));

        return dto;
    }

    /**
     * Add communication log
     */
    @Transactional
    public CommunicationLogDTO addCommunicationLog(Integer contactId, Integer userId, String message) {
        // Validate contact belongs to user
        Contact contact = contactRepository.findByIdAndClientUserId(contactId, userId)
            .orElseThrow(() -> new RuntimeException("Contact not found"));

        // Create log
        CommunicationLog log = new CommunicationLog();
        log.setContactId(contactId);
        log.setMessage(message);
        log.setCreatedBy(userId);
        log = communicationLogRepository.save(log);

        // Get user for name
        Optional<User> userOpt = userRepository.findById(userId);
        String userName = userOpt.map(User::getFullName).orElse(null);

        return convertToLogDTO(log, userName);
    }

    /**
     * Add proposal comment
     */
    @Transactional
    public CommentResponse addProposalComment(Integer contactId, Integer userId, String message) {
        // Validate contact belongs to user
        Contact contact = contactRepository.findByIdAndClientUserId(contactId, userId)
            .orElseThrow(() -> new RuntimeException("Contact not found"));

        // Create comment
        ProposalComment comment = new ProposalComment();
        comment.setContactId(contactId);
        comment.setMessage(message);
        comment.setCreatedBy(userId);
        comment = proposalCommentRepository.save(comment);

        // Update proposal status to "Request for Change"
        List<Proposal> proposals = proposalRepository.findByContactId(contactId);
        if (proposals != null && !proposals.isEmpty()) {
            // Get the most recent proposal
            Proposal proposal = proposals.stream()
                .sorted((p1, p2) -> {
                    if (p1.getCreatedAt() == null && p2.getCreatedAt() == null) return 0;
                    if (p1.getCreatedAt() == null) return 1;
                    if (p2.getCreatedAt() == null) return -1;
                    return p2.getCreatedAt().compareTo(p1.getCreatedAt());
                })
                .findFirst()
                .orElse(null);
            
            if (proposal != null) {
                proposal.setStatus("Request for Change");
                proposalRepository.save(proposal);
            }
        }

        // Also update contact entity for backward compatibility
        contact.setProposalStatus("Request for Change");
        contactRepository.save(contact);

        return new CommentResponse(true, "Comment added successfully", comment.getId());
    }

    /**
     * Cancel consultation
     */
    @Transactional
    public CancelResponse cancelConsultation(Integer contactId, Integer userId, String reason) {
        // Validate contact belongs to user
        Contact contact = contactRepository.findByIdAndClientUserId(contactId, userId)
            .orElseThrow(() -> new RuntimeException("Contact not found"));

        // Create cancellation record
        ConsultationCancellation cancellation = new ConsultationCancellation();
        cancellation.setContactId(contactId);
        cancellation.setReason(reason);
        cancellation.setCancelledBy(userId);
        cancellationRepository.save(cancellation);

        // Update contact status
        contact.setStatus("Closed");
        contactRepository.save(contact);

        return new CancelResponse(true, "Consultation cancelled successfully");
    }

    /**
     * Approve proposal
     */
    @Transactional
    public ApproveResponse approveProposal(Integer contactId, Integer userId) {
        // Validate contact belongs to user
        Contact contact = contactRepository.findByIdAndClientUserId(contactId, userId)
            .orElseThrow(() -> new RuntimeException("Contact not found"));

        // Get proposal for this contact (get the most recent one if multiple exist)
        List<Proposal> proposals = proposalRepository.findByContactId(contactId);
        if (proposals != null && !proposals.isEmpty()) {
            // Get the most recent proposal
            Proposal proposal = proposals.stream()
                .sorted((p1, p2) -> {
                    if (p1.getCreatedAt() == null && p2.getCreatedAt() == null) return 0;
                    if (p1.getCreatedAt() == null) return 1;
                    if (p2.getCreatedAt() == null) return -1;
                    return p2.getCreatedAt().compareTo(p1.getCreatedAt());
                })
                .findFirst()
                .orElse(null);
            
            if (proposal != null) {
                // Update proposal status and updatedAt will be set automatically by @PreUpdate
                proposal.setStatus("Approved");
                proposalRepository.save(proposal);
            }
        }

        // Also update contact entity for backward compatibility
        contact.setProposalStatus("Approved");
        contactRepository.save(contact);

        return new ApproveResponse(true, "Proposal approved successfully");
    }

    /**
     * Map proposal status from backend to frontend format
     */
    private String mapProposalStatusToFrontend(String backendStatus) {
        if (backendStatus == null) {
            return "Pending";
        }
        switch (backendStatus.toLowerCase()) {
            case "draft":
            case "under review":
                return "Pending";
            case "reject":
            case "request for change":
                return "Request for Change";
            case "approved":
                return "Approved";
            default:
                return "Pending";
        }
    }

    /**
     * Generate Contact ID in format CT-yyyy-mm-dd-customer_id-contact_id
     */
    private String generateContactId(Contact contact) {
        if (contact.getCreatedAt() == null || contact.getClientUserId() == null || contact.getId() == null) {
            // Fallback format if missing data
            if (contact.getId() != null) {
                return "CT-0000-00-00-0-" + contact.getId();
            }
            return "CT-0000-00-00-0-0";
        }

        // Format date as yyyy-mm-dd
        java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String dateStr = contact.getCreatedAt().format(dateFormatter);
        
        // Format: CT-yyyy-mm-dd-customer_id-contact_id
        return String.format("CT-%s-%d-%d", dateStr, contact.getClientUserId(), contact.getId());
    }

    /**
     * Convert CommunicationLog list to DTO list
     */
    private List<CommunicationLogDTO> convertToLogDTOs(List<CommunicationLog> logs) {
        return logs.stream()
            .map(log -> {
                Optional<User> userOpt = userRepository.findById(log.getCreatedBy());
                String userName = userOpt.map(User::getFullName).orElse(null);
                return convertToLogDTO(log, userName);
            })
            .collect(Collectors.toList());
    }

    /**
     * Convert CommunicationLog to DTO
     */
    private CommunicationLogDTO convertToLogDTO(CommunicationLog log, String userName) {
        CommunicationLogDTO dto = new CommunicationLogDTO();
        dto.setId(log.getId());
        dto.setMessage(log.getMessage());
        dto.setCreatedAt(log.getCreatedAt() != null 
            ? log.getCreatedAt().format(DATE_TIME_FORMATTER) 
            : "");
        dto.setCreatedBy(log.getCreatedBy());
        dto.setCreatedByName(userName);
        return dto;
    }
}

