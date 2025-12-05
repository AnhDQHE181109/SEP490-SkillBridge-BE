package com.skillbridge.service.sales;

import com.skillbridge.dto.sales.response.SalesContactListResponse;
import com.skillbridge.dto.sales.response.SalesContactListItemDTO;
import com.skillbridge.entity.auth.User;
import com.skillbridge.entity.contact.Contact;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.repository.contact.ContactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SalesContactService
 * Tests business logic for sales contact list with role-based filtering
 */
@ExtendWith(MockitoExtension.class)
class SalesContactServiceTest {

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SalesContactService salesContactService;

    @BeforeEach
    void setUp() {
        // Setup is handled by MockitoExtension
    }

    @Test
    @DisplayName("getContacts - Sales Manager không có filter → trả về tất cả contacts")
    void testGetContacts_SalesManager_NoFilter() {
        // Arrange
        String search = null;
        List<String> status = null;
        Integer assigneeUserId = null;
        Integer currentUserId = 1;
        String currentUserRole = "SALES_MANAGER";
        int page = 0;
        int size = 20;

        Contact contact1 = createContact(1, 10, "Contact 1", "New", 2);
        Contact contact2 = createContact(2, 11, "Contact 2", "Inprogress", 3);
        List<Contact> contacts = List.of(contact1, contact2);

        Page<Contact> contactPage = new PageImpl<>(contacts, PageRequest.of(page, size), 2);
        when(contactRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(contactPage);
        when(userRepository.findById(10)).thenReturn(Optional.of(createUser(10, "Client 1", "client1@example.com")));
        when(userRepository.findById(11)).thenReturn(Optional.of(createUser(11, "Client 2", "client2@example.com")));
        when(userRepository.findById(2)).thenReturn(Optional.of(createUser(2, "Assignee 1", "assignee1@example.com")));
        when(userRepository.findById(3)).thenReturn(Optional.of(createUser(3, "Assignee 2", "assignee2@example.com")));

        // Act
        SalesContactListResponse response = salesContactService.getContacts(
                search, status, assigneeUserId, currentUserId, currentUserRole, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getContacts().size());
        assertEquals(0, response.getPage());
        assertEquals(20, response.getPageSize());
        assertEquals(1, response.getTotalPages());
        assertEquals(2, response.getTotal());

        verify(contactRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("getContacts - Sales Rep → chỉ trả về contacts được assign")
    void testGetContacts_SalesRep_OnlyAssigned() {
        // Arrange
        String search = null;
        List<String> status = null;
        Integer assigneeUserId = null;
        Integer currentUserId = 2; // Sales Rep ID
        String currentUserRole = "SALES_REP";
        int page = 0;
        int size = 20;

        Contact contact = createContact(1, 10, "Contact 1", "New", 2); // Assigned to user 2
        List<Contact> contacts = List.of(contact);

        Page<Contact> contactPage = new PageImpl<>(contacts, PageRequest.of(page, size), 1);
        when(contactRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(contactPage);
        when(userRepository.findById(10)).thenReturn(Optional.of(createUser(10, "Client 1", "client1@example.com")));
        when(userRepository.findById(2)).thenReturn(Optional.of(createUser(2, "Assignee 1", "assignee1@example.com")));

        // Act
        SalesContactListResponse response = salesContactService.getContacts(
                search, status, assigneeUserId, currentUserId, currentUserRole, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContacts().size());
        assertEquals(2, response.getContacts().get(0).getAssigneeUserId());

        verify(contactRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("getContacts - có search query → filter theo search")
    void testGetContacts_WithSearchQuery() {
        // Arrange
        String search = "Project";
        List<String> status = null;
        Integer assigneeUserId = null;
        Integer currentUserId = 1;
        String currentUserRole = "SALES_MANAGER";
        int page = 0;
        int size = 20;

        Contact contact = createContact(1, 10, "Project Inquiry", "New", 2);
        List<Contact> contacts = List.of(contact);

        Page<Contact> contactPage = new PageImpl<>(contacts, PageRequest.of(page, size), 1);
        when(contactRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(contactPage);
        when(userRepository.findById(10)).thenReturn(Optional.of(createUser(10, "Client 1", "client1@example.com")));
        when(userRepository.findById(2)).thenReturn(Optional.of(createUser(2, "Assignee 1", "assignee1@example.com")));

        // Act
        SalesContactListResponse response = salesContactService.getContacts(
                search, status, assigneeUserId, currentUserId, currentUserRole, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContacts().size());
        assertTrue(response.getContacts().get(0).getTitle().contains("Project"));

        verify(contactRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("getContacts - có status filter → filter theo status")
    void testGetContacts_WithStatusFilter() {
        // Arrange
        String search = null;
        List<String> status = List.of("NEW");
        Integer assigneeUserId = null;
        Integer currentUserId = 1;
        String currentUserRole = "SALES_MANAGER";
        int page = 0;
        int size = 20;

        Contact contact = createContact(1, 10, "Contact 1", "New", 2);
        List<Contact> contacts = List.of(contact);

        Page<Contact> contactPage = new PageImpl<>(contacts, PageRequest.of(page, size), 1);
        when(contactRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(contactPage);
        when(userRepository.findById(10)).thenReturn(Optional.of(createUser(10, "Client 1", "client1@example.com")));
        when(userRepository.findById(2)).thenReturn(Optional.of(createUser(2, "Assignee 1", "assignee1@example.com")));

        // Act
        SalesContactListResponse response = salesContactService.getContacts(
                search, status, assigneeUserId, currentUserId, currentUserRole, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContacts().size());
        assertEquals("NEW", response.getContacts().get(0).getStatus());

        verify(contactRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("getContacts - Sales Manager có assigneeUserId filter → filter theo assignee")
    void testGetContacts_SalesManager_WithAssigneeFilter() {
        // Arrange
        String search = null;
        List<String> status = null;
        Integer assigneeUserId = 2; // Filter by assignee
        Integer currentUserId = 1;
        String currentUserRole = "SALES_MANAGER";
        int page = 0;
        int size = 20;

        Contact contact = createContact(1, 10, "Contact 1", "New", 2);
        List<Contact> contacts = List.of(contact);

        Page<Contact> contactPage = new PageImpl<>(contacts, PageRequest.of(page, size), 1);
        when(contactRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(contactPage);
        when(userRepository.findById(10)).thenReturn(Optional.of(createUser(10, "Client 1", "client1@example.com")));
        when(userRepository.findById(2)).thenReturn(Optional.of(createUser(2, "Assignee 1", "assignee1@example.com")));

        // Act
        SalesContactListResponse response = salesContactService.getContacts(
                search, status, assigneeUserId, currentUserId, currentUserRole, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContacts().size());
        assertEquals(2, response.getContacts().get(0).getAssigneeUserId());

        verify(contactRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("getContacts - pagination → trả về đúng page")
    void testGetContacts_WithPagination() {
        // Arrange
        String search = null;
        List<String> status = null;
        Integer assigneeUserId = null;
        Integer currentUserId = 1;
        String currentUserRole = "SALES_MANAGER";
        int page = 1; // Page 1
        int size = 2; // 2 items per page

        Contact contact1 = createContact(1, 10, "Contact 1", "New", 2);
        Contact contact2 = createContact(2, 11, "Contact 2", "Inprogress", 3);
        List<Contact> contacts = List.of(contact1, contact2);

        Page<Contact> contactPage = new PageImpl<>(contacts, PageRequest.of(page, size), 5);
        when(contactRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(contactPage);
        when(userRepository.findById(10)).thenReturn(Optional.of(createUser(10, "Client 1", "client1@example.com")));
        when(userRepository.findById(11)).thenReturn(Optional.of(createUser(11, "Client 2", "client2@example.com")));
        when(userRepository.findById(2)).thenReturn(Optional.of(createUser(2, "Assignee 1", "assignee1@example.com")));
        when(userRepository.findById(3)).thenReturn(Optional.of(createUser(3, "Assignee 2", "assignee2@example.com")));

        // Act
        SalesContactListResponse response = salesContactService.getContacts(
                search, status, assigneeUserId, currentUserId, currentUserRole, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getPage());
        assertEquals(3, response.getTotalPages()); // 5 items / 2 per page = 3 pages
        assertEquals(5, response.getTotal());
        assertEquals(2, response.getContacts().size());
        assertEquals(3, response.getContacts().get(0).getNo()); // First item on page 1 should be #3
    }

    @Test
    @DisplayName("getContacts - không có contacts → trả về empty list")
    void testGetContacts_NoContacts() {
        // Arrange
        String search = null;
        List<String> status = null;
        Integer assigneeUserId = null;
        Integer currentUserId = 1;
        String currentUserRole = "SALES_MANAGER";
        int page = 0;
        int size = 20;

        Page<Contact> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(page, size), 0);
        when(contactRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        // Act
        SalesContactListResponse response = salesContactService.getContacts(
                search, status, assigneeUserId, currentUserId, currentUserRole, page, size);

        // Assert
        assertNotNull(response);
        assertTrue(response.getContacts().isEmpty());
        assertEquals(0, response.getTotal());
    }

    @Test
    @DisplayName("getContacts - status mapping: New → NEW")
    void testGetContacts_StatusMapping_New() {
        // Arrange
        String search = null;
        List<String> status = null;
        Integer assigneeUserId = null;
        Integer currentUserId = 1;
        String currentUserRole = "SALES_MANAGER";
        int page = 0;
        int size = 20;

        Contact contact = createContact(1, 10, "Contact 1", "New", 2);
        List<Contact> contacts = List.of(contact);

        Page<Contact> contactPage = new PageImpl<>(contacts, PageRequest.of(page, size), 1);
        when(contactRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(contactPage);
        when(userRepository.findById(10)).thenReturn(Optional.of(createUser(10, "Client 1", "client1@example.com")));
        when(userRepository.findById(2)).thenReturn(Optional.of(createUser(2, "Assignee 1", "assignee1@example.com")));

        // Act
        SalesContactListResponse response = salesContactService.getContacts(
                search, status, assigneeUserId, currentUserId, currentUserRole, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContacts().size());
        assertEquals("NEW", response.getContacts().get(0).getStatus());
    }

    @Test
    @DisplayName("getContacts - client user không tồn tại → dùng giá trị mặc định")
    void testGetContacts_ClientUserNotFound() {
        // Arrange
        String search = null;
        List<String> status = null;
        Integer assigneeUserId = null;
        Integer currentUserId = 1;
        String currentUserRole = "SALES_MANAGER";
        int page = 0;
        int size = 20;

        Contact contact = createContact(1, 10, "Contact 1", "New", 2);
        List<Contact> contacts = List.of(contact);

        Page<Contact> contactPage = new PageImpl<>(contacts, PageRequest.of(page, size), 1);
        when(contactRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(contactPage);
        when(userRepository.findById(10)).thenReturn(Optional.empty());
        when(userRepository.findById(2)).thenReturn(Optional.of(createUser(2, "Assignee 1", "assignee1@example.com")));

        // Act
        SalesContactListResponse response = salesContactService.getContacts(
                search, status, assigneeUserId, currentUserId, currentUserRole, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContacts().size());
        SalesContactListItemDTO dto = response.getContacts().get(0);
        assertEquals("-", dto.getClientName());
        assertEquals("-", dto.getClientEmail());
        assertEquals("-", dto.getCompany());
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

    private User createUser(Integer id, String fullName, String email) {
        User user = new User();
        user.setId(id);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone("090-1234-5678");
        user.setCompanyName("Test Company");
        return user;
    }
}

