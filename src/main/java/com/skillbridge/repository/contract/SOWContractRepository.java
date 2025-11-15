package com.skillbridge.repository.contract;

import com.skillbridge.entity.contract.SOWContract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SOWContractRepository extends JpaRepository<SOWContract, Integer> {
    
    @Query("SELECT s FROM SOWContract s WHERE s.clientId = :clientId " +
           "AND (:search IS NULL OR s.contractName LIKE CONCAT('%', :search, '%') OR " +
           "CONCAT(s.id, '') LIKE CONCAT('%', :search, '%') OR s.assigneeId LIKE CONCAT('%', :search, '%')) " +
           "AND (:status IS NULL OR s.status = :status) " +
           "AND (:engagementType IS NULL OR s.engagementType = :engagementType) " +
           "ORDER BY s.createdAt DESC")
    Page<SOWContract> findByClientIdWithFilters(
        @Param("clientId") Integer clientId,
        @Param("search") String search,
        @Param("status") SOWContract.SOWContractStatus status,
        @Param("engagementType") String engagementType,
        Pageable pageable
    );
    
    @Query("SELECT s FROM SOWContract s WHERE s.id = :id AND s.clientId = :clientId")
    Optional<SOWContract> findByIdAndClientId(@Param("id") Integer id, @Param("clientId") Integer clientId);
}

