package com.bcbs239.regtech.riskcalculation.infrastructure.database.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA Entity for credit risk mitigation data persistence
 * Maps to the riskcalculation.mitigations table
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mitigations", schema = "riskcalculation", indexes = {
    @Index(name = "idx_mitigations_exposure_id", columnList = "exposure_id"),
    @Index(name = "idx_mitigations_batch_id", columnList = "batch_id")
})
public class MitigationEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "exposure_id", nullable = false, length = 100)
    private String exposureId;
    
    @Column(name = "batch_id", nullable = false, length = 100)
    private String batchId;
    
    @Column(name = "mitigation_type", nullable = false, length = 50)
    private String mitigationType;
    
    @Column(name = "value", nullable = false, precision = 20, scale = 2)
    private BigDecimal value;
    
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
