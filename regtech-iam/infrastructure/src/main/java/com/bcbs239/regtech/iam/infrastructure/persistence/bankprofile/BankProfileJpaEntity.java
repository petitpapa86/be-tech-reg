package com.bcbs239.regtech.iam.infrastructure.persistence.bankprofile;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * JPA Entity for bank_profile table
 * Separate from domain model
 */
@Entity
@Table(name = "bank_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankProfileJpaEntity {
    
    @Id
    @Column(name = "bank_id")
    private Long bankId;
    
    @Column(name = "legal_name", nullable = false)
    private String legalName;
    
    @Column(name = "abi_code", nullable = false, length = 5, unique = true)
    private String abiCode;
    
    @Column(name = "lei_code", nullable = false, length = 20, unique = true)
    private String leiCode;
    
    @Column(name = "group_type", nullable = false)
    private String groupType;
    
    @Column(name = "bank_type", nullable = false)
    private String bankType;
    
    @Column(name = "supervision_category", nullable = false)
    private String supervisionCategory;
    
    @Column(name = "legal_address", nullable = false, columnDefinition = "TEXT")
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
    
    @Column(name = "last_modified", nullable = false)
    private Instant lastModified;
    
    @Column(name = "last_modified_by", nullable = false, length = 100)
    private String lastModifiedBy;
}
