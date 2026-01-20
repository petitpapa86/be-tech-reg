package com.bcbs239.regtech.ingestion.infrastructure.bankinfo.persistence;

import com.bcbs239.regtech.ingestion.domain.bankinfo.BankId;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankInfo;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankInfo.BankStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * JPA entity for bank information.
 * Maps BankInfo domain object to database representation.
 */
@Setter
@Getter
@Entity
@Table(name = "bank_info", schema = "ingestion")
public class BankInfoEntity {

    @Id
    @Column(name = "bank_id", length = 50)
    private String bankId;
    
    @Column(name = "bank_name", length = 100, nullable = false)
    private String bankName;
    
    @Column(name = "bank_country", length = 3, nullable = false)
    private String bankCountry;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "bank_status", length = 20, nullable = false)
    private BankStatus bankStatus;
    
    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;
    
    /**
     * Convert entity to domain object.
     */
    public BankInfo toDomain() {
        return new BankInfo(
            BankId.of(this.bankId).getValueOrThrow(),
            this.bankName,
            this.bankCountry,
            this.bankStatus,
            this.lastUpdated
        );
    }
    
    /**
     * Create entity from domain object.
     */
    public static BankInfoEntity fromDomain(BankInfo bankInfo) {
        BankInfoEntity entity = new BankInfoEntity();
        entity.setBankId(bankInfo.bankId().value());
        entity.setBankName(bankInfo.bankName());
        entity.setBankCountry(bankInfo.bankCountry());
        entity.setBankStatus(bankInfo.bankStatus());
        entity.setLastUpdated(bankInfo.lastUpdated());
        return entity;
    }
}
