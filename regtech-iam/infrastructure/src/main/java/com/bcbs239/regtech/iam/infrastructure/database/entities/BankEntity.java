package com.bcbs239.regtech.iam.infrastructure.database.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Objects;

/**
 * JPA entity representing a bank in the database.
 */
@Setter
@Getter
@Entity
@Table(name = "banks", schema = "iam")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;
    
    @Column(name = "name", nullable = false, unique = true, length = 255)
    private String name;
    
    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;
    
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    // Fields merged from BankProfileJpaEntity
    @Column(name = "legal_name")
    private String legalName;
    
    @Column(name = "abi_code", length = 5, unique = true)
    private String abiCode;
    
    @Column(name = "lei_code", length = 20, unique = true)
    private String leiCode;
    
    @Column(name = "group_type")
    private String groupType;
    
    @Column(name = "bank_type")
    private String bankType;
    
    @Column(name = "supervision_category")
    private String supervisionCategory;
    
    @Column(name = "legal_address", columnDefinition = "TEXT")
    private String legalAddress;
    
    @Column(name = "vat_number", length = 13)
    private String vatNumber;
    
    @Column(name = "tax_code", length = 11)
    private String taxCode;
    
    @Column(name = "company_registry", length = 100)
    private String companyRegistry;
    
    @Column(name = "institutional_email")
    private String institutionalEmail;
    
    @Column(name = "pec")
    private String pec;
    
    @Column(name = "phone", length = 50)
    private String phone;
    
    @Column(name = "website")
    private String website;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BankEntity that = (BankEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
    
    @Override
    public String toString() {
        return "BankEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
