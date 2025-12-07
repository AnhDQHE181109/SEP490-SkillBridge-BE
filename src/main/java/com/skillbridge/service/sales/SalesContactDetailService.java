package com.skillbridge.service.sales;

import com.skillbridge.dto.contact.response.CommunicationLogDTO;
import com.skillbridge.dto.sales.request.CreateCommunicationLogRequest;
import com.skillbridge.dto.sales.request.UpdateContactRequest;
import com.skillbridge.dto.sales.response.SalesContactDetailDTO;
import com.skillbridge.entity.auth.User;
import com.skillbridge.entity.contact.CommunicationLog;
import com.skillbridge.entity.contact.Contact;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.repository.contact.CommunicationLogRepository;
import com.skillbridge.repository.contact.ContactRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Sales Contact Detail Service
 * Handles business logic for sales contact detail operations with role-based permissions
 */
@Service
public class SalesContactDetailService {

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommunicationLogRepository communicationLogRepository;

    @Autowired(required = false)
    private com.skillbridge.service.common.EmailService emailService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
    private static final DateTimeFormatter DATE_TIME_FORMATTER_JST = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm").withZone(java.time.ZoneId.of("Asia/Tokyo"));

    /**
     * Get contact detail with role-based authorization
     */
    public SalesContactDetailDTO getContactDetail(Integer contactId, User currentUser) {
        Contact contact = contactRepository.findById(contactId)
            .orElseThrow(() -> new RuntimeException("Contact not found"));

        // Authorization check: Sales Man can only view assigned contacts
        if ("SALES_REP".equals(currentUser.getRole())) {
            if (contact.getAssigneeUserId() == null || 
                !contact.getAssigneeUserId().equals(currentUser.getId())) {
                throw new RuntimeException("Access denied. You can only view contacts assigned to you");
            }
        }

        return convertToDetailDTO(contact);
    }

    /**
     * Update contact with role-based field permissions
     */
    @Transactional
    public SalesContactDetailDTO updateContact(Integer contactId, UpdateContactRequest request, User currentUser) {
        Contact contact = contactRepository.findById(contactId)
            .orElseThrow(() -> new RuntimeException("Contact not found"));

        // Role-based field update authorization
        if ("SALES_MANAGER".equals(currentUser.getRole())) {
            // Sales Manager can only update: requestType, priority, assigneeUserId
            if (request.getRequestType() != null) {
                contact.setRequestType(request.getRequestType());
            }
            if (request.getPriority() != null) {
                contact.setPriority(request.getPriority());
            }
            if (request.getAssigneeUserId() != null) {
                // Sales Manager can assign to themselves
                User assignee = userRepository.findById(request.getAssigneeUserId())
                    .orElseThrow(() -> new RuntimeException("Assignee not found"));
                contact.setAssigneeUserId(request.getAssigneeUserId());
            }
        } else if ("SALES_REP".equals(currentUser.getRole())) {
            // Sales Man can only update if assigned to them
            if (contact.getAssigneeUserId() == null || 
                !contact.getAssigneeUserId().equals(currentUser.getId())) {
                throw new RuntimeException("Access denied. You can only edit contacts assigned to you");
            }

            // Sales Man can only update: status, internalNotes, onlineMtgLink, onlineMtgDateTime
            if (request.getStatus() != null) {
                contact.setStatus(request.getStatus());
            }
            if (request.getInternalNotes() != null) {
                contact.setInternalNote(request.getInternalNotes());
            }
            if (request.getOnlineMtgLink() != null) {
                contact.setOnlineMtgLink(request.getOnlineMtgLink());
            }
            if (request.getOnlineMtgDateTime() != null) {
                // Parse date-time string to LocalDateTime
                try {
                    LocalDateTime dateTime = LocalDateTime.parse(
                        request.getOnlineMtgDateTime().replace("/", "-"),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    );
                    contact.setOnlineMtgDate(dateTime);
                } catch (Exception e) {
                    // If parsing fails, try alternative format
                    try {
                        LocalDateTime dateTime = LocalDateTime.parse(
                            request.getOnlineMtgDateTime(),
                            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
                        );
                        contact.setOnlineMtgDate(dateTime);
                    } catch (Exception ex) {
                        throw new RuntimeException("Invalid date-time format. Expected: YYYY/MM/DD HH:MM");
                    }
                }
            }
        } else {
            throw new RuntimeException("Access denied. Invalid user role");
        }

        contact = contactRepository.save(contact);
        return convertToDetailDTO(contact);
    }

    /**
     * Convert contact to opportunity
     */
    @Transactional
    public SalesContactDetailDTO convertToOpportunity(Integer contactId, User currentUser) {
        Contact contact = contactRepository.findById(contactId)
            .orElseThrow(() -> new RuntimeException("Contact not found"));

        // Check if user is assigned to this contact
        if (contact.getAssigneeUserId() == null || 
            !contact.getAssigneeUserId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied. You can only convert contacts assigned to you");
        }

        // Allow both SALES_REP and SALES_MANAGER to convert if they are assigned
        // (Sales Manager can assign to themselves and convert)
        String role = currentUser.getRole();
        if (role == null || (!role.equals("SALES_REP") && !role.equals("SALES_MANAGER"))) {
            throw new RuntimeException("Access denied. Only Sales users can convert contact to opportunity");
        }

        contact.setStatus("Converted to Opportunity");
        // Note: We don't have a convertedToOpportunity field in Contact entity,
        // so we use status to track this. If needed, we can add a field later.
        contact = contactRepository.save(contact);

        return convertToDetailDTO(contact);
    }

    /**
     * Add communication log
     */
    @Transactional
    public CommunicationLogDTO addCommunicationLog(Integer contactId, CreateCommunicationLogRequest request, User currentUser) {
        Contact contact = contactRepository.findById(contactId)
            .orElseThrow(() -> new RuntimeException("Contact not found"));

        // Check if user is assigned to this contact
        if (contact.getAssigneeUserId() == null || 
            !contact.getAssigneeUserId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied. You can only add logs to contacts assigned to you");
        }

        // Allow both SALES_REP and SALES_MANAGER to add logs if they are assigned
        // (Sales Manager can assign to themselves and add logs)
        String role = currentUser.getRole();
        if (role == null || (!role.equals("SALES_REP") && !role.equals("SALES_MANAGER"))) {
            throw new RuntimeException("Access denied. Only Sales users can add communication logs");
        }

        CommunicationLog log = new CommunicationLog();
        log.setContactId(contact.getId());
        log.setMessage(request.getMessage());
        log.setCreatedBy(currentUser.getId());
        log.setCreatedAt(LocalDateTime.now());

        log = communicationLogRepository.save(log);
        
        // Get user name for the log
        String userName = currentUser.getFullName();
        return convertToLogDTO(log, userName);
    }

    /**
     * Send meeting email to client and save Online MTG Link and Date time
     */
    @Transactional
    public SalesContactDetailDTO sendMeetingEmail(Integer contactId, com.skillbridge.dto.sales.request.SendMeetingEmailRequest request, User currentUser) {
        Contact contact = contactRepository.findById(contactId)
            .orElseThrow(() -> new RuntimeException("Contact not found"));

        // Check if user is assigned to this contact
        if (contact.getAssigneeUserId() == null || 
            !contact.getAssigneeUserId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied. You can only send meeting emails for contacts assigned to you");
        }

        // Allow both SALES_REP and SALES_MANAGER to send meeting emails if they are assigned
        // (Sales Manager can assign to themselves and send emails)
        String role = currentUser.getRole();
        if (role == null || (!role.equals("SALES_REP") && !role.equals("SALES_MANAGER"))) {
            throw new RuntimeException("Access denied. Only Sales users can send meeting emails");
        }

        // Validate required fields
        if (request.getOnlineMtgLink() == null || request.getOnlineMtgLink().trim().isEmpty()) {
            throw new RuntimeException("Online meeting link is required");
        }
        if (request.getOnlineMtgDateTime() == null || request.getOnlineMtgDateTime().trim().isEmpty()) {
            throw new RuntimeException("Online meeting date-time is required");
        }

        // Save Online MTG Link and Date time to contact
        contact.setOnlineMtgLink(request.getOnlineMtgLink().trim());
        
        // Parse and save date-time
        try {
            LocalDateTime dateTime;
            String dateTimeStr = request.getOnlineMtgDateTime().trim();
            
            // Handle both formats: "YYYY-MM-DDTHH:mm" (from datetime-local input) and "YYYY/MM/DD HH:mm"
            if (dateTimeStr.contains("T")) {
                // Format: "YYYY-MM-DDTHH:mm"
                dateTime = LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            } else if (dateTimeStr.contains("/")) {
                // Format: "YYYY/MM/DD HH:mm"
                dateTime = LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"));
            } else {
                // Try default format
                dateTime = LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            }
            contact.setOnlineMtgDate(dateTime);
        } catch (Exception e) {
            throw new RuntimeException("Invalid date-time format. Expected format: YYYY-MM-DDTHH:mm or YYYY/MM/DD HH:mm");
        }

        // Save contact
        contact = contactRepository.save(contact);

        // Get client information
        String clientEmail = null;
        String clientName = null;
        if (contact.getClientUser() != null) {
            clientEmail = contact.getClientUser().getEmail();
            clientName = contact.getClientUser().getFullName();
        } else if (contact.getClientUserId() != null) {
            Optional<User> clientUserOpt = userRepository.findById(contact.getClientUserId());
            if (clientUserOpt.isPresent()) {
                User clientUser = clientUserOpt.get();
                clientEmail = clientUser.getEmail();
                clientName = clientUser.getFullName();
            }
        }

        if (clientEmail == null) {
            throw new RuntimeException("Client email not found");
        }

        // Format meeting date-time for email
        String meetingDateTime = contact.getOnlineMtgDate().format(DATE_TIME_FORMATTER);

        // Send email
        if (emailService != null) {
            emailService.sendMeetingInvitation(clientEmail, clientName, contact.getOnlineMtgLink(), meetingDateTime);
        } else {
            throw new RuntimeException("Email service is not configured");
        }

        // Return updated contact detail
        return convertToDetailDTO(contact);
    }

    /**
     * Convert Contact entity to SalesContactDetailDTO
     */
    private SalesContactDetailDTO convertToDetailDTO(Contact contact) {
        SalesContactDetailDTO dto = new SalesContactDetailDTO();
        dto.setContactId(contact.getId());
        dto.setId(generateContactId(contact.getId(), contact.getCreatedAt()));

        // Date received
        if (contact.getCreatedAt() != null) {
            // Convert LocalDateTime to ZonedDateTime with JST timezone, then format
            java.time.ZonedDateTime zonedDateTime = contact.getCreatedAt().atZone(java.time.ZoneId.of("Asia/Tokyo"));
            dto.setDateReceived(zonedDateTime.format(DATE_TIME_FORMATTER) + " JST");
        }

        // Client information
        if (contact.getClientUser() != null) {
            User clientUser = contact.getClientUser();
            dto.setClientName(clientUser.getFullName() != null ? clientUser.getFullName() : "");
            dto.setEmail(clientUser.getEmail() != null ? clientUser.getEmail() : "");
            dto.setPhone(clientUser.getPhone() != null ? clientUser.getPhone() : "");
            dto.setClientCompany(clientUser.getCompanyName() != null ? clientUser.getCompanyName() : "");
        } else if (contact.getClientUserId() != null) {
            // Load client user if not already loaded
            Optional<User> clientUserOpt = userRepository.findById(contact.getClientUserId());
            if (clientUserOpt.isPresent()) {
                User clientUser = clientUserOpt.get();
                dto.setClientName(clientUser.getFullName() != null ? clientUser.getFullName() : "");
                dto.setEmail(clientUser.getEmail() != null ? clientUser.getEmail() : "");
                dto.setPhone(clientUser.getPhone() != null ? clientUser.getPhone() : "");
                dto.setClientCompany(clientUser.getCompanyName() != null ? clientUser.getCompanyName() : "");
            }
        }

        // Consultation request (title + description)
        StringBuilder consultationRequest = new StringBuilder();
        if (contact.getTitle() != null && !contact.getTitle().trim().isEmpty()) {
            consultationRequest.append(contact.getTitle().trim());
        }
        if (contact.getDescription() != null && !contact.getDescription().trim().isEmpty()) {
            if (consultationRequest.length() > 0) {
                consultationRequest.append("\n\n");
            }
            consultationRequest.append(contact.getDescription().trim());
        }
        dto.setConsultationRequest(consultationRequest.toString());

        // Classification fields
        dto.setRequestType(contact.getRequestType() != null ? contact.getRequestType() : "");
        dto.setStatus(contact.getStatus() != null ? contact.getStatus() : "New");
        dto.setPriority(contact.getPriority() != null ? contact.getPriority() : "Normal");

        // Assignee
        dto.setAssigneeUserId(contact.getAssigneeUserId());
        if (contact.getAssigneeUserId() != null) {
            Optional<User> assigneeOpt = userRepository.findById(contact.getAssigneeUserId());
            if (assigneeOpt.isPresent()) {
                dto.setAssigneeName(assigneeOpt.get().getFullName());
            }
        }

        // Converted to opportunity (check status)
        dto.setConvertedToOpportunity("Converted to Opportunity".equals(contact.getStatus()));

        // Internal notes
        dto.setInternalNotes(contact.getInternalNote() != null ? contact.getInternalNote() : "");

        // Online meeting fields
        dto.setOnlineMtgLink(contact.getOnlineMtgLink() != null ? contact.getOnlineMtgLink() : "");
        if (contact.getOnlineMtgDate() != null) {
            dto.setOnlineMtgDateTime(contact.getOnlineMtgDate().format(DATE_TIME_FORMATTER));
        }

        // Communication logs
        List<CommunicationLog> logs = communicationLogRepository.findByContactIdOrderByCreatedAtDesc(contact.getId());
        dto.setCommunicationLogs(convertToLogDTOs(logs));

        return dto;
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

    /**
     * Generate contact ID in format CT-YYYY-NN
     */
    private String generateContactId(Integer id, LocalDateTime createdAt) {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        int year = createdAt.getYear();
        // Use ID as sequence number (padded to 2 digits for display)
        String sequence = String.format("%02d", id % 100); // Use last 2 digits
        return String.format("CT-%d-%s", year, sequence);
    }
}

