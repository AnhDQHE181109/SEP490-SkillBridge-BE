package com.skillbridge.service.sales;

import com.skillbridge.dto.sales.request.CreateOpportunityRequest;
import com.skillbridge.dto.sales.request.UpdateOpportunityRequest;
import com.skillbridge.dto.sales.response.OpportunityDetailDTO;
import com.skillbridge.entity.auth.User;
import com.skillbridge.entity.contact.Contact;
import com.skillbridge.entity.opportunity.Opportunity;
import com.skillbridge.entity.proposal.Proposal;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.repository.contact.ContactRepository;
import com.skillbridge.repository.opportunity.OpportunityRepository;
import com.skillbridge.repository.proposal.ProposalHistoryRepository;
import com.skillbridge.repository.proposal.ProposalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SalesOpportunityDetailService
 * Tests business logic for opportunity detail operations
 */
@ExtendWith(MockitoExtension.class)
class SalesOpportunityDetailServiceTest {

    @Mock
    private OpportunityRepository opportunityRepository;

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private ProposalHistoryRepository proposalHistoryRepository;

    @InjectMocks
    private SalesOpportunityDetailService salesOpportunityDetailService;

    @BeforeEach
    void setUp() {
        // Setup is handled by MockitoExtension
    }

    @Test
    @DisplayName("createFromContact - contact không tồn tại → throw RuntimeException")
    void testCreateFromContact_ContactNotFound() {
        // Arrange
        Integer contactId = 1;
        User currentUser = createUser(1, "Sales Rep", "salesrep@example.com", "SALES_REP");
        CreateOpportunityRequest request = new CreateOpportunityRequest();

        when(contactRepository.findById(contactId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            salesOpportunityDetailService.createFromContact(contactId, request, currentUser);
        });

        verify(opportunityRepository, never()).save(any(Opportunity.class));
    }

    @Test
    @DisplayName("createFromContact - Sales Rep không được assign → throw RuntimeException")
    void testCreateFromContact_SalesRep_NotAssigned() {
        // Arrange
        Integer contactId = 1;
        User currentUser = createUser(1, "Sales Rep", "salesrep@example.com", "SALES_REP");
        Contact contact = createContact(contactId, 10, "Contact 1", "New", 2); // Assigned to user 2
        CreateOpportunityRequest request = new CreateOpportunityRequest();

        when(contactRepository.findById(contactId))
                .thenReturn(Optional.of(contact));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            salesOpportunityDetailService.createFromContact(contactId, request, currentUser);
        });

        verify(opportunityRepository, never()).save(any(Opportunity.class));
    }

    @Test
    @DisplayName("createFromContact - Sales Rep được assign → tạo opportunity và update contact")
    void testCreateFromContact_SalesRep_Success() {
        // Arrange
        Integer contactId = 1;
        User currentUser = createUser(1, "Sales Rep", "salesrep@example.com", "SALES_REP");
        Contact contact = createContact(contactId, 10, "Contact 1", "New", 1); // Assigned to user 1
        CreateOpportunityRequest request = new CreateOpportunityRequest();
        request.setClientName("Client Name");
        request.setClientCompany("Client Company");
        request.setClientEmail("client@example.com");
        request.setAssigneeUserId(1);
        request.setProbability(50);
        request.setEstValue(BigDecimal.valueOf(100000));
        request.setCurrency("JPY");

        when(contactRepository.findById(contactId))
                .thenReturn(Optional.of(contact));
        when(opportunityRepository.save(any(Opportunity.class)))
                .thenAnswer(invocation -> {
                    Opportunity opp = invocation.getArgument(0);
                    opp.setId(100);
                    return opp;
                });
        when(contactRepository.save(any(Contact.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(proposalRepository.findByOpportunityIdAndIsCurrent(anyInt(), anyBoolean()))
                .thenReturn(Optional.empty());
        when(proposalRepository.findByOpportunityIdOrderByVersionDesc(anyInt()))
                .thenReturn(new ArrayList<>());
        when(proposalHistoryRepository.findByOpportunityIdOrderByCreatedAtDesc(anyInt()))
                .thenReturn(new ArrayList<>());

        // Act
        OpportunityDetailDTO result = salesOpportunityDetailService.createFromContact(contactId, request, currentUser);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<Opportunity> oppCaptor = ArgumentCaptor.forClass(Opportunity.class);
        verify(opportunityRepository).save(oppCaptor.capture());
        Opportunity saved = oppCaptor.getValue();
        assertEquals(contactId, saved.getContactId());
        assertEquals("Client Name", saved.getClientName());
        assertEquals(50, saved.getProbability());
        assertEquals(BigDecimal.valueOf(100000), saved.getEstValue());
        assertEquals("JPY", saved.getCurrency());
        assertEquals("NEW", saved.getStatus());
        assertEquals(1, saved.getCreatedBy());

        ArgumentCaptor<Contact> contactCaptor = ArgumentCaptor.forClass(Contact.class);
        verify(contactRepository).save(contactCaptor.capture());
        Contact updatedContact = contactCaptor.getValue();
        assertEquals("Converted to Opportunity", updatedContact.getStatus());
    }

    @Test
    @DisplayName("getOpportunityById - opportunity không tồn tại → throw RuntimeException")
    void testGetOpportunityById_NotFound() {
        // Arrange
        String opportunityId = "1";
        User currentUser = createUser(1, "Sales Rep", "salesrep@example.com", "SALES_REP");

        when(opportunityRepository.findById(1))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            salesOpportunityDetailService.getOpportunityById(opportunityId, currentUser);
        });
    }

    @Test
    @DisplayName("getOpportunityById - Sales Rep không tạo opportunity → throw RuntimeException")
    void testGetOpportunityById_SalesRep_NotCreatedBy() {
        // Arrange
        String opportunityId = "1";
        User currentUser = createUser(1, "Sales Rep", "salesrep@example.com", "SALES_REP");
        Opportunity opportunity = createOpportunity(1, "OP-2025-01", "NEW", 2, 2); // Created by user 2

        when(opportunityRepository.findById(1))
                .thenReturn(Optional.of(opportunity));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            salesOpportunityDetailService.getOpportunityById(opportunityId, currentUser);
        });
    }

    @Test
    @DisplayName("getOpportunityById - Sales Manager → trả về opportunity detail")
    void testGetOpportunityById_SalesManager_Success() {
        // Arrange
        String opportunityId = "1";
        User currentUser = createUser(1, "Sales Manager", "manager@example.com", "SALES_MANAGER");
        Opportunity opportunity = createOpportunity(1, "OP-2025-01", "NEW", 2, 2);

        when(opportunityRepository.findById(1))
                .thenReturn(Optional.of(opportunity));
        when(proposalRepository.findByOpportunityIdAndIsCurrent(1, true))
                .thenReturn(Optional.empty());
        when(proposalRepository.findByOpportunityIdOrderByVersionDesc(1))
                .thenReturn(new ArrayList<>());
        when(proposalHistoryRepository.findByOpportunityIdOrderByCreatedAtDesc(1))
                .thenReturn(new ArrayList<>());

        // Act
        OpportunityDetailDTO result = salesOpportunityDetailService.getOpportunityById(opportunityId, currentUser);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("OP-2025-01", result.getOpportunityId());
        assertEquals("NEW", result.getStatus());
    }

    @Test
    @DisplayName("getOpportunityById - với opportunityId string format → tìm bằng opportunityId")
    void testGetOpportunityById_StringFormat() {
        // Arrange
        String opportunityId = "OP-2025-01";
        User currentUser = createUser(1, "Sales Manager", "manager@example.com", "SALES_MANAGER");
        Opportunity opportunity = createOpportunity(1, "OP-2025-01", "NEW", 2, 2);

        when(opportunityRepository.findByOpportunityId("OP-2025-01"))
                .thenReturn(Optional.of(opportunity));
        when(proposalRepository.findByOpportunityIdAndIsCurrent(1, true))
                .thenReturn(Optional.empty());
        when(proposalRepository.findByOpportunityIdOrderByVersionDesc(1))
                .thenReturn(new ArrayList<>());
        when(proposalHistoryRepository.findByOpportunityIdOrderByCreatedAtDesc(1))
                .thenReturn(new ArrayList<>());

        // Act
        OpportunityDetailDTO result = salesOpportunityDetailService.getOpportunityById(opportunityId, currentUser);

        // Assert
        assertNotNull(result);
        assertEquals("OP-2025-01", result.getOpportunityId());
        verify(opportunityRepository).findByOpportunityId("OP-2025-01");
        verify(opportunityRepository, never()).findById(anyInt());
    }

    @Test
    @DisplayName("updateOpportunity - opportunity không tồn tại → throw RuntimeException")
    void testUpdateOpportunity_NotFound() {
        // Arrange
        String opportunityId = "1";
        User currentUser = createUser(1, "Sales Rep", "salesrep@example.com", "SALES_REP");
        UpdateOpportunityRequest request = new UpdateOpportunityRequest();

        when(opportunityRepository.findById(1))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            salesOpportunityDetailService.updateOpportunity(opportunityId, request, currentUser);
        });
    }

    @Test
    @DisplayName("updateOpportunity - Sales Rep không tạo opportunity → throw RuntimeException")
    void testUpdateOpportunity_SalesRep_NotCreatedBy() {
        // Arrange
        String opportunityId = "1";
        User currentUser = createUser(1, "Sales Rep", "salesrep@example.com", "SALES_REP");
        Opportunity opportunity = createOpportunity(1, "OP-2025-01", "NEW", 2, 2); // Created by user 2
        UpdateOpportunityRequest request = new UpdateOpportunityRequest();

        when(opportunityRepository.findById(1))
                .thenReturn(Optional.of(opportunity));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            salesOpportunityDetailService.updateOpportunity(opportunityId, request, currentUser);
        });

        verify(opportunityRepository, never()).save(any(Opportunity.class));
    }

    @Test
    @DisplayName("updateOpportunity - Sales Rep được tạo → update thành công")
    void testUpdateOpportunity_SalesRep_Success() {
        // Arrange
        String opportunityId = "1";
        User currentUser = createUser(1, "Sales Rep", "salesrep@example.com", "SALES_REP");
        Opportunity opportunity = createOpportunity(1, "OP-2025-01", "NEW", 1, 1); // Created by user 1
        UpdateOpportunityRequest request = new UpdateOpportunityRequest();
        request.setProbability(75);
        request.setEstValue(BigDecimal.valueOf(200000));

        when(opportunityRepository.findById(1))
                .thenReturn(Optional.of(opportunity));
        when(opportunityRepository.save(any(Opportunity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(proposalRepository.findByOpportunityIdAndIsCurrent(1, true))
                .thenReturn(Optional.empty());
        when(proposalRepository.findByOpportunityIdOrderByVersionDesc(1))
                .thenReturn(new ArrayList<>());
        when(proposalHistoryRepository.findByOpportunityIdOrderByCreatedAtDesc(1))
                .thenReturn(new ArrayList<>());

        // Act
        OpportunityDetailDTO result = salesOpportunityDetailService.updateOpportunity(opportunityId, request, currentUser);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<Opportunity> captor = ArgumentCaptor.forClass(Opportunity.class);
        verify(opportunityRepository).save(captor.capture());
        Opportunity saved = captor.getValue();
        assertEquals(75, saved.getProbability());
        assertEquals(BigDecimal.valueOf(200000), saved.getEstValue());
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

    private Opportunity createOpportunity(Integer id, String opportunityId, String status, Integer assigneeUserId, Integer createdBy) {
        Opportunity opportunity = new Opportunity();
        opportunity.setId(id);
        opportunity.setOpportunityId(opportunityId);
        opportunity.setStatus(status);
        opportunity.setAssigneeUserId(assigneeUserId);
        opportunity.setCreatedBy(createdBy);
        opportunity.setClientName("Client Name");
        opportunity.setClientEmail("client@example.com");
        opportunity.setEstValue(BigDecimal.valueOf(100000));
        opportunity.setCurrency("JPY");
        opportunity.setProbability(50);
        opportunity.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        return opportunity;
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

