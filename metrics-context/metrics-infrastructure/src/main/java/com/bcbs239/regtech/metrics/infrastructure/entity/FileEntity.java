package com.bcbs239.regtech.metrics.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Persistence entity for file metrics. Simple mapping for now.
 */
@Setter
@Getter
@Entity
@Table(name = "metrics_file", schema = "metrics")
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "filename", nullable = false)
    private String filename;

    @Column(name = "date")
    private String date;

    @Column(name = "score")
    private Double score;

    @Column(name = "status")
    private String status;

    @Column(name = "batch_id", length = 255)
    private String batchId;

    @Column(name = "bank_id", length = 255)
    private String bankId;

    public FileEntity() {
    }

    public FileEntity(String filename, String date, Double score, String status, String batchId, String bankId) {
        this.filename = filename;
        this.date = date;
        this.score = score;
        this.status = status;
        this.batchId = batchId;
        this.bankId = bankId;
    }

}
