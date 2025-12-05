package com.skillbridge.service.contact;

import com.skillbridge.dto.contact.request.CreateContactRequest;
import com.skillbridge.dto.contact.response.ContactListResponse;
import com.skillbridge.dto.contact.response.CreateContactResponse;
import com.skillbridge.entity.contact.Contact;
import com.skillbridge.repository.contact.ContactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ContactListService
 * Tests CRUD operations for contact list
 */
@ExtendWith(MockitoExtension.class)
class ContactListServiceTest {

    @Mock
    private ContactRepository contactRepository;

    @InjectMocks
    private ContactListService contactListService;

    @BeforeEach
    void setUp() {
        // Setup is handled by MockitoExtension
    }

    @Test
    @DisplayName("createContact - thiếu title → fail, không lưu DB")
    void testCreateContact_TitleMissing() {
        // Arrange
        Integer clientUserId = 5;
        CreateContactRequest req = new CreateContactRequest();
        req.setTitle("   ");   // blank
        req.setDescription("Something");

        // Act
        CreateContactResponse res =
                contactListService.createContact(clientUserId, req);

        // Assert
        assertFalse(res.isSuccess());
        assertEquals("Title is required", res.getMessage());
        assertNull(res.getContactId());

        // Không được gọi save
        verify(contactRepository, never()).save(any(Contact.class));
    }

    @Test
    @DisplayName("createContact - dữ liệu hợp lệ → lưu Contact & trả về success")
    void testCreateContact_Success() {
        // Arrange
        Integer clientUserId = 5;
        CreateContactRequest req = new CreateContactRequest();
        req.setTitle("  New project  ");
        req.setDescription("  Need more info  ");

        // mock save: gắn id cho entity
        when(contactRepository.save(any(Contact.class)))
                .thenAnswer(invocation -> {
                    Contact c = invocation.getArgument(0);
                    c.setId(123); // giả lập DB gán id
                    return c;
                });

        // Act
        CreateContactResponse res =
                contactListService.createContact(clientUserId, req);

        // Assert
        assertTrue(res.isSuccess());
        assertEquals("Contact created successfully", res.getMessage());
        assertEquals(123, res.getContactId());

        // Kiểm tra Contact được build đúng
        ArgumentCaptor<Contact> captor = ArgumentCaptor.forClass(Contact.class);
        verify(contactRepository).save(captor.capture());

        Contact saved = captor.getValue();
        assertEquals(clientUserId, saved.getClientUserId());
        assertEquals("New project", saved.getTitle());       // đã trim
        assertEquals("Need more info", saved.getDescription());
        assertEquals("New", saved.getStatus());
        assertEquals("General Inquiry", saved.getRequestType());
        assertEquals("Medium", saved.getPriority());
        assertEquals("AutoReply", saved.getCommunicationProgress());
        assertEquals(clientUserId, saved.getCreatedBy());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    @DisplayName("createContact - title null → fail, không lưu DB")
    void testCreateContact_TitleNull() {
        // Arrange
        Integer clientUserId = 5;
        CreateContactRequest req = new CreateContactRequest();
        req.setTitle(null);
        req.setDescription("Something");

        // Act
        CreateContactResponse res =
                contactListService.createContact(clientUserId, req);

        // Assert
        assertFalse(res.isSuccess());
        assertEquals("Title is required", res.getMessage());
        assertNull(res.getContactId());

        // Không được gọi save
        verify(contactRepository, never()).save(any(Contact.class));
    }

    @Test
    @DisplayName("createContact - description null → vẫn thành công, description null")
    void testCreateContact_DescriptionNull() {
        // Arrange
        Integer clientUserId = 5;
        CreateContactRequest req = new CreateContactRequest();
        req.setTitle("New project");
        req.setDescription(null);

        when(contactRepository.save(any(Contact.class)))
                .thenAnswer(invocation -> {
                    Contact c = invocation.getArgument(0);
                    c.setId(123);
                    return c;
                });

        // Act
        CreateContactResponse res =
                contactListService.createContact(clientUserId, req);

        // Assert
        assertTrue(res.isSuccess());
        assertEquals(123, res.getContactId());

        ArgumentCaptor<Contact> captor = ArgumentCaptor.forClass(Contact.class);
        verify(contactRepository).save(captor.capture());
        Contact saved = captor.getValue();
        assertNull(saved.getDescription());
    }

    @Test
    @DisplayName("getContactsForClient - không có contact → trả về empty list")
    void testGetContactsForClient_NoContacts() {
        // Arrange
        Integer clientUserId = 5;
        String search = null;
        String status = "All";
        int page = 0;
        int size = 10;

        Page<Contact> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(page, size), 0);
        when(contactRepository.findContactsForClient(
                eq(clientUserId), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(emptyPage);

        // Act
        ContactListResponse response = contactListService.getContactsForClient(
                clientUserId, search, status, page, size);

        // Assert
        assertNotNull(response);
        assertTrue(response.getContacts().isEmpty());
        assertEquals(0, response.getCurrentPage());
        assertEquals(0, response.getTotalPages());
        assertEquals(0, response.getTotalElements());

        verify(contactRepository).findContactsForClient(
                eq(clientUserId), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("getContactsForClient - có contacts → trả về danh sách contacts")
    void testGetContactsForClient_WithContacts() {
        // Arrange
        Integer clientUserId = 5;
        String search = null;
        String status = "All";
        int page = 0;
        int size = 10;

        Contact contact1 = createContact(1, clientUserId, "Contact 1", "New");
        Contact contact2 = createContact(2, clientUserId, "Contact 2", "In Progress");
        List<Contact> contacts = List.of(contact1, contact2);

        Page<Contact> contactPage = new PageImpl<>(contacts, PageRequest.of(page, size), 2);
        when(contactRepository.findContactsForClient(
                eq(clientUserId), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(contactPage);

        // Act
        ContactListResponse response = contactListService.getContactsForClient(
                clientUserId, search, status, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getContacts().size());
        assertEquals(0, response.getCurrentPage());
        assertEquals(1, response.getTotalPages());
        assertEquals(2, response.getTotalElements());

        assertEquals("Contact 1", response.getContacts().get(0).getTitle());
        assertEquals("New", response.getContacts().get(0).getStatus());
        assertNotNull(response.getContacts().get(0).getId());
        assertNotNull(response.getContacts().get(0).getCreatedOn());

        verify(contactRepository).findContactsForClient(
                eq(clientUserId), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("getContactsForClient - có search query → filter theo search")
    void testGetContactsForClient_WithSearchQuery() {
        // Arrange
        Integer clientUserId = 5;
        String search = "Project";
        String status = "All";
        int page = 0;
        int size = 10;

        Contact contact = createContact(1, clientUserId, "Project Inquiry", "New");
        Page<Contact> contactPage = new PageImpl<>(List.of(contact), PageRequest.of(page, size), 1);

        when(contactRepository.findContactsForClient(
                eq(clientUserId), eq("Project"), isNull(), any(Pageable.class)))
                .thenReturn(contactPage);

        // Act
        ContactListResponse response = contactListService.getContactsForClient(
                clientUserId, search, status, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContacts().size());
        verify(contactRepository).findContactsForClient(
                eq(clientUserId), eq("Project"), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("getContactsForClient - có status filter → filter theo status")
    void testGetContactsForClient_WithStatusFilter() {
        // Arrange
        Integer clientUserId = 5;
        String search = null;
        String status = "New";
        int page = 0;
        int size = 10;

        Contact contact = createContact(1, clientUserId, "New Contact", "New");
        Page<Contact> contactPage = new PageImpl<>(List.of(contact), PageRequest.of(page, size), 1);

        when(contactRepository.findContactsForClient(
                eq(clientUserId), isNull(), eq("New"), any(Pageable.class)))
                .thenReturn(contactPage);

        // Act
        ContactListResponse response = contactListService.getContactsForClient(
                clientUserId, search, status, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContacts().size());
        verify(contactRepository).findContactsForClient(
                eq(clientUserId), isNull(), eq("New"), any(Pageable.class));
    }

    @Test
    @DisplayName("getContactsForClient - pagination → trả về đúng page")
    void testGetContactsForClient_WithPagination() {
        // Arrange
        Integer clientUserId = 5;
        String search = null;
        String status = "All";
        int page = 1; // Page 1 (second page)
        int size = 2; // 2 items per page

        // Create 5 contacts
        List<Contact> allContacts = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            allContacts.add(createContact(i, clientUserId, "Contact " + i, "New"));
        }

        Page<Contact> contactPage = new PageImpl<>(
                allContacts.subList(2, 4), // Items for page 1 (index 2, 3)
                PageRequest.of(page, size),
                5); // Total 5 items

        when(contactRepository.findContactsForClient(
                eq(clientUserId), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(contactPage);

        // Act
        ContactListResponse response = contactListService.getContactsForClient(
                clientUserId, search, status, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getCurrentPage());
        assertEquals(3, response.getTotalPages()); // 5 items / 2 per page = 3 pages
        assertEquals(5, response.getTotalElements());
        assertEquals(2, response.getContacts().size()); // Page 1 should have 2 items
    }

    @Test
    @DisplayName("getContactsForClient - search query có whitespace → trim và filter")
    void testGetContactsForClient_SearchQueryWithWhitespace() {
        // Arrange
        Integer clientUserId = 5;
        String search = "  Project Inquiry  ";
        String status = "All";
        int page = 0;
        int size = 10;

        Contact contact = createContact(1, clientUserId, "Project Inquiry", "New");
        Page<Contact> contactPage = new PageImpl<>(List.of(contact), PageRequest.of(page, size), 1);

        when(contactRepository.findContactsForClient(
                eq(clientUserId), eq("Project Inquiry"), isNull(), any(Pageable.class)))
                .thenReturn(contactPage);

        // Act
        ContactListResponse response = contactListService.getContactsForClient(
                clientUserId, search, status, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContacts().size());
        // Verify that search query was trimmed
        verify(contactRepository).findContactsForClient(
                eq(clientUserId), eq("Project Inquiry"), isNull(), any(Pageable.class));
    }

    // Helper methods
    private Contact createContact(Integer id, Integer clientUserId, String title, String status) {
        Contact contact = new Contact();
        contact.setId(id);
        contact.setClientUserId(clientUserId);
        contact.setTitle(title);
        contact.setDescription("Test description");
        contact.setStatus(status);
        contact.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        return contact;
    }
}

