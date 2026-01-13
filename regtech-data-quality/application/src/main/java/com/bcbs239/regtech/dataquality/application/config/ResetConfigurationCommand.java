package com.bcbs239.regtech.dataquality.application.config;

import com.bcbs239.regtech.dataquality.domain.config.BankId;

/**
 * Command to reset data quality configuration to system defaults.
 * 
 * <p>CQRS Pattern: Write operation command.
 */
public record ResetConfigurationCommand(BankId bankId) {
}
