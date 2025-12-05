package com.skillbridge.service.contract;

import com.skillbridge.dto.contract.request.UpdateChangeRequestRequest;
import com.skillbridge.dto.contract.response.ChangeRequestDetailDTO;
import com.skillbridge.entity.auth.User;
import com.skillbridge.entity.contract.ChangeRequest;
import com.skillbridge.entity.contract.ChangeRequestAttachment;
import com.skillbridge.entity.contract.ChangeRequestHistory;
import com.skillbridge.entity.contract.Contract;
import com.skillbridge.entity.contract.SOWContract;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.repository.contract.*;
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
 * Unit tests for ChangeRequestDetailService
 * Tests CRUD operations for change request detail
 */
@ExtendWith(MockitoExtension.class)
class ChangeRequestDetailServiceTest {

    @Mock
    private ChangeRequestRepository changeRequestRepository;

    @Mock
    private ChangeRequestAttachmentRepository changeRequestAttachmentRepository;

    @Mock
    private ChangeRequestHistoryRepository changeRequestHistoryRepository;

    @Mock
    private ChangeRequestEngagedEngineerRepository engagedEngineerRepository;

    @Mock
    private ChangeRequestBillingDetailRepository billingDetailRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private SOWContractRepository sowContractRepository;

    @Mock
    private FixedPriceBillingDetailRepository fixedPriceBillingDetailRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.skillbridge.service.sales.SalesSOWContractService salesSOWContractService;

    @InjectMocks
    private ChangeRequestDetailService changeRequestDetailService;

    @BeforeEach
    void setUp() {
        // Setup is handled by MockitoExtension
    }

    @Test
    @DisplayName("getChangeRequestDetail - change request không tồn tại → throw EntityNotFoundException")
    void testGetChangeRequestDetail_NotFound() {
        // Arrange
        Integer contractId = 1;
        Integer changeRequestId = 100;
        Integer clientUserId = 5;

        when(contractRepository.findByIdAndClientId(contractId, clientUserId))
                .thenReturn(Optional.empty());
        when(sowContractRepository.findByIdAndClientId(contractId, clientUserId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            changeRequestDetailService.getChangeRequestDetail(contractId, changeRequestId, clientUserId);
        });
    }

    @Test
    @DisplayName("getChangeRequestDetail - MSA change request hợp lệ → trả về ChangeRequestDetailDTO")
    void testGetChangeRequestDetail_MSASuccess() {
        // Arrange
        Integer contractId = 1;
        Integer changeRequestId = 100;
        Integer clientUserId = 5;

        Contract contract = createMSAContract(contractId, clientUserId);
        ChangeRequest changeRequest = createChangeRequest(changeRequestId, contractId, null, "MSA", "Draft");

        when(contractRepository.findByIdAndClientId(contractId, clientUserId))
                .thenReturn(Optional.of(contract));
        when(changeRequestRepository.findById(changeRequestId))
                .thenReturn(Optional.of(changeRequest));
        when(userRepository.findById(clientUserId))
                .thenReturn(Optional.of(createUser(clientUserId, "Test User", "test@example.com")));
        when(changeRequestHistoryRepository.findByChangeRequestIdOrderByTimestampDesc(changeRequestId))
                .thenReturn(new ArrayList<>());
        when(changeRequestAttachmentRepository.findByChangeRequestId(changeRequestId))
                .thenReturn(new ArrayList<>());

        // Act
        ChangeRequestDetailDTO result = changeRequestDetailService.getChangeRequestDetail(
                contractId, changeRequestId, clientUserId);

        // Assert
        assertNotNull(result);
        assertEquals(changeRequestId, result.getId());
        assertEquals("Test Change Request", result.getTitle());
        assertEquals("Draft", result.getStatus());
        assertNotNull(result.getCreatedBy());

        verify(changeRequestRepository).findById(changeRequestId);
        verify(changeRequestHistoryRepository).findByChangeRequestIdOrderByTimestampDesc(changeRequestId);
    }

    @Test
    @DisplayName("updateChangeRequest - change request không phải Draft → throw IllegalStateException")
    void testUpdateChangeRequest_NotDraft() {
        // Arrange
        Integer contractId = 1;
        Integer changeRequestId = 100;
        Integer clientUserId = 5;
        UpdateChangeRequestRequest request = createUpdateRequest();

        Contract contract = createMSAContract(contractId, clientUserId);
        ChangeRequest changeRequest = createChangeRequest(changeRequestId, contractId, null, "MSA", "Processing");

        when(contractRepository.findByIdAndClientId(contractId, clientUserId))
                .thenReturn(Optional.of(contract));
        when(changeRequestRepository.findById(changeRequestId))
                .thenReturn(Optional.of(changeRequest));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            changeRequestDetailService.updateChangeRequest(contractId, changeRequestId, clientUserId, request);
        });

        verify(changeRequestRepository, never()).save(any(ChangeRequest.class));
    }

    @Test
    @DisplayName("updateChangeRequest - Draft change request hợp lệ → update các field")
    void testUpdateChangeRequest_DraftSuccess() {
        // Arrange
        Integer contractId = 1;
        Integer changeRequestId = 100;
        Integer clientUserId = 5;
        UpdateChangeRequestRequest request = createUpdateRequest();

        Contract contract = createMSAContract(contractId, clientUserId);
        ChangeRequest changeRequest = createChangeRequest(changeRequestId, contractId, null, "MSA", "Draft");

        when(contractRepository.findByIdAndClientId(contractId, clientUserId))
                .thenReturn(Optional.of(contract));
        when(changeRequestRepository.findById(changeRequestId))
                .thenReturn(Optional.of(changeRequest));
        when(changeRequestRepository.save(any(ChangeRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        changeRequestDetailService.updateChangeRequest(contractId, changeRequestId, clientUserId, request);

        // Assert
        ArgumentCaptor<ChangeRequest> captor = ArgumentCaptor.forClass(ChangeRequest.class);
        verify(changeRequestRepository).save(captor.capture());
        ChangeRequest updated = captor.getValue();

        assertEquals(request.getTitle(), updated.getTitle());
        assertEquals(request.getType(), updated.getType());
        assertEquals(request.getDescription(), updated.getDescription());
        assertEquals(request.getReason(), updated.getReason());
        assertEquals(request.getDesiredStartDate(), updated.getDesiredStartDate());
        assertEquals(request.getDesiredEndDate(), updated.getDesiredEndDate());
        assertEquals(request.getExpectedExtraCost(), updated.getExpectedExtraCost());
    }

    @Test
    @DisplayName("submitChangeRequest - change request không phải Draft → throw IllegalStateException")
    void testSubmitChangeRequest_NotDraft() {
        // Arrange
        Integer contractId = 1;
        Integer changeRequestId = 100;
        Integer clientUserId = 5;

        Contract contract = createMSAContract(contractId, clientUserId);
        ChangeRequest changeRequest = createChangeRequest(changeRequestId, contractId, null, "MSA", "Processing");

        when(contractRepository.findByIdAndClientId(contractId, clientUserId))
                .thenReturn(Optional.of(contract));
        when(changeRequestRepository.findById(changeRequestId))
                .thenReturn(Optional.of(changeRequest));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            changeRequestDetailService.submitChangeRequest(contractId, changeRequestId, clientUserId);
        });

        verify(changeRequestRepository, never()).save(any(ChangeRequest.class));
    }

    @Test
    @DisplayName("submitChangeRequest - Draft change request hợp lệ → update status thành Under Review và tạo history")
    void testSubmitChangeRequest_DraftSuccess() {
        // Arrange
        Integer contractId = 1;
        Integer changeRequestId = 100;
        Integer clientUserId = 5;

        Contract contract = createMSAContract(contractId, clientUserId);
        ChangeRequest changeRequest = createChangeRequest(changeRequestId, contractId, null, "MSA", "Draft");

        when(contractRepository.findByIdAndClientId(contractId, clientUserId))
                .thenReturn(Optional.of(contract));
        when(changeRequestRepository.findById(changeRequestId))
                .thenReturn(Optional.of(changeRequest));
        when(changeRequestRepository.save(any(ChangeRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(clientUserId))
                .thenReturn(Optional.of(createUser(clientUserId, "Test User", "test@example.com")));
        when(changeRequestHistoryRepository.save(any(ChangeRequestHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        changeRequestDetailService.submitChangeRequest(contractId, changeRequestId, clientUserId);

        // Assert
        ArgumentCaptor<ChangeRequest> captor = ArgumentCaptor.forClass(ChangeRequest.class);
        verify(changeRequestRepository).save(captor.capture());
        assertEquals("Under Review", captor.getValue().getStatus());

        ArgumentCaptor<ChangeRequestHistory> historyCaptor = ArgumentCaptor.forClass(ChangeRequestHistory.class);
        verify(changeRequestHistoryRepository).save(historyCaptor.capture());
        ChangeRequestHistory history = historyCaptor.getValue();
        assertEquals(changeRequestId, history.getChangeRequestId());
        assertEquals("Submitted", history.getAction());
        assertEquals(clientUserId, history.getUserId());
    }

    @Test
    @DisplayName("approveChangeRequest - change request không phải Under Review hoặc Client Under Review → throw IllegalStateException")
    void testApproveChangeRequest_InvalidStatus() {
        // Arrange
        Integer contractId = 1;
        Integer changeRequestId = 100;
        Integer clientUserId = 5;

        Contract contract = createMSAContract(contractId, clientUserId);
        ChangeRequest changeRequest = createChangeRequest(changeRequestId, contractId, null, "MSA", "Draft");

        when(contractRepository.findByIdAndClientId(contractId, clientUserId))
                .thenReturn(Optional.of(contract));
        when(changeRequestRepository.findById(changeRequestId))
                .thenReturn(Optional.of(changeRequest));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            changeRequestDetailService.approveChangeRequest(contractId, changeRequestId, clientUserId);
        });

        verify(changeRequestRepository, never()).save(any(ChangeRequest.class));
    }

    @Test
    @DisplayName("approveChangeRequest - Under Review change request hợp lệ → update status thành Active và tạo history")
    void testApproveChangeRequest_UnderReviewSuccess() {
        // Arrange
        Integer contractId = 1;
        Integer changeRequestId = 100;
        Integer clientUserId = 5;

        Contract contract = createMSAContract(contractId, clientUserId);
        ChangeRequest changeRequest = createChangeRequest(changeRequestId, contractId, null, "MSA", "Under Review");

        when(contractRepository.findByIdAndClientId(contractId, clientUserId))
                .thenReturn(Optional.of(contract));
        when(changeRequestRepository.findById(changeRequestId))
                .thenReturn(Optional.of(changeRequest));
        when(changeRequestRepository.save(any(ChangeRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(clientUserId))
                .thenReturn(Optional.of(createUser(clientUserId, "Test User", "test@example.com")));
        when(changeRequestHistoryRepository.save(any(ChangeRequestHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        changeRequestDetailService.approveChangeRequest(contractId, changeRequestId, clientUserId);

        // Assert
        ArgumentCaptor<ChangeRequest> captor = ArgumentCaptor.forClass(ChangeRequest.class);
        verify(changeRequestRepository).save(captor.capture());
        assertEquals("Active", captor.getValue().getStatus());

        ArgumentCaptor<ChangeRequestHistory> historyCaptor = ArgumentCaptor.forClass(ChangeRequestHistory.class);
        verify(changeRequestHistoryRepository).save(historyCaptor.capture());
        ChangeRequestHistory history = historyCaptor.getValue();
        assertEquals("Approved", history.getAction());
    }

    @Test
    @DisplayName("requestForChange - change request không phải Under Review hoặc Client Under Review → throw IllegalStateException")
    void testRequestForChange_InvalidStatus() {
        // Arrange
        Integer contractId = 1;
        Integer changeRequestId = 100;
        Integer clientUserId = 5;
        String message = "Need changes";

        Contract contract = createMSAContract(contractId, clientUserId);
        ChangeRequest changeRequest = createChangeRequest(changeRequestId, contractId, null, "MSA", "Draft");

        when(contractRepository.findByIdAndClientId(contractId, clientUserId))
                .thenReturn(Optional.of(contract));
        when(changeRequestRepository.findById(changeRequestId))
                .thenReturn(Optional.of(changeRequest));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            changeRequestDetailService.requestForChange(contractId, changeRequestId, clientUserId, message);
        });

        verify(changeRequestRepository, never()).save(any(ChangeRequest.class));
    }

    @Test
    @DisplayName("requestForChange - Under Review change request hợp lệ → update status thành Request for Change và tạo history")
    void testRequestForChange_UnderReviewSuccess() {
        // Arrange
        Integer contractId = 1;
        Integer changeRequestId = 100;
        Integer clientUserId = 5;
        String message = "Need to modify scope";

        Contract contract = createMSAContract(contractId, clientUserId);
        ChangeRequest changeRequest = createChangeRequest(changeRequestId, contractId, null, "MSA", "Under Review");

        when(contractRepository.findByIdAndClientId(contractId, clientUserId))
                .thenReturn(Optional.of(contract));
        when(changeRequestRepository.findById(changeRequestId))
                .thenReturn(Optional.of(changeRequest));
        when(changeRequestRepository.save(any(ChangeRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(clientUserId))
                .thenReturn(Optional.of(createUser(clientUserId, "Test User", "test@example.com")));
        when(changeRequestHistoryRepository.save(any(ChangeRequestHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        changeRequestDetailService.requestForChange(contractId, changeRequestId, clientUserId, message);

        // Assert
        ArgumentCaptor<ChangeRequest> captor = ArgumentCaptor.forClass(ChangeRequest.class);
        verify(changeRequestRepository).save(captor.capture());
        assertEquals("Request for Change", captor.getValue().getStatus());

        ArgumentCaptor<ChangeRequestHistory> historyCaptor = ArgumentCaptor.forClass(ChangeRequestHistory.class);
        verify(changeRequestHistoryRepository).save(historyCaptor.capture());
        ChangeRequestHistory history = historyCaptor.getValue();
        assertTrue(history.getAction().contains("Request for Change"));
        assertTrue(history.getAction().contains(message));
    }

    @Test
    @DisplayName("terminateChangeRequest - change request hợp lệ → update status thành Terminated và tạo history")
    void testTerminateChangeRequest_Success() {
        // Arrange
        Integer contractId = 1;
        Integer changeRequestId = 100;
        Integer clientUserId = 5;

        Contract contract = createMSAContract(contractId, clientUserId);
        ChangeRequest changeRequest = createChangeRequest(changeRequestId, contractId, null, "MSA", "Under Review");

        when(contractRepository.findByIdAndClientId(contractId, clientUserId))
                .thenReturn(Optional.of(contract));
        when(changeRequestRepository.findById(changeRequestId))
                .thenReturn(Optional.of(changeRequest));
        when(changeRequestRepository.save(any(ChangeRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(clientUserId))
                .thenReturn(Optional.of(createUser(clientUserId, "Test User", "test@example.com")));
        when(changeRequestHistoryRepository.save(any(ChangeRequestHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        changeRequestDetailService.terminateChangeRequest(contractId, changeRequestId, clientUserId);

        // Assert
        ArgumentCaptor<ChangeRequest> captor = ArgumentCaptor.forClass(ChangeRequest.class);
        verify(changeRequestRepository).save(captor.capture());
        assertEquals("Terminated", captor.getValue().getStatus());

        ArgumentCaptor<ChangeRequestHistory> historyCaptor = ArgumentCaptor.forClass(ChangeRequestHistory.class);
        verify(changeRequestHistoryRepository).save(historyCaptor.capture());
        ChangeRequestHistory history = historyCaptor.getValue();
        assertEquals("Terminated", history.getAction());
    }

    // Helper methods
    private ChangeRequest createChangeRequest(Integer id, Integer contractId, Integer sowContractId, 
                                             String contractType, String status) {
        ChangeRequest cr = new ChangeRequest();
        cr.setId(id);
        cr.setContractId(contractId);
        cr.setSowContractId(sowContractId);
        cr.setContractType(contractType);
        cr.setChangeRequestId("CR-2025-01");
        cr.setTitle("Test Change Request");
        cr.setType("Other");
        cr.setDescription("Test description");
        cr.setReason("Test reason");
        cr.setStatus(status);
        cr.setCreatedBy(5);
        cr.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        return cr;
    }

    private Contract createMSAContract(Integer id, Integer clientId) {
        Contract contract = new Contract();
        contract.setId(id);
        contract.setClientId(clientId);
        contract.setContractName("Test MSA");
        contract.setStatus(Contract.ContractStatus.Active);
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

    private UpdateChangeRequestRequest createUpdateRequest() {
        UpdateChangeRequestRequest request = new UpdateChangeRequestRequest();
        request.setTitle("Updated Title");
        request.setType("Other");
        request.setDescription("Updated description");
        request.setReason("Updated reason");
        request.setDesiredStartDate(LocalDate.of(2025, 2, 1));
        request.setDesiredEndDate(LocalDate.of(2025, 2, 28));
        request.setExpectedExtraCost(new BigDecimal("150000"));
        return request;
    }
}

