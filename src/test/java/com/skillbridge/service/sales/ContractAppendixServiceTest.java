package com.skillbridge.service.sales;

import com.skillbridge.entity.contract.ChangeRequest;
import com.skillbridge.entity.contract.ContractAppendix;
import com.skillbridge.entity.contract.CRBillingEvent;
import com.skillbridge.repository.contract.ContractAppendixRepository;
import com.skillbridge.repository.contract.ChangeRequestRepository;
import com.skillbridge.repository.contract.CRBillingEventRepository;
import com.skillbridge.repository.contract.CRResourceEventRepository;
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
 * Unit tests for ContractAppendixService
 * Tests business logic for contract appendices
 */
@ExtendWith(MockitoExtension.class)
class ContractAppendixServiceTest {

    @Mock
    private ContractAppendixRepository contractAppendixRepository;

    @Mock
    private ChangeRequestRepository changeRequestRepository;

    @Mock
    private CRResourceEventRepository crResourceEventRepository;

    @Mock
    private CRBillingEventRepository crBillingEventRepository;

    @InjectMocks
    private ContractAppendixService contractAppendixService;

    @BeforeEach
    void setUp() {
        // Setup is handled by MockitoExtension
    }

    @Test
    @DisplayName("generateAppendix - Change Request không có sowContractId → throw IllegalArgumentException")
    void testGenerateAppendix_NoSowContractId() {
        // Arrange
        ChangeRequest cr = createChangeRequest(1, null, "RESOURCE_CHANGE", "Approved");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            contractAppendixService.generateAppendix(cr);
        });

        verify(contractAppendixRepository, never()).save(any(ContractAppendix.class));
    }

    @Test
    @DisplayName("generateAppendix - appendix đã tồn tại → trả về appendix hiện có")
    void testGenerateAppendix_AlreadyExists() {
        // Arrange
        Integer sowContractId = 1;
        ChangeRequest cr = createChangeRequest(1, sowContractId, "RESOURCE_CHANGE", "Approved");
        ContractAppendix existing = createAppendix(1, sowContractId, 1, "AP-001");

        when(contractAppendixRepository.findByChangeRequestId(1))
                .thenReturn(Optional.of(existing));

        // Act
        ContractAppendix result = contractAppendixService.generateAppendix(cr);

        // Assert
        assertNotNull(result);
        assertEquals(existing.getId(), result.getId());
        assertEquals("AP-001", result.getAppendixNumber());

        verify(contractAppendixRepository, never()).save(any(ContractAppendix.class));
    }

    @Test
    @DisplayName("generateAppendix - Change Request hợp lệ → tạo appendix mới")
    void testGenerateAppendix_Success() {
        // Arrange
        Integer sowContractId = 1;
        ChangeRequest cr = createChangeRequest(1, sowContractId, "RESOURCE_CHANGE", "Approved");
        cr.setSummary("Test summary");
        cr.setEffectiveFrom(LocalDate.of(2025, 1, 1));

        when(contractAppendixRepository.findByChangeRequestId(1))
                .thenReturn(Optional.empty());
        when(contractAppendixRepository.getNextAppendixNumber(sowContractId))
                .thenReturn(1);
        when(contractAppendixRepository.save(any(ContractAppendix.class)))
                .thenAnswer(invocation -> {
                    ContractAppendix appendix = invocation.getArgument(0);
                    appendix.setId(100);
                    return appendix;
                });
        when(changeRequestRepository.save(any(ChangeRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(crBillingEventRepository.findByChangeRequestId(1))
                .thenReturn(new ArrayList<>());

        // Act
        ContractAppendix result = contractAppendixService.generateAppendix(cr);

        // Assert
        assertNotNull(result);
        assertEquals(100, result.getId());
        assertEquals("AP-001", result.getAppendixNumber());
        assertEquals(sowContractId, result.getSowContractId());
        assertEquals(1, result.getChangeRequestId());
        assertTrue(result.getTitle().contains("AP-001"));
        assertTrue(result.getTitle().contains("Resource Change"));

        ArgumentCaptor<ContractAppendix> appendixCaptor = ArgumentCaptor.forClass(ContractAppendix.class);
        verify(contractAppendixRepository).save(appendixCaptor.capture());
        ContractAppendix saved = appendixCaptor.getValue();
        assertEquals("AP-001", saved.getAppendixNumber());

        ArgumentCaptor<ChangeRequest> crCaptor = ArgumentCaptor.forClass(ChangeRequest.class);
        verify(changeRequestRepository).save(crCaptor.capture());
        ChangeRequest savedCr = crCaptor.getValue();
        assertEquals(100, savedCr.getAppendixId());
    }

    @Test
    @DisplayName("generateAppendix - có billing events → include trong summary")
    void testGenerateAppendix_WithBillingEvents() {
        // Arrange
        Integer sowContractId = 1;
        ChangeRequest cr = createChangeRequest(1, sowContractId, "RESOURCE_CHANGE", "Approved");
        cr.setSummary("Test summary");

        CRBillingEvent event1 = createBillingEvent(1, 1, "2025-01", BigDecimal.valueOf(1000));
        CRBillingEvent event2 = createBillingEvent(2, 1, "2025-02", BigDecimal.valueOf(-500));
        List<CRBillingEvent> billingEvents = List.of(event1, event2);

        when(contractAppendixRepository.findByChangeRequestId(1))
                .thenReturn(Optional.empty());
        when(contractAppendixRepository.getNextAppendixNumber(sowContractId))
                .thenReturn(1);
        when(contractAppendixRepository.save(any(ContractAppendix.class)))
                .thenAnswer(invocation -> {
                    ContractAppendix appendix = invocation.getArgument(0);
                    appendix.setId(100);
                    return appendix;
                });
        when(changeRequestRepository.save(any(ChangeRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(crBillingEventRepository.findByChangeRequestId(1))
                .thenReturn(billingEvents);

        // Act
        ContractAppendix result = contractAppendixService.generateAppendix(cr);

        // Assert
        assertNotNull(result);
        assertTrue(result.getSummary().contains("Billing Changes"));
        assertTrue(result.getSummary().contains("Month 2025-01"));
        assertTrue(result.getSummary().contains("Month 2025-02"));
    }

    @Test
    @DisplayName("getAppendices - có appendices → trả về danh sách")
    void testGetAppendices_Success() {
        // Arrange
        Integer sowContractId = 1;
        ContractAppendix appendix1 = createAppendix(1, sowContractId, 1, "AP-001");
        ContractAppendix appendix2 = createAppendix(2, sowContractId, 2, "AP-002");
        List<ContractAppendix> appendices = List.of(appendix1, appendix2);

        when(contractAppendixRepository.findBySowContractIdOrderByCreatedAtDesc(sowContractId))
                .thenReturn(appendices);

        // Act
        List<ContractAppendix> result = contractAppendixService.getAppendices(sowContractId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("AP-001", result.get(0).getAppendixNumber());
        assertEquals("AP-002", result.get(1).getAppendixNumber());

        verify(contractAppendixRepository).findBySowContractIdOrderByCreatedAtDesc(sowContractId);
    }

    @Test
    @DisplayName("getAppendix - appendix không tồn tại → throw EntityNotFoundException")
    void testGetAppendix_NotFound() {
        // Arrange
        Integer appendixId = 1;

        when(contractAppendixRepository.findById(appendixId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> {
            contractAppendixService.getAppendix(appendixId);
        });
    }

    @Test
    @DisplayName("getAppendix - appendix tồn tại → trả về appendix")
    void testGetAppendix_Success() {
        // Arrange
        Integer appendixId = 1;
        ContractAppendix appendix = createAppendix(appendixId, 1, 1, "AP-001");

        when(contractAppendixRepository.findById(appendixId))
                .thenReturn(Optional.of(appendix));

        // Act
        ContractAppendix result = contractAppendixService.getAppendix(appendixId);

        // Assert
        assertNotNull(result);
        assertEquals(appendixId, result.getId());
        assertEquals("AP-001", result.getAppendixNumber());
    }

    @Test
    @DisplayName("updateAppendixStatus - update signedAt → thành công")
    void testUpdateAppendixStatus_Success() {
        // Arrange
        Integer appendixId = 1;
        LocalDateTime signedAt = LocalDateTime.of(2025, 1, 1, 10, 0);
        ContractAppendix appendix = createAppendix(appendixId, 1, 1, "AP-001");

        when(contractAppendixRepository.findById(appendixId))
                .thenReturn(Optional.of(appendix));
        when(contractAppendixRepository.save(any(ContractAppendix.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        contractAppendixService.updateAppendixStatus(appendixId, signedAt);

        // Assert
        ArgumentCaptor<ContractAppendix> captor = ArgumentCaptor.forClass(ContractAppendix.class);
        verify(contractAppendixRepository).save(captor.capture());
        ContractAppendix saved = captor.getValue();
        assertEquals(signedAt, saved.getSignedAt());
    }

    @Test
    @DisplayName("signAppendix - sign appendix → set signedAt to now")
    void testSignAppendix_Success() {
        // Arrange
        Integer appendixId = 1;
        ContractAppendix appendix = createAppendix(appendixId, 1, 1, "AP-001");

        when(contractAppendixRepository.findById(appendixId))
                .thenReturn(Optional.of(appendix));
        when(contractAppendixRepository.save(any(ContractAppendix.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        contractAppendixService.signAppendix(appendixId);

        // Assert
        ArgumentCaptor<ContractAppendix> captor = ArgumentCaptor.forClass(ContractAppendix.class);
        verify(contractAppendixRepository).save(captor.capture());
        ContractAppendix saved = captor.getValue();
        assertNotNull(saved.getSignedAt());
    }

    // Helper methods
    private ChangeRequest createChangeRequest(Integer id, Integer sowContractId, String type, String status) {
        ChangeRequest cr = new ChangeRequest();
        cr.setId(id);
        cr.setSowContractId(sowContractId);
        cr.setType(type);
        cr.setStatus(status);
        cr.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        return cr;
    }

    private ContractAppendix createAppendix(Integer id, Integer sowContractId, Integer changeRequestId, String appendixNumber) {
        ContractAppendix appendix = new ContractAppendix();
        appendix.setId(id);
        appendix.setSowContractId(sowContractId);
        appendix.setChangeRequestId(changeRequestId);
        appendix.setAppendixNumber(appendixNumber);
        appendix.setTitle("Test Appendix");
        appendix.setSummary("Test Summary");
        appendix.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        return appendix;
    }

    private CRBillingEvent createBillingEvent(Integer id, Integer changeRequestId, String billingMonth, BigDecimal deltaAmount) {
        CRBillingEvent event = new CRBillingEvent();
        event.setId(id);
        event.setChangeRequestId(changeRequestId);
        // Parse billingMonth string (format: "2025-01") to LocalDate
        java.time.YearMonth yearMonth = java.time.YearMonth.parse(billingMonth);
        event.setBillingMonth(yearMonth.atDay(1));
        event.setDeltaAmount(deltaAmount);
        event.setDescription("Test billing event");
        event.setType(CRBillingEvent.BillingEventType.SCOPE_ADJUSTMENT);
        return event;
    }
}

