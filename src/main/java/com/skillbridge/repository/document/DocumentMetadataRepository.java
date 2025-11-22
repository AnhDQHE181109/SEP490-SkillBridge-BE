package com.skillbridge.repository.document;

import com.skillbridge.entity.document.DocumentMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentMetadataRepository extends JpaRepository<DocumentMetadata, Integer> {
    Optional<DocumentMetadata> findByS3Key(String s3Key);

    List<DocumentMetadata> findByEntityIdAndEntityType(Integer entityId, String entityType);
}

