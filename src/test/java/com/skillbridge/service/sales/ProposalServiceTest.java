package com.skillbridge.service.sales;

import com.skillbridge.dto.sales.request.AssignReviewerRequest;
import com.skillbridge.dto.sales.request.SubmitReviewRequest;
import com.skillbridge.dto.sales.response.ProposalDTO;
import com.skillbridge.entity.auth.User;
import com.skillbridge.entity.opportunity.Opportunity;
import com.skillbridge.entity.proposal.Proposal;
import com.skillbridge.entity.proposal.ProposalHistory;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.repository.opportunity.OpportunityRepository;
import com.skillbridge.repository.proposal.ProposalHistoryRepository;
import com.skillbridge.repository.proposal.ProposalRepository;
import com.skillbridge.service.common.S3Service;
import com.skillbridge.service.common.DocumentPermissionService;
import com.skillbridge.repository.document.DocumentMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProposalService
 * Tests business logic for proposal operations
 */
@ExtendWith(MockitoExtension.class)
class ProposalServiceTest {

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private OpportunityRepository opportunityRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProposalHistoryRepository proposalHistoryRepository;

    @Mock
    private S3Service s3Service;

    @Mock
    private DocumentMetadataRepository documentMetadataRepository;

    @Mock
    private DocumentPermissionService documentPermissionService;

    @InjectMocks
    private ProposalService proposalService;

    @BeforeEach
    void setUp() {
        // Setup is handled by MockitoExtension
    }

    @Test
    @DisplayName("createProposal - opportunity không tồn tại → throw RuntimeException")
    void testCreateProposal_OpportunityNotFound() {
        // Arrange
        String opportunityId = "1";
        String title = "Test Proposal";
        Integer reviewerId = null;
        MultipartFile[] files = null;
        User currentUser = createUser(1, "Sales Rep", "salesrep@example.com", "SALES_REP");

        when(opportunityRepository.findById(1))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            proposalService.createProposal(opportunityId, title, reviewerId, files, currentUser);
        });

        verify(proposalRepository, never()).save(any(Proposal.class));
    }

    @Test
    @DisplayName("createProposal - Sales Rep không được tạo/assign opportunity → throw RuntimeException")
    void testCreateProposal_SalesRep_NotAuthorized() {
        // Arrange
        String opportunityId = "1";
        String title = "Test Proposal";
        Integer reviewerId = null;
        MultipartFile[] files = null;
        User currentUser = createUser(1, "Sales Rep", "salesrep@example.com", "SALES_REP");
        Opportunity opportunity = createOpportunity(1, "OP-2025-01", "NEW", 2, 2); // Created by user 2, assigned to user 2

        when(opportunityRepository.findById(1))
                .thenReturn(Optional.of(opportunity));
        when(proposalRepository.findByOpportunityIdAndIsCurrent(1, true))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            proposalService.createProposal(opportunityId, title, reviewerId, files, currentUser);
        });

        verify(proposalRepository, never()).save(any(Proposal.class));
    }

    @Test
    @DisplayName("createProposal - Sales Rep được tạo opportunity → tạo proposal thành công")
    void testCreateProposal_SalesRep_Success() {
        // Arrange
        String opportunityId = "1";
        String title = "Test Proposal";
        Integer reviewerId = null;
        MultipartFile[] files = null;
        User currentUser = createUser(1, "Sales Rep", "salesrep@example.com", "SALES_REP");
        Opportunity opportunity = createOpportunity(1, "OP-2025-01", "NEW", 1, 1); // Created by user 1

        when(opportunityRepository.findById(1))
                .thenReturn(Optional.of(opportunity));
        when(proposalRepository.findByOpportunityIdAndIsCurrent(1, true))
                .thenReturn(Optional.empty());
        when(proposalRepository.findMaxVersionByOpportunityId(1))
                .thenReturn(0);
        when(proposalRepository.save(any(Proposal.class)))
                .thenAnswer(invocation -> {
                    Proposal proposal = invocation.getArgument(0);
                    proposal.setId(100);
                    return proposal;
                });
        when(proposalHistoryRepository.save(any(ProposalHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(opportunityRepository.save(any(Opportunity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ProposalDTO result = proposalService.createProposal(opportunityId, title, reviewerId, files, currentUser);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<Proposal> captor = ArgumentCaptor.forClass(Proposal.class);
        verify(proposalRepository, atLeastOnce()).save(captor.capture());
        Proposal saved = captor.getAllValues().get(0);
        assertEquals(1, saved.getOpportunityId());
        assertEquals(title, saved.getTitle());
        assertEquals("draft", saved.getStatus());
        assertEquals(1, saved.getVersion());
        assertTrue(saved.getIsCurrent());
        assertEquals(1, saved.getCreatedBy());
    }

    @Test
    @DisplayName("createProposal - có reviewerId → set reviewer và status internal_review")
    void testCreateProposal_WithReviewer() {
        // Arrange
        String opportunityId = "1";
        String title = "Test Proposal";
        Integer reviewerId = 2;
        MultipartFile[] files = null;
        User currentUser = createUser(1, "Sales Rep", "salesrep@example.com", "SALES_REP");
        User reviewer = createUser(2, "Sales Manager", "manager@example.com", "SALES_MANAGER");
        Opportunity opportunity = createOpportunity(1, "OP-2025-01", "NEW", 1, 1);

        when(opportunityRepository.findById(1))
                .thenReturn(Optional.of(opportunity));
        when(proposalRepository.findByOpportunityIdAndIsCurrent(1, true))
                .thenReturn(Optional.empty());
        when(proposalRepository.findMaxVersionByOpportunityId(1))
                .thenReturn(0);
        when(userRepository.findById(2))
                .thenReturn(Optional.of(reviewer));
        when(proposalRepository.save(any(Proposal.class)))
                .thenAnswer(invocation -> {
                    Proposal proposal = invocation.getArgument(0);
                    proposal.setId(100);
                    return proposal;
                });
        when(proposalHistoryRepository.save(any(ProposalHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(opportunityRepository.save(any(Opportunity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ProposalDTO result = proposalService.createProposal(opportunityId, title, reviewerId, files, currentUser);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<Proposal> captor = ArgumentCaptor.forClass(Proposal.class);
        verify(proposalRepository, atLeastOnce()).save(captor.capture());
        Proposal saved = captor.getAllValues().get(0);
        assertEquals(2, saved.getReviewerId());
        assertEquals("internal_review", saved.getStatus());
    }

    @Test
    @DisplayName("createProposal - reviewer không phải Sales Manager → throw RuntimeException")
    void testCreateProposal_ReviewerNotSalesManager() {
        // Arrange
        String opportunityId = "1";
        String title = "Test Proposal";
        Integer reviewerId = 2;
        MultipartFile[] files = null;
        User currentUser = createUser(1, "Sales Rep", "salesrep@example.com", "SALES_REP");
        User reviewer = createUser(2, "Sales Rep", "salesrep2@example.com", "SALES_REP"); // Not a manager
        Opportunity opportunity = createOpportunity(1, "OP-2025-01", "NEW", 1, 1);

        when(opportunityRepository.findById(1))
                .thenReturn(Optional.of(opportunity));
        when(proposalRepository.findByOpportunityIdAndIsCurrent(1, true))
                .thenReturn(Optional.empty());
        when(proposalRepository.findMaxVersionByOpportunityId(1))
                .thenReturn(0);
        when(userRepository.findById(2))
                .thenReturn(Optional.of(reviewer));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            proposalService.createProposal(opportunityId, title, reviewerId, files, currentUser);
        });

        verify(proposalRepository, never()).save(any(Proposal.class));
    }

    @Test
    @DisplayName("createProposal - proposal đã tồn tại → tạo version mới")
    void testCreateProposal_ExistingProposal_CreateNewVersion() {
        // Arrange
        String opportunityId = "1";
        String title = "Test Proposal v2";
        Integer reviewerId = null;
        MultipartFile[] files = null;
        User currentUser = createUser(1, "Sales Rep", "salesrep@example.com", "SALES_REP");
        Opportunity opportunity = createOpportunity(1, "OP-2025-01", "NEW", 1, 1);
        Proposal existingProposal = createProposal(1, 1, "draft", 1, true);

        when(opportunityRepository.findById(1))
                .thenReturn(Optional.of(opportunity));
        when(proposalRepository.findByOpportunityIdAndIsCurrent(1, true))
                .thenReturn(Optional.of(existingProposal));
        when(proposalRepository.findMaxVersionByOpportunityId(1))
                .thenReturn(1);
        when(proposalRepository.save(any(Proposal.class)))
                .thenAnswer(invocation -> {
                    Proposal proposal = invocation.getArgument(0);
                    if (proposal.getId() == null) {
                        proposal.setId(100);
                    }
                    return proposal;
                });
        when(proposalHistoryRepository.save(any(ProposalHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(opportunityRepository.save(any(Opportunity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ProposalDTO result = proposalService.createProposal(opportunityId, title, reviewerId, files, currentUser);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<Proposal> captor = ArgumentCaptor.forClass(Proposal.class);
        verify(proposalRepository, atLeast(2)).save(captor.capture());
        List<Proposal> saved = captor.getAllValues();
        // First save: mark old proposal as not current
        assertFalse(saved.get(0).getIsCurrent());
        // Second save: new proposal
        assertEquals(2, saved.get(1).getVersion()); // New version should be 2
        assertTrue(saved.get(1).getIsCurrent());
    }

    @Test
    @DisplayName("updateProposal - proposal không tồn tại → throw RuntimeException")
    void testUpdateProposal_NotFound() {
        // Arrange
        Integer proposalId = 1;
        String title = "Updated Proposal";
        Integer reviewerId = null;
        MultipartFile[] files = null;
        User currentUser = createUser(1, "Sales Rep", "salesrep@example.com", "SALES_REP");

        when(proposalRepository.findById(proposalId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            proposalService.updateProposal(proposalId, title, reviewerId, files, currentUser);
        });
    }

    @Test
    @DisplayName("updateProposal - Sales Rep không được tạo proposal → throw RuntimeException")
    void testUpdateProposal_SalesRep_NotAuthorized() {
        // Arrange
        Integer proposalId = 1;
        String title = "Updated Proposal";
        Integer reviewerId = null;
        MultipartFile[] files = null;
        User currentUser = createUser(1, "Sales Rep", "salesrep@example.com", "SALES_REP");
        Proposal proposal = createProposal(1, 1, "draft", 2, true); // Created by user 2

        when(proposalRepository.findById(proposalId))
                .thenReturn(Optional.of(proposal));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            proposalService.updateProposal(proposalId, title, reviewerId, files, currentUser);
        });

        verify(proposalRepository, never()).save(any(Proposal.class));
    }

    @Test
    @DisplayName("updateProposal - Sales Rep được tạo proposal → update thành công")
    void testUpdateProposal_SalesRep_Success() {
        // Arrange
        Integer proposalId = 1;
        String title = "Updated Proposal";
        Integer reviewerId = null;
        MultipartFile[] files = null;
        User currentUser = createUser(1, "Sales Rep", "salesrep@example.com", "SALES_REP");
        Proposal proposal = createProposal(1, 1, "draft", 1, true); // Created by user 1
        Opportunity opportunity = createOpportunity(1, "OP-2025-01", "NEW", 1, 1);

        when(proposalRepository.findById(proposalId))
                .thenReturn(Optional.of(proposal));
        when(opportunityRepository.findById(1))
                .thenReturn(Optional.of(opportunity));
        when(proposalRepository.save(any(Proposal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(proposalHistoryRepository.save(any(ProposalHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(opportunityRepository.save(any(Opportunity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ProposalDTO result = proposalService.updateProposal(proposalId, title, reviewerId, files, currentUser);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<Proposal> captor = ArgumentCaptor.forClass(Proposal.class);
        verify(proposalRepository).save(captor.capture());
        Proposal saved = captor.getValue();
        assertEquals(title, saved.getTitle());
    }

    // Helper methods
    private User createUser(Integer id, String fullName, String email, String role) {
        User user = new User();
        user.setId(id);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setRole(role);
        return user;
    }

    private Opportunity createOpportunity(Integer id, String opportunityId, String status, Integer assigneeUserId, Integer createdBy) {
        Opportunity opportunity = new Opportunity();
        opportunity.setId(id);
        opportunity.setOpportunityId(opportunityId);
        opportunity.setStatus(status);
        opportunity.setAssigneeUserId(assigneeUserId);
        opportunity.setCreatedBy(createdBy);
        opportunity.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        return opportunity;
    }

    private Proposal createProposal(Integer id, Integer opportunityId, String status, Integer createdBy, Boolean isCurrent) {
        Proposal proposal = new Proposal();
        proposal.setId(id);
        proposal.setOpportunityId(opportunityId);
        proposal.setStatus(status);
        proposal.setCreatedBy(createdBy);
        proposal.setIsCurrent(isCurrent);
        proposal.setVersion(1);
        proposal.setTitle("Test Proposal");
        proposal.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        return proposal;
    }
}

