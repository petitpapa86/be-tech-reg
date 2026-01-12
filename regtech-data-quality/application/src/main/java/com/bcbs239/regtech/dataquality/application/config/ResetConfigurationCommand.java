package com.bcbs239.regtech.dataquality.application.config;

/**
 * Command to reset data quality configuration to system defaults.
 * 
 * <p>CQRS Pattern: Write operation command.
 */
public record ResetConfigurationCommand(String bankId) {
}
