package com.skillbridge.service.contract;

import com.skillbridge.dto.contract.response.ContractDetailDTO;
import com.skillbridge.entity.auth.User;
import com.skillbridge.entity.contract.Contract;
import com.skillbridge.entity.contract.ContractHistory;
import com.skillbridge.entity.contract.SOWContract;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.repository.contract.*;
import com.skillbridge.repository.document.DocumentMetadataRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
 * Unit tests for ContractDetailService
 * Tests CRUD operations for contract detail
 */
@ExtendWith(MockitoExtension.class)
class ContractDetailServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private SOWContractRepository sowContractRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContractHistoryRepository contractHistoryRepository;

    @Mock
    private ChangeRequestRepository changeRequestRepository;

    @Mock
    private MilestoneDeliverableRepository milestoneDeliverableRepository;

    @Mock
    private FixedPriceBillingDetailRepository fixedPriceBillingDetailRepository;

    @Mock
    private DeliveryItemRepository deliveryItemRepository;

    @Mock
    private RetainerBillingDetailRepository retainerBillingDetailRepository;

    @Mock
    private SOWEngagedEngineerRepository sowEngagedEngineerRepository;

    @Mock
    private DocumentMetadataRepository documentMetadataRepository;

    @InjectMocks
    private ContractDetailService contractDetailService;

    @BeforeEach
    void setUp() {
        // Setup is handled by MockitoExtension
    }

    @Test
    @DisplayName("getContractDetail - MSA contract không tồn tại → throw EntityNotFoundException")
    void testGetContractDetail_MSANotFound() {
        // Arrange
        Integer contractId = 1;
        Integer clientUserId = 5;

        when(contractRepository.findByIdAndClientId(contractId, clientUserId))
                .thenReturn(Optional.empty());
        when(sowContractRepository.findByIdAndClientId(contractId, clientUserId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            contractDetailService.getContractDetail(contractId, clientUserId);
        });

        verify(contractRepository).findByIdAndClientId(contractId, clientUserId);
        verify(sowContractRepository).findByIdAndClientId(contractId, clientUserId);
    }

    @Test
    @DisplayName("getContractDetail - MSA contract hợp lệ → trả về ContractDetailDTO")
    void testGetContractDetail_MSASuccess() {
        // Arrange
        Integer contractId = 1;
        Integer clientUserId = 5;

        Contract contract = createMSAContract(contractId, clientUserId);
        User client = createUser(clientUserId, "Client User", "client@example.com");

        when(contractRepository.findByIdAndClientId(contractId, clientUserId))
                .thenReturn(Optional.of(contract));
        when(userRepository.findById(clientUserId))
                .thenReturn(Optional.of(client));
        when(contractHistoryRepository.findByContractIdOrderByEntryDateDesc(contractId))
                .thenReturn(new ArrayList<>());
        when(documentMetadataRepository.findByEntityIdAndEntityType(contractId, "msa_contract"))
                .thenReturn(new ArrayList<>());

        // Act
        ContractDetailDTO result = contractDetailService.getContractDetail(contractId, clientUserId);

        // Assert
        assertNotNull(result);
        assertEquals("MSA", result.getContractType());
        assertEquals("Test Contract", result.getContractName());
        assertEquals("Active", result.getStatus());
        assertNotNull(result.getClientContact());
        assertEquals("client@example.com", result.getClientContact().getEmail());

        verify(contractRepository).findByIdAndClientId(contractId, clientUserId);
        verify(userRepository).findById(clientUserId);
        verify(contractHistoryRepository).findByContractIdOrderByEntryDateDesc(contractId);
    }

    @Test
    @DisplayName("getContractDetail - SOW contract hợp lệ → trả về ContractDetailDTO")
    void testGetContractDetail_SOWSuccess() {
        // Arrange
        Integer contractId = 1;
        Integer clientUserId = 5;

        SOWContract sow = createSOWContract(contractId, clientUserId);
        User client = createUser(clientUserId, "Client User", "client@example.com");

        when(contractRepository.findByIdAndClientId(contractId, clientUserId))
                .thenReturn(Optional.empty());
        when(sowContractRepository.findByIdAndClientId(contractId, clientUserId))
                .thenReturn(Optional.of(sow));
        when(userRepository.findById(clientUserId))
                .thenReturn(Optional.of(client));
        when(contractHistoryRepository.findBySowContractIdOrderByEntryDateDesc(contractId))
                .thenReturn(new ArrayList<>());
        when(changeRequestRepository.findBySowContractIdOrderByCreatedAtDesc(contractId))
                .thenReturn(new ArrayList<>());
        when(sowEngagedEngineerRepository.findBySowContractIdOrderByStartDateAsc(contractId))
                .thenReturn(new ArrayList<>());
        when(deliveryItemRepository.findBySowContractIdOrderByPaymentDateDesc(contractId))
                .thenReturn(new ArrayList<>());
        when(retainerBillingDetailRepository.findBySowContractIdOrderByPaymentDateDesc(contractId))
                .thenReturn(new ArrayList<>());
        when(documentMetadataRepository.findByEntityIdAndEntityType(contractId, "sow_contract"))
                .thenReturn(new ArrayList<>());

        // Act
        ContractDetailDTO result = contractDetailService.getContractDetail(contractId, clientUserId);

        // Assert
        assertNotNull(result);
        assertEquals("SOW", result.getContractType());
        assertEquals("Test SOW", result.getContractName());
        assertEquals("Active", result.getStatus());

        verify(contractRepository).findByIdAndClientId(contractId, clientUserId);
        verify(sowContractRepository).findByIdAndClientId(contractId, clientUserId);
        verify(userRepository).findById(clientUserId);
    }

    @Test
    @DisplayName("approveContract - MSA contract đã Active → throw IllegalStateException")
    void testApproveContract_MSAAlreadyActive() {
        // Arrange
        Integer contractId = 1;
        Integer clientUserId = 5;

        Contract contract = createMSAContract(contractId, clientUserId);
        contract.setStatus(Contract.ContractStatus.Active);

        when(contractRepository.findByIdAndClientId(contractId, clientUserId))
                .thenReturn(Optional.of(contract));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            contractDetailService.approveContract(contractId, clientUserId);
        });

        verify(contractRepository, never()).save(any(Contract.class));
    }

    @Test
    @DisplayName("approveContract - MSA contract hợp lệ → update status thành Active và tạo history")
    void testApproveContract_MSASuccess() {
        // Arrange
        Integer contractId = 1;
        Integer clientUserId = 5;

        Contract contract = createMSAContract(contractId, clientUserId);
        contract.setStatus(Contract.ContractStatus.Pending);

        when(contractRepository.findByIdAndClientId(contractId, clientUserId))
                .thenReturn(Optional.of(contract));
        when(contractRepository.save(any(Contract.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(contractHistoryRepository.save(any(ContractHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        contractDetailService.approveContract(contractId, clientUserId);

        // Assert
        ArgumentCaptor<Contract> contractCaptor = ArgumentCaptor.forClass(Contract.class);
        verify(contractRepository).save(contractCaptor.capture());
        assertEquals(Contract.ContractStatus.Active, contractCaptor.getValue().getStatus());

        ArgumentCaptor<ContractHistory> historyCaptor = ArgumentCaptor.forClass(ContractHistory.class);
        verify(contractHistoryRepository).save(historyCaptor.capture());
        ContractHistory history = historyCaptor.getValue();
        assertEquals(contractId, history.getContractId());
        assertEquals(clientUserId, history.getCreatedBy());
        assertEquals("MSA", history.getHistoryType());
        assertTrue(history.getDescription().contains("Status changed from Pending to Active"));
    }

    @Test
    @DisplayName("addComment - MSA contract hợp lệ → update status thành Request_for_Change và tạo history")
    void testAddComment_MSASuccess() {
        // Arrange
        Integer contractId = 1;
        Integer clientUserId = 5;
        String comment = "Need to change payment terms";

        Contract contract = createMSAContract(contractId, clientUserId);
        contract.setStatus(Contract.ContractStatus.Active);

        when(contractRepository.findByIdAndClientId(contractId, clientUserId))
                .thenReturn(Optional.of(contract));
        when(contractRepository.save(any(Contract.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(contractHistoryRepository.save(any(ContractHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        contractDetailService.addComment(contractId, clientUserId, comment);

        // Assert
        ArgumentCaptor<Contract> contractCaptor = ArgumentCaptor.forClass(Contract.class);
        verify(contractRepository).save(contractCaptor.capture());
        assertEquals(Contract.ContractStatus.Request_for_Change, contractCaptor.getValue().getStatus());

        ArgumentCaptor<ContractHistory> historyCaptor = ArgumentCaptor.forClass(ContractHistory.class);
        verify(contractHistoryRepository).save(historyCaptor.capture());
        ContractHistory history = historyCaptor.getValue();
        assertEquals(contractId, history.getContractId());
        assertEquals(clientUserId, history.getCreatedBy());
        assertEquals("MSA", history.getHistoryType());
        assertTrue(history.getDescription().contains(comment));
        assertTrue(history.getDescription().contains("Request for Change"));
    }

    @Test
    @DisplayName("cancelContract - MSA contract đã Completed → throw IllegalStateException")
    void testCancelContract_MSACompleted() {
        // Arrange
        Integer contractId = 1;
        Integer clientUserId = 5;
        String reason = "No longer needed";

        Contract contract = createMSAContract(contractId, clientUserId);
        contract.setStatus(Contract.ContractStatus.Completed);

        when(contractRepository.findByIdAndClientId(contractId, clientUserId))
                .thenReturn(Optional.of(contract));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            contractDetailService.cancelContract(contractId, clientUserId, reason);
        });

        verify(contractRepository, never()).save(any(Contract.class));
    }

    @Test
    @DisplayName("cancelContract - MSA contract Active → update status thành Pending và tạo history")
    void testCancelContract_MSAActive() {
        // Arrange
        Integer contractId = 1;
        Integer clientUserId = 5;
        String reason = "Client request";

        Contract contract = createMSAContract(contractId, clientUserId);
        contract.setStatus(Contract.ContractStatus.Active);

        when(contractRepository.findByIdAndClientId(contractId, clientUserId))
                .thenReturn(Optional.of(contract));
        when(contractRepository.save(any(Contract.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(contractHistoryRepository.save(any(ContractHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        contractDetailService.cancelContract(contractId, clientUserId, reason);

        // Assert
        ArgumentCaptor<Contract> contractCaptor = ArgumentCaptor.forClass(Contract.class);
        verify(contractRepository).save(contractCaptor.capture());
        assertEquals(Contract.ContractStatus.Pending, contractCaptor.getValue().getStatus());

        ArgumentCaptor<ContractHistory> historyCaptor = ArgumentCaptor.forClass(ContractHistory.class);
        verify(contractHistoryRepository).save(historyCaptor.capture());
        ContractHistory history = historyCaptor.getValue();
        assertEquals(contractId, history.getContractId());
        assertEquals(clientUserId, history.getCreatedBy());
        assertEquals("MSA", history.getHistoryType());
        assertTrue(history.getDescription().contains(reason));
    }

    @Test
    @DisplayName("cancelContract - SOW contract Under_Review → update status thành Cancelled và tạo history")
    void testCancelContract_SOWUnderReview() {
        // Arrange
        Integer contractId = 1;
        Integer clientUserId = 5;
        String reason = "Scope changed";

        SOWContract sow = createSOWContract(contractId, clientUserId);
        sow.setStatus(SOWContract.SOWContractStatus.Under_Review);

        when(contractRepository.findByIdAndClientId(contractId, clientUserId))
                .thenReturn(Optional.empty());
        when(sowContractRepository.findByIdAndClientId(contractId, clientUserId))
                .thenReturn(Optional.of(sow));
        when(sowContractRepository.save(any(SOWContract.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(contractHistoryRepository.save(any(ContractHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        contractDetailService.cancelContract(contractId, clientUserId, reason);

        // Assert
        ArgumentCaptor<SOWContract> sowCaptor = ArgumentCaptor.forClass(SOWContract.class);
        verify(sowContractRepository).save(sowCaptor.capture());
        assertEquals(SOWContract.SOWContractStatus.Cancelled, sowCaptor.getValue().getStatus());

        ArgumentCaptor<ContractHistory> historyCaptor = ArgumentCaptor.forClass(ContractHistory.class);
        verify(contractHistoryRepository).save(historyCaptor.capture());
        ContractHistory history = historyCaptor.getValue();
        assertEquals(contractId, history.getSowContractId());
        assertEquals(clientUserId, history.getCreatedBy());
        assertEquals("SOW", history.getHistoryType());
        assertTrue(history.getDescription().contains(reason));
    }

    // Helper methods
    private Contract createMSAContract(Integer id, Integer clientId) {
        Contract contract = new Contract();
        contract.setId(id);
        contract.setClientId(clientId);
        contract.setContractName("Test Contract");
        contract.setStatus(Contract.ContractStatus.Active);
        contract.setPeriodStart(LocalDate.of(2025, 1, 1));
        contract.setPeriodEnd(LocalDate.of(2025, 12, 31));
        contract.setValue(new BigDecimal("1000000"));
        contract.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        return contract;
    }

    private SOWContract createSOWContract(Integer id, Integer clientId) {
        SOWContract sow = new SOWContract();
        sow.setId(id);
        sow.setClientId(clientId);
        sow.setContractName("Test SOW");
        sow.setStatus(SOWContract.SOWContractStatus.Active);
        sow.setEngagementType("Retainer");
        sow.setParentMsaId(1);
        sow.setProjectName("Test Project");
        sow.setPeriodStart(LocalDate.of(2025, 1, 1));
        sow.setPeriodEnd(LocalDate.of(2025, 12, 31));
        sow.setValue(new BigDecimal("500000"));
        sow.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        return sow;
    }

    private User createUser(Integer id, String fullName, String email) {
        User user = new User();
        user.setId(id);
        user.setFullName(fullName);
        user.setEmail(email);
        return user;
    }
}

