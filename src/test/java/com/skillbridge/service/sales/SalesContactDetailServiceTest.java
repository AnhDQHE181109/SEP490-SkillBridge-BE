package com.skillbridge.service.sales;

import com.skillbridge.dto.contact.response.CommunicationLogDTO;
import com.skillbridge.dto.sales.request.CreateCommunicationLogRequest;
import com.skillbridge.dto.sales.request.SendMeetingEmailRequest;
import com.skillbridge.dto.sales.request.UpdateContactRequest;
import com.skillbridge.dto.sales.response.SalesContactDetailDTO;
import com.skillbridge.entity.auth.User;
import com.skillbridge.entity.contact.CommunicationLog;
import com.skillbridge.entity.contact.Contact;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.repository.contact.CommunicationLogRepository;
import com.skillbridge.repository.contact.ContactRepository;
import com.skillbridge.service.common.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SalesContactDetailService
 * Tests business logic for sales contact detail operations with role-based permissions
 */
@ExtendWith(MockitoExtension.class)
class SalesContactDetailServiceTest {

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommunicationLogRepository communicationLogRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private SalesContactDetailService salesContactDetailService;

    @BeforeEach
    void setUp() {
        // Setup is handled by MockitoExtension
    }

    @Test
    @DisplayName("getContactDetail - contact không tồn tại → throw RuntimeException")
    void testGetContactDetail_NotFound() {
        // Arrange
        Integer contactId = 1;
        User currentUser = createUser(1, "Sales User", "sales@example.com", "SALES_MANAGER");

        when(contactRepository.findById(contactId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            salesContactDetailService.getContactDetail(contactId, currentUser);
        });

        verify(contactRepository).findById(contactId);
    }

    @Test
    @DisplayName("getContactDetail - Sales Rep không được assign → throw RuntimeException")
    void testGetContactDetail_SalesRep_NotAssigned() {
        // Arrange
        Integer contactId = 1;
        User currentUser = createUser(1, "Sales Rep", "salesrep@example.com", "SALES_REP");
        Contact contact = createContact(contactId, 10, "Contact 1", "New", 2); // Assigned to user 2

        when(contactRepository.findById(contactId))
                .thenReturn(Optional.of(contact));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            salesContactDetailService.getContactDetail(contactId, currentUser);
        });
    }

    @Test
    @DisplayName("getContactDetail - Sales Manager → trả về contact detail")
    void testGetContactDetail_SalesManager_Success() {
        // Arrange
        Integer contactId = 1;
        User currentUser = createUser(1, "Sales Manager", "manager@example.com", "SALES_MANAGER");
        Contact contact = createContact(contactId, 10, "Contact 1", "New", 2);
        User clientUser = createUser(10, "Client User", "client@example.com", "CLIENT");

        when(contactRepository.findById(contactId))
                .thenReturn(Optional.of(contact));
        when(userRepository.findById(10)).thenReturn(Optional.of(clientUser));
        when(communicationLogRepository.findByContactIdOrderByCreatedAtDesc(contactId))
                .thenReturn(new ArrayList<>());

        // Act
        SalesContactDetailDTO result = salesContactDetailService.getContactDetail(contactId, currentUser);

        // Assert
        assertNotNull(result);
        assertEquals(contactId, result.getContactId());
        assertEquals("Contact 1", result.getConsultationRequest());
        assertEquals("New", result.getStatus());

        verify(contactRepository).findById(contactId);
    }

    @Test
    @DisplayName("updateContact - Sales Manager update requestType → thành công")
    void testUpdateContact_SalesManager_UpdateRequestType() {
        // Arrange
        Integer contactId = 1;
        User currentUser = createUser(1, "Sales Manager", "manager@example.com", "SALES_MANAGER");
        Contact contact = createContact(contactId, 10, "Contact 1", "New", 2);
        UpdateContactRequest request = new UpdateContactRequest();
        request.setRequestType("General Inquiry");

        when(contactRepository.findById(contactId))
                .thenReturn(Optional.of(contact));
        when(contactRepository.save(any(Contact.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(10)).thenReturn(Optional.of(createUser(10, "Client", "client@example.com", "CLIENT")));
        when(communicationLogRepository.findByContactIdOrderByCreatedAtDesc(contactId))
                .thenReturn(new ArrayList<>());

        // Act
        SalesContactDetailDTO result = salesContactDetailService.updateContact(contactId, request, currentUser);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<Contact> captor = ArgumentCaptor.forClass(Contact.class);
        verify(contactRepository).save(captor.capture());
        Contact saved = captor.getValue();
        assertEquals("General Inquiry", saved.getRequestType());
    }

    @Test
    @DisplayName("updateContact - Sales Rep không được assign → throw RuntimeException")
    void testUpdateContact_SalesRep_NotAssigned() {
        // Arrange
        Integer contactId = 1;
        User currentUser = createUser(1, "Sales Rep", "salesrep@example.com", "SALES_REP");
        Contact contact = createContact(contactId, 10, "Contact 1", "New", 2); // Assigned to user 2
        UpdateContactRequest request = new UpdateContactRequest();
        request.setStatus("In Progress");

        when(contactRepository.findById(contactId))
                .thenReturn(Optional.of(contact));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            salesContactDetailService.updateContact(contactId, request, currentUser);
        });

        verify(contactRepository, never()).save(any(Contact.class));
    }

    @Test
    @DisplayName("updateContact - Sales Rep update status → thành công")
    void testUpdateContact_SalesRep_UpdateStatus() {
        // Arrange
        Integer contactId = 1;
        User currentUser = createUser(1, "Sales Rep", "salesrep@example.com", "SALES_REP");
        Contact contact = createContact(contactId, 10, "Contact 1", "New", 1); // Assigned to user 1
        UpdateContactRequest request = new UpdateContactRequest();
        request.setStatus("In Progress");

        when(contactRepository.findById(contactId))
                .thenReturn(Optional.of(contact));
        when(contactRepository.save(any(Contact.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(10)).thenReturn(Optional.of(createUser(10, "Client", "client@example.com", "CLIENT")));
        when(communicationLogRepository.findByContactIdOrderByCreatedAtDesc(contactId))
                .thenReturn(new ArrayList<>());

        // Act
        SalesContactDetailDTO result = salesContactDetailService.updateContact(contactId, request, currentUser);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<Contact> captor = ArgumentCaptor.forClass(Contact.class);
        verify(contactRepository).save(captor.capture());
        Contact saved = captor.getValue();
        assertEquals("In Progress", saved.getStatus());
    }

    @Test
    @DisplayName("updateContact - Sales Rep update onlineMtgDateTime → thành công")
    void testUpdateContact_SalesRep_UpdateOnlineMtgDateTime() {
        // Arrange
        Integer contactId = 1;
        User currentUser = createUser(1, "Sales Rep", "salesrep@example.com", "SALES_REP");
        Contact contact = createContact(contactId, 10, "Contact 1", "New", 1);
        UpdateContactRequest request = new UpdateContactRequest();
        request.setOnlineMtgDateTime("2025/12/15 14:30");

        when(contactRepository.findById(contactId))
                .thenReturn(Optional.of(contact));
        when(contactRepository.save(any(Contact.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(10)).thenReturn(Optional.of(createUser(10, "Client", "client@example.com", "CLIENT")));
        when(communicationLogRepository.findByContactIdOrderByCreatedAtDesc(contactId))
                .thenReturn(new ArrayList<>());

        // Act
        SalesContactDetailDTO result = salesContactDetailService.updateContact(contactId, request, currentUser);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<Contact> captor = ArgumentCaptor.forClass(Contact.class);
        verify(contactRepository).save(captor.capture());
        Contact saved = captor.getValue();
        assertNotNull(saved.getOnlineMtgDate());
        assertEquals(2025, saved.getOnlineMtgDate().getYear());
        assertEquals(12, saved.getOnlineMtgDate().getMonthValue());
        assertEquals(15, saved.getOnlineMtgDate().getDayOfMonth());
    }

    @Test
    @DisplayName("updateContact - invalid date-time format → throw RuntimeException")
    void testUpdateContact_InvalidDateTimeFormat() {
        // Arrange
        Integer contactId = 1;
        User currentUser = createUser(1, "Sales Rep", "salesrep@example.com", "SALES_REP");
        Contact contact = createContact(contactId, 10, "Contact 1", "New", 1);
        UpdateContactRequest request = new UpdateContactRequest();
        request.setOnlineMtgDateTime("invalid-format");

        when(contactRepository.findById(contactId))
                .thenReturn(Optional.of(contact));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            salesContactDetailService.updateContact(contactId, request, currentUser);
        });
    }

    @Test
    @DisplayName("convertToOpportunity - Sales Rep không được assign → throw RuntimeException")
    void testConvertToOpportunity_SalesRep_NotAssigned() {
        // Arrange
        Integer contactId = 1;
        User currentUser = createUser(1, "Sales Rep", "salesrep@example.com", "SALES_REP");
        Contact contact = createContact(contactId, 10, "Contact 1", "New", 2); // Assigned to user 2

        when(contactRepository.findById(contactId))
                .thenReturn(Optional.of(contact));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            salesContactDetailService.convertToOpportunity(contactId, currentUser);
        });
    }

    @Test
    @DisplayName("convertToOpportunity - Sales Rep được assign → thành công")
    void testConvertToOpportunity_SalesRep_Success() {
        // Arrange
        Integer contactId = 1;
        User currentUser = createUser(1, "Sales Rep", "salesrep@example.com", "SALES_REP");
        Contact contact = createContact(contactId, 10, "Contact 1", "New", 1);

        when(contactRepository.findById(contactId))
                .thenReturn(Optional.of(contact));
        when(contactRepository.save(any(Contact.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(10)).thenReturn(Optional.of(createUser(10, "Client", "client@example.com", "CLIENT")));
        when(communicationLogRepository.findByContactIdOrderByCreatedAtDesc(contactId))
                .thenReturn(new ArrayList<>());

        // Act
        SalesContactDetailDTO result = salesContactDetailService.convertToOpportunity(contactId, currentUser);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<Contact> captor = ArgumentCaptor.forClass(Contact.class);
        verify(contactRepository).save(captor.capture());
        Contact saved = captor.getValue();
        assertEquals("Converted to Opportunity", saved.getStatus());
    }

    @Test
    @DisplayName("addCommunicationLog - Sales Rep không được assign → throw RuntimeException")
    void testAddCommunicationLog_SalesRep_NotAssigned() {
        // Arrange
        Integer contactId = 1;
        User currentUser = createUser(1, "Sales Rep", "salesrep@example.com", "SALES_REP");
        Contact contact = createContact(contactId, 10, "Contact 1", "New", 2);
        CreateCommunicationLogRequest request = new CreateCommunicationLogRequest();
        request.setMessage("Test message");

        when(contactRepository.findById(contactId))
                .thenReturn(Optional.of(contact));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            salesContactDetailService.addCommunicationLog(contactId, request, currentUser);
        });

        verify(communicationLogRepository, never()).save(any(CommunicationLog.class));
    }

    @Test
    @DisplayName("addCommunicationLog - Sales Rep được assign → thành công")
    void testAddCommunicationLog_SalesRep_Success() {
        // Arrange
        Integer contactId = 1;
        User currentUser = createUser(1, "Sales Rep", "salesrep@example.com", "SALES_REP");
        Contact contact = createContact(contactId, 10, "Contact 1", "New", 1);
        CreateCommunicationLogRequest request = new CreateCommunicationLogRequest();
        request.setMessage("Test message");

        when(contactRepository.findById(contactId))
                .thenReturn(Optional.of(contact));
        when(communicationLogRepository.save(any(CommunicationLog.class)))
                .thenAnswer(invocation -> {
                    CommunicationLog log = invocation.getArgument(0);
                    log.setId(100);
                    return log;
                });

        // Act
        CommunicationLogDTO result = salesContactDetailService.addCommunicationLog(contactId, request, currentUser);

        // Assert
        assertNotNull(result);
        assertEquals("Test message", result.getMessage());
        assertEquals(1, result.getCreatedBy());
        assertEquals("Sales Rep", result.getCreatedByName());

        ArgumentCaptor<CommunicationLog> captor = ArgumentCaptor.forClass(CommunicationLog.class);
        verify(communicationLogRepository).save(captor.capture());
        CommunicationLog saved = captor.getValue();
        assertEquals(contactId, saved.getContactId());
        assertEquals("Test message", saved.getMessage());
        assertEquals(1, saved.getCreatedBy());
    }

    @Test
    @DisplayName("sendMeetingEmail - thiếu onlineMtgLink → throw RuntimeException")
    void testSendMeetingEmail_MissingLink() {
        // Arrange
        Integer contactId = 1;
        User currentUser = createUser(1, "Sales Rep", "salesrep@example.com", "SALES_REP");
        Contact contact = createContact(contactId, 10, "Contact 1", "New", 1);
        SendMeetingEmailRequest request = new SendMeetingEmailRequest();
        request.setOnlineMtgLink("");
        request.setOnlineMtgDateTime("2025/12/15 14:30");

        when(contactRepository.findById(contactId))
                .thenReturn(Optional.of(contact));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            salesContactDetailService.sendMeetingEmail(contactId, request, currentUser);
        });
    }

    @Test
    @DisplayName("sendMeetingEmail - thiếu onlineMtgDateTime → throw RuntimeException")
    void testSendMeetingEmail_MissingDateTime() {
        // Arrange
        Integer contactId = 1;
        User currentUser = createUser(1, "Sales Rep", "salesrep@example.com", "SALES_REP");
        Contact contact = createContact(contactId, 10, "Contact 1", "New", 1);
        SendMeetingEmailRequest request = new SendMeetingEmailRequest();
        request.setOnlineMtgLink("https://meet.google.com/xxx");
        request.setOnlineMtgDateTime("");

        when(contactRepository.findById(contactId))
                .thenReturn(Optional.of(contact));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            salesContactDetailService.sendMeetingEmail(contactId, request, currentUser);
        });
    }

    @Test
    @DisplayName("sendMeetingEmail - Sales Rep được assign → thành công và gửi email")
    void testSendMeetingEmail_SalesRep_Success() {
        // Arrange
        Integer contactId = 1;
        User currentUser = createUser(1, "Sales Rep", "salesrep@example.com", "SALES_REP");
        Contact contact = createContact(contactId, 10, "Contact 1", "New", 1);
        User clientUser = createUser(10, "Client User", "client@example.com", "CLIENT");
        SendMeetingEmailRequest request = new SendMeetingEmailRequest();
        request.setOnlineMtgLink("https://meet.google.com/xxx");
        request.setOnlineMtgDateTime("2025/12/15 14:30");

        when(contactRepository.findById(contactId))
                .thenReturn(Optional.of(contact));
        when(contactRepository.save(any(Contact.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(10)).thenReturn(Optional.of(clientUser));
        when(communicationLogRepository.findByContactIdOrderByCreatedAtDesc(contactId))
                .thenReturn(new ArrayList<>());
        doNothing().when(emailService).sendMeetingInvitation(anyString(), anyString(), anyString(), anyString());

        // Act
        SalesContactDetailDTO result = salesContactDetailService.sendMeetingEmail(contactId, request, currentUser);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<Contact> captor = ArgumentCaptor.forClass(Contact.class);
        verify(contactRepository).save(captor.capture());
        Contact saved = captor.getValue();
        assertEquals("https://meet.google.com/xxx", saved.getOnlineMtgLink());
        assertNotNull(saved.getOnlineMtgDate());

        verify(emailService).sendMeetingInvitation(
                eq("client@example.com"),
                eq("Client User"),
                eq("https://meet.google.com/xxx"),
                anyString()
        );
    }

    @Test
    @DisplayName("sendMeetingEmail - client email không tồn tại → throw RuntimeException")
    void testSendMeetingEmail_ClientEmailNotFound() {
        // Arrange
        Integer contactId = 1;
        User currentUser = createUser(1, "Sales Rep", "salesrep@example.com", "SALES_REP");
        Contact contact = createContact(contactId, 10, "Contact 1", "New", 1);
        SendMeetingEmailRequest request = new SendMeetingEmailRequest();
        request.setOnlineMtgLink("https://meet.google.com/xxx");
        request.setOnlineMtgDateTime("2025/12/15 14:30");

        when(contactRepository.findById(contactId))
                .thenReturn(Optional.of(contact));
        when(contactRepository.save(any(Contact.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(10)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            salesContactDetailService.sendMeetingEmail(contactId, request, currentUser);
        });
    }

    // Helper methods
    private Contact createContact(Integer id, Integer clientUserId, String title, String status, Integer assigneeUserId) {
        Contact contact = new Contact();
        contact.setId(id);
        contact.setClientUserId(clientUserId);
        contact.setTitle(title);
        contact.setDescription("Test description");
        contact.setStatus(status);
        contact.setAssigneeUserId(assigneeUserId);
        contact.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        return contact;
    }

    private User createUser(Integer id, String fullName, String email, String role) {
        User user = new User();
        user.setId(id);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone("090-1234-5678");
        user.setCompanyName("Test Company");
        user.setRole(role);
        return user;
    }
}

