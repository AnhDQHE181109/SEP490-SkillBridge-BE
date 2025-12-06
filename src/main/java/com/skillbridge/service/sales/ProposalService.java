package com.skillbridge.service.sales;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.skillbridge.dto.sales.request.AssignReviewerRequest;
import com.skillbridge.dto.sales.request.SubmitReviewRequest;
import com.skillbridge.dto.sales.response.ProposalDTO;
import com.skillbridge.entity.auth.User;
import com.skillbridge.entity.opportunity.Opportunity;
import com.skillbridge.entity.proposal.Proposal;
import com.skillbridge.entity.proposal.ProposalHistory;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.repository.opportunity.OpportunityRepository;
import com.skillbridge.repository.proposal.ProposalRepository;
import com.skillbridge.repository.proposal.ProposalHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.skillbridge.service.common.S3Service;
import com.skillbridge.service.common.DocumentPermissionService;
import com.skillbridge.entity.document.DocumentMetadata;
import com.skillbridge.repository.document.DocumentMetadataRepository;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.skillbridge.dto.common.AttachmentInfo;

/**
 * Proposal Service
 * Handles business logic for proposal operations
 */
@Service
public class ProposalService {

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private OpportunityRepository opportunityRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProposalHistoryRepository proposalHistoryRepository;

    @Autowired(required = false)
    private S3Service s3Service;

    @Autowired
    private DocumentMetadataRepository documentMetadataRepository;

    @Autowired
    private DocumentPermissionService documentPermissionService;

    @Value("${app.upload.dir:uploads/proposals}")
    private String uploadDir;

    @Value("${aws.s3.enabled:false}")
    private boolean s3Enabled;

    private final Gson gson = new Gson();

    /**
     * Create proposal for opportunity
     * Supports both numeric ID and opportunityId string format (e.g., "OP-2025-01")
     */
    @Transactional
    public ProposalDTO createProposal(String opportunityId, String title, Integer reviewerId, MultipartFile[] files, User currentUser) {
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

        // Check if proposal already exists - if exists, create new version
        Optional<Proposal> existingProposal = proposalRepository.findByOpportunityIdAndIsCurrent(opportunity.getId(), true);

        // Authorization check
        if ("SALES_REP".equals(currentUser.getRole())) {
            // SALES_REP can create proposal if they created the opportunity OR are assigned to it
            boolean canCreate = (opportunity.getCreatedBy() != null && opportunity.getCreatedBy().equals(currentUser.getId())) ||
                    (opportunity.getAssigneeUserId() != null && opportunity.getAssigneeUserId().equals(currentUser.getId()));
            if (!canCreate) {
                throw new RuntimeException("Access denied. You can only create proposals for opportunities created by you or assigned to you");
            }
        }

        // If existing proposal exists, mark it as not current and create new version
        if (existingProposal.isPresent()) {
            Proposal oldProposal = existingProposal.get();
            oldProposal.setIsCurrent(false);
            proposalRepository.save(oldProposal);
        }

        // Get next version number
        Integer maxVersion = proposalRepository.findMaxVersionByOpportunityId(opportunity.getId());
        Integer nextVersion = (maxVersion == null || maxVersion == 0) ? 1 : maxVersion + 1;

        // Create proposal
        Proposal proposal = new Proposal();
        proposal.setOpportunityId(opportunity.getId());
        proposal.setContactId(opportunity.getContactId()); // Set contact_id from opportunity if available
        proposal.setVersion(nextVersion);
        proposal.setIsCurrent(true);
        proposal.setTitle(title);
        proposal.setStatus("draft");
        proposal.setCreatedBy(currentUser.getId());

        // Set reviewer if provided
        if (reviewerId != null) {
            // Validate reviewer is a SALES_MANAGER
            User reviewer = userRepository.findById(reviewerId)
                    .orElseThrow(() -> new RuntimeException("Reviewer not found"));
            if (!"SALES_MANAGER".equals(reviewer.getRole())) {
                throw new RuntimeException("Reviewer must be a Sales Manager");
            }
            proposal.setReviewerId(reviewerId);
            proposal.setStatus("internal_review");
        }

        // Save proposal first to get ID
        proposal = proposalRepository.save(proposal);

        // Upload files and save metadata
        if (files != null && files.length > 0) {
            List<AttachmentInfo> fileInfos = uploadFiles(files, opportunity.getId(), proposal.getId(), currentUser.getId());
            if (!fileInfos.isEmpty()) {
                proposal.setLink(fileInfos.get(0).getS3Key()); // Store first file S3 key
                proposal.setAttachmentsManifest(gson.toJson(fileInfos));
                proposal = proposalRepository.save(proposal); // Update with file links
            }
        }

        // Create history entry
        createHistoryEntry(opportunity.getId(), proposal.getId(), "CREATED",
                "Proposal Draft v" + proposal.getVersion() + " created by " + currentUser.getFullName(),
                null, null, currentUser.getId());

        // Update opportunity status
        updateOpportunityStatus(opportunity);

        ProposalDTO dto = convertProposalToDTO(proposal);
        dto.setCanEdit(true); // New proposal can be edited
        return dto;
    }

    /**
     * Update proposal
     */
    @Transactional
    public ProposalDTO updateProposal(Integer proposalId, String title, Integer reviewerId, MultipartFile[] files, User currentUser) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new RuntimeException("Proposal not found"));

        // Check if proposal can be edited
        if (!canEditProposal(proposal)) {
            throw new RuntimeException("Proposal cannot be edited after reviewer assignment");
        }

        // Authorization check
        Opportunity opportunity = opportunityRepository.findById(proposal.getOpportunityId())
                .orElseThrow(() -> new RuntimeException("Opportunity not found"));

        if ("SALES_REP".equals(currentUser.getRole())) {
            // SALES_REP can update proposal if they created the opportunity OR are assigned to it
            boolean canUpdate = (opportunity.getCreatedBy() != null && opportunity.getCreatedBy().equals(currentUser.getId())) ||
                    (opportunity.getAssigneeUserId() != null && opportunity.getAssigneeUserId().equals(currentUser.getId()));
            if (!canUpdate) {
                throw new RuntimeException("Access denied. You can only update proposals for opportunities created by you or assigned to you");
            }
        }

        // Update title
        if (title != null && !title.isEmpty()) {
            proposal.setTitle(title);
        }

        // Update reviewer if provided and proposal can still be edited
        if (reviewerId != null && proposal.getReviewerId() == null) {
            // Validate reviewer is a SALES_MANAGER
            User reviewer = userRepository.findById(reviewerId)
                    .orElseThrow(() -> new RuntimeException("Reviewer not found"));
            if (!"SALES_MANAGER".equals(reviewer.getRole())) {
                throw new RuntimeException("Reviewer must be a Sales Manager");
            }
            proposal.setReviewerId(reviewerId);
            proposal.setStatus("internal_review");

            // Create history entry for reviewer assignment
            createHistoryEntry(opportunity.getId(), proposal.getId(), "REVIEWED",
                    "Reviewer assigned: " + reviewer.getFullName(),
                    null, null, currentUser.getId());
        }

        // Update files
        if (files != null && files.length > 0) {
            List<AttachmentInfo> fileInfos = uploadFiles(files, opportunity.getId(), proposal.getId(), currentUser.getId());
            if (!fileInfos.isEmpty()) {
                AttachmentInfo firstFile = fileInfos.get(0);
                proposal.setLink(firstFile.getS3Key());
                proposal.setAttachmentsManifest(gson.toJson(fileInfos));

                // Create history entry for file upload
                createHistoryEntry(opportunity.getId(), proposal.getId(), "UPLOADED",
                        "Proposal Draft v" + proposal.getVersion() + " uploaded by " + currentUser.getFullName(),
                    firstFile.getFileName(), firstFile.getS3Key(), currentUser.getId());
            }
        }

        proposal = proposalRepository.save(proposal);

        // Update opportunity status
        updateOpportunityStatus(opportunity);

        ProposalDTO dto = convertProposalToDTO(proposal);
        dto.setCanEdit(canEditProposal(proposal));
        return dto;
    }

    /**
     * Assign reviewer to proposal
     */
    @Transactional
    public ProposalDTO assignReviewer(Integer proposalId, Integer reviewerId, User currentUser) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new RuntimeException("Proposal not found"));

        // Verify reviewer is Sales Manager
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new RuntimeException("Reviewer not found"));

        if (!"SALES_MANAGER".equals(reviewer.getRole())) {
            throw new RuntimeException("Reviewer must be a Sales Manager");
        }

        // Assign reviewer
        proposal.setReviewerId(reviewerId);
        proposal.setStatus("internal_review");
        proposal = proposalRepository.save(proposal);

        // Create history entry
        Opportunity opportunity = opportunityRepository.findById(proposal.getOpportunityId())
                .orElseThrow(() -> new RuntimeException("Opportunity not found"));
        createHistoryEntry(opportunity.getId(), proposal.getId(), "REVIEWED",
                "Reviewer " + reviewer.getFullName() + " assigned to proposal v" + proposal.getVersion(),
                null, null, currentUser.getId());

        // Update opportunity status
        updateOpportunityStatus(opportunity);

        ProposalDTO dto = convertProposalToDTO(proposal);
        dto.setCanEdit(false); // Cannot edit after reviewer assignment
        return dto;
    }

    /**
     * Submit review
     */
    @Transactional
    public ProposalDTO submitReview(Integer proposalId, SubmitReviewRequest request, User currentUser) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new RuntimeException("Proposal not found"));

        // Authorization: Only assigned reviewer can submit review
        if (proposal.getReviewerId() == null ||
                !proposal.getReviewerId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied. Only assigned reviewer can submit review");
        }

        // Update proposal with review
        proposal.setReviewNotes(request.getReviewNotes());
        proposal.setReviewAction(request.getAction());
        proposal.setReviewSubmittedAt(LocalDateTime.now());

        // Update proposal status based on action
        // When Sales Manager approves, proposal is sent to client for review
        if ("APPROVE".equals(request.getAction())) {
            proposal.setStatus("sent_to_client");
        } else if ("REQUEST_REVISION".equals(request.getAction())) {
            proposal.setStatus("revision_requested");
        } else if ("REJECT".equals(request.getAction())) {
            proposal.setStatus("rejected");
        }

        proposal = proposalRepository.save(proposal);

        // Create history entry
        Opportunity opportunity = opportunityRepository.findById(proposal.getOpportunityId())
                .orElseThrow(() -> new RuntimeException("Opportunity not found"));
        String actionText = "APPROVE".equals(request.getAction()) ? "Approved" :
                "REQUEST_REVISION".equals(request.getAction()) ? "Requested Revision" : "Rejected";
        createHistoryEntry(opportunity.getId(), proposal.getId(), "REVIEWED",
                "Proposal v" + proposal.getVersion() + " " + actionText + " by " + currentUser.getFullName(),
                null, null, currentUser.getId());

        // Update opportunity status
        updateOpportunityStatus(opportunity);

        ProposalDTO dto = convertProposalToDTO(proposal);
        dto.setCanEdit(false); // Cannot edit after review submission
        return dto;
    }

    /**
     * Upload files to S3 or local file system and save metadata
     * @param files Files to upload
     * @param opportunityId Opportunity ID
     * @param proposalId Proposal ID (can be null for new proposals)
     * @param ownerId Owner user ID
     * @return List of S3 keys or local file paths
     */
    private List<AttachmentInfo> uploadFiles(MultipartFile[] files, Integer opportunityId, Integer proposalId, Integer ownerId) {
        List<AttachmentInfo> fileInfos = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            // Validate PDF file
            String contentType = file.getContentType();
            if (contentType == null || !contentType.equals("application/pdf")) {
                continue; // Skip non-PDF files
            }

            try {
                String originalFileName = file.getOriginalFilename();
                String s3Key;
                if (s3Enabled && s3Service != null) {
                    // Upload to S3 (returns S3 key)
                    s3Key = s3Service.uploadFile(file, "proposals");

                    // Save document metadata
                    DocumentMetadata metadata = new DocumentMetadata();
                    metadata.setS3Key(s3Key);
                    metadata.setOwnerId(ownerId);
                    metadata.setDocumentType("proposal");
                    metadata.setEntityId(proposalId);
                    metadata.setEntityType("proposal");
                    // Allow SALES_MANAGER and SALES_REP roles to access
                    metadata.setAllowedRoles(gson.toJson(Arrays.asList("SALES_MANAGER", "SALES_REP")));
                    documentMetadataRepository.save(metadata);
                } else {
                    // Fallback to local file system
                    s3Key = saveFileLocally(file, opportunityId);
                }
                fileInfos.add(new AttachmentInfo(s3Key, originalFileName));
            } catch (IOException e) {
                throw new RuntimeException("Failed to upload file: " + file.getOriginalFilename(), e);
            }
        }

        return fileInfos;
    }

    /**
     * Save file to local file system (fallback when S3 is not available)
     */
    private String saveFileLocally(MultipartFile file, Integer opportunityId) throws IOException {
        java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);
        if (!java.nio.file.Files.exists(uploadPath)) {
            java.nio.file.Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".pdf";
        String uniqueFilename = System.currentTimeMillis() + "_" + opportunityId + "_" + originalFilename;
        java.nio.file.Path filePath = uploadPath.resolve(uniqueFilename);

        file.transferTo(filePath.toFile());

        return "/uploads/proposals/" + uniqueFilename;
    }

    /**
     * Create history entry for proposal activity
     */
    private void createHistoryEntry(Integer opportunityId, Integer proposalId, String activityType,
                                    String activityDescription, String fileLink, String fileUrl, Integer createdBy) {
        ProposalHistory history = new ProposalHistory();
        history.setOpportunityId(opportunityId);
        history.setProposalId(proposalId);
        history.setActivityType(activityType);
        history.setActivityDescription(activityDescription);
        history.setFileLink(fileLink);
        history.setFileUrl(fileUrl);
        history.setCreatedBy(createdBy);
        proposalHistoryRepository.save(history);
    }

    /**
     * Update opportunity status based on proposal state
     */
    private void updateOpportunityStatus(Opportunity opportunity) {
        // Get the current proposal (is_current = true) for this opportunity
        Optional<Proposal> proposalOpt = proposalRepository.findByOpportunityIdAndIsCurrent(opportunity.getId(), true);

        if (!proposalOpt.isPresent()) {
            opportunity.setStatus("NEW");
            opportunityRepository.save(opportunity);
            return;
        }

        Proposal proposal = proposalOpt.get();
        String proposalStatus = proposal.getStatus();

        if (proposalStatus.equals("sent_to_client")) {
            // When proposal is sent to client (Sales Manager approved), opportunity is under client review
            opportunity.setStatus("CLIENT_UNDER_REVIEW");
        } else if (proposalStatus.equals("revision_requested")) {
            opportunity.setStatus("REVISION");
        } else if (proposalStatus.equals("draft") || proposalStatus.equals("internal_review")) {
            opportunity.setStatus("PROPOSAL_DRAFTING");
        } else if (proposalStatus.equals("approved")) {
            // When client approves proposal, opportunity is WON
            // This happens when client approves (not Sales Manager)
            opportunity.setStatus("WON");
        } else {
            opportunity.setStatus("NEW");
        }

        opportunityRepository.save(opportunity);
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
        List<ProposalDTO.AttachmentDTO> attachments = new ArrayList<>();
        if (proposal.getAttachmentsManifest() != null && !proposal.getAttachmentsManifest().isEmpty()) {
            try {
                // Try to parse as List<AttachmentInfo> (new format with fileName)
                Type attachmentInfoListType = new TypeToken<List<AttachmentInfo>>(){}.getType();
                List<AttachmentInfo> attachmentInfos = gson.fromJson(proposal.getAttachmentsManifest(), attachmentInfoListType);
                if (attachmentInfos != null && !attachmentInfos.isEmpty()) {
                    for (AttachmentInfo info : attachmentInfos) {
                        attachments.add(new ProposalDTO.AttachmentDTO(info.getS3Key(), info.getFileName(), null));
                    }
                } else {
                    // Fallback: try to parse as List<String> (old format)
                    Type stringListType = new TypeToken<List<String>>(){}.getType();
                    List<String> attachmentLinks = gson.fromJson(proposal.getAttachmentsManifest(), stringListType);
                    if (attachmentLinks != null) {
                        for (String s3Key : attachmentLinks) {
                            String fileName = s3Key;
                            if (fileName.contains("/")) {
                                fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                            }
                            attachments.add(new ProposalDTO.AttachmentDTO(s3Key, fileName, null));
                        }
                    }
                }
            } catch (Exception e) {
                // If parsing fails, try to parse as List<String> (old format)
                try {
                    Type stringListType = new TypeToken<List<String>>(){}.getType();
                    List<String> attachmentLinks = gson.fromJson(proposal.getAttachmentsManifest(), stringListType);
                    if (attachmentLinks != null) {
                        for (String s3Key : attachmentLinks) {
                            String fileName = s3Key;
                            if (fileName.contains("/")) {
                                fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                            }
                            attachments.add(new ProposalDTO.AttachmentDTO(s3Key, fileName, null));
                        }
                    }
                } catch (Exception e2) {
                    // If both fail, use link as single attachment
                if (proposal.getLink() != null) {
                        String fileName = proposal.getLink();
                        if (fileName.contains("/")) {
                            fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                        }
                        attachments.add(new ProposalDTO.AttachmentDTO(proposal.getLink(), fileName, null));
                }
                }
            }
        } else if (proposal.getLink() != null) {
            // If no manifest but has link, use link as single attachment
            String fileName = proposal.getLink();
            if (fileName.contains("/")) {
                fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
            }
            attachments.add(new ProposalDTO.AttachmentDTO(proposal.getLink(), fileName, null));
        }
            dto.setAttachments(attachments);

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
}

