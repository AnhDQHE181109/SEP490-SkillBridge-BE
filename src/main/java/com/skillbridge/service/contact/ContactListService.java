package com.skillbridge.service.contact;

import com.skillbridge.dto.contact.request.CreateContactRequest;
import com.skillbridge.dto.contact.response.ContactListItemDTO;
import com.skillbridge.dto.contact.response.ContactListResponse;
import com.skillbridge.dto.contact.response.CreateContactResponse;
import com.skillbridge.entity.contact.Contact;
import com.skillbridge.repository.contact.ContactRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Contact List Service
 * Handles business logic for contact list operations
 */
@Service
public class ContactListService {

    @Autowired
    private ContactRepository contactRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /**
     * Get contacts for client with search, filter, and pagination
     */
    public ContactListResponse getContactsForClient(
        Integer clientUserId,
        String search,
        String status,
        int page,
        int size
    ) {
        // Normalize search query
        String searchQuery = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        
        // Normalize status filter
        String statusFilter = (status != null && !status.trim().isEmpty() && !status.equals("All")) 
            ? status.trim() 
            : null;

        // Create pageable with sorting by created_at DESC
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Execute query
        Page<Contact> contactPage = contactRepository.findContactsForClient(
            clientUserId,
            searchQuery,
            statusFilter,
            pageable
        );

        // Convert to DTOs
        List<Contact> contacts = contactPage.getContent();
        List<ContactListItemDTO> contactDTOs = IntStream.range(0, contacts.size())
            .mapToObj(index -> convertToListItemDTO(contacts.get(index), page * size + index + 1))
            .collect(Collectors.toList());

        // Build response
        ContactListResponse response = new ContactListResponse();
        response.setContacts(contactDTOs);
        response.setCurrentPage(contactPage.getNumber());
        response.setTotalPages(contactPage.getTotalPages());
        response.setTotalElements(contactPage.getTotalElements());

        return response;
    }

    /**
     * Convert Contact entity to ContactListItemDTO
     */
    private ContactListItemDTO convertToListItemDTO(Contact contact, int no) {
        ContactListItemDTO dto = new ContactListItemDTO();
        dto.setNo(no);
        dto.setInternalId(contact.getId()); // Add internal ID for navigation
        dto.setId(generateContactId(contact));
        dto.setTitle(contact.getTitle() != null ? contact.getTitle() : "");
        dto.setDescription(contact.getDescription() != null ? contact.getDescription() : "");
        dto.setCreatedOn(contact.getCreatedAt() != null 
            ? contact.getCreatedAt().format(DATE_FORMATTER) 
            : "");
        dto.setStatus(contact.getStatus() != null ? contact.getStatus() : "New");
        return dto;
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
     * Create a new contact for authenticated client
     */
    @Transactional
    public CreateContactResponse createContact(Integer clientUserId, CreateContactRequest request) {
        // Validate request
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            return new CreateContactResponse(false, "Title is required", null);
        }

        // Create new contact
        Contact contact = new Contact();
        contact.setClientUserId(clientUserId);
        contact.setTitle(request.getTitle().trim());
        contact.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        contact.setStatus("New");
        contact.setRequestType("General Inquiry");
        contact.setPriority("Medium");
        contact.setCommunicationProgress("AutoReply");
        contact.setCreatedBy(clientUserId);
        contact.setCreatedAt(LocalDateTime.now());
        contact.setUpdatedAt(LocalDateTime.now());

        // Save contact
        Contact savedContact = contactRepository.save(contact);

        return new CreateContactResponse(true, "Contact created successfully", savedContact.getId());
    }
}

