package com.bcbs239.regtech.iam.domain.bankprofile;

import com.bcbs239.regtech.iam.domain.bankprofile.valueobject.*;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import lombok.Builder;
import lombok.Value;
import java.time.Instant;

/**
 * BankProfile - Aggregate Root
 * 
 * Represents the bank's institutional profile.
 * Immutable - use builder for updates.
 */
@Value
@Builder(toBuilder = true)
public class BankProfile {
    
    // Identity
    String bankId;
    
    // Required fields
    LegalName legalName;
    AbiCode abiCode;
    LeiCode leiCode;
    GroupType groupType;
    BankType bankType;
    SupervisionCategory supervisionCategory;
    String legalAddress;
    
    // Optional fields (Maybe pattern)
    Maybe<VatNumber> vatNumber;
    Maybe<TaxCode> taxCode;
    Maybe<String> companyRegistry;
    Maybe<EmailAddress> institutionalEmail;
    Maybe<EmailAddress> pec;
    Maybe<PhoneNumber> phone;
    Maybe<WebsiteUrl> website;
    
    // Metadata
    Instant lastModified;
    String lastModifiedBy;
    
    /**
     * Domain behavior: Is this a significant bank under SSM?
     */
    public boolean isSignificant() {
        return supervisionCategory == SupervisionCategory.SIGNIFICANT_SSM;
    }
    
    /**
     * Domain behavior: Is this an independent institution?
     */
    public boolean isIndependent() {
        return groupType == GroupType.INDEPENDENT;
    }
    
    /**
     * Domain method: Update profile with new values
     * Returns a new immutable instance
     */
    public BankProfile update(
            LegalName legalName,
            AbiCode abiCode,
            LeiCode leiCode,
            GroupType groupType,
            BankType bankType,
            SupervisionCategory supervisionCategory,
            String legalAddress,
            Maybe<VatNumber> vatNumber,
            Maybe<TaxCode> taxCode,
            Maybe<String> companyRegistry,
            Maybe<EmailAddress> institutionalEmail,
            Maybe<EmailAddress> pec,
            Maybe<PhoneNumber> phone,
            Maybe<WebsiteUrl> website,
            String modifiedBy) {
        
        return this.toBuilder()
                .legalName(legalName)
                .abiCode(abiCode)
                .leiCode(leiCode)
                .groupType(groupType)
                .bankType(bankType)
                .supervisionCategory(supervisionCategory)
                .legalAddress(legalAddress)
                .vatNumber(vatNumber)
                .taxCode(taxCode)
                .companyRegistry(companyRegistry)
                .institutionalEmail(institutionalEmail)
                .pec(pec)
                .phone(phone)
                .website(website)
                .lastModified(Instant.now())
                .lastModifiedBy(modifiedBy)
                .build();
    }
}
