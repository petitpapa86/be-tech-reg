package com.bcbs239.regtech.iam.presentation.bankprofile;

import java.time.format.DateTimeFormatter;

/**
 * Response DTO for bank profile
 */
public record BankProfileResponse(
    Long bankId,
    String legalName,
    String abiCode,
    String leiCode,
    String groupType,
    String bankType,
    String supervisionCategory,
    String legalAddress,
    String vatNumber,
    String taxCode,
    String companyRegistry,
    String institutionalEmail,
    String pec,
    String phone,
    String website,
    String lastModified,
    String lastModifiedBy
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_INSTANT;
    
    /**
     * Map domain model → DTO
     */
    public static BankProfileResponse from(com.bcbs239.regtech.iam.domain.bankprofile.BankProfile profile) {
        return new BankProfileResponse(
                profile.getBankId(),
                profile.getLegalName().getValue(),
                profile.getAbiCode().getValue(),
                profile.getLeiCode().getValue(),
                profile.getGroupType().name(),
                profile.getBankType().name(),
                profile.getSupervisionCategory().name(),
                profile.getLegalAddress(),
                profile.getVatNumber().map(v -> v.getValue()).orElse(null),
                profile.getTaxCode().map(t -> t.getValue()).orElse(null),
                profile.getCompanyRegistry().orElse(null),
                profile.getInstitutionalEmail().map(e -> e.getValue()).orElse(null),
                profile.getPec().map(p -> p.getValue()).orElse(null),
                profile.getPhone().map(p -> p.getValue()).orElse(null),
                profile.getWebsite().map(w -> w.getValue()).orElse(null),
                FORMATTER.format(profile.getLastModified()),
                profile.getLastModifiedBy()
        );
    }
}
