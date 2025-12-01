package com.bcbs239.regtech.riskcalculation.infrastructure.dto;

/**
 * DTO for bank information
 */
public record BankInfoDTO(
    String bankName,
    String abiCode,
    String leiCode
) {}
