package com.bcbs239.regtech.core.infrastructure.eventprocessing;

import com.bcbs239.regtech.core.domain.eventprocessing.EventProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface EventProcessingFailureJpaRepository extends JpaRepository<EventProcessingFailureEntity, String> {

    @Query("SELECT e FROM EventProcessingFailureEntity e WHERE e.status = :status AND e.nextRetryAt <= :now ORDER BY e.nextRetryAt ASC")
    List<EventProcessingFailureEntity> findEventsReadyForRetry(
        @Param("status") EventProcessingStatus status,
        @Param("now") Instant now);

    @Query("SELECT e FROM EventProcessingFailureEntity e WHERE e.metadata LIKE CONCAT('%\"userId\":\"', :userId, '\"%')")
    List<EventProcessingFailureEntity> findByMetadataUserId(@Param("userId") String userId);

    @Modifying
    @Query("DELETE FROM EventProcessingFailureEntity e WHERE e.status = :status AND e.updatedAt < :cutoffDate")
    int deleteByStatusAndUpdatedAtBefore(
        @Param("status") EventProcessingStatus status,
        @Param("cutoffDate") Instant cutoffDate);
}