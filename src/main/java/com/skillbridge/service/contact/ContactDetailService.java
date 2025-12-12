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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.skillbridge.entity.opportunity.Opportunity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import com.skillbridge.dto.common.AttachmentInfo;

/**
 * Contact Detail Service
 * Handles business logic for contact detail operations
 */
@Service
public class ContactDetailService {
    
    private final Gson gson = new Gson();

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

    @Autowired
    private com.skillbridge.repository.opportunity.OpportunityRepository opportunityRepository;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    /**
     * Get contact detail for client
     */
    public ContactDetailDTO getContactDetail(Integer contactId, Integer clientUserId) {
        // Validate contact belongs to client
        Contact contact = contactRepository.findByIdAndClientUserId(contactId, clientUserId)
            .orElseThrow(() -> new IllegalArgumentException("Contact not found or does not belong to this client"));

        // Get client user information
        User clientUser = null;
        if (contact.getClientUserId() != null) {
            Optional<User> userOpt = userRepository.findById(contact.getClientUserId());
            clientUser = userOpt.orElse(null);
        }

        // Get communication logs
        List<CommunicationLog> logs = communicationLogRepository.findByContactIdOrderByCreatedAtDesc(contactId);

        // Get proposal for this contact
        // Priority: Get the most recent proposal that can be reviewed (sent_to_client, revision_requested, approved)
        // If no reviewable proposal exists, get the most recent proposal
        List<Proposal> proposals = proposalRepository.findByContactId(contactId);
        
        // Also check if contact has an opportunity and get proposals from opportunity
        // This ensures we get proposals created from opportunities even if contactId is set
        List<Opportunity> opportunities = opportunityRepository.findByContactId(contactId);
        
        // Get all proposals from opportunities linked to this contact
        List<Proposal> opportunityProposals = new ArrayList<>();
        for (Opportunity opp : opportunities) {
            List<Proposal> oppProposals = proposalRepository.findByOpportunityIdOrderByVersionDesc(opp.getId());
            if (oppProposals != null && !oppProposals.isEmpty()) {
                opportunityProposals.addAll(oppProposals);
            }
        }
        
        // Combine proposals from contactId and opportunityId
        List<Proposal> allProposals = new ArrayList<>();
        if (proposals != null && !proposals.isEmpty()) {
            allProposals.addAll(proposals);
        }
        if (!opportunityProposals.isEmpty()) {
            // Add opportunity proposals that are not already in the list (by ID)
            for (Proposal oppProp : opportunityProposals) {
                boolean exists = allProposals.stream().anyMatch(p -> p.getId().equals(oppProp.getId()));
                if (!exists) {
                    allProposals.add(oppProp);
                }
            }
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
        
        // Convert all proposals to DTOs and set in response
        List<ContactProposalDTO> proposalDTOs = new ArrayList<>();
        for (Proposal p : allProposals) {
            ContactProposalDTO proposalDTO = new ContactProposalDTO();
            proposalDTO.setId(p.getId());
            proposalDTO.setVersion(p.getVersion());
            proposalDTO.setTitle(p.getTitle());
            proposalDTO.setStatus(p.getStatus()); // Keep backend status
            proposalDTO.setProposalLink(p.getLink());
            proposalDTO.setIsCurrent(Boolean.TRUE.equals(p.getIsCurrent()));
            proposalDTO.setClientFeedback(p.getClientFeedback());
            
            if (p.getCreatedAt() != null) {
                proposalDTO.setCreatedAt(p.getCreatedAt().format(DATE_TIME_FORMATTER));
            }
            
            if ("approved".equalsIgnoreCase(p.getStatus()) && p.getUpdatedAt() != null) {
                proposalDTO.setProposalApprovedAt(p.getUpdatedAt().format(DATE_TIME_FORMATTER));
            }
            
            // Parse attachments_manifest JSON
            List<ContactProposalDTO.AttachmentDTO> attachments = new ArrayList<>();
            if (p.getAttachmentsManifest() != null && !p.getAttachmentsManifest().isEmpty()) {
                try {
                    // Try to parse as List<AttachmentInfo> (new format with fileName)
                    Type attachmentInfoListType = new TypeToken<List<AttachmentInfo>>(){}.getType();
                    List<AttachmentInfo> attachmentInfos = gson.fromJson(p.getAttachmentsManifest(), attachmentInfoListType);
                    if (attachmentInfos != null && !attachmentInfos.isEmpty()) {
                        for (AttachmentInfo info : attachmentInfos) {
                            attachments.add(new ContactProposalDTO.AttachmentDTO(info.getS3Key(), info.getFileName(), null));
                        }
                    } else {
                        // Fallback: try to parse as List<String> (old format)
                        Type stringListType = new TypeToken<List<String>>(){}.getType();
                        List<String> attachmentLinks = gson.fromJson(p.getAttachmentsManifest(), stringListType);
                        if (attachmentLinks != null) {
                            for (String s3Key : attachmentLinks) {
                                String fileName = s3Key;
                                if (fileName.contains("/")) {
                                    fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                                }
                                attachments.add(new ContactProposalDTO.AttachmentDTO(s3Key, fileName, null));
                            }
                        }
                    }
                } catch (Exception e) {
                    // If parsing as AttachmentInfo fails, try List<String> (old format)
                    try {
                        Type stringListType = new TypeToken<List<String>>(){}.getType();
                        List<String> attachmentLinks = gson.fromJson(p.getAttachmentsManifest(), stringListType);
                        if (attachmentLinks != null) {
                            for (String s3Key : attachmentLinks) {
                                String fileName = s3Key;
                                if (fileName.contains("/")) {
                                    fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                                }
                                attachments.add(new ContactProposalDTO.AttachmentDTO(s3Key, fileName, null));
                            }
                        }
                    } catch (Exception e2) {
                        // If both fail, use link as single attachment
                        if (p.getLink() != null) {
                            String fileName = p.getLink();
                            if (fileName.contains("/")) {
                                fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                            }
                            attachments.add(new ContactProposalDTO.AttachmentDTO(p.getLink(), fileName, null));
                        }
                    }
                }
            } else if (p.getLink() != null) {
                // If no manifest but has link, use link as single attachment
                String fileName = p.getLink();
                if (fileName.contains("/")) {
                    fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                }
                attachments.add(new ContactProposalDTO.AttachmentDTO(p.getLink(), fileName, null));
            }
            proposalDTO.setAttachments(attachments);
            
            proposalDTOs.add(proposalDTO);
        }
        
        // Sort proposals: newest first (by createdAt DESC)
        proposalDTOs.sort((p1, p2) -> {
            if (p1.getCreatedAt() == null && p2.getCreatedAt() == null) return 0;
            if (p1.getCreatedAt() == null) return 1;
            if (p2.getCreatedAt() == null) return -1;
            return p2.getCreatedAt().compareTo(p1.getCreatedAt());
        });
        
        dto.setProposals(proposalDTOs);
        
        // Set proposal link and status from the most recent reviewable proposal (for backward compatibility)
        // Priority: sent_to_client > approved > revision_requested
        Proposal displayProposal = null;
        if (!allProposals.isEmpty()) {
            List<Proposal> sentToClient = allProposals.stream()
                .filter(p -> "sent_to_client".equals(p.getStatus()))
                .sorted((p1, p2) -> {
                    if (p1.getCreatedAt() == null && p2.getCreatedAt() == null) return 0;
                    if (p1.getCreatedAt() == null) return 1;
                    if (p2.getCreatedAt() == null) return -1;
                    return p2.getCreatedAt().compareTo(p1.getCreatedAt());
                })
                .collect(Collectors.toList());
            
            if (!sentToClient.isEmpty()) {
                displayProposal = sentToClient.get(0);
            } else {
                List<Proposal> approved = allProposals.stream()
                    .filter(p -> "approved".equals(p.getStatus()))
                    .sorted((p1, p2) -> {
                        if (p1.getCreatedAt() == null && p2.getCreatedAt() == null) return 0;
                        if (p1.getCreatedAt() == null) return 1;
                        if (p2.getCreatedAt() == null) return -1;
                        return p2.getCreatedAt().compareTo(p1.getCreatedAt());
                    })
                    .collect(Collectors.toList());
                
                if (!approved.isEmpty()) {
                    displayProposal = approved.get(0);
                } else {
                    List<Proposal> revisionRequested = allProposals.stream()
                        .filter(p -> "revision_requested".equals(p.getStatus()))
                        .sorted((p1, p2) -> {
                            if (p1.getCreatedAt() == null && p2.getCreatedAt() == null) return 0;
                            if (p1.getCreatedAt() == null) return 1;
                            if (p2.getCreatedAt() == null) return -1;
                            return p2.getCreatedAt().compareTo(p1.getCreatedAt());
                        })
                        .collect(Collectors.toList());
                    
                    if (!revisionRequested.isEmpty()) {
                        displayProposal = revisionRequested.get(0);
                    }
                }
            }
        }
        
        if (displayProposal != null) {
            dto.setProposalLink(displayProposal.getLink());
            dto.setProposalStatus(mapProposalStatusToFrontend(displayProposal.getStatus()));
            
            // Set approved time if proposal is approved
            if ("approved".equalsIgnoreCase(displayProposal.getStatus()) && displayProposal.getUpdatedAt() != null) {
                dto.setProposalApprovedAt(displayProposal.getUpdatedAt().format(DATE_TIME_FORMATTER));
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

        // Update proposal: save client feedback and set status to "revision_requested"
        // First, try to find proposal by contactId
        List<Proposal> proposals = proposalRepository.findByContactId(contactId);
        Proposal proposal = null;
        
        if (proposals != null && !proposals.isEmpty()) {
            // Get the most recent proposal (current proposal) by contactId
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
        
        // If proposal found and has opportunityId, find the current proposal by opportunityId
        if (proposal != null && proposal.getOpportunityId() != null) {
            Optional<Proposal> currentProposalOpt = proposalRepository.findByOpportunityIdAndIsCurrent(
                proposal.getOpportunityId(), true);
            if (currentProposalOpt.isPresent()) {
                proposal = currentProposalOpt.get();
            }
        }
            
            if (proposal != null) {
            // Save client comment to clientFeedback field
            proposal.setClientFeedback(message);
            proposal.setStatus("revision_requested");
                proposalRepository.save(proposal);
            
            // Update opportunity status if proposal is linked to an opportunity
            if (proposal.getOpportunityId() != null) {
                opportunityRepository.findById(proposal.getOpportunityId()).ifPresent(opportunity -> {
                    opportunity.setStatus("REVISION");
                    opportunityRepository.save(opportunity);
                });
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

        // Get proposal for this contact
        // Priority: Get the current proposal (is_current = true) with status "sent_to_client"
        // This ensures we approve the correct proposal that the client is viewing
        List<Proposal> proposals = proposalRepository.findByContactId(contactId);
        Proposal proposal = null;
        
        // Also check if contact has an opportunity and get proposals from opportunity
        List<Opportunity> opportunities = opportunityRepository.findByContactId(contactId);
        
        // First, try to get the current proposal (is_current = true) with status "sent_to_client"
        for (Opportunity opp : opportunities) {
            Optional<Proposal> currentOpt = proposalRepository.findByOpportunityIdAndIsCurrent(opp.getId(), true);
            if (currentOpt.isPresent()) {
                Proposal candidate = currentOpt.get();
                // Only approve proposals with status "sent_to_client" (the one client is reviewing)
                if ("sent_to_client".equals(candidate.getStatus())) {
                    proposal = candidate;
                    break; // Found the correct proposal, exit loop
                }
            }
        }
        
        // If not found via opportunity, try to find from contactId proposals
        if (proposal == null && proposals != null && !proposals.isEmpty()) {
            // Filter proposals that can be reviewed (sent_to_client, revision_requested, approved)
            List<Proposal> reviewableProposals = proposals.stream()
                .filter(p -> {
                    String status = p.getStatus();
                    return "sent_to_client".equals(status) || 
                           "revision_requested".equals(status) || 
                           "approved".equals(status);
                })
                .sorted((p1, p2) -> {
                    // Priority: sent_to_client > approved > revision_requested
                    // Within same status, prioritize is_current = true, then by createdAt DESC
                    boolean p1Current = Boolean.TRUE.equals(p1.getIsCurrent());
                    boolean p2Current = Boolean.TRUE.equals(p2.getIsCurrent());
                    if (p1Current != p2Current) {
                        return p1Current ? -1 : 1;
                    }
                    // Sort by createdAt DESC (newest first)
                    if (p1.getCreatedAt() == null && p2.getCreatedAt() == null) return 0;
                    if (p1.getCreatedAt() == null) return 1;
                    if (p2.getCreatedAt() == null) return -1;
                    return p2.getCreatedAt().compareTo(p1.getCreatedAt());
                })
                .collect(Collectors.toList());
            
            // If reviewable proposals exist, get the first one (highest priority)
            if (!reviewableProposals.isEmpty()) {
                proposal = reviewableProposals.get(0);
            } else {
                // If no reviewable proposal, get the most recent proposal overall
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
        }
            
            if (proposal != null) {
            // Update proposal status to "approved" (lowercase) when client approves
            proposal.setStatus("approved");
                proposalRepository.save(proposal);
            
            // Update opportunity status to WON when client approves proposal
            if (proposal.getOpportunityId() != null) {
                opportunityRepository.findById(proposal.getOpportunityId()).ifPresent(opportunity -> {
                    opportunity.setStatus("WON");
                    opportunityRepository.save(opportunity);
                });
            }
        } else {
            throw new RuntimeException("No reviewable proposal found to approve");
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
            case "internal_review":
            case "under review":
                return "Pending";
            case "sent_to_client":
                // Proposal sent to client for review - client can approve or comment
                return "Pending";
            case "revision_requested":
            case "request for change":
                // Client has requested changes - status shows as "Request for Change"
                return "Request for Change";
            case "approved":
                return "Approved";
            case "rejected":
                return "Pending"; // Rejected proposals may still be reviewable
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

