package com.bcbs239.regtech.core.infrastructure.eventprocessing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InboxMessageRepository extends JpaRepository<InboxMessageEntity, String> {

    @Query("SELECT im FROM EventProcessingInboxMessageEntity im WHERE im.processingStatus = :status ORDER BY im.receivedAt ASC")
    List<InboxMessageEntity> findByProcessingStatusOrderByReceivedAt(@Param("status") InboxMessageEntity.ProcessingStatus status);

    Optional<InboxMessageEntity> findByEventId(String eventId);
}

