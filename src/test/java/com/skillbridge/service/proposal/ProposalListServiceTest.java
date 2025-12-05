package com.skillbridge.service.proposal;

import com.skillbridge.dto.proposal.response.ProposalListResponse;
import com.skillbridge.dto.proposal.response.ProposalListItemDTO;
import com.skillbridge.entity.contact.Contact;
import com.skillbridge.entity.proposal.Proposal;
import com.skillbridge.repository.contact.ContactRepository;
import com.skillbridge.repository.proposal.ProposalRepository;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProposalListService
 * Tests business logic for proposal list operations
 */
@ExtendWith(MockitoExtension.class)
class ProposalListServiceTest {

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private ContactRepository contactRepository;

    @InjectMocks
    private ProposalListService proposalListService;

    @BeforeEach
    void setUp() {
        // Setup is handled by MockitoExtension
    }

    @Test
    @DisplayName("getProposalsForClient - không có proposals → trả về empty list")
    void testGetProposalsForClient_NoProposals() {
        // Arrange
        Integer clientUserId = 5;
        String search = null;
        String status = "All";
        int page = 0;
        int size = 10;

        Page<Proposal> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(page, size), 0);
        when(proposalRepository.findProposalsForClient(
                eq(clientUserId), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(emptyPage);

        // Act
        ProposalListResponse response = proposalListService.getProposalsForClient(
                clientUserId, search, status, page, size);

        // Assert
        assertNotNull(response);
        assertTrue(response.getProposals().isEmpty());
        assertEquals(0, response.getCurrentPage());
        assertEquals(0, response.getTotalPages());
        assertEquals(0, response.getTotalElements());

        verify(proposalRepository).findProposalsForClient(
                eq(clientUserId), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("getProposalsForClient - có proposals → trả về danh sách proposals")
    void testGetProposalsForClient_WithProposals() {
        // Arrange
        Integer clientUserId = 5;
        String search = null;
        String status = "All";
        int page = 0;
        int size = 10;

        Contact contact1 = createContact(1, clientUserId, "Contact 1", "Test description 1");
        Contact contact2 = createContact(2, clientUserId, "Contact 2", "Test description 2");

        Proposal proposal1 = createProposal(1, 1, "sent_to_client", "Proposal 1");
        Proposal proposal2 = createProposal(2, 2, "approved", "Proposal 2");
        List<Proposal> proposals = List.of(proposal1, proposal2);

        Page<Proposal> proposalPage = new PageImpl<>(proposals, PageRequest.of(page, size), 2);
        when(proposalRepository.findProposalsForClient(
                eq(clientUserId), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(proposalPage);
        when(contactRepository.findById(1)).thenReturn(Optional.of(contact1));
        when(contactRepository.findById(2)).thenReturn(Optional.of(contact2));
        when(proposalRepository.findAll()).thenReturn(proposals);

        // Act
        ProposalListResponse response = proposalListService.getProposalsForClient(
                clientUserId, search, status, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getProposals().size());
        assertEquals(0, response.getCurrentPage());
        assertEquals(1, response.getTotalPages());
        assertEquals(2, response.getTotalElements());

        ProposalListItemDTO dto1 = response.getProposals().get(0);
        assertEquals(1, dto1.getNo());
        assertEquals(1, dto1.getInternalId());
        assertEquals("Proposal 1", dto1.getTitle());
        assertEquals("Under review", dto1.getStatus());
        assertNotNull(dto1.getId());
        assertNotNull(dto1.getContactId());
        assertNotNull(dto1.getCreatedOn());

        ProposalListItemDTO dto2 = response.getProposals().get(1);
        assertEquals(2, dto2.getNo());
        assertEquals(2, dto2.getInternalId());
        assertEquals("Proposal 2", dto2.getTitle());
        assertEquals("Approved", dto2.getStatus());

        verify(proposalRepository).findProposalsForClient(
                eq(clientUserId), isNull(), isNull(), any(Pageable.class));
        verify(contactRepository).findById(1);
        verify(contactRepository).findById(2);
    }

    @Test
    @DisplayName("getProposalsForClient - có search query → filter theo search")
    void testGetProposalsForClient_WithSearchQuery() {
        // Arrange
        Integer clientUserId = 5;
        String search = "Project";
        String status = "All";
        int page = 0;
        int size = 10;

        Contact contact = createContact(1, clientUserId, "Contact 1", "Project description");
        Proposal proposal = createProposal(1, 1, "sent_to_client", "Project Proposal");
        List<Proposal> proposals = List.of(proposal);

        Page<Proposal> proposalPage = new PageImpl<>(proposals, PageRequest.of(page, size), 1);
        when(proposalRepository.findProposalsForClient(
                eq(clientUserId), eq("Project"), isNull(), any(Pageable.class)))
                .thenReturn(proposalPage);
        when(contactRepository.findById(1)).thenReturn(Optional.of(contact));
        when(proposalRepository.findAll()).thenReturn(proposals);

        // Act
        ProposalListResponse response = proposalListService.getProposalsForClient(
                clientUserId, search, status, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getProposals().size());
        verify(proposalRepository).findProposalsForClient(
                eq(clientUserId), eq("Project"), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("getProposalsForClient - search query có whitespace → trim và filter")
    void testGetProposalsForClient_SearchQueryWithWhitespace() {
        // Arrange
        Integer clientUserId = 5;
        String search = "  Project  ";
        String status = "All";
        int page = 0;
        int size = 10;

        Contact contact = createContact(1, clientUserId, "Contact 1", "Project description");
        Proposal proposal = createProposal(1, 1, "sent_to_client", "Project Proposal");
        List<Proposal> proposals = List.of(proposal);

        Page<Proposal> proposalPage = new PageImpl<>(proposals, PageRequest.of(page, size), 1);
        when(proposalRepository.findProposalsForClient(
                eq(clientUserId), eq("Project"), isNull(), any(Pageable.class)))
                .thenReturn(proposalPage);
        when(contactRepository.findById(1)).thenReturn(Optional.of(contact));
        when(proposalRepository.findAll()).thenReturn(proposals);

        // Act
        ProposalListResponse response = proposalListService.getProposalsForClient(
                clientUserId, search, status, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getProposals().size());
        // Verify that search query was trimmed
        verify(proposalRepository).findProposalsForClient(
                eq(clientUserId), eq("Project"), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("getProposalsForClient - status = 'All' → không filter theo status")
    void testGetProposalsForClient_StatusAll() {
        // Arrange
        Integer clientUserId = 5;
        String search = null;
        String status = "All";
        int page = 0;
        int size = 10;

        Contact contact = createContact(1, clientUserId, "Contact 1", "Test description");
        Proposal proposal = createProposal(1, 1, "sent_to_client", "Proposal 1");
        List<Proposal> proposals = List.of(proposal);

        Page<Proposal> proposalPage = new PageImpl<>(proposals, PageRequest.of(page, size), 1);
        when(proposalRepository.findProposalsForClient(
                eq(clientUserId), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(proposalPage);
        when(contactRepository.findById(1)).thenReturn(Optional.of(contact));
        when(proposalRepository.findAll()).thenReturn(proposals);

        // Act
        ProposalListResponse response = proposalListService.getProposalsForClient(
                clientUserId, search, status, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getProposals().size());
        verify(proposalRepository).findProposalsForClient(
                eq(clientUserId), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("getProposalsForClient - status = 'Under review' → map thành 'sent_to_client'")
    void testGetProposalsForClient_StatusUnderReview() {
        // Arrange
        Integer clientUserId = 5;
        String search = null;
        String status = "Under review";
        int page = 0;
        int size = 10;

        Contact contact = createContact(1, clientUserId, "Contact 1", "Test description");
        Proposal proposal = createProposal(1, 1, "sent_to_client", "Proposal 1");
        List<Proposal> proposals = List.of(proposal);

        Page<Proposal> proposalPage = new PageImpl<>(proposals, PageRequest.of(page, size), 1);
        when(proposalRepository.findProposalsForClient(
                eq(clientUserId), isNull(), eq("sent_to_client"), any(Pageable.class)))
                .thenReturn(proposalPage);
        when(contactRepository.findById(1)).thenReturn(Optional.of(contact));
        when(proposalRepository.findAll()).thenReturn(proposals);

        // Act
        ProposalListResponse response = proposalListService.getProposalsForClient(
                clientUserId, search, status, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getProposals().size());
        assertEquals("Under review", response.getProposals().get(0).getStatus());
        verify(proposalRepository).findProposalsForClient(
                eq(clientUserId), isNull(), eq("sent_to_client"), any(Pageable.class));
    }

    @Test
    @DisplayName("getProposalsForClient - status = 'Request for change' → map thành 'revision_requested'")
    void testGetProposalsForClient_StatusRequestForChange() {
        // Arrange
        Integer clientUserId = 5;
        String search = null;
        String status = "Request for change";
        int page = 0;
        int size = 10;

        Contact contact = createContact(1, clientUserId, "Contact 1", "Test description");
        Proposal proposal = createProposal(1, 1, "revision_requested", "Proposal 1");
        List<Proposal> proposals = List.of(proposal);

        Page<Proposal> proposalPage = new PageImpl<>(proposals, PageRequest.of(page, size), 1);
        when(proposalRepository.findProposalsForClient(
                eq(clientUserId), isNull(), eq("revision_requested"), any(Pageable.class)))
                .thenReturn(proposalPage);
        when(contactRepository.findById(1)).thenReturn(Optional.of(contact));
        when(proposalRepository.findAll()).thenReturn(proposals);

        // Act
        ProposalListResponse response = proposalListService.getProposalsForClient(
                clientUserId, search, status, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getProposals().size());
        assertEquals("Request for change", response.getProposals().get(0).getStatus());
        verify(proposalRepository).findProposalsForClient(
                eq(clientUserId), isNull(), eq("revision_requested"), any(Pageable.class));
    }

    @Test
    @DisplayName("getProposalsForClient - status = 'Approved' → map thành 'approved'")
    void testGetProposalsForClient_StatusApproved() {
        // Arrange
        Integer clientUserId = 5;
        String search = null;
        String status = "Approved";
        int page = 0;
        int size = 10;

        Contact contact = createContact(1, clientUserId, "Contact 1", "Test description");
        Proposal proposal = createProposal(1, 1, "approved", "Proposal 1");
        List<Proposal> proposals = List.of(proposal);

        Page<Proposal> proposalPage = new PageImpl<>(proposals, PageRequest.of(page, size), 1);
        when(proposalRepository.findProposalsForClient(
                eq(clientUserId), isNull(), eq("approved"), any(Pageable.class)))
                .thenReturn(proposalPage);
        when(contactRepository.findById(1)).thenReturn(Optional.of(contact));
        when(proposalRepository.findAll()).thenReturn(proposals);

        // Act
        ProposalListResponse response = proposalListService.getProposalsForClient(
                clientUserId, search, status, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getProposals().size());
        assertEquals("Approved", response.getProposals().get(0).getStatus());
        verify(proposalRepository).findProposalsForClient(
                eq(clientUserId), isNull(), eq("approved"), any(Pageable.class));
    }

    @Test
    @DisplayName("getProposalsForClient - pagination → trả về đúng page")
    void testGetProposalsForClient_WithPagination() {
        // Arrange
        Integer clientUserId = 5;
        String search = null;
        String status = "All";
        int page = 1; // Page 1 (second page)
        int size = 2; // 2 items per page

        // Create 5 proposals
        List<Proposal> allProposals = new ArrayList<>();
        List<Contact> allContacts = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Contact contact = createContact(i, clientUserId, "Contact " + i, "Description " + i);
            allContacts.add(contact);
            allProposals.add(createProposal(i, i, "sent_to_client", "Proposal " + i));
        }

        Page<Proposal> proposalPage = new PageImpl<>(
                allProposals.subList(2, 4), // Items for page 1 (index 2, 3)
                PageRequest.of(page, size),
                5); // Total 5 items

        when(proposalRepository.findProposalsForClient(
                eq(clientUserId), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(proposalPage);
        when(contactRepository.findById(3)).thenReturn(Optional.of(allContacts.get(2)));
        when(contactRepository.findById(4)).thenReturn(Optional.of(allContacts.get(3)));
        when(proposalRepository.findAll()).thenReturn(allProposals);

        // Act
        ProposalListResponse response = proposalListService.getProposalsForClient(
                clientUserId, search, status, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getCurrentPage());
        assertEquals(3, response.getTotalPages()); // 5 items / 2 per page = 3 pages
        assertEquals(5, response.getTotalElements());
        assertEquals(2, response.getProposals().size()); // Page 1 should have 2 items
        assertEquals(3, response.getProposals().get(0).getNo()); // First item on page 1 should be #3
        assertEquals(4, response.getProposals().get(1).getNo()); // Second item on page 1 should be #4
    }

    @Test
    @DisplayName("getProposalsForClient - contact không tồn tại → dùng fallback contact ID")
    void testGetProposalsForClient_ContactNotFound() {
        // Arrange
        Integer clientUserId = 5;
        String search = null;
        String status = "All";
        int page = 0;
        int size = 10;

        Proposal proposal = createProposal(1, 1, "sent_to_client", "Proposal 1");
        List<Proposal> proposals = List.of(proposal);

        Page<Proposal> proposalPage = new PageImpl<>(proposals, PageRequest.of(page, size), 1);
        when(proposalRepository.findProposalsForClient(
                eq(clientUserId), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(proposalPage);
        when(contactRepository.findById(1)).thenReturn(Optional.empty());
        when(proposalRepository.findAll()).thenReturn(proposals);

        // Act
        ProposalListResponse response = proposalListService.getProposalsForClient(
                clientUserId, search, status, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getProposals().size());
        ProposalListItemDTO dto = response.getProposals().get(0);
        assertEquals("CT-1", dto.getContactId()); // Fallback format
        assertEquals(1, dto.getContactInternalId());
        assertEquals("", dto.getContactDescription());
    }

    @Test
    @DisplayName("getProposalsForClient - exception xảy ra → trả về empty response")
    void testGetProposalsForClient_Exception() {
        // Arrange
        Integer clientUserId = 5;
        String search = null;
        String status = "All";
        int page = 0;
        int size = 10;

        when(proposalRepository.findProposalsForClient(
                eq(clientUserId), isNull(), isNull(), any(Pageable.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act
        ProposalListResponse response = proposalListService.getProposalsForClient(
                clientUserId, search, status, page, size);

        // Assert
        assertNotNull(response);
        assertTrue(response.getProposals().isEmpty());
        assertEquals(0, response.getCurrentPage());
        assertEquals(0, response.getTotalPages());
        assertEquals(0, response.getTotalElements());
    }

    @Test
    @DisplayName("getProposalsForClient - status mapping: sent_to_client → Under review")
    void testGetProposalsForClient_StatusMappingSentToClient() {
        // Arrange
        Integer clientUserId = 5;
        String search = null;
        String status = "All";
        int page = 0;
        int size = 10;

        Contact contact = createContact(1, clientUserId, "Contact 1", "Test description");
        Proposal proposal = createProposal(1, 1, "sent_to_client", "Proposal 1");
        List<Proposal> proposals = List.of(proposal);

        Page<Proposal> proposalPage = new PageImpl<>(proposals, PageRequest.of(page, size), 1);
        when(proposalRepository.findProposalsForClient(
                eq(clientUserId), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(proposalPage);
        when(contactRepository.findById(1)).thenReturn(Optional.of(contact));
        when(proposalRepository.findAll()).thenReturn(proposals);

        // Act
        ProposalListResponse response = proposalListService.getProposalsForClient(
                clientUserId, search, status, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getProposals().size());
        assertEquals("Under review", response.getProposals().get(0).getStatus());
    }

    @Test
    @DisplayName("getProposalsForClient - status mapping: revision_requested → Request for change")
    void testGetProposalsForClient_StatusMappingRevisionRequested() {
        // Arrange
        Integer clientUserId = 5;
        String search = null;
        String status = "All";
        int page = 0;
        int size = 10;

        Contact contact = createContact(1, clientUserId, "Contact 1", "Test description");
        Proposal proposal = createProposal(1, 1, "revision_requested", "Proposal 1");
        List<Proposal> proposals = List.of(proposal);

        Page<Proposal> proposalPage = new PageImpl<>(proposals, PageRequest.of(page, size), 1);
        when(proposalRepository.findProposalsForClient(
                eq(clientUserId), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(proposalPage);
        when(contactRepository.findById(1)).thenReturn(Optional.of(contact));
        when(proposalRepository.findAll()).thenReturn(proposals);

        // Act
        ProposalListResponse response = proposalListService.getProposalsForClient(
                clientUserId, search, status, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getProposals().size());
        assertEquals("Request for change", response.getProposals().get(0).getStatus());
    }

    @Test
    @DisplayName("getProposalsForClient - status mapping: approved → Approved")
    void testGetProposalsForClient_StatusMappingApproved() {
        // Arrange
        Integer clientUserId = 5;
        String search = null;
        String status = "All";
        int page = 0;
        int size = 10;

        Contact contact = createContact(1, clientUserId, "Contact 1", "Test description");
        Proposal proposal = createProposal(1, 1, "approved", "Proposal 1");
        List<Proposal> proposals = List.of(proposal);

        Page<Proposal> proposalPage = new PageImpl<>(proposals, PageRequest.of(page, size), 1);
        when(proposalRepository.findProposalsForClient(
                eq(clientUserId), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(proposalPage);
        when(contactRepository.findById(1)).thenReturn(Optional.of(contact));
        when(proposalRepository.findAll()).thenReturn(proposals);

        // Act
        ProposalListResponse response = proposalListService.getProposalsForClient(
                clientUserId, search, status, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getProposals().size());
        assertEquals("Approved", response.getProposals().get(0).getStatus());
    }

    @Test
    @DisplayName("getProposalsForClient - proposal có null title → dùng empty string")
    void testGetProposalsForClient_NullTitle() {
        // Arrange
        Integer clientUserId = 5;
        String search = null;
        String status = "All";
        int page = 0;
        int size = 10;

        Contact contact = createContact(1, clientUserId, "Contact 1", "Test description");
        Proposal proposal = createProposal(1, 1, "sent_to_client", null);
        proposal.setTitle(null);
        List<Proposal> proposals = List.of(proposal);

        Page<Proposal> proposalPage = new PageImpl<>(proposals, PageRequest.of(page, size), 1);
        when(proposalRepository.findProposalsForClient(
                eq(clientUserId), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(proposalPage);
        when(contactRepository.findById(1)).thenReturn(Optional.of(contact));
        when(proposalRepository.findAll()).thenReturn(proposals);

        // Act
        ProposalListResponse response = proposalListService.getProposalsForClient(
                clientUserId, search, status, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getProposals().size());
        assertEquals("", response.getProposals().get(0).getTitle());
    }

    @Test
    @DisplayName("getProposalsForClient - proposal có null createdAt → dùng empty string cho createdOn")
    void testGetProposalsForClient_NullCreatedAt() {
        // Arrange
        Integer clientUserId = 5;
        String search = null;
        String status = "All";
        int page = 0;
        int size = 10;

        Contact contact = createContact(1, clientUserId, "Contact 1", "Test description");
        Proposal proposal = createProposal(1, 1, "sent_to_client", "Proposal 1");
        proposal.setCreatedAt(null);
        List<Proposal> proposals = List.of(proposal);

        Page<Proposal> proposalPage = new PageImpl<>(proposals, PageRequest.of(page, size), 1);
        when(proposalRepository.findProposalsForClient(
                eq(clientUserId), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(proposalPage);
        when(contactRepository.findById(1)).thenReturn(Optional.of(contact));
        // Note: When createdAt is null, generateProposalId uses fallback and doesn't call findAll()

        // Act
        ProposalListResponse response = proposalListService.getProposalsForClient(
                clientUserId, search, status, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getProposals().size());
        assertEquals("", response.getProposals().get(0).getCreatedOn());
    }

    // Helper methods
    private Contact createContact(Integer id, Integer clientUserId, String title, String description) {
        Contact contact = new Contact();
        contact.setId(id);
        contact.setClientUserId(clientUserId);
        contact.setTitle(title);
        contact.setDescription(description);
        contact.setStatus("New");
        contact.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        return contact;
    }

    private Proposal createProposal(Integer id, Integer contactId, String status, String title) {
        Proposal proposal = new Proposal();
        proposal.setId(id);
        proposal.setContactId(contactId);
        proposal.setTitle(title);
        proposal.setStatus(status);
        proposal.setLink("http://example.com/proposal");
        proposal.setIsCurrent(true);
        proposal.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        proposal.setUpdatedAt(LocalDateTime.of(2025, 1, 2, 10, 0));
        return proposal;
    }
}

