package com.bcbs239.regtech.core.application;

import com.bcbs239.regtech.core.infrastructure.eventprocessing.InboxMessageEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class InboxMessageOperations {

    @Autowired
    private InboxMessageRepository repository;

    public InboxMessageEntity save(InboxMessageEntity entity) {
        return repository.save(entity);
    }

    public Optional<InboxMessageEntity> findById(String id) {
        return repository.findById(id);
    }

    public List<InboxMessageEntity> findByProcessingStatusOrderByReceivedAt(InboxMessageEntity.ProcessingStatus status) {
        return repository.findByProcessingStatusOrderByReceivedAt(status);
    }
}
