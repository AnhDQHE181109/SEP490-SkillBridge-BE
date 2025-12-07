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
import java.util.List;

/**
 * SOW Baseline Service
 * Handles business logic for baseline (original) contract data
 * Baseline is immutable snapshot of original contract
 */
@Service
@Transactional
public class SOWBaselineService {
    
    private static final Logger logger = LoggerFactory.getLogger(SOWBaselineService.class);
    
    @Autowired
    private SOWContractRepository sowContractRepository;
    
    @Autowired
    private SOWEngagedEngineerBaseRepository sowEngagedEngineerBaseRepository;
    
    @Autowired
    private RetainerBillingBaseRepository retainerBillingBaseRepository;
    
    @Autowired
    private SOWEngagedEngineerRepository sowEngagedEngineerRepository;
    
    @Autowired
    private RetainerBillingDetailRepository retainerBillingDetailRepository;
    
    /**
     * Create baseline from current contract data
     * This should be called when contract is first approved/activated
     * @param sowContractId SOW contract ID
     */
    public void createBaseline(Integer sowContractId) {
        logger.info("Creating baseline for SOW contract: {}", sowContractId);
        
        SOWContract contract = sowContractRepository.findById(sowContractId)
            .orElseThrow(() -> new EntityNotFoundException("SOW Contract not found: " + sowContractId));
        
        // Check if baseline already exists
        List<SOWEngagedEngineerBase> existingEngineers = sowEngagedEngineerBaseRepository.findBySowContractId(sowContractId);
        if (!existingEngineers.isEmpty()) {
            logger.warn("Baseline already exists for SOW contract: {}", sowContractId);
            return;
        }
        
        // Only create baseline for Retainer contracts
        if (!"Retainer".equals(contract.getEngagementType())) {
            logger.info("Skipping baseline creation for non-Retainer contract: {}", sowContractId);
            return;
        }
        
        // Copy engaged engineers to baseline
        List<SOWEngagedEngineer> engineers = sowEngagedEngineerRepository.findBySowContractId(sowContractId);
        for (SOWEngagedEngineer engineer : engineers) {
            SOWEngagedEngineerBase baseEngineer = new SOWEngagedEngineerBase();
            baseEngineer.setSowContractId(sowContractId);
            baseEngineer.setRole(engineer.getEngineerLevel()); // Map engineerLevel to role
            baseEngineer.setLevel(engineer.getEngineerLevel());
            baseEngineer.setRating(engineer.getRating());
            baseEngineer.setUnitRate(engineer.getSalary());
            baseEngineer.setStartDate(engineer.getStartDate());
            baseEngineer.setEndDate(engineer.getEndDate());
            sowEngagedEngineerBaseRepository.save(baseEngineer);
        }
        
        // Copy billing details to baseline
        List<RetainerBillingDetail> billingDetails = retainerBillingDetailRepository
            .findBySowContractIdOrderByPaymentDateDesc(sowContractId);
        for (RetainerBillingDetail billing : billingDetails) {
            RetainerBillingBase baseBilling = new RetainerBillingBase();
            baseBilling.setSowContractId(sowContractId);
            baseBilling.setBillingMonth(billing.getPaymentDate().withDayOfMonth(1)); // First day of month
            baseBilling.setAmount(billing.getAmount());
            baseBilling.setDescription(billing.getDeliveryNote());
            retainerBillingBaseRepository.save(baseBilling);
        }
        
        // Calculate and set base_total_amount
        BigDecimal totalAmount = billingDetails.stream()
            .map(RetainerBillingDetail::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        contract.setBaseTotalAmount(totalAmount);
        sowContractRepository.save(contract);
        
        logger.info("Baseline created successfully for SOW contract: {}", sowContractId);
    }
    
    /**
     * Get baseline resources (engineers) for a SOW contract
     * @param sowContractId SOW contract ID
     * @return List of baseline engineers
     */
    public List<SOWEngagedEngineerBase> getBaselineResources(Integer sowContractId) {
        return sowEngagedEngineerBaseRepository.findBySowContractIdOrderByStartDateAsc(sowContractId);
    }
    
    /**
     * Get baseline billing for a SOW contract
     * @param sowContractId SOW contract ID
     * @return List of baseline billing
     */
    public List<RetainerBillingBase> getBaselineBilling(Integer sowContractId) {
        return retainerBillingBaseRepository.findBySowContractIdOrderByBillingMonthDesc(sowContractId);
    }
    
    /**
     * Check if baseline is locked (has approved CRs)
     * @param sowContractId SOW contract ID
     * @return true if baseline is locked
     */
    public boolean isBaselineLocked(Integer sowContractId) {
        // Baseline is locked if there are any approved change requests
        // This will be checked in the service that uses this method
        return false; // For now, baseline can be modified before first CR approval
    }
}

