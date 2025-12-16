package com.bcbs239.regtech.core.domain.shared.valueobjects;

import com.bcbs239.regtech.core.domain.shared.dto.BankInfoDTO;

import java.util.Objects;

/**
 * Bank information value object containing identifying details
 * Immutable value object that encapsulates bank metadata
 */
public record BankInfo(
    String bankName,
    String abiCode,
    String leiCode
) {

    private static final int MAX_ABI_CODE_LENGTH = 10;
    private static final int MAX_LEI_CODE_LENGTH = 50;
    
    public BankInfo {
        Objects.requireNonNull(bankName, "Bank name cannot be null");
        Objects.requireNonNull(abiCode, "ABI code cannot be null");
        Objects.requireNonNull(leiCode, "LEI code cannot be null");

        bankName = bankName.trim();
        abiCode = abiCode.trim();
        leiCode = leiCode.trim();
        
        if (bankName.trim().isEmpty()) {
            throw new IllegalArgumentException("Bank name cannot be empty");
        }
        if (abiCode.trim().isEmpty()) {
            throw new IllegalArgumentException("ABI code cannot be empty");
        }
        if (leiCode.trim().isEmpty()) {
            throw new IllegalArgumentException("LEI code cannot be empty");
        }

        // Defensive validation: keep persisted representations within DB constraints.
        // (Some modules store abi_code VARCHAR(10) and lei_code VARCHAR(50).)
        if (abiCode.trim().length() > MAX_ABI_CODE_LENGTH) {
            throw new IllegalArgumentException(
                "ABI code must be at most " + MAX_ABI_CODE_LENGTH + " characters"
            );
        }

        // LEI format validation is handled by data-quality rules (e.g. "Valid LEI Format").
        // Here we only enforce a max length to avoid DB overflow.
        if (leiCode.length() > MAX_LEI_CODE_LENGTH) {
            throw new IllegalArgumentException(
                "LEI code must be at most " + MAX_LEI_CODE_LENGTH + " characters"
            );
        }
    }
    
    public static BankInfo of(String bankName, String abiCode, String leiCode) {
        return new BankInfo(bankName, abiCode, leiCode);
    }
    
    /**
     * Create from DTO.
     * Following DDD: the object knows how to construct itself from external data.
     * 
     * @param dto The BankInfoDTO from inter-module communication
     * @return A new BankInfo value object
     */
    public static BankInfo fromDTO(BankInfoDTO dto) {
        Objects.requireNonNull(dto, "BankInfoDTO cannot be null");
        return of(dto.bankName(), dto.abiCode(), dto.leiCode());
    }
}
