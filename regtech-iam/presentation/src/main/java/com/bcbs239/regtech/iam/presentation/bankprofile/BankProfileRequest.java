package com.bcbs239.regtech.iam.presentation.bankprofile;

import jakarta.validation.constraints.*;

/**
 * Request DTO for updating bank profile
 * Validation at the boundary
 */
public record BankProfileRequest(
    
    @NotBlank(message = "Bank ID is required")
    String bankId,
    
    @NotBlank(message = "Legal name is required")
    @Size(max = 255, message = "Legal name too long")
    String legalName,
    
    @NotBlank(message = "ABI code is required")
    @Pattern(regexp = "\\d{5}", message = "ABI code must be exactly 5 digits")
    String abiCode,
    
    @NotBlank(message = "LEI code is required")
    @Pattern(regexp = "[A-Z0-9]{20}", message = "LEI code must be exactly 20 alphanumeric characters")
    String leiCode,
    
    @NotBlank(message = "Group type is required")
    String groupType,
    
    @NotBlank(message = "Bank type is required")
    String bankType,
    
    @NotBlank(message = "Supervision category is required")
    String supervisionCategory,
    
    @NotBlank(message = "Legal address is required")
    String legalAddress,
    
    @Pattern(regexp = "IT\\d{11}|^$", message = "Invalid VAT number format")
    String vatNumber,
    
    @Pattern(regexp = "\\d{11}|^$", message = "Invalid tax code format")
    String taxCode,
    
    String companyRegistry,
    
    @Email(message = "Invalid email format")
    String institutionalEmail,
    
    @Email(message = "Invalid PEC format")
    String pec,
    
    String phone,
    
    @Pattern(regexp = "https?://.+|^$", message = "Invalid website URL")
    String website
) {
    /**
     * Map DTO → Command
     */
    public com.bcbs239.regtech.iam.application.bankprofile.UpdateBankProfileHandler.UpdateCommand toCommand(String modifiedBy) {
        return new com.bcbs239.regtech.iam.application.bankprofile.UpdateBankProfileHandler.UpdateCommand(
                bankId,
                legalName,
                abiCode,
                leiCode,
                groupType,
                bankType,
                supervisionCategory,
                legalAddress,
                vatNumber,
                taxCode,
                companyRegistry,
                institutionalEmail,
                pec,
                phone,
                website,
                modifiedBy
        );
    }
}
