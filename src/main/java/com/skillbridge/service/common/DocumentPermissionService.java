package com.skillbridge.service.common;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.skillbridge.entity.auth.User;
import com.skillbridge.entity.contact.Contact;
import com.skillbridge.entity.contract.Contract;
import com.skillbridge.entity.contract.SOWContract;
import com.skillbridge.entity.document.DocumentMetadata;
import com.skillbridge.entity.proposal.Proposal;
import com.skillbridge.repository.contact.ContactRepository;
import com.skillbridge.repository.contract.ContractRepository;
import com.skillbridge.repository.contract.SOWContractRepository;
import com.skillbridge.repository.document.DocumentMetadataRepository;
import com.skillbridge.repository.proposal.ProposalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

/**
 * Document Permission Service
 * Checks user permissions for accessing documents
 */
@Service
public class DocumentPermissionService {

    @Autowired
    private DocumentMetadataRepository documentMetadataRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private SOWContractRepository sowContractRepository;

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private ContactRepository contactRepository;

    private final Gson gson = new Gson();

    /**
     * Check if user has permission to access document
     * @param s3Key S3 key of the document
     * @param user Current user
     * @return true if user has permission, false otherwise
     */
    public boolean hasPermission(String s3Key, User user) {
        if (user == null) {
            return false;
        }

        DocumentMetadata metadata = documentMetadataRepository.findByS3Key(s3Key)
                .orElse(null);

        // If no metadata found, try fallback check for proposal (legacy support)
        if (metadata == null) {
            // Check if this might be a proposal document (S3 key contains "proposals/")
            if (s3Key != null && s3Key.contains("proposals/")) {
                // For CLIENT/CLIENT_USER, check if they have access to any proposal with this S3 key
                if ("CLIENT".equals(user.getRole()) || "CLIENT_USER".equals(user.getRole())) {
                    // Find proposal by link (s3Key)
                    List<Proposal> proposals = proposalRepository.findAll();
                    for (Proposal proposal : proposals) {
                        if (s3Key.equals(proposal.getLink())) {
                            // Found proposal with this link, check if contact belongs to client
                            if (proposal.getContactId() != null) {
                                Optional<Contact> contactOpt = contactRepository.findById(proposal.getContactId());
                                if (contactOpt.isPresent()) {
                                    Contact contact = contactOpt.get();
                                    if (contact.getClientUserId() != null && contact.getClientUserId().equals(user.getId())) {
                                        return true; // Client has access to this proposal
                                    }
                                }
                            }
                        }
                        // Also check attachments manifest (JSON array of file links)
                        if (proposal.getAttachmentsManifest() != null && !proposal.getAttachmentsManifest().isEmpty()) {
                            try {
                                Type listType = new TypeToken<List<String>>(){}.getType();
                                List<String> attachments = gson.fromJson(proposal.getAttachmentsManifest(), listType);
                                if (attachments != null && attachments.contains(s3Key)) {
                                    // Found proposal with this attachment, check if contact belongs to client
                                    if (proposal.getContactId() != null) {
                                        Optional<Contact> contactOpt = contactRepository.findById(proposal.getContactId());
                                        if (contactOpt.isPresent()) {
                                            Contact contact = contactOpt.get();
                                            if (contact.getClientUserId() != null && contact.getClientUserId().equals(user.getId())) {
                                                return true; // Client has access to this proposal attachment
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                // Invalid JSON, skip
                            }
                        }
                    }
                }
            }
            // If no metadata found and not a proposal, deny access
            return false;
        }

        // Owner always has access
        if (metadata.getOwnerId().equals(user.getId())) {
            return true;
        }

        // Admin always has access
        if ("ADMIN".equals(user.getRole())) {
            return true;
        }

        // Check allowed roles
        if (metadata.getAllowedRoles() != null && !metadata.getAllowedRoles().isEmpty()) {
            Type listType = new TypeToken<List<String>>(){}.getType();
            List<String> allowedRoles = gson.fromJson(metadata.getAllowedRoles(), listType);
            if (allowedRoles != null && allowedRoles.contains(user.getRole())) {
                return true;
            }
        }

        // Check allowed users
        if (metadata.getAllowedUsers() != null && !metadata.getAllowedUsers().isEmpty()) {
            Type listType = new TypeToken<List<Integer>>(){}.getType();
            List<Integer> allowedUsers = gson.fromJson(metadata.getAllowedUsers(), listType);
            if (allowedUsers != null && allowedUsers.contains(user.getId())) {
                return true;
            }
        }

        // Check if document belongs to a contract/proposal and user is the client of that entity
        if (metadata.getEntityType() != null && metadata.getEntityId() != null) {
            String entityType = metadata.getEntityType();
            Integer entityId = metadata.getEntityId();

            // Check if user is CLIENT or CLIENT_USER
            if ("CLIENT".equals(user.getRole()) || "CLIENT_USER".equals(user.getRole())) {
                // Check proposal - client can view proposals for their contacts
                if ("proposal".equals(entityType)) {
                    Optional<Proposal> proposalOpt = proposalRepository.findById(entityId);
                    if (proposalOpt.isPresent()) {
                        Proposal proposal = proposalOpt.get();
                        // Check if proposal has a contact_id
                        if (proposal.getContactId() != null) {
                            Optional<Contact> contactOpt = contactRepository.findById(proposal.getContactId());
                            if (contactOpt.isPresent()) {
                                Contact contact = contactOpt.get();
                                // Check if contact belongs to this client
                                if (contact.getClientUserId() != null && contact.getClientUserId().equals(user.getId())) {
                                    return true; // User is the client of this contact's proposal
                                }
                            }
                        }
                    }
                }
                // Check MSA contract
                else if ("msa_contract".equals(entityType)) {
                    Optional<Contract> contractOpt = contractRepository.findByIdAndClientId(entityId, user.getId());
                    if (contractOpt.isPresent()) {
                        return true; // User is the client of this MSA contract
                    }
                }
                // Check SOW contract
                else if ("sow_contract".equals(entityType)) {
                    Optional<SOWContract> sowContractOpt = sowContractRepository.findByIdAndClientId(entityId, user.getId());
                    if (sowContractOpt.isPresent()) {
                        return true; // User is the client of this SOW contract
                    }
                }
            }
        }

        return false;
    }

    /**
     * Check if user can delete document (only admin or owner)
     * @param s3Key S3 key of the document
     * @param user Current user
     * @return true if user can delete, false otherwise
     */
    public boolean canDelete(String s3Key, User user) {
        if (user == null) {
            return false;
        }

        DocumentMetadata metadata = documentMetadataRepository.findByS3Key(s3Key)
                .orElse(null);

        if (metadata == null) {
            return false;
        }

        // Admin or owner can delete
        return "ADMIN".equals(user.getRole()) || metadata.getOwnerId().equals(user.getId());
    }

    /**
     * Get document metadata
     * @param s3Key S3 key of the document
     * @return DocumentMetadata or null if not found
     */
    public DocumentMetadata getMetadata(String s3Key) {
        return documentMetadataRepository.findByS3Key(s3Key).orElse(null);
    }
}

