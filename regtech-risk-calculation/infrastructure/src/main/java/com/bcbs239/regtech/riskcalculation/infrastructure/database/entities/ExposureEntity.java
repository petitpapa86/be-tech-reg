package com.bcbs239.regtech.riskcalculation.infrastructure.database.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA Entity for exposure records persistence
 * Maps to the riskcalculation.exposures table
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "exposures", schema = "riskcalculation", indexes = {
    @Index(name = "idx_exposures_batch_id", columnList = "batch_id"),
    @Index(name = "idx_exposures_country_code", columnList = "country_code"),
    @Index(name = "idx_exposures_instrument_type", columnList = "instrument_type"),
    @Index(name = "idx_exposures_product_type", columnList = "product_type")
})
public class ExposureEntity {
    
    @Id
    @Column(name = "exposure_id", length = 100)
    private String exposureId;
    
    @Column(name = "batch_id", nullable = false, length = 100)
    private String batchId;
    
    @Column(name = "instrument_id", nullable = false, length = 100)
    private String instrumentId;
    
    // Counterparty information
    @Column(name = "counterparty_id", nullable = false, length = 100)
    private String counterpartyId;
    
    @Column(name = "counterparty_name", nullable = false, length = 255)
    private String counterpartyName;
    
    @Column(name = "counterparty_lei", length = 20)
    private String counterpartyLei;
    
    // Monetary amounts
    @Column(name = "exposure_amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal exposureAmount;
    
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;
    
    // Classification
    @Column(name = "product_type", nullable = false, length = 100)
    private String productType;
    
    @Column(name = "instrument_type", nullable = false, length = 20)
    private String instrumentType;
    
    @Column(name = "balance_sheet_type", nullable = false, length = 20)
    private String balanceSheetType;
    
    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;
    
    // Metadata
    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
