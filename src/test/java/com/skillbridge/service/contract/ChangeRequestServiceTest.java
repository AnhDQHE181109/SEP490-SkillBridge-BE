package com.skillbridge.service.contract;

import com.skillbridge.dto.contract.request.CreateChangeRequestRequest;
import com.skillbridge.dto.contract.response.ChangeRequestResponse;
import com.skillbridge.entity.contract.ChangeRequest;
import com.skillbridge.entity.contract.Contract;
import com.skillbridge.entity.contract.SOWContract;
import com.skillbridge.repository.contract.ChangeRequestAttachmentRepository;
import com.skillbridge.repository.contract.ChangeRequestRepository;
import com.skillbridge.repository.contract.ContractRepository;
import com.skillbridge.repository.contract.SOWContractRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
 * Unit tests for ChangeRequestService
 * Tests CRUD operations for change requests
 */
@ExtendWith(MockitoExtension.class)
class ChangeRequestServiceTest {

    @Mock
    private ChangeRequestRepository changeRequestRepository;

    @Mock
    private ChangeRequestAttachmentRepository changeRequestAttachmentRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private SOWContractRepository sowContractRepository;

    @InjectMocks
    private ChangeRequestService changeRequestService;

    @BeforeEach
    void setUp() {
        // Set upload directory for testing
        ReflectionTestUtils.setField(changeRequestService, "uploadDir", "test-uploads");
    }

    @Test
    @DisplayName("createChangeRequest - contract không tồn tại → throw EntityNotFoundException")
    void testCreateChangeRequest_ContractNotFound() {
        // Arrange
        Integer contractId = 1;
        Integer clientUserId = 5;
        CreateChangeRequestRequest request = createChangeRequestRequest();
        List<org.springframework.web.multipart.MultipartFile> attachments = new ArrayList<>();

        when(contractRepository.findByIdAndClientId(contractId, clientUserId))
                .thenReturn(Optional.empty());
        when(sowContractRepository.findByIdAndClientId(contractId, clientUserId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            changeRequestService.createChangeRequest(contractId, clientUserId, request, attachments);
        });

        verify(changeRequestRepository, never()).save(any(ChangeRequest.class));
    }

    @Test
    @DisplayName("createChangeRequest - MSA contract hợp lệ → tạo ChangeRequest với status Processing")
    void testCreateChangeRequest_MSASuccess() {
        // Arrange
        Integer contractId = 1;
        Integer clientUserId = 5;
        CreateChangeRequestRequest request = createChangeRequestRequest();
        List<org.springframework.web.multipart.MultipartFile> attachments = new ArrayList<>();

        Contract contract = createMSAContract(contractId, clientUserId);

        when(contractRepository.findByIdAndClientId(contractId, clientUserId))
                .thenReturn(Optional.of(contract));
        when(changeRequestRepository.findAll())
                .thenReturn(new ArrayList<>());
        when(changeRequestRepository.save(any(ChangeRequest.class)))
                .thenAnswer(invocation -> {
                    ChangeRequest cr = invocation.getArgument(0);
                    cr.setId(123);
                    cr.setChangeRequestId("CR-2025-01");
                    return cr;
                });

        // Act
        ChangeRequestResponse response = changeRequestService.createChangeRequest(
                contractId, clientUserId, request, attachments);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Change request created successfully", response.getMessage());
        assertEquals(123, response.getChangeRequestId());
        assertNotNull(response.getChangeRequestDisplayId());

        ArgumentCaptor<ChangeRequest> captor = ArgumentCaptor.forClass(ChangeRequest.class);
        verify(changeRequestRepository).save(captor.capture());
        ChangeRequest saved = captor.getValue();

        assertEquals(contractId, saved.getContractId());
        assertEquals("MSA", saved.getContractType());
        assertEquals(request.getTitle(), saved.getTitle());
        assertEquals(request.getType(), saved.getType());
        assertEquals("Processing", saved.getStatus());
        assertEquals(clientUserId, saved.getCreatedBy());
    }

    @Test
    @DisplayName("createChangeRequest - SOW contract Fixed Price với type hợp lệ → tạo ChangeRequest thành công")
    void testCreateChangeRequest_SOWFixedPriceSuccess() {
        // Arrange
        Integer contractId = 1;
        Integer clientUserId = 5;
        CreateChangeRequestRequest request = createChangeRequestRequest();
        request.setType("Add Scope");
        List<org.springframework.web.multipart.MultipartFile> attachments = new ArrayList<>();

        SOWContract sow = createSOWContract(contractId, clientUserId);
        sow.setEngagementType("Fixed Price");

        when(contractRepository.findByIdAndClientId(contractId, clientUserId))
                .thenReturn(Optional.empty());
        when(sowContractRepository.findByIdAndClientId(contractId, clientUserId))
                .thenReturn(Optional.of(sow));
        when(changeRequestRepository.findAll())
                .thenReturn(new ArrayList<>());
        when(changeRequestRepository.save(any(ChangeRequest.class)))
                .thenAnswer(invocation -> {
                    ChangeRequest cr = invocation.getArgument(0);
                    cr.setId(123);
                    cr.setChangeRequestId("CR-2025-01");
                    return cr;
                });

        // Act
        ChangeRequestResponse response = changeRequestService.createChangeRequest(
                contractId, clientUserId, request, attachments);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Change request created successfully", response.getMessage());

        ArgumentCaptor<ChangeRequest> captor = ArgumentCaptor.forClass(ChangeRequest.class);
        verify(changeRequestRepository).save(captor.capture());
        ChangeRequest saved = captor.getValue();

        assertEquals(contractId, saved.getSowContractId());
        assertEquals("SOW", saved.getContractType());
        assertEquals("Processing", saved.getStatus());
    }

    @Test
    @DisplayName("createChangeRequest - SOW Retainer với type không hợp lệ → throw IllegalArgumentException")
    void testCreateChangeRequest_SOWRetainerInvalidType() {
        // Arrange
        Integer contractId = 1;
        Integer clientUserId = 5;
        CreateChangeRequestRequest request = createChangeRequestRequest();
        request.setType("Add Scope"); // Invalid for Retainer
        List<org.springframework.web.multipart.MultipartFile> attachments = new ArrayList<>();

        SOWContract sow = createSOWContract(contractId, clientUserId);
        sow.setEngagementType("Retainer");

        when(contractRepository.findByIdAndClientId(contractId, clientUserId))
                .thenReturn(Optional.empty());
        when(sowContractRepository.findByIdAndClientId(contractId, clientUserId))
                .thenReturn(Optional.of(sow));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            changeRequestService.createChangeRequest(contractId, clientUserId, request, attachments);
        });

        verify(changeRequestRepository, never()).save(any(ChangeRequest.class));
    }

    @Test
    @DisplayName("saveChangeRequestDraft - MSA contract hợp lệ → tạo ChangeRequest với status Draft")
    void testSaveChangeRequestDraft_MSASuccess() {
        // Arrange
        Integer contractId = 1;
        Integer clientUserId = 5;
        CreateChangeRequestRequest request = createChangeRequestRequest();
        List<org.springframework.web.multipart.MultipartFile> attachments = new ArrayList<>();

        Contract contract = createMSAContract(contractId, clientUserId);

        when(contractRepository.findByIdAndClientId(contractId, clientUserId))
                .thenReturn(Optional.of(contract));
        when(changeRequestRepository.findAll())
                .thenReturn(new ArrayList<>());
        when(changeRequestRepository.save(any(ChangeRequest.class)))
                .thenAnswer(invocation -> {
                    ChangeRequest cr = invocation.getArgument(0);
                    cr.setId(123);
                    cr.setChangeRequestId("CR-2025-01");
                    return cr;
                });

        // Act
        ChangeRequestResponse response = changeRequestService.saveChangeRequestDraft(
                contractId, clientUserId, request, attachments);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Change request saved as draft", response.getMessage());
        assertEquals(123, response.getChangeRequestId());

        ArgumentCaptor<ChangeRequest> captor = ArgumentCaptor.forClass(ChangeRequest.class);
        verify(changeRequestRepository).save(captor.capture());
        ChangeRequest saved = captor.getValue();

        assertEquals("Draft", saved.getStatus());
        assertEquals(contractId, saved.getContractId());
        assertEquals("MSA", saved.getContractType());
    }

    @Test
    @DisplayName("saveChangeRequestDraft - SOW contract hợp lệ → tạo ChangeRequest với status Draft")
    void testSaveChangeRequestDraft_SOWSuccess() {
        // Arrange
        Integer contractId = 1;
        Integer clientUserId = 5;
        CreateChangeRequestRequest request = createChangeRequestRequest();
        List<org.springframework.web.multipart.MultipartFile> attachments = new ArrayList<>();

        SOWContract sow = createSOWContract(contractId, clientUserId);

        when(contractRepository.findByIdAndClientId(contractId, clientUserId))
                .thenReturn(Optional.empty());
        when(sowContractRepository.findByIdAndClientId(contractId, clientUserId))
                .thenReturn(Optional.of(sow));
        when(changeRequestRepository.findAll())
                .thenReturn(new ArrayList<>());
        when(changeRequestRepository.save(any(ChangeRequest.class)))
                .thenAnswer(invocation -> {
                    ChangeRequest cr = invocation.getArgument(0);
                    cr.setId(123);
                    cr.setChangeRequestId("CR-2025-01");
                    return cr;
                });

        // Act
        ChangeRequestResponse response = changeRequestService.saveChangeRequestDraft(
                contractId, clientUserId, request, attachments);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Change request saved as draft", response.getMessage());

        ArgumentCaptor<ChangeRequest> captor = ArgumentCaptor.forClass(ChangeRequest.class);
        verify(changeRequestRepository).save(captor.capture());
        ChangeRequest saved = captor.getValue();

        assertEquals("Draft", saved.getStatus());
        assertEquals(contractId, saved.getSowContractId());
        assertEquals("SOW", saved.getContractType());
    }

    // Helper methods
    private CreateChangeRequestRequest createChangeRequestRequest() {
        CreateChangeRequestRequest request = new CreateChangeRequestRequest();
        request.setTitle("Test Change Request");
        request.setType("Other");
        request.setDescription("Test description");
        request.setReason("Test reason");
        request.setDesiredStartDate(LocalDate.of(2025, 2, 1));
        request.setDesiredEndDate(LocalDate.of(2025, 2, 28));
        request.setExpectedExtraCost(new BigDecimal("100000"));
        return request;
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
}

