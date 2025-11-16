package com.skillbridge.repository.contract;

import com.skillbridge.entity.contract.Contract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Integer>, JpaSpecificationExecutor<Contract> {
    
    @Query("SELECT c FROM Contract c WHERE c.clientId = :clientId " +
           "AND (:search IS NULL OR c.contractName LIKE CONCAT('%', :search, '%') OR " +
           "CONCAT(c.id, '') LIKE CONCAT('%', :search, '%') OR c.assigneeId LIKE CONCAT('%', :search, '%')) " +
           "AND (:status IS NULL OR c.status = :status) " +
           "ORDER BY c.createdAt DESC")
    Page<Contract> findByClientIdWithFilters(
        @Param("clientId") Integer clientId,
        @Param("search") String search,
        @Param("status") Contract.ContractStatus status,
        Pageable pageable
    );
    
    @Query("SELECT c FROM Contract c WHERE c.id = :id AND c.clientId = :clientId")
    Optional<Contract> findByIdAndClientId(@Param("id") Integer id, @Param("clientId") Integer clientId);
}

