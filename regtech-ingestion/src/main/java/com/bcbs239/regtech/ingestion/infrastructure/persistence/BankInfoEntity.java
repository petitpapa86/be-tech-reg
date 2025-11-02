package com.bcbs239.regtech.ingestion.infrastructure.persistence;

import com.bcbs239.regtech.ingestion.domain.model.BankId;
import com.bcbs239.regtech.ingestion.domain.model.BankInfo;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

/**
 * JPA entity mapping to the bank_info table.
 * Caches bank information from Bank Registry service.
 */
@Entity
@Table(name = "bank_info", schema = "regtech")
public class BankInfoEntity {
    
    @Id
    @Column(name = "bank_id", length = 20)
    private String bankId;
    
    @Column(name = "bank_name", length = 100, nullable = false)
    private String bankName;
    
    @Column(name = "bank_country", length = 3, nullable = false)
    private String bankCountry;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "bank_status", length = 20, nullable = false)
    private BankInfo.BankStatus bankStatus;
    
    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    // Default constructor for JPA
    protected BankInfoEntity() {}
    
    // Constructor for creating new entities
    public BankInfoEntity(String bankId, String bankName, String bankCountry, 
                         BankInfo.BankStatus bankStatus, Instant lastUpdated) {
        this.bankId = Objects.requireNonNull(bankId, "Bank ID cannot be null");
        this.bankName = Objects.requireNonNull(bankName, "Bank name cannot be null");
        this.bankCountry = Objects.requireNonNull(bankCountry, "Bank country cannot be null");
        this.bankStatus = Objects.requireNonNull(bankStatus, "Bank status cannot be null");
        this.lastUpdated = Objects.requireNonNull(lastUpdated, "Last updated cannot be null");
        this.createdAt = Instant.now();
    }
    
    /**
     * Convert from domain value object to JPA entity.
     */
    public static BankInfoEntity fromDomain(BankInfo bankInfo) {
        return new BankInfoEntity(
            bankInfo.bankId().value(),
            bankInfo.bankName(),
            bankInfo.bankCountry(),
            bankInfo.bankStatus(),
            bankInfo.lastUpdated()
        );
    }
    
    /**
     * Convert from JPA entity to domain value object.
     */
    public BankInfo toDomain() {
        return new BankInfo(
            new BankId(bankId),
            bankName,
            bankCountry,
            bankStatus,
            lastUpdated
        );
    }
    
    /**
     * Update the entity with fresh bank information.
     */
    public void updateWith(BankInfo bankInfo) {
        this.bankName = bankInfo.bankName();
        this.bankCountry = bankInfo.bankCountry();
        this.bankStatus = bankInfo.bankStatus();
        this.lastUpdated = bankInfo.lastUpdated();
    }
    
    /**
     * Check if the cached bank information is fresh (less than 24 hours old).
     */
    public boolean isFresh() {
        return lastUpdated.isAfter(Instant.now().minusSeconds(24 * 60 * 60));
    }
    
    /**
     * Check if the bank is active and can process uploads.
     */
    public boolean isActive() {
        return bankStatus == BankInfo.BankStatus.ACTIVE;
    }
    
    // Getters and setters
    public String getBankId() { return bankId; }
    public void setBankId(String bankId) { this.bankId = bankId; }
    
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    
    public String getBankCountry() { return bankCountry; }
    public void setBankCountry(String bankCountry) { this.bankCountry = bankCountry; }
    
    public BankInfo.BankStatus getBankStatus() { return bankStatus; }
    public void setBankStatus(BankInfo.BankStatus bankStatus) { this.bankStatus = bankStatus; }
    
    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BankInfoEntity that = (BankInfoEntity) o;
        return Objects.equals(bankId, that.bankId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(bankId);
    }
    
    @Override
    public String toString() {
        return "BankInfoEntity{" +
                "bankId='" + bankId + '\'' +
                ", bankName='" + bankName + '\'' +
                ", bankCountry='" + bankCountry + '\'' +
                ", bankStatus=" + bankStatus +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}