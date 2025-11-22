package com.skillbridge.service.sales;

import com.skillbridge.dto.sales.response.SalesContactListItemDTO;
import com.skillbridge.dto.sales.response.SalesContactListResponse;
import com.skillbridge.entity.auth.User;
import com.skillbridge.entity.contact.Contact;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.repository.contact.ContactRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sales Contact Service
 * Handles business logic for sales contact list with role-based filtering
 */
@Service
public class SalesContactService {

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private UserRepository userRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /**
     * Get contacts for sales users with role-based filtering
     * Sales Manager: all contacts
     * Sales Man: only assigned contacts
     */
    public SalesContactListResponse getContacts(
            String search,
            List<String> status,
            Integer assigneeUserId, // For Sales Manager filtering
            Integer currentUserId,
            String currentUserRole,
            int page,
            int size
    ) {
        // Build specification with role-based filtering
        Specification<Contact> spec = buildSpecification(search, status, assigneeUserId, currentUserId, currentUserRole);

        // Create pageable with sorting by createdAt descending
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Execute query
        Page<Contact> contactPage = contactRepository.findAll(spec, pageable);

        // Convert to DTOs
        List<SalesContactListItemDTO> contacts = contactPage.getContent().stream()
                .map(contact -> convertToDTO(contact, (page * size) + 1))
                .collect(Collectors.toList());

        // Update sequential numbers
        for (int i = 0; i < contacts.size(); i++) {
            contacts.get(i).setNo((page * size) + i + 1);
        }

        // Build response
        SalesContactListResponse response = new SalesContactListResponse();
        response.setContacts(contacts);
        response.setPage(page);
        response.setPageSize(size);
        response.setTotalPages(contactPage.getTotalPages());
        response.setTotal(contactPage.getTotalElements());

        return response;
    }

    /**
     * Build JPA Specification for filtering contacts
     */
    private Specification<Contact> buildSpecification(
            String search,
            List<String> status,
            Integer assigneeUserId,
            Integer currentUserId,
            String currentUserRole
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Role-based filtering: Sales Man only sees assigned contacts
            if ("SALES_REP".equals(currentUserRole)) {
                predicates.add(cb.equal(root.get("assigneeUserId"), currentUserId));
            } else if (assigneeUserId != null) {
                // Sales Manager can filter by assignee
                predicates.add(cb.equal(root.get("assigneeUserId"), assigneeUserId));
            }
            // Sales Manager without assignee filter sees all contacts

            // Search filter (searches in client name, email, company, title, description)
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.toLowerCase() + "%";

                // Join with client user for searching (only if clientUserId is not null)
                jakarta.persistence.criteria.Join<Contact, User> clientUserJoin = root.join("clientUser", jakarta.persistence.criteria.JoinType.LEFT);

                Predicate titlePredicate = cb.like(cb.lower(root.get("title")), searchPattern);
                Predicate descriptionPredicate = cb.like(cb.lower(root.get("description")), searchPattern);
                Predicate clientNamePredicate = cb.like(cb.lower(clientUserJoin.get("fullName")), searchPattern);
                Predicate clientEmailPredicate = cb.like(cb.lower(clientUserJoin.get("email")), searchPattern);
                Predicate companyPredicate = cb.like(cb.lower(clientUserJoin.get("companyName")), searchPattern);

                predicates.add(cb.or(
                        titlePredicate,
                        descriptionPredicate,
                        clientNamePredicate,
                        clientEmailPredicate,
                        companyPredicate
                ));
            }

            // Status filter
            if (status != null && !status.isEmpty()) {
                // Map status values to match database values
                List<String> dbStatuses = status.stream()
                        .map(s -> {
                            switch (s.toUpperCase()) {
                                case "NEW": return "New";
                                case "INPROGRESS": return "Inprogress";
                                case "COMPLETED": return "Completed";
                                case "CLOSED": return "Closed";
                                case "CONVERTED_TO_OPPORTUNITY": return "Converted to Opportunity";
                                default: return s;
                            }
                        })
                        .collect(Collectors.toList());
                predicates.add(root.get("status").in(dbStatuses));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Convert Contact entity to SalesContactListItemDTO
     */
    private SalesContactListItemDTO convertToDTO(Contact contact, int baseNo) {
        SalesContactListItemDTO dto = new SalesContactListItemDTO();

        // Generate contact ID format: CT-YYYY-NN
        String contactId = generateContactId(contact.getId(), contact.getCreatedAt());
        dto.setContactId(contactId);
        dto.setInternalId(contact.getId());

        // Get client user information
        User clientUser = null;
        if (contact.getClientUser() != null) {
            clientUser = contact.getClientUser();
        } else if (contact.getClientUserId() != null) {
            // Load client user if not already loaded
            clientUser = userRepository.findById(contact.getClientUserId()).orElse(null);
        }

        if (clientUser != null) {
            dto.setClientName(clientUser.getFullName() != null ? clientUser.getFullName() : "-");
            dto.setClientEmail(clientUser.getEmail() != null ? clientUser.getEmail() : "-");
            dto.setCompany(clientUser.getCompanyName() != null ? clientUser.getCompanyName() : "-");
        } else {
            dto.setClientName("-");
            dto.setClientEmail("-");
            dto.setCompany("-");
        }

        dto.setTitle(contact.getTitle());

        // Map status to uppercase format
        String status = contact.getStatus();
        if (status != null) {
            switch (status) {
                case "New": dto.setStatus("NEW"); break;
                case "Inprogress": dto.setStatus("INPROGRESS"); break;
                case "Completed": dto.setStatus("COMPLETED"); break;
                case "Closed": dto.setStatus("CLOSED"); break;
                case "Converted to Opportunity": dto.setStatus("CONVERTED_TO_OPPORTUNITY"); break;
                default: dto.setStatus(status.toUpperCase().replace(" ", "_"));
            }
        }

        // Get assignee information
        dto.setAssigneeUserId(contact.getAssigneeUserId());
        if (contact.getAssigneeUserId() != null) {
            User assignee = userRepository.findById(contact.getAssigneeUserId()).orElse(null);
            if (assignee != null) {
                dto.setAssigneeName(assignee.getFullName());
            }
        }

        return dto;
    }

    /**
     * Generate contact ID in format CT-YYYY-NN
     */
    private String generateContactId(Integer id, java.time.LocalDateTime createdAt) {
        if (createdAt == null) {
            createdAt = java.time.LocalDateTime.now();
        }
        int year = createdAt.getYear();
        // Use ID as sequence number (padded to 2 digits for display)
        String sequence = String.format("%02d", id % 100); // Use last 2 digits
        return String.format("CT-%d-%s", year, sequence);
    }
}

