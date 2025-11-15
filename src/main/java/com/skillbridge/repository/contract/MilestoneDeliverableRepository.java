package com.skillbridge.repository.contract;

import com.skillbridge.entity.contract.MilestoneDeliverable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Milestone Deliverable Repository
 * Handles database operations for milestone deliverables
 */
@Repository
public interface MilestoneDeliverableRepository extends JpaRepository<MilestoneDeliverable, Integer> {
    
    /**
     * Find all milestone deliverables for a SOW contract, ordered by planned_end
     * @param sowContractId SOW Contract ID
     * @return List of milestone deliverables
     */
    @Query("SELECT md FROM MilestoneDeliverable md WHERE md.sowContractId = :sowContractId ORDER BY md.plannedEnd ASC")
    List<MilestoneDeliverable> findBySowContractIdOrderByPlannedEndAsc(@Param("sowContractId") Integer sowContractId);
}

