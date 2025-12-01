package com.bcbs239.regtech.riskcalculation.infrastructure.database.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * JPA Entity for batch metadata persistence
 * Maps to the riskcalculation.batches table
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "batches", schema = "riskcalculation", indexes = {
    @Index(name = "idx_batches_report_date", columnList = "report_date"),
    @Index(name = "idx_batches_status", columnList = "status"),
    @Index(name = "idx_batches_bank_name", columnList = "bank_name")
})
public class BatchEntity {
    
    @Id
    @Column(name = "batch_id", length = 100)
    private String batchId;
    
    @Column(name = "bank_name", nullable = false, length = 255)
    private String bankName;
    
    @Column(name = "abi_code", nullable = false, length = 10)
    private String abiCode;
    
    @Column(name = "lei_code", nullable = false, length = 20)
    private String leiCode;
    
    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;
    
    @Column(name = "total_exposures", nullable = false)
    private Integer totalExposures;
    
    @Column(name = "status", nullable = false, length = 20)
    private String status;
    
    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;
    
    @Column(name = "processed_at")
    private Instant processedAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
