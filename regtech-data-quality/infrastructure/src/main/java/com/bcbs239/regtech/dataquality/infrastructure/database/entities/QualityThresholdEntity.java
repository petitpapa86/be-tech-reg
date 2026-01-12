package com.bcbs239.regtech.dataquality.infrastructure.database.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "quality_thresholds", schema = "dataquality")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QualityThresholdEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bank_id", nullable = false, length = 100)
    private String bankId;

    @Column(name = "completeness_min_percent", nullable = false)
    private Double completenessMinPercent;

    @Column(name = "accuracy_max_error_percent", nullable = false)
    private Double accuracyMaxErrorPercent;

    @Column(name = "timeliness_days", nullable = false)
    private Integer timelinessDays;

    @Column(name = "consistency_percent", nullable = false)
    private Double consistencyPercent;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
