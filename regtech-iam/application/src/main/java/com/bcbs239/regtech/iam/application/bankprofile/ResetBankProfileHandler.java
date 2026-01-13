package com.bcbs239.regtech.iam.application.bankprofile;

import com.bcbs239.regtech.iam.domain.bankprofile.*;
import com.bcbs239.regtech.iam.domain.bankprofile.valueobject.*;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Command Handler: Reset Bank Profile to Default
 * 
 * Application layer = Coordinator
 * Orchestrates the reset use case
 */
@Service
@RequiredArgsConstructor
public class ResetBankProfileHandler {
    
    private final BankProfileRepository repository;
    private final GetBankProfileHandler getBankProfileHandler;
    
    // Default value objects
    private static final LegalName DEFAULT_LEGAL_NAME = 
        LegalName.of("Banca Italiana SpA").getValueOrThrow();
    
    private static final AbiCode DEFAULT_ABI_CODE = 
        AbiCode.of("12345").getValueOrThrow();
    
    private static final LeiCode DEFAULT_LEI_CODE = 
        LeiCode.of("549300ABCDEFGH123456").getValueOrThrow();
    
    @Transactional
    public BankProfile handle(Long bankId, String modifiedBy) {
        // Load existing profile (if any) to preserve bankId
        Maybe<BankProfile> existingProfile = getBankProfileHandler.handle(bankId);
        
        // Create default profile using correct enum values and Maybe API
        BankProfile defaultProfile = BankProfile.builder()
            .bankId(bankId)
            .legalName(DEFAULT_LEGAL_NAME)
            .abiCode(DEFAULT_ABI_CODE)
            .leiCode(DEFAULT_LEI_CODE)
            .groupType(GroupType.INDEPENDENT)
            .bankType(BankType.COMMERCIAL)
            .supervisionCategory(SupervisionCategory.SIGNIFICANT_SSM)
            .legalAddress("Via Roma 1, 20121 Milano MI, Italia")
            // Optional fields with defaults - value objects return Maybe<T> directly
            .vatNumber(VatNumber.of("IT12345678901"))
            .taxCode(TaxCode.of("12345678901"))
            .companyRegistry(Maybe.some("MI-123456"))
            .institutionalEmail(EmailAddress.of("info@bancaitaliana.it"))
            .pec(EmailAddress.of("pec@bancaitaliana.pec.it"))
            .phone(PhoneNumber.of("+39 02 1234567"))
            .website(WebsiteUrl.of("https://www.bancaitaliana.it"))
            // Metadata
            .lastModified(Instant.now())
            .lastModifiedBy(modifiedBy)
            .build();
        
        return repository.save(defaultProfile);
    }
}
