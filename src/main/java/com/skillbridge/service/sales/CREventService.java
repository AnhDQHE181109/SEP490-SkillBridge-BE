package com.skillbridge.service.sales;

import com.skillbridge.entity.contract.*;
import com.skillbridge.repository.contract.*;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CR Event Service
 * Handles business logic for Change Request events (resource and billing)
 * Events represent deltas/changes from approved CRs
 */
@Service
@Transactional
public class CREventService {

    private static final Logger logger = LoggerFactory.getLogger(CREventService.class);

    @Autowired
    private CRResourceEventRepository crResourceEventRepository;
    
    @Autowired
    private CRBillingEventRepository crBillingEventRepository;
    
    @Autowired
    private ChangeRequestRepository changeRequestRepository;
    
    @Autowired
    private SOWEngagedEngineerBaseRepository sowEngagedEngineerBaseRepository;
    
    @Autowired
    private RetainerBillingBaseRepository retainerBillingBaseRepository;

    /**
     * Create resource event from Change Request
     * @param cr Change Request
     * @param action Action type (ADD, REMOVE, MODIFY)
     * @param engineerId Engineer ID (for MODIFY/REMOVE)
     * @param role Role
     * @param level Level
     * @param ratingOld Old rating (for MODIFY)
     * @param ratingNew New rating
     * @param unitRateOld Old unit rate (for MODIFY)
     * @param unitRateNew New unit rate
     * @param startDateOld Old start date (for MODIFY)
     * @param startDateNew New start date
     * @param endDateOld Old end date (for MODIFY)
     * @param endDateNew New end date
     * @param effectiveStart Effective start date
     * @return Created resource event
     */
    public CRResourceEvent createResourceEvent(
            ChangeRequest cr,
            CRResourceEvent.ResourceAction action,
            Integer engineerId,
            String role,
            String level,
            BigDecimal ratingOld,
            BigDecimal ratingNew,
            BigDecimal unitRateOld,
            BigDecimal unitRateNew,
            LocalDate startDateOld,
            LocalDate startDateNew,
            LocalDate endDateOld,
            LocalDate endDateNew,
            LocalDate effectiveStart) {

        CRResourceEvent event = new CRResourceEvent();
        event.setChangeRequestId(cr.getId());
        event.setAction(action);
        event.setEngineerId(engineerId);
        event.setRole(role);
        event.setLevel(level);
        event.setRatingOld(ratingOld);
        event.setRatingNew(ratingNew);
        event.setUnitRateOld(unitRateOld);
        event.setUnitRateNew(unitRateNew);
        event.setStartDateOld(startDateOld);
        event.setStartDateNew(startDateNew);
        event.setEndDateOld(endDateOld);
        event.setEndDateNew(endDateNew);
        event.setEffectiveStart(effectiveStart);

        return crResourceEventRepository.save(event);
    }
    
    /**
     * Create billing event from Change Request
     * @param cr Change Request
     * @param billingMonth Billing month
     * @param deltaAmount Delta amount (positive or negative)
     * @param description Description
     * @param type Event type
     * @return Created billing event
     */
    public CRBillingEvent createBillingEvent(
            ChangeRequest cr,
            LocalDate billingMonth,
            BigDecimal deltaAmount,
            String description,
            CRBillingEvent.BillingEventType type) {
        
        CRBillingEvent event = new CRBillingEvent();
        event.setChangeRequestId(cr.getId());
        event.setBillingMonth(billingMonth);
        event.setDeltaAmount(deltaAmount);
        event.setDescription(description);
        event.setType(type);
        
        return crBillingEventRepository.save(event);
    }
    
    /**
     * Get resource events for a SOW contract up to a specific date
     * Only returns events from approved CRs
     * @param sowContractId SOW contract ID
     * @param asOfDate Date to check up to
     * @return List of resource events
     */
    public List<CRResourceEvent> getResourceEvents(Integer sowContractId, LocalDate asOfDate) {
        return crResourceEventRepository.findApprovedEventsUpToDate(sowContractId, asOfDate);
    }
    
    /**
     * Get billing events for a specific month
     * Only returns events from approved CRs
     * @param sowContractId SOW contract ID
     * @param month Billing month
     * @return List of billing events for the month
     */
    public List<CRBillingEvent> getBillingEvents(Integer sowContractId, LocalDate month) {
        return crBillingEventRepository.findApprovedEventsByMonth(sowContractId, month);
    }

    /**
     * Calculate current resources at a specific date
     * Current = Baseline + All approved events up to date
     * @param sowContractId SOW contract ID
     * @param asOfDate Date to calculate for
     * @return List of current engineers (simulated from baseline + events)
     */
    public List<CurrentEngineerState> calculateCurrentResources(Integer sowContractId, LocalDate asOfDate) {
        // Get baseline engineers active at the date
        List<SOWEngagedEngineerBase> baselineEngineers = sowEngagedEngineerBaseRepository
                .findActiveAtDate(sowContractId, asOfDate);

        // Get all approved resource events up to the date
        List<CRResourceEvent> events = getResourceEvents(sowContractId, asOfDate);

        // Start with baseline engineers
        List<CurrentEngineerState> currentState = new ArrayList<>();
        for (SOWEngagedEngineerBase base : baselineEngineers) {
            CurrentEngineerState state = new CurrentEngineerState();
            state.setEngineerId(base.getId());
            state.setRole(base.getRole());
            state.setLevel(base.getLevel());
            state.setRating(base.getRating());
            state.setUnitRate(base.getUnitRate());
            state.setStartDate(base.getStartDate());
            state.setEndDate(base.getEndDate());
            currentState.add(state);
        }

        // Apply events in chronological order
        for (CRResourceEvent event : events) {
            if (event.getEffectiveStart().isAfter(asOfDate)) {
                continue; // Skip future events
            }

            switch (event.getAction()) {
                case ADD:
                    // Add new engineer
                    CurrentEngineerState newEngineer = new CurrentEngineerState();
                    newEngineer.setEngineerId(null); // New engineer, no base ID
                    newEngineer.setRole(event.getRole());
                    newEngineer.setLevel(event.getLevel());
                    newEngineer.setRating(event.getRatingNew());
                    newEngineer.setUnitRate(event.getUnitRateNew());
                    newEngineer.setStartDate(event.getStartDateNew());
                    newEngineer.setEndDate(event.getEndDateNew());
                    currentState.add(newEngineer);
                    break;

                case REMOVE:
                    // Remove engineer (set end date)
                    if (event.getEngineerId() != null) {
                        currentState.stream()
                                .filter(e -> e.getEngineerId() != null && e.getEngineerId().equals(event.getEngineerId()))
                                .forEach(e -> e.setEndDate(event.getEndDateNew()));
                    }
                    break;

                case MODIFY:
                    // Modify existing engineer
                    if (event.getEngineerId() != null) {
                        currentState.stream()
                                .filter(e -> e.getEngineerId() != null && e.getEngineerId().equals(event.getEngineerId()))
                                .forEach(e -> {
                                    if (event.getRatingNew() != null) e.setRating(event.getRatingNew());
                                    if (event.getUnitRateNew() != null) e.setUnitRate(event.getUnitRateNew());
                                    if (event.getStartDateNew() != null) e.setStartDate(event.getStartDateNew());
                                    if (event.getEndDateNew() != null) e.setEndDate(event.getEndDateNew());
                                });
                    }
                    break;
            }
        }

        // Filter out engineers that are not active at asOfDate
        return currentState.stream()
                .filter(e -> e.getStartDate().compareTo(asOfDate) <= 0 &&
                        (e.getEndDate() == null || e.getEndDate().compareTo(asOfDate) >= 0))
                .collect(Collectors.toList());
    }

    /**
     * Calculate current billing for a specific month
     * Current = Baseline + Sum of all approved events for the month
     * @param sowContractId SOW contract ID
     * @param month Billing month
     * @return Total billing amount for the month
     */
    public BigDecimal calculateCurrentBilling(Integer sowContractId, LocalDate month) {
        // Get baseline billing for the month
        BigDecimal baselineAmount = BigDecimal.ZERO;
        var baselineBillingOpt = retainerBillingBaseRepository.findBySowContractIdAndBillingMonth(sowContractId, month);
        if (baselineBillingOpt.isPresent()) {
            baselineAmount = baselineBillingOpt.get().getAmount();
        }
        
        // Get all billing events for the month
        List<CRBillingEvent> events = getBillingEvents(sowContractId, month);
        BigDecimal eventTotal = events.stream()
            .map(CRBillingEvent::getDeltaAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return baselineAmount.add(eventTotal);
    }
}

