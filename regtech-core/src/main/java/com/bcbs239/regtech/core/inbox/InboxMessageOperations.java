package com.bcbs239.regtech.core.inbox;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Repository
public class InboxMessageOperations {

    @Autowired
    private InboxMessageRepository repository;

    @Autowired
    private Function<InboxMessageEntity.ProcessingStatus, List<InboxMessageEntity>> findPendingMessagesFn;

    public Function<InboxMessageEntity.ProcessingStatus, List<InboxMessageEntity>> findPendingMessagesFn() {
        return findPendingMessagesFn;
    }

    public InboxMessageEntity save(InboxMessageEntity entity) {
        return repository.save(entity);
    }

    public Optional<InboxMessageEntity> findById(String id) {
        return repository.findById(id);
    }
}