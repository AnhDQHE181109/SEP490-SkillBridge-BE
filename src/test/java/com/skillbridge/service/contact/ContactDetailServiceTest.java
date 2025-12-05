package com.skillbridge.service.contact;

import com.skillbridge.dto.contact.response.*;
import com.skillbridge.entity.auth.User;
import com.skillbridge.entity.contact.CommunicationLog;
import com.skillbridge.entity.contact.ConsultationCancellation;
import com.skillbridge.entity.contact.Contact;
import com.skillbridge.entity.proposal.Proposal;
import com.skillbridge.entity.proposal.ProposalComment;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.repository.contact.CommunicationLogRepository;
import com.skillbridge.repository.contact.ConsultationCancellationRepository;
import com.skillbridge.repository.contact.ContactRepository;
import com.skillbridge.repository.proposal.ProposalCommentRepository;
import com.skillbridge.repository.proposal.ProposalRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ContactDetailService
 * Tests CRUD operations for contact detail
 */
@ExtendWith(MockitoExtension.class)
class ContactDetailServiceTest {

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private CommunicationLogRepository communicationLogRepository;

    @Mock
    private ProposalCommentRepository proposalCommentRepository;

    @Mock
    private ConsultationCancellationRepository cancellationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private com.skillbridge.repository.opportunity.OpportunityRepository opportunityRepository;

    @InjectMocks
    private ContactDetailService contactDetailService;

    @BeforeEach
    void setUp() {
        // Setup is handled by MockitoExtension
    }

    @Test
    @DisplayName("getContactDetail - contact không tồn tại → throw RuntimeException")
    void testGetContactDetail_NotFound() {
        // Arrange
        Integer contactId = 1;
        Integer clientUserId = 5;

        when(contactRepository.findByIdAndClientUserId(contactId, clientUserId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            contactDetailService.getContactDetail(contactId, clientUserId);
        });

        verify(contactRepository).findByIdAndClientUserId(contactId, clientUserId);
    }

    @Test
    @DisplayName("getContactDetail - contact hợp lệ → trả về ContactDetailDTO")
    void testGetContactDetail_Success() {
        // Arrange
        Integer contactId = 1;
        Integer clientUserId = 5;

        Contact contact = createContact(contactId, clientUserId);
        User client = createUser(clientUserId, "Test User", "test@example.com");

        when(contactRepository.findByIdAndClientUserId(contactId, clientUserId))
                .thenReturn(Optional.of(contact));
        when(userRepository.findById(clientUserId))
                .thenReturn(Optional.of(client));
        when(communicationLogRepository.findByContactIdOrderByCreatedAtDesc(contactId))
                .thenReturn(new ArrayList<>());
        when(proposalRepository.findByContactId(contactId))
                .thenReturn(new ArrayList<>());
        when(opportunityRepository.findByContactId(contactId))
                .thenReturn(new ArrayList<>());
        when(proposalCommentRepository.findByContactIdOrderByCreatedAtDesc(contactId))
                .thenReturn(new ArrayList<>());

        // Act
        ContactDetailDTO result = contactDetailService.getContactDetail(contactId, clientUserId);

        // Assert
        assertNotNull(result);
        assertEquals(contactId, result.getContactId());
        assertEquals("Test Contact", result.getConsultationRequest());
        assertEquals("New", result.getStatus());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("Test User", result.getClientName());

        verify(contactRepository).findByIdAndClientUserId(contactId, clientUserId);
        verify(userRepository).findById(clientUserId);
        verify(communicationLogRepository).findByContactIdOrderByCreatedAtDesc(contactId);
    }

    @Test
    @DisplayName("addCommunicationLog - contact không tồn tại → throw RuntimeException")
    void testAddCommunicationLog_ContactNotFound() {
        // Arrange
        Integer contactId = 1;
        Integer userId = 5;
        String message = "Test message";

        when(contactRepository.findByIdAndClientUserId(contactId, userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            contactDetailService.addCommunicationLog(contactId, userId, message);
        });

        verify(communicationLogRepository, never()).save(any(CommunicationLog.class));
    }

    @Test
    @DisplayName("addCommunicationLog - contact hợp lệ → tạo CommunicationLog và trả về DTO")
    void testAddCommunicationLog_Success() {
        // Arrange
        Integer contactId = 1;
        Integer userId = 5;
        String message = "Test message";

        Contact contact = createContact(contactId, userId);
        User user = createUser(userId, "Test User", "test@example.com");

        when(contactRepository.findByIdAndClientUserId(contactId, userId))
                .thenReturn(Optional.of(contact));
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(communicationLogRepository.save(any(CommunicationLog.class)))
                .thenAnswer(invocation -> {
                    CommunicationLog log = invocation.getArgument(0);
                    log.setId(100);
                    return log;
                });

        // Act
        CommunicationLogDTO result = contactDetailService.addCommunicationLog(contactId, userId, message);

        // Assert
        assertNotNull(result);
        assertEquals(message, result.getMessage());
        assertEquals(userId, result.getCreatedBy());
        assertEquals("Test User", result.getCreatedByName());

        ArgumentCaptor<CommunicationLog> captor = ArgumentCaptor.forClass(CommunicationLog.class);
        verify(communicationLogRepository).save(captor.capture());
        CommunicationLog saved = captor.getValue();
        assertEquals(contactId, saved.getContactId());
        assertEquals(message, saved.getMessage());
        assertEquals(userId, saved.getCreatedBy());
    }

    @Test
    @DisplayName("addProposalComment - contact không tồn tại → throw RuntimeException")
    void testAddProposalComment_ContactNotFound() {
        // Arrange
        Integer contactId = 1;
        Integer userId = 5;
        String message = "Test comment";

        when(contactRepository.findByIdAndClientUserId(contactId, userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            contactDetailService.addProposalComment(contactId, userId, message);
        });

        verify(proposalCommentRepository, never()).save(any(ProposalComment.class));
    }

    @Test
    @DisplayName("addProposalComment - contact hợp lệ → tạo ProposalComment và update proposal")
    void testAddProposalComment_Success() {
        // Arrange
        Integer contactId = 1;
        Integer userId = 5;
        String message = "Need changes";

        Contact contact = createContact(contactId, userId);
        Proposal proposal = createProposal(1, contactId, "sent_to_client");

        when(contactRepository.findByIdAndClientUserId(contactId, userId))
                .thenReturn(Optional.of(contact));
        when(proposalRepository.findByContactId(contactId))
                .thenReturn(List.of(proposal));
        when(proposalCommentRepository.save(any(ProposalComment.class)))
                .thenAnswer(invocation -> {
                    ProposalComment comment = invocation.getArgument(0);
                    comment.setId(100);
                    return comment;
                });
        when(proposalRepository.save(any(Proposal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(contactRepository.save(any(Contact.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CommentResponse result = contactDetailService.addProposalComment(contactId, userId, message);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("Comment added successfully", result.getMessage());
        assertEquals(100, result.getCommentId());

        ArgumentCaptor<ProposalComment> commentCaptor = ArgumentCaptor.forClass(ProposalComment.class);
        verify(proposalCommentRepository).save(commentCaptor.capture());
        ProposalComment savedComment = commentCaptor.getValue();
        assertEquals(contactId, savedComment.getContactId());
        assertEquals(message, savedComment.getMessage());
        assertEquals(userId, savedComment.getCreatedBy());

        ArgumentCaptor<Proposal> proposalCaptor = ArgumentCaptor.forClass(Proposal.class);
        verify(proposalRepository).save(proposalCaptor.capture());
        Proposal updatedProposal = proposalCaptor.getValue();
        assertEquals("revision_requested", updatedProposal.getStatus());
        assertEquals(message, updatedProposal.getClientFeedback());
    }

    @Test
    @DisplayName("cancelConsultation - contact không tồn tại → throw RuntimeException")
    void testCancelConsultation_ContactNotFound() {
        // Arrange
        Integer contactId = 1;
        Integer userId = 5;
        String reason = "No longer needed";

        when(contactRepository.findByIdAndClientUserId(contactId, userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            contactDetailService.cancelConsultation(contactId, userId, reason);
        });

        verify(cancellationRepository, never()).save(any(ConsultationCancellation.class));
        verify(contactRepository, never()).save(any(Contact.class));
    }

    @Test
    @DisplayName("cancelConsultation - contact hợp lệ → tạo ConsultationCancellation và update status")
    void testCancelConsultation_Success() {
        // Arrange
        Integer contactId = 1;
        Integer userId = 5;
        String reason = "No longer needed";

        Contact contact = createContact(contactId, userId);

        when(contactRepository.findByIdAndClientUserId(contactId, userId))
                .thenReturn(Optional.of(contact));
        when(cancellationRepository.save(any(ConsultationCancellation.class)))
                .thenAnswer(invocation -> {
                    ConsultationCancellation cancel = invocation.getArgument(0);
                    cancel.setId(100);
                    return cancel;
                });
        when(contactRepository.save(any(Contact.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CancelResponse result = contactDetailService.cancelConsultation(contactId, userId, reason);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("Consultation cancelled successfully", result.getMessage());

        ArgumentCaptor<ConsultationCancellation> cancelCaptor = ArgumentCaptor.forClass(ConsultationCancellation.class);
        verify(cancellationRepository).save(cancelCaptor.capture());
        ConsultationCancellation saved = cancelCaptor.getValue();
        assertEquals(contactId, saved.getContactId());
        assertEquals(reason, saved.getReason());
        assertEquals(userId, saved.getCancelledBy());

        ArgumentCaptor<Contact> contactCaptor = ArgumentCaptor.forClass(Contact.class);
        verify(contactRepository).save(contactCaptor.capture());
        Contact updated = contactCaptor.getValue();
        assertEquals("Closed", updated.getStatus());
    }

    @Test
    @DisplayName("approveProposal - contact không tồn tại → throw RuntimeException")
    void testApproveProposal_ContactNotFound() {
        // Arrange
        Integer contactId = 1;
        Integer userId = 5;

        when(contactRepository.findByIdAndClientUserId(contactId, userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            contactDetailService.approveProposal(contactId, userId);
        });

        verify(proposalRepository, never()).save(any(Proposal.class));
    }

    @Test
    @DisplayName("approveProposal - không có proposal → throw RuntimeException")
    void testApproveProposal_NoProposal() {
        // Arrange
        Integer contactId = 1;
        Integer userId = 5;

        Contact contact = createContact(contactId, userId);

        when(contactRepository.findByIdAndClientUserId(contactId, userId))
                .thenReturn(Optional.of(contact));
        when(proposalRepository.findByContactId(contactId))
                .thenReturn(new ArrayList<>());
        when(opportunityRepository.findByContactId(contactId))
                .thenReturn(new ArrayList<>());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            contactDetailService.approveProposal(contactId, userId);
        });

        verify(proposalRepository, never()).save(any(Proposal.class));
    }

    @Test
    @DisplayName("approveProposal - proposal hợp lệ → update status thành approved và update contact")
    void testApproveProposal_Success() {
        // Arrange
        Integer contactId = 1;
        Integer userId = 5;

        Contact contact = createContact(contactId, userId);
        Proposal proposal = createProposal(1, contactId, "sent_to_client");

        when(contactRepository.findByIdAndClientUserId(contactId, userId))
                .thenReturn(Optional.of(contact));
        when(proposalRepository.findByContactId(contactId))
                .thenReturn(List.of(proposal));
        when(opportunityRepository.findByContactId(contactId))
                .thenReturn(new ArrayList<>());
        when(proposalRepository.save(any(Proposal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(contactRepository.save(any(Contact.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ApproveResponse result = contactDetailService.approveProposal(contactId, userId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("Proposal approved successfully", result.getMessage());

        ArgumentCaptor<Proposal> proposalCaptor = ArgumentCaptor.forClass(Proposal.class);
        verify(proposalRepository).save(proposalCaptor.capture());
        Proposal updated = proposalCaptor.getValue();
        assertEquals("approved", updated.getStatus());

        ArgumentCaptor<Contact> contactCaptor = ArgumentCaptor.forClass(Contact.class);
        verify(contactRepository).save(contactCaptor.capture());
        Contact updatedContact = contactCaptor.getValue();
        assertEquals("Approved", updatedContact.getProposalStatus());
    }

    // Helper methods
    private Contact createContact(Integer id, Integer clientUserId) {
        Contact contact = new Contact();
        contact.setId(id);
        contact.setClientUserId(clientUserId);
        contact.setTitle("Test Contact");
        contact.setDescription("Test Contact");
        contact.setStatus("New");
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

    private Proposal createProposal(Integer id, Integer contactId, String status) {
        Proposal proposal = new Proposal();
        proposal.setId(id);
        proposal.setContactId(contactId);
        proposal.setTitle("Test Proposal");
        proposal.setStatus(status);
        proposal.setLink("http://example.com/proposal");
        proposal.setIsCurrent(true);
        proposal.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        return proposal;
    }
}

