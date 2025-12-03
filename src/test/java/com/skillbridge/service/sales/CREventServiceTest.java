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
 * Unit tests for CREventService
 * Tests business logic for Change Request events (resource and billing)
 */
@ExtendWith(MockitoExtension.class)
class CREventServiceTest {

    @Mock
    private CRResourceEventRepository crResourceEventRepository;

    @Mock
    private CRBillingEventRepository crBillingEventRepository;

    @Mock
    private ChangeRequestRepository changeRequestRepository;

    @Mock
    private SOWEngagedEngineerBaseRepository sowEngagedEngineerBaseRepository;

    @Mock
    private RetainerBillingBaseRepository retainerBillingBaseRepository;

    @InjectMocks
    private CREventService crEventService;

    @BeforeEach
    void setUp() {
        // Setup is handled by MockitoExtension
    }

    @Test
    @DisplayName("createResourceEvent - tạo resource event ADD → thành công")
    void testCreateResourceEvent_ADD() {
        // Arrange
        ChangeRequest cr = createChangeRequest(1, 1);
        CRResourceEvent.ResourceAction action = CRResourceEvent.ResourceAction.ADD;
        String role = "Engineer";
        String level = "Senior";
        BigDecimal ratingNew = BigDecimal.valueOf(4.5);
        BigDecimal unitRateNew = BigDecimal.valueOf(1000);
        LocalDate startDateNew = LocalDate.of(2025, 1, 1);
        LocalDate endDateNew = LocalDate.of(2025, 12, 31);
        LocalDate effectiveStart = LocalDate.of(2025, 1, 1);

        when(crResourceEventRepository.save(any(CRResourceEvent.class)))
                .thenAnswer(invocation -> {
                    CRResourceEvent event = invocation.getArgument(0);
                    event.setId(100);
                    return event;
                });

        // Act
        CRResourceEvent result = crEventService.createResourceEvent(
                cr, action, null, role, level,
                null, ratingNew, null, unitRateNew,
                null, startDateNew, null, endDateNew,
                effectiveStart);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<CRResourceEvent> captor = ArgumentCaptor.forClass(CRResourceEvent.class);
        verify(crResourceEventRepository).save(captor.capture());
        CRResourceEvent saved = captor.getValue();
        assertEquals(1, saved.getChangeRequestId());
        assertEquals(CRResourceEvent.ResourceAction.ADD, saved.getAction());
        assertEquals(role, saved.getRole());
        assertEquals(level, saved.getLevel());
        assertEquals(ratingNew, saved.getRatingNew());
        assertEquals(unitRateNew, saved.getUnitRateNew());
        assertEquals(startDateNew, saved.getStartDateNew());
        assertEquals(endDateNew, saved.getEndDateNew());
    }

    @Test
    @DisplayName("createResourceEvent - tạo resource event MODIFY → thành công")
    void testCreateResourceEvent_MODIFY() {
        // Arrange
        ChangeRequest cr = createChangeRequest(1, 1);
        CRResourceEvent.ResourceAction action = CRResourceEvent.ResourceAction.MODIFY;
        Integer engineerId = 10;
        String role = "Engineer";
        String level = "Senior";
        BigDecimal ratingOld = BigDecimal.valueOf(4.0);
        BigDecimal ratingNew = BigDecimal.valueOf(4.5);
        BigDecimal unitRateOld = BigDecimal.valueOf(900);
        BigDecimal unitRateNew = BigDecimal.valueOf(1000);
        LocalDate startDateOld = LocalDate.of(2025, 1, 1);
        LocalDate startDateNew = LocalDate.of(2025, 2, 1);
        LocalDate endDateOld = LocalDate.of(2025, 12, 31);
        LocalDate endDateNew = LocalDate.of(2026, 1, 31);
        LocalDate effectiveStart = LocalDate.of(2025, 2, 1);

        when(crResourceEventRepository.save(any(CRResourceEvent.class)))
                .thenAnswer(invocation -> {
                    CRResourceEvent event = invocation.getArgument(0);
                    event.setId(100);
                    return event;
                });

        // Act
        CRResourceEvent result = crEventService.createResourceEvent(
                cr, action, engineerId, role, level,
                ratingOld, ratingNew, unitRateOld, unitRateNew,
                startDateOld, startDateNew, endDateOld, endDateNew,
                effectiveStart);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<CRResourceEvent> captor = ArgumentCaptor.forClass(CRResourceEvent.class);
        verify(crResourceEventRepository).save(captor.capture());
        CRResourceEvent saved = captor.getValue();
        assertEquals(engineerId, saved.getEngineerId());
        assertEquals(CRResourceEvent.ResourceAction.MODIFY, saved.getAction());
        assertEquals(ratingOld, saved.getRatingOld());
        assertEquals(ratingNew, saved.getRatingNew());
    }

    @Test
    @DisplayName("createBillingEvent - tạo billing event → thành công")
    void testCreateBillingEvent_Success() {
        // Arrange
        ChangeRequest cr = createChangeRequest(1, 1);
        LocalDate billingMonth = LocalDate.of(2025, 1, 1);
        BigDecimal deltaAmount = BigDecimal.valueOf(5000);
        String description = "Additional billing";
        CRBillingEvent.BillingEventType type = CRBillingEvent.BillingEventType.SCOPE_ADJUSTMENT;

        when(crBillingEventRepository.save(any(CRBillingEvent.class)))
                .thenAnswer(invocation -> {
                    CRBillingEvent event = invocation.getArgument(0);
                    event.setId(100);
                    return event;
                });

        // Act
        CRBillingEvent result = crEventService.createBillingEvent(
                cr, billingMonth, deltaAmount, description, type);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<CRBillingEvent> captor = ArgumentCaptor.forClass(CRBillingEvent.class);
        verify(crBillingEventRepository).save(captor.capture());
        CRBillingEvent saved = captor.getValue();
        assertEquals(1, saved.getChangeRequestId());
        assertEquals(billingMonth, saved.getBillingMonth());
        assertEquals(deltaAmount, saved.getDeltaAmount());
        assertEquals(description, saved.getDescription());
        assertEquals(type, saved.getType());
    }

    @Test
    @DisplayName("getResourceEvents - lấy resource events → trả về danh sách")
    void testGetResourceEvents_Success() {
        // Arrange
        Integer sowContractId = 1;
        LocalDate asOfDate = LocalDate.of(2025, 6, 1);
        CRResourceEvent event1 = createResourceEvent(1, 1, CRResourceEvent.ResourceAction.ADD);
        CRResourceEvent event2 = createResourceEvent(2, 1, CRResourceEvent.ResourceAction.MODIFY);
        List<CRResourceEvent> events = List.of(event1, event2);

        when(crResourceEventRepository.findApprovedEventsUpToDate(sowContractId, asOfDate))
                .thenReturn(events);

        // Act
        List<CRResourceEvent> result = crEventService.getResourceEvents(sowContractId, asOfDate);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(crResourceEventRepository).findApprovedEventsUpToDate(sowContractId, asOfDate);
    }

    @Test
    @DisplayName("getBillingEvents - lấy billing events → trả về danh sách")
    void testGetBillingEvents_Success() {
        // Arrange
        Integer sowContractId = 1;
        LocalDate month = LocalDate.of(2025, 1, 1);
        CRBillingEvent event1 = createBillingEvent(1, 1, month, BigDecimal.valueOf(1000));
        CRBillingEvent event2 = createBillingEvent(2, 1, month, BigDecimal.valueOf(-500));
        List<CRBillingEvent> events = List.of(event1, event2);

        when(crBillingEventRepository.findApprovedEventsByMonth(sowContractId, month))
                .thenReturn(events);

        // Act
        List<CRBillingEvent> result = crEventService.getBillingEvents(sowContractId, month);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(crBillingEventRepository).findApprovedEventsByMonth(sowContractId, month);
    }

    @Test
    @DisplayName("calculateCurrentBilling - có baseline và events → tính tổng đúng")
    void testCalculateCurrentBilling_WithBaselineAndEvents() {
        // Arrange
        Integer sowContractId = 1;
        LocalDate month = LocalDate.of(2025, 1, 1);
        BigDecimal baselineAmount = BigDecimal.valueOf(10000);
        CRBillingEvent event1 = createBillingEvent(1, 1, month, BigDecimal.valueOf(1000));
        CRBillingEvent event2 = createBillingEvent(2, 1, month, BigDecimal.valueOf(-500));
        List<CRBillingEvent> events = List.of(event1, event2);

        RetainerBillingBase baseline = new RetainerBillingBase();
        baseline.setAmount(baselineAmount);

        when(retainerBillingBaseRepository.findBySowContractIdAndBillingMonth(sowContractId, month))
                .thenReturn(Optional.of(baseline));
        when(crBillingEventRepository.findApprovedEventsByMonth(sowContractId, month))
                .thenReturn(events);

        // Act
        BigDecimal result = crEventService.calculateCurrentBilling(sowContractId, month);

        // Assert
        assertNotNull(result);
        // baselineAmount (10000) + event1 (1000) + event2 (-500) = 10500
        assertEquals(BigDecimal.valueOf(10500), result);
    }

    @Test
    @DisplayName("calculateCurrentBilling - không có baseline → chỉ tính events")
    void testCalculateCurrentBilling_NoBaseline() {
        // Arrange
        Integer sowContractId = 1;
        LocalDate month = LocalDate.of(2025, 1, 1);
        CRBillingEvent event1 = createBillingEvent(1, 1, month, BigDecimal.valueOf(1000));
        List<CRBillingEvent> events = List.of(event1);

        when(retainerBillingBaseRepository.findBySowContractIdAndBillingMonth(sowContractId, month))
                .thenReturn(Optional.empty());
        when(crBillingEventRepository.findApprovedEventsByMonth(sowContractId, month))
                .thenReturn(events);

        // Act
        BigDecimal result = crEventService.calculateCurrentBilling(sowContractId, month);

        // Assert
        assertNotNull(result);
        // 0 (no baseline) + event1 (1000) = 1000
        assertEquals(BigDecimal.valueOf(1000), result);
    }

    // Helper methods
    private ChangeRequest createChangeRequest(Integer id, Integer sowContractId) {
        ChangeRequest cr = new ChangeRequest();
        cr.setId(id);
        cr.setSowContractId(sowContractId);
        cr.setStatus("Approved");
        cr.setType("RESOURCE_CHANGE");
        return cr;
    }

    private CRResourceEvent createResourceEvent(Integer id, Integer changeRequestId, CRResourceEvent.ResourceAction action) {
        CRResourceEvent event = new CRResourceEvent();
        event.setId(id);
        event.setChangeRequestId(changeRequestId);
        event.setAction(action);
        event.setRole("Engineer");
        event.setLevel("Senior");
        event.setRatingNew(BigDecimal.valueOf(4.5));
        event.setUnitRateNew(BigDecimal.valueOf(1000));
        event.setStartDateNew(LocalDate.of(2025, 1, 1));
        event.setEndDateNew(LocalDate.of(2025, 12, 31));
        event.setEffectiveStart(LocalDate.of(2025, 1, 1));
        return event;
    }

    private CRBillingEvent createBillingEvent(Integer id, Integer changeRequestId, LocalDate billingMonth, BigDecimal deltaAmount) {
        CRBillingEvent event = new CRBillingEvent();
        event.setId(id);
        event.setChangeRequestId(changeRequestId);
        event.setBillingMonth(billingMonth);
        event.setDeltaAmount(deltaAmount);
        event.setDescription("Test billing event");
        event.setType(CRBillingEvent.BillingEventType.SCOPE_ADJUSTMENT);
        return event;
    }
}

