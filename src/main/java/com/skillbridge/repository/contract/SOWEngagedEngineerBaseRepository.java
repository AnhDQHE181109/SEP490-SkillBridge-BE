package com.skillbridge.repository.contract;

import com.skillbridge.entity.contract.SOWEngagedEngineerBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * SOW Engaged Engineer Base Repository
 * Handles database operations for baseline engaged engineers
 */
@Repository
public interface SOWEngagedEngineerBaseRepository extends JpaRepository<SOWEngagedEngineerBase, Integer> {
    
    /**
     * Find all baseline engineers for a SOW contract
     * @param sowContractId SOW contract ID
     * @return List of baseline engineers
     */
    List<SOWEngagedEngineerBase> findBySowContractId(Integer sowContractId);
    
    /**
     * Find all baseline engineers for a SOW contract, ordered by start date ascending
     * @param sowContractId SOW contract ID
     * @return List of baseline engineers ordered by start date
     */
    List<SOWEngagedEngineerBase> findBySowContractIdOrderByStartDateAsc(Integer sowContractId);
    
    /**
     * Find baseline engineers active at a specific date
     * @param sowContractId SOW contract ID
     * @param date Date to check
     * @return List of active engineers at the date
     */
    @Query("SELECT e FROM SOWEngagedEngineerBase e WHERE e.sowContractId = :sowContractId " +
           "AND e.startDate <= :date AND (e.endDate IS NULL OR e.endDate >= :date)")
    List<SOWEngagedEngineerBase> findActiveAtDate(@Param("sowContractId") Integer sowContractId, 
                                                   @Param("date") LocalDate date);
    
    /**
     * Delete all baseline engineers for a SOW contract
     * @param sowContractId SOW contract ID
     */
    void deleteBySowContractId(Integer sowContractId);
}

