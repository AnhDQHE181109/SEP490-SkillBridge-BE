package com.skillbridge.repository.contract;

import com.skillbridge.entity.contract.SOWContract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SOWContractRepository extends JpaRepository<SOWContract, Integer>, JpaSpecificationExecutor<SOWContract> {
    
    /**
     * Find maximum version number for contracts with the same parent version ID
     */
    @Query("SELECT MAX(s.version) FROM SOWContract s WHERE s.parentVersionId = :parentVersionId OR (s.parentVersionId IS NULL AND s.id = :parentVersionId)")
    Integer findMaxVersionByParentVersionId(@Param("parentVersionId") Integer parentVersionId);
    
    /**
     * Find all versions of a contract (by parent version ID or original ID)
     */
    @Query("SELECT s FROM SOWContract s WHERE s.parentVersionId = :parentVersionId OR (s.parentVersionId IS NULL AND s.id = :parentVersionId) ORDER BY s.version ASC")
    List<SOWContract> findAllVersionsByParentVersionId(@Param("parentVersionId") Integer parentVersionId);
    
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

