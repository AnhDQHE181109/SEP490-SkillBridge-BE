package com.skillbridge.repository;

import com.skillbridge.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Certificate Repository
 * Handles database operations for certificates
 */
@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Integer> {

    /**
     * Find all certificates for a specific engineer
     * @param engineerId The engineer ID
     * @return List of certificates
     */
    List<Certificate> findByEngineerId(Integer engineerId);

    /**
     * Find certificates by name (partial match)
     * @param name Certificate name
     * @return List of matching certificates
     */
    List<Certificate> findByNameContainingIgnoreCase(String name);

    /**
     * Find certificates issued by a specific organization
     * @param issuedBy Organization name
     * @return List of certificates
     */
    List<Certificate> findByIssuedBy(String issuedBy);

    /**
     * Count certificates for a specific engineer
     * @param engineerId The engineer ID
     * @return Number of certificates
     */
    Long countByEngineerId(Integer engineerId);
}

