package com.skillbridge.repository.contract;

import com.skillbridge.entity.contract.ContractInternalReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractInternalReviewRepository extends JpaRepository<ContractInternalReview, Integer> {
    
    /**
     * Find latest review for MSA contract
     */
    Optional<ContractInternalReview> findFirstByContractIdAndContractTypeOrderByReviewedAtDesc(
        Integer contractId, String contractType
    );
    
    /**
     * Find latest review for SOW contract
     */
    Optional<ContractInternalReview> findFirstBySowContractIdAndContractTypeOrderByReviewedAtDesc(
        Integer sowContractId, String contractType
    );
    
    /**
     * Find all reviews for MSA contract
     */
    List<ContractInternalReview> findByContractIdAndContractTypeOrderByReviewedAtDesc(
        Integer contractId, String contractType
    );
    
    /**
     * Find all reviews for SOW contract
     */
    List<ContractInternalReview> findBySowContractIdAndContractTypeOrderByReviewedAtDesc(
        Integer sowContractId, String contractType
    );

    /**
     * Delete all reviews for a specific MSA contract
     */
    void deleteByContractIdAndContractType(Integer contractId, String contractType);

    /**
     * Delete all reviews for a specific SOW contract
     */
    void deleteBySowContractIdAndContractType(Integer sowContractId, String contractType);
}

