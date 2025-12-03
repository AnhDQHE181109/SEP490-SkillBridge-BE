package com.skillbridge.service.sales;

import com.skillbridge.dto.sales.request.CreateSOWRequest;
import com.skillbridge.dto.sales.response.SOWContractDTO;
import com.skillbridge.dto.sales.response.SOWContractDetailDTO;
import com.skillbridge.entity.auth.User;
import com.skillbridge.entity.contract.Contract;
import com.skillbridge.entity.contract.SOWContract;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.repository.contract.*;
import com.skillbridge.repository.document.DocumentMetadataRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SalesSOWContractService
 * Tests business logic for SOW contract operations
 */
@ExtendWith(MockitoExtension.class)
class SalesSOWContractServiceTest {

    @Mock
    private SOWContractRepository sowContractRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private DeliveryItemRepository deliveryItemRepository;

    @Mock
    private MilestoneDeliverableRepository milestoneDeliverableRepository;

    @Mock
    private RetainerBillingDetailRepository retainerBillingDetailRepository;

    @Mock
    private FixedPriceBillingDetailRepository fixedPriceBillingDetailRepository;

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
    private SOWEngagedEngineerRepository sowEngagedEngineerRepository;

    @Mock
    private ChangeRequestBillingDetailRepository changeRequestBillingDetailRepository;

    @Mock
    private ChangeRequestAttachmentRepository changeRequestAttachmentRepository;

    @Mock
    private ChangeRequestHistoryRepository changeRequestHistoryRepository;

    @Mock
    private SOWBaselineService sowBaselineService;

    @Mock
    private CREventService crEventService;

    @Mock
    private ContractAppendixService contractAppendixService;

    @Mock
    private CRBillingEventRepository crBillingEventRepository;

    @InjectMocks
    private SalesSOWContractService salesSOWContractService;

    @BeforeEach
    void setUp() {
        // Setup is handled by MockitoExtension
    }

    @Test
    @DisplayName("createSOWContract - parent MSA không tồn tại → throw RuntimeException")
    void testCreateSOWContract_ParentMSANotFound() {
        // Arrange
        CreateSOWRequest request = new CreateSOWRequest();
        request.setMsaId("MSA-2025-01");
        request.setEngagementType("Fixed Price");
        request.setEffectiveStart("2025-01-01");
        request.setEffectiveEnd("2025-12-31");
        request.setStatus("Draft");
        MultipartFile[] attachments = null;
        User currentUser = createUser(1, "Sales Manager", "manager@example.com", "SALES_MANAGER");

        // Mock findMSAByContractId to return null
        when(contractRepository.findAll()).thenReturn(new ArrayList<>());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            salesSOWContractService.createSOWContract(request, attachments, currentUser);
        });

        verify(sowContractRepository, never()).save(any(SOWContract.class));
    }

    @Test
    @DisplayName("createSOWContract - parent MSA không Active → throw RuntimeException")
    void testCreateSOWContract_ParentMSANotActive() {
        // Arrange
        CreateSOWRequest request = new CreateSOWRequest();
        request.setMsaId("MSA-2025-01");
        request.setEngagementType("Fixed Price");
        request.setEffectiveStart("2025-01-01");
        request.setEffectiveEnd("2025-12-31");
        request.setStatus("Draft");
        MultipartFile[] attachments = null;
        User currentUser = createUser(1, "Sales Manager", "manager@example.com", "SALES_MANAGER");
        Contract parentMSA = createMSAContract(1, Contract.ContractStatus.Draft); // Not Active

        when(contractRepository.findAll()).thenReturn(List.of(parentMSA));
        when(contractInternalReviewRepository.findFirstByContractIdAndContractTypeOrderByReviewedAtDesc(1, "MSA"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            salesSOWContractService.createSOWContract(request, attachments, currentUser);
        });

        verify(sowContractRepository, never()).save(any(SOWContract.class));
    }

    @Test
    @DisplayName("createSOWContract - hợp lệ → tạo SOW contract thành công")
    void testCreateSOWContract_Success() {
        // Arrange
        CreateSOWRequest request = new CreateSOWRequest();
        request.setMsaId("MSA-2025-01");
        request.setEngagementType("Fixed Price");
        request.setEffectiveStart("2025-01-01");
        request.setEffectiveEnd("2025-12-31");
        request.setStatus("Draft");
        request.setContractValue(100000.0);
        MultipartFile[] attachments = null;
        User currentUser = createUser(1, "Sales Manager", "manager@example.com", "SALES_MANAGER");
        Contract parentMSA = createMSAContract(1, Contract.ContractStatus.Active);

        when(contractRepository.findAll()).thenReturn(List.of(parentMSA));
        when(sowContractRepository.save(any(SOWContract.class)))
                .thenAnswer(invocation -> {
                    SOWContract contract = invocation.getArgument(0);
                    contract.setId(100);
                    return contract;
                });
        when(contractHistoryRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryItemRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(milestoneDeliverableRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(fixedPriceBillingDetailRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        SOWContractDTO result = salesSOWContractService.createSOWContract(request, attachments, currentUser);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<SOWContract> captor = ArgumentCaptor.forClass(SOWContract.class);
        verify(sowContractRepository).save(captor.capture());
        SOWContract saved = captor.getValue();
        assertEquals(1, saved.getParentMsaId()); // Parent MSA ID
        assertEquals("Fixed Price", saved.getEngagementType());
        assertEquals(LocalDate.of(2025, 1, 1), saved.getPeriodStart());
        assertEquals(LocalDate.of(2025, 12, 31), saved.getPeriodEnd());
    }

    @Test
    @DisplayName("getSOWContractDetail - contract không tồn tại → throw RuntimeException")
    void testGetSOWContractDetail_NotFound() {
        // Arrange
        String contractId = "1";
        User currentUser = createUser(1, "Sales Manager", "manager@example.com", "SALES_MANAGER");

        when(sowContractRepository.findById(1))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            salesSOWContractService.getSOWContractDetail(Integer.parseInt(contractId), currentUser);
        });
    }

    @Test
    @DisplayName("getSOWContractDetail - Sales Rep không được assign → throw RuntimeException")
    void testGetSOWContractDetail_SalesRep_NotAssigned() {
        // Arrange
        String contractId = "1";
        User currentUser = createUser(1, "Sales Rep", "salesrep@example.com", "SALES_REP");
        SOWContract contract = createSOWContract(1, SOWContract.SOWContractStatus.Active, 2); // Assigned to user 2

        when(sowContractRepository.findById(1))
                .thenReturn(Optional.of(contract));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            salesSOWContractService.getSOWContractDetail(Integer.parseInt(contractId), currentUser);
        });
    }

    @Test
    @DisplayName("getSOWContractDetail - Sales Manager → trả về contract detail")
    void testGetSOWContractDetail_SalesManager_Success() {
        // Arrange
        String contractId = "1";
        User currentUser = createUser(1, "Sales Manager", "manager@example.com", "SALES_MANAGER");
        SOWContract contract = createSOWContract(1, SOWContract.SOWContractStatus.Active, 2);
        User client = createUser(10, "Client", "client@example.com", "CLIENT");
        Contract parentMSA = createMSAContract(1, Contract.ContractStatus.Active);

        when(sowContractRepository.findById(1))
                .thenReturn(Optional.of(contract));
        when(userRepository.findById(10))
                .thenReturn(Optional.of(client));
        when(contractRepository.findById(1))
                .thenReturn(Optional.of(parentMSA));
        when(changeRequestRepository.findAll())
                .thenReturn(new ArrayList<>());
        when(contractHistoryRepository.findAll())
                .thenReturn(new ArrayList<>());
        when(deliveryItemRepository.findAll())
                .thenReturn(new ArrayList<>());
        when(milestoneDeliverableRepository.findAll())
                .thenReturn(new ArrayList<>());
        when(retainerBillingDetailRepository.findAll())
                .thenReturn(new ArrayList<>());
        when(fixedPriceBillingDetailRepository.findAll())
                .thenReturn(new ArrayList<>());
        when(sowEngagedEngineerRepository.findBySowContractId(1))
                .thenReturn(new ArrayList<>());

        // Act
        SOWContractDetailDTO result = salesSOWContractService.getSOWContractDetail(Integer.parseInt(contractId), currentUser);

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

    private Contract createMSAContract(Integer id, Contract.ContractStatus status) {
        Contract contract = new Contract();
        contract.setId(id);
        contract.setStatus(status);
        contract.setClientId(10);
        contract.setContractName("Test MSA Contract");
        contract.setPeriodStart(LocalDate.of(2025, 1, 1));
        contract.setPeriodEnd(LocalDate.of(2025, 12, 31));
        contract.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        return contract;
    }

    private SOWContract createSOWContract(Integer id, SOWContract.SOWContractStatus status, Integer assigneeUserId) {
        SOWContract contract = new SOWContract();
        contract.setId(id);
        contract.setStatus(status);
        contract.setAssigneeUserId(assigneeUserId);
        contract.setClientId(10);
        contract.setParentMsaId(1); // Parent MSA ID
        contract.setContractName("Test SOW Contract");
        contract.setEngagementType("Fixed Price");
        contract.setPeriodStart(LocalDate.of(2025, 1, 1));
        contract.setPeriodEnd(LocalDate.of(2025, 12, 31));
        contract.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        return contract;
    }
}

