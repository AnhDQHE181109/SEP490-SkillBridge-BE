package com.skillbridge.service.contact;

import com.skillbridge.dto.contact.request.ContactFormData;
import com.skillbridge.dto.contact.response.ContactSubmissionResponse;
import com.skillbridge.entity.auth.User;
import com.skillbridge.entity.contact.Contact;
import com.skillbridge.entity.contact.ContactStatusHistory;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.repository.contact.ContactRepository;
import com.skillbridge.repository.contact.ContactStatusHistoryRepository;
import com.skillbridge.service.auth.PasswordService;
import com.skillbridge.service.common.EmailService;
import com.skillbridge.service.common.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ContactService
 * Tests CRUD operations for contact form submission
 */
@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ContactStatusHistoryRepository contactStatusHistoryRepository;

    @Mock
    private PasswordService passwordService;

    @InjectMocks
    private ContactService contactService;

    @BeforeEach
    void setUp() {
        // Setup is handled by MockitoExtension
    }

    @Test
    @DisplayName("processContactSubmission - user mới → tạo User và Contact, generate password")
    void testProcessContactSubmission_NewUser() {
        // Arrange
        ContactFormData contactData = createContactFormData("newuser@example.com");
        
        when(userRepository.findByEmail("newuser@example.com"))
                .thenReturn(Optional.empty());
        when(passwordService.generateRandomPassword())
                .thenReturn("GeneratedPassword123!");
        when(passwordService.hashPassword("GeneratedPassword123!"))
                .thenReturn("hashedPassword");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setId(100);
                    return user;
                });
        when(contactRepository.save(any(Contact.class)))
                .thenAnswer(invocation -> {
                    Contact contact = invocation.getArgument(0);
                    contact.setId(200);
                    return contact;
                });
        doNothing().when(emailService).sendConfirmationEmail(any(User.class), any(Contact.class), anyString());
        doNothing().when(notificationService).notifySalesManager(any(Contact.class));
        when(contactStatusHistoryRepository.save(any(ContactStatusHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ContactSubmissionResponse response = contactService.processContactSubmission(contactData);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Contact submitted successfully", response.getMessage());
        assertEquals(200, response.getContactId());

        // Verify User was created with password
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("newuser@example.com", savedUser.getEmail());
        assertEquals("John Doe", savedUser.getFullName());
        assertEquals("Test Company", savedUser.getCompanyName());
        assertEquals("CLIENT", savedUser.getRole());
        assertTrue(savedUser.getActive());
        assertNotNull(savedUser.getPassword());
        assertEquals("GeneratedPassword123!", savedUser.getFirstPassword());

        // Verify Contact was created
        ArgumentCaptor<Contact> contactCaptor = ArgumentCaptor.forClass(Contact.class);
        verify(contactRepository).save(contactCaptor.capture());
        Contact savedContact = contactCaptor.getValue();
        assertEquals(100, savedContact.getClientUserId());
        assertEquals("Test Project", savedContact.getTitle());
        assertEquals("Need consultation", savedContact.getDescription());
        assertEquals("New", savedContact.getStatus());

        // Verify email was sent with password
        verify(emailService).sendConfirmationEmail(eq(savedUser), eq(savedContact), eq("GeneratedPassword123!"));

        // Verify notification was sent
        verify(notificationService).notifySalesManager(eq(savedContact));

        // Verify status history was logged
        verify(contactStatusHistoryRepository).save(any(ContactStatusHistory.class));
    }

    @Test
    @DisplayName("processContactSubmission - user đã tồn tại → update User và tạo Contact mới, không generate password")
    void testProcessContactSubmission_ExistingUser() {
        // Arrange
        ContactFormData contactData = createContactFormData("existing@example.com");
        
        User existingUser = createUser(50, "Existing User", "existing@example.com");

        when(userRepository.findByEmail("existing@example.com"))
                .thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(contactRepository.save(any(Contact.class)))
                .thenAnswer(invocation -> {
                    Contact contact = invocation.getArgument(0);
                    contact.setId(201);
                    return contact;
                });
        doNothing().when(emailService).sendConfirmationEmail(any(User.class), any(Contact.class), isNull());
        doNothing().when(notificationService).notifySalesManager(any(Contact.class));
        when(contactStatusHistoryRepository.save(any(ContactStatusHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ContactSubmissionResponse response = contactService.processContactSubmission(contactData);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Contact submitted successfully", response.getMessage());
        assertEquals(201, response.getContactId());

        // Verify User was updated (not created)
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User updatedUser = userCaptor.getValue();
        assertEquals(50, updatedUser.getId()); // Same ID
        assertEquals("John Doe", updatedUser.getFullName()); // Updated name
        assertEquals("Test Company", updatedUser.getCompanyName()); // Updated company

        // Verify password was NOT generated for existing user
        verify(passwordService, never()).generateRandomPassword();
        verify(passwordService, never()).hashPassword(anyString());

        // Verify email was sent without password (null)
        verify(emailService).sendConfirmationEmail(eq(updatedUser), any(Contact.class), isNull());

        // Verify Contact was created
        ArgumentCaptor<Contact> contactCaptor = ArgumentCaptor.forClass(Contact.class);
        verify(contactRepository).save(contactCaptor.capture());
        Contact savedContact = contactCaptor.getValue();
        assertEquals(50, savedContact.getClientUserId());
    }

    @Test
    @DisplayName("processContactSubmission - title null → tự động tạo title từ name")
    void testProcessContactSubmission_TitleNull() {
        // Arrange
        ContactFormData contactData = createContactFormData("user@example.com");
        contactData.setTitle(null); // No title provided

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.empty());
        when(passwordService.generateRandomPassword())
                .thenReturn("Password123!");
        when(passwordService.hashPassword("Password123!"))
                .thenReturn("hashedPassword");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setId(101);
                    return user;
                });
        when(contactRepository.save(any(Contact.class)))
                .thenAnswer(invocation -> {
                    Contact contact = invocation.getArgument(0);
                    contact.setId(202);
                    return contact;
                });
        doNothing().when(emailService).sendConfirmationEmail(any(User.class), any(Contact.class), anyString());
        doNothing().when(notificationService).notifySalesManager(any(Contact.class));
        when(contactStatusHistoryRepository.save(any(ContactStatusHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ContactSubmissionResponse response = contactService.processContactSubmission(contactData);

        // Assert
        assertTrue(response.isSuccess());

        // Verify Contact title was auto-generated
        ArgumentCaptor<Contact> contactCaptor = ArgumentCaptor.forClass(Contact.class);
        verify(contactRepository).save(contactCaptor.capture());
        Contact savedContact = contactCaptor.getValue();
        assertNotNull(savedContact.getTitle());
        assertTrue(savedContact.getTitle().contains("John Doe")); // Contains name
    }

    @Test
    @DisplayName("processContactSubmission - user mới → password được hash và lưu vào DB")
    void testProcessContactSubmission_PasswordHashed() {
        // Arrange
        ContactFormData contactData = createContactFormData("newuser@example.com");
        String plainPassword = "PlainPassword123!";
        String hashedPassword = "hashedPassword123";

        when(userRepository.findByEmail("newuser@example.com"))
                .thenReturn(Optional.empty());
        when(passwordService.generateRandomPassword())
                .thenReturn(plainPassword);
        when(passwordService.hashPassword(plainPassword))
                .thenReturn(hashedPassword);
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setId(102);
                    return user;
                });
        when(contactRepository.save(any(Contact.class)))
                .thenAnswer(invocation -> {
                    Contact contact = invocation.getArgument(0);
                    contact.setId(203);
                    return contact;
                });
        doNothing().when(emailService).sendConfirmationEmail(any(User.class), any(Contact.class), anyString());
        doNothing().when(notificationService).notifySalesManager(any(Contact.class));
        when(contactStatusHistoryRepository.save(any(ContactStatusHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        contactService.processContactSubmission(contactData);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        
        // Verify password was hashed
        assertEquals(hashedPassword, savedUser.getPassword());
        // Verify plain password stored in firstPassword for email
        assertEquals(plainPassword, savedUser.getFirstPassword());

        // Verify password service was called correctly
        verify(passwordService).generateRandomPassword();
        verify(passwordService).hashPassword(plainPassword);
    }

    @Test
    @DisplayName("processContactSubmission - status history được log đúng")
    void testProcessContactSubmission_StatusHistoryLogged() {
        // Arrange
        ContactFormData contactData = createContactFormData("user@example.com");

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.empty());
        when(passwordService.generateRandomPassword())
                .thenReturn("Password123!");
        when(passwordService.hashPassword("Password123!"))
                .thenReturn("hashedPassword");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setId(103);
                    return user;
                });
        when(contactRepository.save(any(Contact.class)))
                .thenAnswer(invocation -> {
                    Contact contact = invocation.getArgument(0);
                    contact.setId(204);
                    contact.setCreatedBy(103);
                    return contact;
                });
        doNothing().when(emailService).sendConfirmationEmail(any(User.class), any(Contact.class), anyString());
        doNothing().when(notificationService).notifySalesManager(any(Contact.class));
        when(contactStatusHistoryRepository.save(any(ContactStatusHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        contactService.processContactSubmission(contactData);

        // Assert
        ArgumentCaptor<ContactStatusHistory> historyCaptor = ArgumentCaptor.forClass(ContactStatusHistory.class);
        verify(contactStatusHistoryRepository).save(historyCaptor.capture());
        ContactStatusHistory history = historyCaptor.getValue();
        
        assertEquals(204, history.getContactId());
        assertEquals("Guest", history.getFromStatus());
        assertEquals("New", history.getToStatus());
        assertEquals(103, history.getChangedBy());
    }

    // Helper methods
    private ContactFormData createContactFormData(String email) {
        ContactFormData data = new ContactFormData();
        data.setName("John Doe");
        data.setCompanyName("Test Company");
        data.setPhone("090-1234-5678");
        data.setEmail(email);
        data.setTitle("Test Project");
        data.setMessage("Need consultation");
        return data;
    }

    private User createUser(Integer id, String fullName, String email) {
        User user = new User();
        user.setId(id);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setRole("CLIENT");
        user.setActive(true);
        return user;
    }
}

