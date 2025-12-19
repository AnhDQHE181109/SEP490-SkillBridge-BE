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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Autowired
    private SOWEngagedEngineerRepository sowEngagedEngineerRepository;
    
    @Autowired
    private ChangeRequestEngagedEngineerRepository changeRequestEngagedEngineerRepository;

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
        logger.debug("Getting resource events for SOW contract: {}, asOfDate: {}", sowContractId, asOfDate);
        
        // Debug: Check all events without status filter
        List<CRResourceEvent> allEvents = crResourceEventRepository.findAllEventsBySowContractId(sowContractId);
        logger.debug("Total events found for SOW {} (without status filter): {}", sowContractId, allEvents.size());
        if (!allEvents.isEmpty()) {
            logger.debug("First event changeRequestId: {}, effectiveStart: {}", 
                allEvents.get(0).getChangeRequestId(), allEvents.get(0).getEffectiveStart());
        }
        
        List<CRResourceEvent> approvedEvents = crResourceEventRepository.findApprovedEventsUpToDate(sowContractId, asOfDate);
        logger.debug("Approved events found for SOW {} up to {}: {}", sowContractId, asOfDate, approvedEvents.size());
        
        return approvedEvents;
    }
    
    /**
     * Get all resource events for a SOW contract (without date filter)
     * Only returns events from approved CRs
     * @param sowContractId SOW contract ID
     * @return List of resource events
     */
    public List<CRResourceEvent> getAllResourceEvents(Integer sowContractId) {
        logger.debug("Getting all resource events for SOW contract: {}", sowContractId);
        
        // Debug: Check all events without status filter
        List<CRResourceEvent> allEvents = crResourceEventRepository.findAllEventsBySowContractId(sowContractId);
        logger.debug("Total events found for SOW {} (without status filter): {}", sowContractId, allEvents.size());
        if (!allEvents.isEmpty()) {
            logger.debug("First event changeRequestId: {}, effectiveStart: {}", 
                allEvents.get(0).getChangeRequestId(), allEvents.get(0).getEffectiveStart());
        }
        
        List<CRResourceEvent> approvedEvents = crResourceEventRepository.findApprovedEventsBySowContractId(sowContractId);
        logger.debug("Approved events found for SOW {}: {}", sowContractId, approvedEvents.size());
        
        return approvedEvents;
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

    /**
     * Calculate monthly resource snapshot for a specific month
     * Combines baseline engineers with CR events to show engineers active in that month
     * @param sowContractId SOW contract ID
     * @param yearMonth Year and month in format YYYY-MM
     * @return List of engineers active in that month
     */
    public List<MonthlyEngineerSnapshot> calculateMonthlyResourceSnapshot(Integer sowContractId, String yearMonth) {
        logger.debug("Calculating monthly resource snapshot for SOW: {}, month: {}", sowContractId, yearMonth);
        
        // Parse year-month to get month start and end
        LocalDate monthStart = LocalDate.parse(yearMonth + "-01");
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
        
        // Get baseline engineers that overlap with the month
        List<SOWEngagedEngineerBase> baselineEngineers = sowEngagedEngineerBaseRepository
            .findBySowContractIdOrderByStartDateAsc(sowContractId);
        
        // Build map of engineers by baseline ID for tracking
        Map<Integer, MonthlyEngineerSnapshot> engineerMap = new HashMap<>();
        
        // Start with baseline engineers that overlap the month
        if (!baselineEngineers.isEmpty()) {
        for (SOWEngagedEngineerBase base : baselineEngineers) {
            if (overlapsMonth(base.getStartDate(), base.getEndDate(), monthStart, monthEnd)) {
                MonthlyEngineerSnapshot snapshot = new MonthlyEngineerSnapshot();
                snapshot.setEngineerId(base.getId());
                snapshot.setEngineerLevel(base.getLevel() != null ? base.getLevel() : base.getRole());
                snapshot.setStartDate(base.getStartDate());
                snapshot.setEndDate(base.getEndDate());
                snapshot.setBillingType("Monthly"); // Default for baseline
                snapshot.setRating(base.getRating() != null ? base.getRating() : BigDecimal.valueOf(100));
                snapshot.setSalary(base.getUnitRate() != null ? base.getUnitRate() : BigDecimal.ZERO);
                snapshot.setHourlyRate(null);
                snapshot.setHours(null);
                snapshot.setSubtotal(null);
                engineerMap.put(base.getId(), snapshot);
            }
        }
        } else {
            // Fallback: use legacy engineers if no baseline exists (e.g. new contract in review)
            List<SOWEngagedEngineer> legacyEngineers = sowEngagedEngineerRepository
                .findBySowContractIdOrderByStartDateAsc(sowContractId);
            for (SOWEngagedEngineer legacy : legacyEngineers) {
                if (overlapsMonth(legacy.getStartDate(), legacy.getEndDate(), monthStart, monthEnd)) {
                    MonthlyEngineerSnapshot snapshot = new MonthlyEngineerSnapshot();
                    snapshot.setEngineerId(legacy.getId());
                    snapshot.setEngineerLevel(legacy.getEngineerLevel());
                    snapshot.setStartDate(legacy.getStartDate());
                    snapshot.setEndDate(legacy.getEndDate());
                    snapshot.setBillingType(legacy.getBillingType() != null ? legacy.getBillingType() : "Monthly");
                    snapshot.setRating(legacy.getRating() != null ? legacy.getRating() : BigDecimal.valueOf(100));
                    snapshot.setSalary(legacy.getSalary() != null ? legacy.getSalary() : BigDecimal.ZERO);
                    snapshot.setHourlyRate(legacy.getHourlyRate());
                    snapshot.setHours(legacy.getHours());
                    snapshot.setSubtotal(legacy.getSubtotal());
                    engineerMap.put(legacy.getId(), snapshot);
                }
            }
        }
        
        // Get all approved resource events
        List<CRResourceEvent> allEvents = getAllResourceEvents(sowContractId);
        
        // Apply events that affect this month, sorted by effectiveStart (latest wins)
        List<CRResourceEvent> monthEvents = allEvents.stream()
            .filter(e -> {
                // Check if event's effective range overlaps with the month
                LocalDate eventStart = e.getEffectiveStart();
                LocalDate eventEnd = e.getEndDateNew() != null ? e.getEndDateNew() : 
                                    (e.getStartDateNew() != null ? e.getStartDateNew().plusYears(10) : LocalDate.now().plusYears(10));
                return overlapsMonth(eventStart, eventEnd, monthStart, monthEnd);
            })
            .sorted((e1, e2) -> {
                // Sort by effectiveStart descending (latest first)
                int dateCompare = e2.getEffectiveStart().compareTo(e1.getEffectiveStart());
                if (dateCompare != 0) return dateCompare;
                // Then by createdAt descending
                return e2.getCreatedAt().compareTo(e1.getCreatedAt());
            })
            .collect(Collectors.toList());
        
        // Apply events (latest wins for same engineer)
        for (CRResourceEvent event : monthEvents) {
            switch (event.getAction()) {
                case ADD:
                    // Add new engineer (only if not already in map from baseline)
                    if (event.getEngineerId() == null || !engineerMap.containsKey(event.getEngineerId())) {
                        MonthlyEngineerSnapshot snapshot = new MonthlyEngineerSnapshot();
                        snapshot.setEngineerId(event.getEngineerId());
                        snapshot.setEngineerLevel(event.getLevel() != null ? event.getLevel() : event.getRole());
                        snapshot.setStartDate(event.getStartDateNew() != null ? event.getStartDateNew() : monthStart);
                        snapshot.setEndDate(event.getEndDateNew() != null ? event.getEndDateNew() : monthEnd);
                        snapshot.setRating(event.getRatingNew() != null ? event.getRatingNew() : BigDecimal.valueOf(100));
                        snapshot.setSalary(event.getUnitRateNew() != null ? event.getUnitRateNew() : BigDecimal.ZERO);
                        
                        // Get billing type and hourly fields from ChangeRequestEngagedEngineer
                        ChangeRequestEngagedEngineer crEngineer = findMatchingCREngineer(event);
                        if (crEngineer != null) {
                            snapshot.setBillingType(crEngineer.getBillingType() != null ? crEngineer.getBillingType() : "Monthly");
                            snapshot.setHourlyRate(crEngineer.getHourlyRate());
                            snapshot.setHours(crEngineer.getHours());
                            snapshot.setSubtotal(crEngineer.getSubtotal());
                            // For hourly billing, salary should be subtotal
                            if ("Hourly".equalsIgnoreCase(crEngineer.getBillingType()) && crEngineer.getSubtotal() != null) {
                                snapshot.setSalary(crEngineer.getSubtotal());
                            }
                        } else {
                            // Default to Monthly if not found
                            snapshot.setBillingType("Monthly");
                        snapshot.setHourlyRate(null);
                        snapshot.setHours(null);
                        snapshot.setSubtotal(null);
                        }
                        
                        // Use a temporary key for new engineers
                        Integer tempKey = event.getEngineerId() != null ? event.getEngineerId() : 
                                         (event.getId() * -1); // Negative ID for new engineers
                        engineerMap.put(tempKey, snapshot);
                    }
                    break;
                    
                case REMOVE:
                    // Remove engineer from snapshot
                    if (event.getEngineerId() != null) {
                        engineerMap.remove(event.getEngineerId());
                    }
                    break;
                    
                case MODIFY:
                    // Modify existing engineer
                    if (event.getEngineerId() != null && engineerMap.containsKey(event.getEngineerId())) {
                        MonthlyEngineerSnapshot snapshot = engineerMap.get(event.getEngineerId());
                        if (event.getLevel() != null) snapshot.setEngineerLevel(event.getLevel());
                        if (event.getStartDateNew() != null) snapshot.setStartDate(event.getStartDateNew());
                        if (event.getEndDateNew() != null) snapshot.setEndDate(event.getEndDateNew());
                        if (event.getRatingNew() != null) snapshot.setRating(event.getRatingNew());
                        if (event.getUnitRateNew() != null) snapshot.setSalary(event.getUnitRateNew());
                        
                        // Update billing type and hourly fields from ChangeRequestEngagedEngineer
                        ChangeRequestEngagedEngineer crEngineer = findMatchingCREngineer(event);
                        if (crEngineer != null) {
                            snapshot.setBillingType(crEngineer.getBillingType() != null ? crEngineer.getBillingType() : "Monthly");
                            snapshot.setHourlyRate(crEngineer.getHourlyRate());
                            snapshot.setHours(crEngineer.getHours());
                            snapshot.setSubtotal(crEngineer.getSubtotal());
                            // For hourly billing, salary should be subtotal
                            if ("Hourly".equalsIgnoreCase(crEngineer.getBillingType()) && crEngineer.getSubtotal() != null) {
                                snapshot.setSalary(crEngineer.getSubtotal());
                            }
                        }
                    }
                    break;
            }
        }
        
        // Filter to only engineers active during the month and sort
        List<MonthlyEngineerSnapshot> result = engineerMap.values().stream()
            .filter(e -> overlapsMonth(e.getStartDate(), e.getEndDate(), monthStart, monthEnd))
            .sorted((e1, e2) -> {
                int levelCompare = (e1.getEngineerLevel() != null ? e1.getEngineerLevel() : "")
                    .compareTo(e2.getEngineerLevel() != null ? e2.getEngineerLevel() : "");
                if (levelCompare != 0) return levelCompare;
                return e1.getStartDate().compareTo(e2.getStartDate());
            })
            .collect(Collectors.toList());
        
        // Additional safety: if after applying baseline + events the month still has no engineers,
        // fallback one more time to legacy table (defensive for edge cases / data issues)
        if (result.isEmpty()) {
            List<SOWEngagedEngineer> legacyEngineers = sowEngagedEngineerRepository
                .findBySowContractIdOrderByStartDateAsc(sowContractId);

            result = legacyEngineers.stream()
                .filter(legacy -> overlapsMonth(legacy.getStartDate(), legacy.getEndDate(), monthStart, monthEnd))
                .map(legacy -> {
                    MonthlyEngineerSnapshot snapshot = new MonthlyEngineerSnapshot();
                    snapshot.setEngineerId(legacy.getId());
                    snapshot.setEngineerLevel(legacy.getEngineerLevel());
                    snapshot.setStartDate(legacy.getStartDate());
                    snapshot.setEndDate(legacy.getEndDate());
                    snapshot.setBillingType(legacy.getBillingType() != null ? legacy.getBillingType() : "Monthly");
                    snapshot.setRating(legacy.getRating() != null ? legacy.getRating() : BigDecimal.valueOf(100));
                    snapshot.setSalary(legacy.getSalary() != null ? legacy.getSalary() : BigDecimal.ZERO);
                    snapshot.setHourlyRate(legacy.getHourlyRate());
                    snapshot.setHours(legacy.getHours());
                    snapshot.setSubtotal(legacy.getSubtotal());
                    return snapshot;
                })
                .sorted((e1, e2) -> {
                    int levelCompare = (e1.getEngineerLevel() != null ? e1.getEngineerLevel() : "")
                        .compareTo(e2.getEngineerLevel() != null ? e2.getEngineerLevel() : "");
                    if (levelCompare != 0) return levelCompare;
                    return e1.getStartDate().compareTo(e2.getStartDate());
                })
                .collect(Collectors.toList());
        }
        
        logger.debug("Monthly snapshot for SOW {} month {}: {} engineers", sowContractId, yearMonth, result.size());
        return result;
    }
    
    /**
     * Find matching ChangeRequestEngagedEngineer for a CRResourceEvent
     * Matches by changeRequestId, engineerLevel (from level or role), and startDate
     */
    private ChangeRequestEngagedEngineer findMatchingCREngineer(CRResourceEvent event) {
        try {
            List<ChangeRequestEngagedEngineer> crEngineers = changeRequestEngagedEngineerRepository
                .findByChangeRequestId(event.getChangeRequestId());
            
            if (crEngineers.isEmpty()) {
                return null;
            }
            
            // Build engineer level from event (prefer level, fallback to role)
            String eventEngineerLevel = event.getLevel() != null ? event.getLevel() : event.getRole();
            LocalDate eventStartDate = event.getStartDateNew() != null ? event.getStartDateNew() : event.getEffectiveStart();
            
            // Try to find exact match by engineerLevel and startDate
            for (ChangeRequestEngagedEngineer crEngineer : crEngineers) {
                if (eventEngineerLevel != null && eventEngineerLevel.equals(crEngineer.getEngineerLevel())) {
                    // If startDate matches or is close (within 1 day), consider it a match
                    if (eventStartDate != null && crEngineer.getStartDate() != null) {
                        long daysDiff = Math.abs(java.time.temporal.ChronoUnit.DAYS.between(eventStartDate, crEngineer.getStartDate()));
                        if (daysDiff <= 1) {
                            return crEngineer;
                        }
                    } else if (eventStartDate == null && crEngineer.getStartDate() == null) {
                        return crEngineer;
                    }
                }
            }
            
            // If no exact match, return first engineer from the same CR (fallback)
            // This handles cases where engineerLevel format might differ slightly
            return crEngineers.get(0);
        } catch (Exception e) {
            logger.warn("Error finding matching CR engineer for event {}: {}", event.getId(), e.getMessage());
            return null;
        }
    }

    /**
     * Check if a date range overlaps with a month
     */
    private boolean overlapsMonth(LocalDate rangeStart, LocalDate rangeEnd, LocalDate monthStart, LocalDate monthEnd) {
        if (rangeStart == null) return false;
        if (rangeEnd == null) rangeEnd = LocalDate.now().plusYears(100); // Treat null as far future
        
        return !rangeStart.isAfter(monthEnd) && !rangeEnd.isBefore(monthStart);
    }
    
    /**
     * Inner class to represent current engineer state
     */
    public static class CurrentEngineerState {
        private Integer engineerId;
        private String role;
        private String level;
        private BigDecimal rating;
        private BigDecimal unitRate;
        private LocalDate startDate;
        private LocalDate endDate;
        
        // Getters and Setters
        public Integer getEngineerId() { return engineerId; }
        public void setEngineerId(Integer engineerId) { this.engineerId = engineerId; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        public BigDecimal getRating() { return rating; }
        public void setRating(BigDecimal rating) { this.rating = rating; }
        public BigDecimal getUnitRate() { return unitRate; }
        public void setUnitRate(BigDecimal unitRate) { this.unitRate = unitRate; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    }
    
    /**
     * DTO for monthly engineer snapshot
     */
    public static class MonthlyEngineerSnapshot {
        private Integer engineerId;
        private String engineerLevel;
        private LocalDate startDate;
        private LocalDate endDate;
        private String billingType;
        private BigDecimal rating;
        private BigDecimal salary;
        private BigDecimal hourlyRate;
        private BigDecimal hours;
        private BigDecimal subtotal;
        
        // Getters and Setters
        public Integer getEngineerId() { return engineerId; }
        public void setEngineerId(Integer engineerId) { this.engineerId = engineerId; }
        public String getEngineerLevel() { return engineerLevel; }
        public void setEngineerLevel(String engineerLevel) { this.engineerLevel = engineerLevel; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public String getBillingType() { return billingType; }
        public void setBillingType(String billingType) { this.billingType = billingType; }
        public BigDecimal getRating() { return rating; }
        public void setRating(BigDecimal rating) { this.rating = rating; }
        public BigDecimal getSalary() { return salary; }
        public void setSalary(BigDecimal salary) { this.salary = salary; }
        public BigDecimal getHourlyRate() { return hourlyRate; }
        public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }
        public BigDecimal getHours() { return hours; }
        public void setHours(BigDecimal hours) { this.hours = hours; }
        public BigDecimal getSubtotal() { return subtotal; }
        public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    }
}

