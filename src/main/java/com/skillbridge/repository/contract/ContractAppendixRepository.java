package com.skillbridge.repository.contract;

import com.skillbridge.entity.contract.ContractAppendix;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Contract Appendix Repository
 * Handles database operations for contract appendices
 */
@Repository
public interface ContractAppendixRepository extends JpaRepository<ContractAppendix, Integer> {
    
    /**
     * Find all appendices for a SOW contract, ordered by creation date descending
     * @param sowContractId SOW contract ID
     * @return List of appendices
     */
    List<ContractAppendix> findBySowContractIdOrderByCreatedAtDesc(Integer sowContractId);
    
    /**
     * Find appendix by SOW contract ID and appendix number
     * @param sowContractId SOW contract ID
     * @param appendixNumber Appendix number (PL-001, PL-002, etc.)
     * @return Optional appendix
     */
    Optional<ContractAppendix> findBySowContractIdAndAppendixNumber(Integer sowContractId, String appendixNumber);
    
    /**
     * Find appendix by change request ID
     * @param changeRequestId Change request ID
     * @return Optional appendix
     */
    Optional<ContractAppendix> findByChangeRequestId(Integer changeRequestId);
    
    /**
     * Get next appendix number for a SOW contract
     * @param sowContractId SOW contract ID
     * @return Next appendix number (e.g., 1, 2, 3...)
     * Note: This query extracts the numeric part from appendixNumber (e.g., "PL-001" -> 1)
     */
    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING(a.appendix_number, 4) AS UNSIGNED)), 0) + 1 " +
           "FROM contract_appendices a WHERE a.sow_contract_id = :sowContractId AND a.appendix_number LIKE 'AP-%'", nativeQuery = true)
    Integer getNextAppendixNumber(@Param("sowContractId") Integer sowContractId);
}

