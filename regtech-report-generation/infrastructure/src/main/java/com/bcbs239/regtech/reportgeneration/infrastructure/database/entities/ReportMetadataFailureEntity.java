package com.bcbs239.regtech.reportgeneration.infrastructure.database.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity for report metadata failures (fallback table).
 * Used when database insert fails after S3 upload for later reconciliation.
 * Maps to report_metadata_failures table.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "report_metadata_failures")
public class ReportMetadataFailureEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "batch_id", nullable = false, length = 255)
    private String batchId;

    @Column(name = "html_s3_uri", columnDefinition = "TEXT")
    private String htmlS3Uri;

    @Column(name = "xbrl_s3_uri", columnDefinition = "TEXT")
    private String xbrlS3Uri;

    @Column(name = "failed_at", nullable = false)
    private Instant failedAt;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "last_retry_at")
    private Instant lastRetryAt;
}
