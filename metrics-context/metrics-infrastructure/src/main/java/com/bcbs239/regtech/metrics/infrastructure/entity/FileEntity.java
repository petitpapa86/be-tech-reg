package com.bcbs239.regtech.metrics.infrastructure.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Persistence entity for file metrics. Simple mapping for now.
 */
@Entity
@Table(name = "metrics_file", schema = "metrics")
public class FileEntity {

    @Id
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

    public FileEntity(String filename, String date, Double score, String status) {
        this.filename = filename;
        this.date = date;
        this.score = score;
        this.status = status;
    }

    public FileEntity(String filename, String date, Double score, String status, String batchId, String bankId) {
        this.filename = filename;
        this.date = date;
        this.score = score;
        this.status = status;
        this.batchId = batchId;
        this.bankId = bankId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getBankId() {
        return bankId;
    }

    public void setBankId(String bankId) {
        this.bankId = bankId;
    }
}
