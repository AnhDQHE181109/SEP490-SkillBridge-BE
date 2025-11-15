package com.skillbridge.repository.contract;

import com.skillbridge.entity.contract.ContractHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Contract History Repository
 * Handles database operations for contract history
 */
@Repository
public interface ContractHistoryRepository extends JpaRepository<ContractHistory, Integer> {
    
    /**
     * Find all history entries for an MSA contract, ordered by entry date descending
     * @param contractId MSA Contract ID
     * @return List of contract history entries
     */
    @Query("SELECT ch FROM ContractHistory ch WHERE ch.contractId = :contractId " +
           "ORDER BY ch.entryDate DESC, ch.createdAt DESC")
    List<ContractHistory> findByContractIdOrderByEntryDateDesc(@Param("contractId") Integer contractId);
    
    /**
     * Find all history entries for a SOW contract, ordered by entry date descending
     * @param sowContractId SOW Contract ID
     * @return List of contract history entries
     */
    @Query("SELECT ch FROM ContractHistory ch WHERE ch.sowContractId = :sowContractId " +
           "ORDER BY ch.entryDate DESC, ch.createdAt DESC")
    List<ContractHistory> findBySowContractIdOrderByEntryDateDesc(@Param("sowContractId") Integer sowContractId);
}

