package com.skillbridge.service.sales;

import com.skillbridge.dto.sales.request.CreateMSARequest;
import com.skillbridge.dto.sales.response.MSAContractDTO;
import com.skillbridge.dto.sales.response.MSAContractDetailDTO;
import com.skillbridge.entity.auth.User;
import com.skillbridge.entity.contract.Contract;
import com.skillbridge.entity.opportunity.Opportunity;
import com.skillbridge.entity.proposal.Proposal;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.repository.contract.*;
import com.skillbridge.repository.document.DocumentMetadataRepository;
import com.skillbridge.repository.opportunity.OpportunityRepository;
import com.skillbridge.repository.proposal.ProposalRepository;
import com.skillbridge.service.common.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SalesMSAContractService
 * Tests business logic for MSA contract operations
 */
@ExtendWith(MockitoExtension.class)
class SalesMSAContractServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private OpportunityRepository opportunityRepository;

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private S3Service s3Service;

    @Mock
    private ContractHistoryRepository contractHistoryRepository;

    @Mock
    private ContractInternalReviewRepository contractInternalReviewRepository;

    @Mock
    private DocumentMetadataRepository documentMetadataRepository;

    @Mock
    private ChangeRequestRepository changeRequestRepository;

    @Mock
    private ChangeRequestEngagedEngineerRepository changeRequestEngagedEngineerRepository;

    @Mock
    private ChangeRequestBillingDetailRepository changeRequestBillingDetailRepository;

    @Mock
    private ChangeRequestAttachmentRepository changeRequestAttachmentRepository;

    @Mock
    private ChangeRequestHistoryRepository changeRequestHistoryRepository;

    @InjectMocks
    private SalesMSAContractService salesMSAContractService;

    @BeforeEach
    void setUp() {
        // Setup is handled by MockitoExtension
    }

    @Test
    @DisplayName("createMSAContract - opportunity không tồn tại → throw RuntimeException")
    void testCreateMSAContract_OpportunityNotFound() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setOpportunityId("OP-2025-01");
        request.setClientId(10);
        request.setAssigneeUserId(1);
        MultipartFile[] attachments = null;
        User currentUser = createUser(1, "Sales Manager", "manager@example.com", "SALES_MANAGER");

        when(opportunityRepository.findByOpportunityId("OP-2025-01"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            salesMSAContractService.createMSAContract(request, attachments, currentUser);
        });

        verify(contractRepository, never()).save(any(Contract.class));
    }

    @Test
    @DisplayName("createMSAContract - opportunity không có approved proposal → throw RuntimeException")
    void testCreateMSAContract_NoApprovedProposal() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setOpportunityId("OP-2025-01");
        request.setClientId(10);
        request.setAssigneeUserId(1);
        MultipartFile[] attachments = null;
        User currentUser = createUser(1, "Sales Manager", "manager@example.com", "SALES_MANAGER");
        Opportunity opportunity = createOpportunity(1, "OP-2025-01");
        Proposal proposal = createProposal(1, 1, "draft", null); // Not approved

        when(opportunityRepository.findByOpportunityId("OP-2025-01"))
                .thenReturn(Optional.of(opportunity));
        when(proposalRepository.findByOpportunityIdOrderByVersionDesc(1))
                .thenReturn(List.of(proposal));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            salesMSAContractService.createMSAContract(request, attachments, currentUser);
        });

        verify(contractRepository, never()).save(any(Contract.class));
    }

    @Test
    @DisplayName("createMSAContract - client không tồn tại → throw RuntimeException")
    void testCreateMSAContract_ClientNotFound() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setClientId(10);
        request.setAssigneeUserId(1);
        MultipartFile[] attachments = null;
        User currentUser = createUser(1, "Sales Manager", "manager@example.com", "SALES_MANAGER");

        when(userRepository.findById(10))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            salesMSAContractService.createMSAContract(request, attachments, currentUser);
        });

        verify(contractRepository, never()).save(any(Contract.class));
    }

    @Test
    @DisplayName("createMSAContract - assignee không tồn tại → throw RuntimeException")
    void testCreateMSAContract_AssigneeNotFound() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setClientId(10);
        request.setAssigneeUserId(1);
        MultipartFile[] attachments = null;
        User currentUser = createUser(1, "Sales Manager", "manager@example.com", "SALES_MANAGER");
        User client = createUser(10, "Client", "client@example.com", "CLIENT");

        when(userRepository.findById(10))
                .thenReturn(Optional.of(client));
        when(userRepository.findById(1))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            salesMSAContractService.createMSAContract(request, attachments, currentUser);
        });

        verify(contractRepository, never()).save(any(Contract.class));
    }

    @Test
    @DisplayName("createMSAContract - hợp lệ → tạo MSA contract thành công")
    void testCreateMSAContract_Success() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setOpportunityId("OP-2025-01");
        request.setClientId(10);
        request.setAssigneeUserId(1);
        request.setEffectiveStart("2025-01-01");
        request.setEffectiveEnd("2025-12-31");
        request.setStatus("Draft");
        request.setClientContactId(11);
        request.setLandbridgeContactId(12);
        MultipartFile[] attachments = null;
        User currentUser = createUser(1, "Sales Manager", "manager@example.com", "SALES_MANAGER");
        User client = createUser(10, "Client", "client@example.com", "CLIENT");
        User assignee = createUser(1, "Sales Rep", "salesrep@example.com", "SALES_REP");
        User clientContact = createUser(11, "Client Contact", "clientcontact@example.com", "CLIENT");
        User landbridgeContact = createUser(12, "LandBridge Contact", "landbridgecontact@example.com", "SALES_MANAGER");
        Opportunity opportunity = createOpportunity(1, "OP-2025-01");
        Proposal proposal = createProposal(1, 1, "approved", "APPROVE"); // Approved proposal

        when(opportunityRepository.findByOpportunityId("OP-2025-01"))
                .thenReturn(Optional.of(opportunity));
        when(proposalRepository.findByOpportunityIdOrderByVersionDesc(1))
                .thenReturn(List.of(proposal));
        when(userRepository.findById(10))
                .thenReturn(Optional.of(client));
        when(userRepository.findById(1))
                .thenReturn(Optional.of(assignee));
        when(userRepository.findById(11))
                .thenReturn(Optional.of(clientContact));
        when(userRepository.findById(12))
                .thenReturn(Optional.of(landbridgeContact));
        when(contractRepository.save(any(Contract.class)))
                .thenAnswer(invocation -> {
                    Contract contract = invocation.getArgument(0);
                    contract.setId(100);
                    return contract;
                });
        when(contractHistoryRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        MSAContractDTO result = salesMSAContractService.createMSAContract(request, attachments, currentUser);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<Contract> captor = ArgumentCaptor.forClass(Contract.class);
        verify(contractRepository).save(captor.capture());
        Contract saved = captor.getValue();
        assertEquals(10, saved.getClientId());
        assertEquals(1, saved.getAssigneeUserId());
        assertEquals(LocalDate.of(2025, 1, 1), saved.getPeriodStart());
        assertEquals(LocalDate.of(2025, 12, 31), saved.getPeriodEnd());
    }

    @Test
    @DisplayName("getMSAContractDetail - contract không tồn tại → throw RuntimeException")
    void testGetMSAContractDetail_NotFound() {
        // Arrange
        String contractId = "1";
        User currentUser = createUser(1, "Sales Manager", "manager@example.com", "SALES_MANAGER");

        when(contractRepository.findById(1))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            salesMSAContractService.getMSAContractDetail(Integer.parseInt(contractId), currentUser);
        });
    }

    @Test
    @DisplayName("getMSAContractDetail - Sales Rep không được assign → throw RuntimeException")
    void testGetMSAContractDetail_SalesRep_NotAssigned() {
        // Arrange
        String contractId = "1";
        User currentUser = createUser(1, "Sales Rep", "salesrep@example.com", "SALES_REP");
        Contract contract = createContract(1, Contract.ContractStatus.Active, 2); // Assigned to user 2

        when(contractRepository.findById(1))
                .thenReturn(Optional.of(contract));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            salesMSAContractService.getMSAContractDetail(Integer.parseInt(contractId), currentUser);
        });
    }

    @Test
    @DisplayName("getMSAContractDetail - Sales Manager → trả về contract detail")
    void testGetMSAContractDetail_SalesManager_Success() {
        // Arrange
        String contractId = "1";
        User currentUser = createUser(1, "Sales Manager", "manager@example.com", "SALES_MANAGER");
        Contract contract = createContract(1, Contract.ContractStatus.Active, 2);
        User client = createUser(10, "Client", "client@example.com", "CLIENT");

        when(contractRepository.findById(1))
                .thenReturn(Optional.of(contract));
        when(userRepository.findById(10))
                .thenReturn(Optional.of(client));
        when(changeRequestRepository.findAll())
                .thenReturn(new ArrayList<>());
        when(contractHistoryRepository.findAll())
                .thenReturn(new ArrayList<>());

        // Act
        MSAContractDetailDTO result = salesMSAContractService.getMSAContractDetail(Integer.parseInt(contractId), currentUser);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
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

    private Opportunity createOpportunity(Integer id, String opportunityId) {
        Opportunity opportunity = new Opportunity();
        opportunity.setId(id);
        opportunity.setOpportunityId(opportunityId);
        opportunity.setStatus("WON");
        opportunity.setClientEmail("client@example.com");
        opportunity.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        return opportunity;
    }

    private Proposal createProposal(Integer id, Integer opportunityId, String status, String reviewAction) {
        Proposal proposal = new Proposal();
        proposal.setId(id);
        proposal.setOpportunityId(opportunityId);
        proposal.setStatus(status);
        proposal.setReviewAction(reviewAction);
        proposal.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        return proposal;
    }

    private Contract createContract(Integer id, Contract.ContractStatus status, Integer assigneeUserId) {
        Contract contract = new Contract();
        contract.setId(id);
        contract.setStatus(status);
        contract.setAssigneeUserId(assigneeUserId);
        contract.setClientId(10);
        contract.setContractName("Test MSA Contract");
        contract.setPeriodStart(LocalDate.of(2025, 1, 1));
        contract.setPeriodEnd(LocalDate.of(2025, 12, 31));
        contract.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        return contract;
    }
}

