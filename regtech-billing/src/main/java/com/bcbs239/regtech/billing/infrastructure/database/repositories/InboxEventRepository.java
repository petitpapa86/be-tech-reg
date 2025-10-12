package com.bcbs239.regtech.billing.infrastructure.database.repositories;

import com.bcbs239.regtech.billing.infrastructure.database.entities.InboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InboxEventRepository extends JpaRepository<InboxEventEntity, String> {
    List<InboxEventEntity> findByEventType(String eventType);
}
