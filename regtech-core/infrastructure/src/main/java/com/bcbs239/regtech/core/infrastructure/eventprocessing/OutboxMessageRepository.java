package com.bcbs239.regtech.core.infrastructure.eventprocessing;

import com.bcbs239.regtech.core.domain.outbox.OutboxMessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxMessageRepository extends JpaRepository<OutboxMessageEntity, String> {

    @Query("SELECT om FROM OutboxMessageEntity om WHERE om.status = :status ORDER BY om.occurredOnUtc ASC")
    List<OutboxMessageEntity> findByStatusOrderByOccurredOnUtc(@Param("status") OutboxMessageStatus status);
}