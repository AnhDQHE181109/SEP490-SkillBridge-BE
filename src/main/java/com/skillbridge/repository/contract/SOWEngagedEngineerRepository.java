package com.skillbridge.repository.contract;

import com.skillbridge.entity.contract.SOWEngagedEngineer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * SOW Engaged Engineer Repository
 * Handles database operations for SOW engaged engineers
 */
@Repository
public interface SOWEngagedEngineerRepository extends JpaRepository<SOWEngagedEngineer, Integer> {
    
    /**
     * Find all engaged engineers for a SOW contract
     * @param sowContractId SOW contract ID
     * @return List of engaged engineers
     */
    List<SOWEngagedEngineer> findBySowContractId(Integer sowContractId);
    
    /**
     * Delete all engaged engineers for a SOW contract
     * @param sowContractId SOW contract ID
     */
    void deleteBySowContractId(Integer sowContractId);
}

