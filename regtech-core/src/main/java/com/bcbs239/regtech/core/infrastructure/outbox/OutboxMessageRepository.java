package com.bcbs239.regtech.core.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA Repository for outbox messages.
 */
@Repository
public interface OutboxMessageRepository extends JpaRepository<OutboxMessageEntity, String> {

    @Query("SELECT om FROM OutboxMessageEntity om WHERE om.status = :status ORDER BY om.occurredOnUtc ASC")
    List<OutboxMessageEntity> findByStatusOrderByOccurredOnUtc(@Param("status") OutboxMessageStatus status);

    @Query("SELECT COUNT(om) FROM OutboxMessageEntity om WHERE om.status = :status")
    long countByStatus(@Param("status") OutboxMessageStatus status);
}