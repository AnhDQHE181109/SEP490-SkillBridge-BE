package com.skillbridge.repository.contract;

import com.skillbridge.entity.contract.ChangeRequestAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChangeRequestAttachmentRepository extends JpaRepository<ChangeRequestAttachment, Integer> {
    List<ChangeRequestAttachment> findByChangeRequestId(Integer changeRequestId);
    
    void deleteByChangeRequestId(Integer changeRequestId);
}

