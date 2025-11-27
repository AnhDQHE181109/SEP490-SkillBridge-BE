package com.skillbridge.service.sales;

import com.skillbridge.entity.contract.ChangeRequest;
import com.skillbridge.entity.contract.ContractAppendix;
import com.skillbridge.entity.contract.CRResourceEvent;
import com.skillbridge.entity.contract.CRBillingEvent;
import com.skillbridge.repository.contract.ContractAppendixRepository;
import com.skillbridge.repository.contract.ChangeRequestRepository;
import com.skillbridge.repository.contract.CRResourceEventRepository;
import com.skillbridge.repository.contract.CRBillingEventRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Contract Appendix Service
 * Handles business logic for contract appendices (phụ lục)
 * Each approved CR generates one appendix
 */
@Service
@Transactional
public class ContractAppendixService {
    
    private static final Logger logger = LoggerFactory.getLogger(ContractAppendixService.class);
    
    @Autowired
    private ContractAppendixRepository contractAppendixRepository;
    
    @Autowired
    private ChangeRequestRepository changeRequestRepository;
    
    @Autowired
    private com.skillbridge.repository.contract.CRResourceEventRepository crResourceEventRepository;
    
    @Autowired
    private com.skillbridge.repository.contract.CRBillingEventRepository crBillingEventRepository;
    
    /**
     * Generate appendix from an approved Change Request
     * @param cr Change Request (must be approved)
     * @return Generated appendix
     */
    public ContractAppendix generateAppendix(ChangeRequest cr) {
        logger.info("Generating appendix for Change Request: {}", cr.getId());
        
        if (cr.getSowContractId() == null) {
            throw new IllegalArgumentException("Change Request must be for a SOW contract");
        }
        
        // Check if appendix already exists
        Optional<ContractAppendix> existing = contractAppendixRepository.findByChangeRequestId(cr.getId());
        if (existing.isPresent()) {
            logger.warn("Appendix already exists for Change Request: {}", cr.getId());
            return existing.get();
        }
        
        // Get next appendix number
        Integer nextNumber = contractAppendixRepository.getNextAppendixNumber(cr.getSowContractId());
        String appendixNumber = String.format("AP-%03d", nextNumber);
        
        // Generate title
        String title = generateTitle(cr, appendixNumber);
        
        // Generate summary
        String summary = generateSummary(cr);
        
        // Create appendix
        ContractAppendix appendix = new ContractAppendix();
        appendix.setSowContractId(cr.getSowContractId());
        appendix.setChangeRequestId(cr.getId());
        appendix.setAppendixNumber(appendixNumber);
        appendix.setTitle(title);
        appendix.setSummary(summary);
        
        ContractAppendix saved = contractAppendixRepository.save(appendix);
        
        // Link CR to appendix
        cr.setAppendixId(saved.getId());
        changeRequestRepository.save(cr);
        
        logger.info("Appendix generated successfully: {}", appendixNumber);
        return saved;
    }
    
    /**
     * Generate appendix title
     * @param cr Change Request
     * @param appendixNumber Appendix number (AP-001, etc.)
     * @return Generated title
     */
    private String generateTitle(ChangeRequest cr, String appendixNumber) {
        String type = cr.getType();
        String effectiveDate = cr.getEffectiveFrom() != null 
            ? cr.getEffectiveFrom().toString() 
            : "N/A";
        
        // Map CR type to readable English
        String typeDisplay = type;
        if ("RESOURCE_CHANGE".equals(type)) {
            typeDisplay = "Resource Change";
        } else if ("SCHEDULE_CHANGE".equals(type)) {
            typeDisplay = "Schedule Change";
        } else if ("SCOPE_ADJUSTMENT".equals(type)) {
            typeDisplay = "Scope Adjustment";
        }
        
        return String.format("Appendix %s - %s effective from %s", 
            appendixNumber, typeDisplay, effectiveDate);
    }
    
    /**
     * Generate appendix summary
     * @param cr Change Request
     * @return Generated summary
     */
    private String generateSummary(ChangeRequest cr) {
        StringBuilder summary = new StringBuilder();
        
        // Get change summary from CR (if available)
        if (cr.getSummary() != null && !cr.getSummary().trim().isEmpty()) {
            summary.append(cr.getSummary());
            summary.append("\n\n");
        }
        
        // Get billing events
        List<CRBillingEvent> billingEvents = crBillingEventRepository.findByChangeRequestId(cr.getId());
        if (!billingEvents.isEmpty()) {
            summary.append("Billing Changes:\n");
            for (CRBillingEvent event : billingEvents) {
                String sign = event.getDeltaAmount().compareTo(java.math.BigDecimal.ZERO) >= 0 ? "+" : "";
                summary.append(String.format("- Month %s: %s%s\n", 
                    event.getBillingMonth(), sign, event.getDeltaAmount()));
            }
        }
        
        return summary.toString();
    }
    
    /**
     * Get all appendices for a SOW contract
     * @param sowContractId SOW contract ID
     * @return List of appendices
     */
    public List<ContractAppendix> getAppendices(Integer sowContractId) {
        return contractAppendixRepository.findBySowContractIdOrderByCreatedAtDesc(sowContractId);
    }
    
    /**
     * Get appendix by ID
     * @param appendixId Appendix ID
     * @return Appendix
     */
    public ContractAppendix getAppendix(Integer appendixId) {
        return contractAppendixRepository.findById(appendixId)
            .orElseThrow(() -> new EntityNotFoundException("Appendix not found: " + appendixId));
    }
    
    /**
     * Update appendix signing status
     * @param appendixId Appendix ID
     * @param signedAt Signing timestamp (null to clear)
     */
    public void updateAppendixStatus(Integer appendixId, LocalDateTime signedAt) {
        ContractAppendix appendix = getAppendix(appendixId);
        appendix.setSignedAt(signedAt);
        contractAppendixRepository.save(appendix);
        logger.info("Appendix {} signing status updated", appendixId);
    }
    
    /**
     * Mark appendix as signed
     * @param appendixId Appendix ID
     */
    public void signAppendix(Integer appendixId) {
        updateAppendixStatus(appendixId, LocalDateTime.now());
    }
    
    /**
     * Generate PDF for appendix (placeholder - to be implemented)
     * @param appendixId Appendix ID
     * @return PDF file path
     */
    public String generateAppendixPDF(Integer appendixId) {
        // TODO: Implement PDF generation using template engine
        // For now, return null
        logger.info("PDF generation for appendix {} - to be implemented", appendixId);
        return null;
    }
}

