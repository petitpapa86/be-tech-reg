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
    
    public BankInfo {
        Objects.requireNonNull(bankName, "Bank name cannot be null");
        Objects.requireNonNull(abiCode, "ABI code cannot be null");
        Objects.requireNonNull(leiCode, "LEI code cannot be null");
        
        if (bankName.trim().isEmpty()) {
            throw new IllegalArgumentException("Bank name cannot be empty");
        }
        if (abiCode.trim().isEmpty()) {
            throw new IllegalArgumentException("ABI code cannot be empty");
        }
        if (leiCode.trim().isEmpty()) {
            throw new IllegalArgumentException("LEI code cannot be empty");
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
