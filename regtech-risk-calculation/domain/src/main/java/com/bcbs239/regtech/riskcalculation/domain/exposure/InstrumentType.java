package com.bcbs239.regtech.riskcalculation.domain.exposure;

/**
 * Type of financial instrument
 * Supports various exposure types for comprehensive risk calculation
 */
public enum InstrumentType {
    LOAN,
    BOND,
    DERIVATIVE,
    GUARANTEE,
    CREDIT_LINE,
    REPO,
    SECURITY,
    INTERBANK,
    OTHER
}
