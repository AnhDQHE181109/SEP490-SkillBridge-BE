package com.skillbridge.service.sales;

import com.skillbridge.entity.contract.*;
import com.skillbridge.repository.contract.*;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SOWBaselineService
 * Tests business logic for baseline (original) contract data
 */
@ExtendWith(MockitoExtension.class)
class SOWBaselineServiceTest {

    @Mock
    private SOWContractRepository sowContractRepository;

    @Mock
    private SOWEngagedEngineerBaseRepository sowEngagedEngineerBaseRepository;

    @Mock
    private RetainerBillingBaseRepository retainerBillingBaseRepository;

    @Mock
    private SOWEngagedEngineerRepository sowEngagedEngineerRepository;

    @Mock
    private RetainerBillingDetailRepository retainerBillingDetailRepository;

    @InjectMocks
    private SOWBaselineService sowBaselineService;

    @BeforeEach
    void setUp() {
        // Setup is handled by MockitoExtension
    }

    @Test
    @DisplayName("createBaseline - contract không tồn tại → throw EntityNotFoundException")
    void testCreateBaseline_ContractNotFound() {
        // Arrange
        Integer sowContractId = 1;

        when(sowContractRepository.findById(sowContractId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> {
            sowBaselineService.createBaseline(sowContractId);
        });

        verify(sowEngagedEngineerBaseRepository, never()).save(any(SOWEngagedEngineerBase.class));
    }

    @Test
    @DisplayName("createBaseline - baseline đã tồn tại → không tạo lại")
    void testCreateBaseline_AlreadyExists() {
        // Arrange
        Integer sowContractId = 1;
        SOWContract contract = createSOWContract(sowContractId, "Retainer");
        SOWEngagedEngineerBase existingBase = new SOWEngagedEngineerBase();
        existingBase.setId(1);
        existingBase.setSowContractId(sowContractId);

        when(sowContractRepository.findById(sowContractId))
                .thenReturn(Optional.of(contract));
        when(sowEngagedEngineerBaseRepository.findBySowContractId(sowContractId))
                .thenReturn(List.of(existingBase));

        // Act
        sowBaselineService.createBaseline(sowContractId);

        // Assert
        verify(sowEngagedEngineerBaseRepository, never()).save(any(SOWEngagedEngineerBase.class));
        verify(retainerBillingBaseRepository, never()).save(any(RetainerBillingBase.class));
    }

    @Test
    @DisplayName("createBaseline - contract không phải Retainer → không tạo baseline")
    void testCreateBaseline_NotRetainer() {
        // Arrange
        Integer sowContractId = 1;
        SOWContract contract = createSOWContract(sowContractId, "Fixed Price");

        when(sowContractRepository.findById(sowContractId))
                .thenReturn(Optional.of(contract));
        when(sowEngagedEngineerBaseRepository.findBySowContractId(sowContractId))
                .thenReturn(new ArrayList<>());

        // Act
        sowBaselineService.createBaseline(sowContractId);

        // Assert
        verify(sowEngagedEngineerBaseRepository, never()).save(any(SOWEngagedEngineerBase.class));
        verify(retainerBillingBaseRepository, never()).save(any(RetainerBillingBase.class));
    }

    @Test
    @DisplayName("createBaseline - Retainer contract hợp lệ → tạo baseline")
    void testCreateBaseline_Retainer_Success() {
        // Arrange
        Integer sowContractId = 1;
        SOWContract contract = createSOWContract(sowContractId, "Retainer");
        SOWEngagedEngineer engineer1 = createEngagedEngineer(1, sowContractId);
        SOWEngagedEngineer engineer2 = createEngagedEngineer(2, sowContractId);
        List<SOWEngagedEngineer> engineers = List.of(engineer1, engineer2);

        RetainerBillingDetail billing1 = createBillingDetail(1, sowContractId, LocalDate.of(2025, 1, 15), BigDecimal.valueOf(10000));
        RetainerBillingDetail billing2 = createBillingDetail(2, sowContractId, LocalDate.of(2025, 2, 15), BigDecimal.valueOf(12000));
        List<RetainerBillingDetail> billingDetails = List.of(billing1, billing2);

        when(sowContractRepository.findById(sowContractId))
                .thenReturn(Optional.of(contract));
        when(sowEngagedEngineerBaseRepository.findBySowContractId(sowContractId))
                .thenReturn(new ArrayList<>());
        when(sowEngagedEngineerRepository.findBySowContractId(sowContractId))
                .thenReturn(engineers);
        when(retainerBillingDetailRepository.findBySowContractIdOrderByPaymentDateDesc(sowContractId))
                .thenReturn(billingDetails);
        when(sowEngagedEngineerBaseRepository.save(any(SOWEngagedEngineerBase.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(retainerBillingBaseRepository.save(any(RetainerBillingBase.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(sowContractRepository.save(any(SOWContract.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        sowBaselineService.createBaseline(sowContractId);

        // Assert
        ArgumentCaptor<SOWEngagedEngineerBase> engineerCaptor = ArgumentCaptor.forClass(SOWEngagedEngineerBase.class);
        verify(sowEngagedEngineerBaseRepository, times(2)).save(engineerCaptor.capture());
        List<SOWEngagedEngineerBase> savedEngineers = engineerCaptor.getAllValues();
        assertEquals(2, savedEngineers.size());
        assertEquals(sowContractId, savedEngineers.get(0).getSowContractId());

        ArgumentCaptor<RetainerBillingBase> billingCaptor = ArgumentCaptor.forClass(RetainerBillingBase.class);
        verify(retainerBillingBaseRepository, times(2)).save(billingCaptor.capture());
        List<RetainerBillingBase> savedBillings = billingCaptor.getAllValues();
        assertEquals(2, savedBillings.size());
        assertEquals(sowContractId, savedBillings.get(0).getSowContractId());

        ArgumentCaptor<SOWContract> contractCaptor = ArgumentCaptor.forClass(SOWContract.class);
        verify(sowContractRepository).save(contractCaptor.capture());
        SOWContract savedContract = contractCaptor.getValue();
        assertEquals(BigDecimal.valueOf(22000), savedContract.getBaseTotalAmount()); // 10000 + 12000
    }

    // Helper methods
    private SOWContract createSOWContract(Integer id, String engagementType) {
        SOWContract contract = new SOWContract();
        contract.setId(id);
        contract.setEngagementType(engagementType);
        contract.setBaseTotalAmount(BigDecimal.ZERO);
        return contract;
    }

    private SOWEngagedEngineer createEngagedEngineer(Integer id, Integer sowContractId) {
        SOWEngagedEngineer engineer = new SOWEngagedEngineer();
        engineer.setId(id);
        engineer.setSowContractId(sowContractId);
        engineer.setEngineerLevel("Senior");
        engineer.setRating(BigDecimal.valueOf(4.5));
        engineer.setSalary(BigDecimal.valueOf(1000));
        engineer.setStartDate(LocalDate.of(2025, 1, 1));
        engineer.setEndDate(LocalDate.of(2025, 12, 31));
        return engineer;
    }

    private RetainerBillingDetail createBillingDetail(Integer id, Integer sowContractId, LocalDate paymentDate, BigDecimal amount) {
        RetainerBillingDetail billing = new RetainerBillingDetail();
        billing.setId(id);
        billing.setSowContractId(sowContractId);
        billing.setPaymentDate(paymentDate);
        billing.setAmount(amount);
        billing.setDeliveryNote("Test delivery note");
        return billing;
    }
}

