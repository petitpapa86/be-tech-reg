package com.bcbs239.regtech.dataquality.domain.validation.validators;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SectorValidator Tests")
class SectorValidatorTest {

    @Test
    @DisplayName("Should validate valid financial sectors")
    void shouldValidateValidFinancialSectors() {
        assertTrue(SectorValidator.isValidSector("BANKING"));
        assertTrue(SectorValidator.isValidSector("INSURANCE"));
        assertTrue(SectorValidator.isValidSector("INVESTMENT_FUNDS"));
        assertTrue(SectorValidator.isValidSector("PENSION_FUNDS"));
        assertTrue(SectorValidator.isValidSector("CENTRAL_BANK"));
    }

    @Test
    @DisplayName("Should validate valid corporate sectors")
    void shouldValidateValidCorporateSectors() {
        assertTrue(SectorValidator.isValidSector("CORPORATE_FINANCIAL"));
        assertTrue(SectorValidator.isValidSector("CORPORATE_NON_FINANCIAL"));
        assertTrue(SectorValidator.isValidSector("MANUFACTURING"));
        assertTrue(SectorValidator.isValidSector("CONSTRUCTION"));
        assertTrue(SectorValidator.isValidSector("REAL_ESTATE"));
    }

    @Test
    @DisplayName("Should validate valid government sectors")
    void shouldValidateValidGovernmentSectors() {
        assertTrue(SectorValidator.isValidSector("SOVEREIGN"));
        assertTrue(SectorValidator.isValidSector("REGIONAL_GOVERNMENT"));
        assertTrue(SectorValidator.isValidSector("LOCAL_GOVERNMENT"));
        assertTrue(SectorValidator.isValidSector("PUBLIC_SECTOR_ENTITY"));
    }

    @Test
    @DisplayName("Should validate valid retail sectors")
    void shouldValidateValidRetailSectors() {
        assertTrue(SectorValidator.isValidSector("RETAIL_INDIVIDUAL"));
        assertTrue(SectorValidator.isValidSector("RETAIL_SME"));
        assertTrue(SectorValidator.isValidSector("RETAIL_MORTGAGE"));
    }

    @Test
    @DisplayName("Should reject invalid sector codes")
    void shouldRejectInvalidSectorCodes() {
        assertFalse(SectorValidator.isValidSector("INVALID_SECTOR"));
        assertFalse(SectorValidator.isValidSector("UNKNOWN"));
        assertFalse(SectorValidator.isValidSector("XYZ"));
    }

    @Test
    @DisplayName("Should reject null sector")
    void shouldRejectNullSector() {
        assertFalse(SectorValidator.isValidSector(null));
    }

    @Test
    @DisplayName("Should reject empty sector")
    void shouldRejectEmptySector() {
        assertFalse(SectorValidator.isValidSector(""));
        assertFalse(SectorValidator.isValidSector("   "));
    }

    @Test
    @DisplayName("Should handle case-insensitive sector validation")
    void shouldHandleCaseInsensitiveSectorValidation() {
        assertTrue(SectorValidator.isValidSector("banking"));
        assertTrue(SectorValidator.isValidSector("Banking"));
        assertTrue(SectorValidator.isValidSector("BANKING"));
        assertTrue(SectorValidator.isValidSector("  banking  "));
    }

    @Test
    @DisplayName("Should validate valid counterparty types")
    void shouldValidateValidCounterpartyTypes() {
        assertTrue(SectorValidator.isValidCounterpartyType("INDIVIDUAL"));
        assertTrue(SectorValidator.isValidCounterpartyType("SME"));
        assertTrue(SectorValidator.isValidCounterpartyType("CORPORATE"));
        assertTrue(SectorValidator.isValidCounterpartyType("BANK"));
        assertTrue(SectorValidator.isValidCounterpartyType("SOVEREIGN"));
    }

    @Test
    @DisplayName("Should reject invalid counterparty types")
    void shouldRejectInvalidCounterpartyTypes() {
        assertFalse(SectorValidator.isValidCounterpartyType("INVALID_TYPE"));
        assertFalse(SectorValidator.isValidCounterpartyType("UNKNOWN"));
    }

    @Test
    @DisplayName("Should reject null counterparty type")
    void shouldRejectNullCounterpartyType() {
        assertFalse(SectorValidator.isValidCounterpartyType(null));
    }

    @Test
    @DisplayName("Should reject empty counterparty type")
    void shouldRejectEmptyCounterpartyType() {
        assertFalse(SectorValidator.isValidCounterpartyType(""));
        assertFalse(SectorValidator.isValidCounterpartyType("   "));
    }

    @Test
    @DisplayName("Should check if sector is financial")
    void shouldCheckIfSectorIsFinancial() {
        assertTrue(SectorValidator.isFinancialSector("BANKING"));
        assertTrue(SectorValidator.isFinancialSector("INSURANCE"));
        assertTrue(SectorValidator.isFinancialSector("CENTRAL_BANK"));
        assertFalse(SectorValidator.isFinancialSector("MANUFACTURING"));
        assertFalse(SectorValidator.isFinancialSector("RETAIL_INDIVIDUAL"));
    }

    @Test
    @DisplayName("Should check if sector is corporate")
    void shouldCheckIfSectorIsCorporate() {
        assertTrue(SectorValidator.isCorporateSector("CORPORATE_FINANCIAL"));
        assertTrue(SectorValidator.isCorporateSector("MANUFACTURING"));
        assertTrue(SectorValidator.isCorporateSector("CONSTRUCTION"));
        assertFalse(SectorValidator.isCorporateSector("BANKING"));
        assertFalse(SectorValidator.isCorporateSector("SOVEREIGN"));
    }

    @Test
    @DisplayName("Should check if sector is government")
    void shouldCheckIfSectorIsGovernment() {
        assertTrue(SectorValidator.isGovernmentSector("SOVEREIGN"));
        assertTrue(SectorValidator.isGovernmentSector("REGIONAL_GOVERNMENT"));
        assertTrue(SectorValidator.isGovernmentSector("PUBLIC_ADMINISTRATION"));
        assertFalse(SectorValidator.isGovernmentSector("BANKING"));
        assertFalse(SectorValidator.isGovernmentSector("CORPORATE_FINANCIAL"));
    }

    @Test
    @DisplayName("Should check if sector is retail")
    void shouldCheckIfSectorIsRetail() {
        assertTrue(SectorValidator.isRetailSector("RETAIL_INDIVIDUAL"));
        assertTrue(SectorValidator.isRetailSector("RETAIL_SME"));
        assertTrue(SectorValidator.isRetailSector("RETAIL_MORTGAGE"));
        assertFalse(SectorValidator.isRetailSector("BANKING"));
        assertFalse(SectorValidator.isRetailSector("CORPORATE_FINANCIAL"));
    }

    @Test
    @DisplayName("Should normalize valid sector")
    void shouldNormalizeValidSector() {
        assertEquals("BANKING", SectorValidator.normalizeSector("banking"));
        assertEquals("BANKING", SectorValidator.normalizeSector("  Banking  "));
        assertEquals("INSURANCE", SectorValidator.normalizeSector("insurance"));
    }

    @Test
    @DisplayName("Should return null for invalid sector normalization")
    void shouldReturnNullForInvalidSectorNormalization() {
        assertNull(SectorValidator.normalizeSector("INVALID_SECTOR"));
        assertNull(SectorValidator.normalizeSector(null));
        assertNull(SectorValidator.normalizeSector(""));
    }

    @Test
    @DisplayName("Should normalize valid counterparty type")
    void shouldNormalizeValidCounterpartyType() {
        assertEquals("BANK", SectorValidator.normalizeCounterpartyType("bank"));
        assertEquals("CORPORATE", SectorValidator.normalizeCounterpartyType("  Corporate  "));
        assertEquals("SME", SectorValidator.normalizeCounterpartyType("sme"));
    }

    @Test
    @DisplayName("Should return null for invalid counterparty type normalization")
    void shouldReturnNullForInvalidCounterpartyTypeNormalization() {
        assertNull(SectorValidator.normalizeCounterpartyType("INVALID_TYPE"));
        assertNull(SectorValidator.normalizeCounterpartyType(null));
        assertNull(SectorValidator.normalizeCounterpartyType(""));
    }

    @Test
    @DisplayName("Should check compatibility between financial sector and counterparty type")
    void shouldCheckCompatibilityFinancialSector() {
        assertTrue(SectorValidator.isCompatibleWithCounterpartyType("BANKING", "BANK"));
        assertTrue(SectorValidator.isCompatibleWithCounterpartyType("INSURANCE", "INSURANCE_COMPANY"));
        assertTrue(SectorValidator.isCompatibleWithCounterpartyType("INVESTMENT_FUNDS", "INVESTMENT_FUND"));
        assertFalse(SectorValidator.isCompatibleWithCounterpartyType("BANKING", "INDIVIDUAL"));
        assertFalse(SectorValidator.isCompatibleWithCounterpartyType("BANKING", "CORPORATE"));
    }

    @Test
    @DisplayName("Should check compatibility between corporate sector and counterparty type")
    void shouldCheckCompatibilityCorporateSector() {
        assertTrue(SectorValidator.isCompatibleWithCounterpartyType("MANUFACTURING", "CORPORATE"));
        assertTrue(SectorValidator.isCompatibleWithCounterpartyType("CONSTRUCTION", "SME"));
        assertTrue(SectorValidator.isCompatibleWithCounterpartyType("REAL_ESTATE", "NON_FINANCIAL_CORPORATE"));
        assertFalse(SectorValidator.isCompatibleWithCounterpartyType("MANUFACTURING", "BANK"));
        assertFalse(SectorValidator.isCompatibleWithCounterpartyType("CONSTRUCTION", "SOVEREIGN"));
    }

    @Test
    @DisplayName("Should check compatibility between government sector and counterparty type")
    void shouldCheckCompatibilityGovernmentSector() {
        assertTrue(SectorValidator.isCompatibleWithCounterpartyType("SOVEREIGN", "SOVEREIGN"));
        assertTrue(SectorValidator.isCompatibleWithCounterpartyType("REGIONAL_GOVERNMENT", "REGIONAL_GOVERNMENT"));
        assertTrue(SectorValidator.isCompatibleWithCounterpartyType("PUBLIC_SECTOR_ENTITY", "PUBLIC_SECTOR_ENTITY"));
        assertFalse(SectorValidator.isCompatibleWithCounterpartyType("SOVEREIGN", "BANK"));
        assertFalse(SectorValidator.isCompatibleWithCounterpartyType("SOVEREIGN", "CORPORATE"));
    }

    @Test
    @DisplayName("Should check compatibility between retail sector and counterparty type")
    void shouldCheckCompatibilityRetailSector() {
        assertTrue(SectorValidator.isCompatibleWithCounterpartyType("RETAIL_INDIVIDUAL", "INDIVIDUAL"));
        assertTrue(SectorValidator.isCompatibleWithCounterpartyType("RETAIL_SME", "SME"));
        assertFalse(SectorValidator.isCompatibleWithCounterpartyType("RETAIL_INDIVIDUAL", "BANK"));
        assertFalse(SectorValidator.isCompatibleWithCounterpartyType("RETAIL_MORTGAGE", "CORPORATE"));
    }

    @Test
    @DisplayName("Should reject compatibility check with invalid inputs")
    void shouldRejectCompatibilityCheckWithInvalidInputs() {
        assertFalse(SectorValidator.isCompatibleWithCounterpartyType(null, "BANK"));
        assertFalse(SectorValidator.isCompatibleWithCounterpartyType("BANKING", null));
        assertFalse(SectorValidator.isCompatibleWithCounterpartyType("INVALID", "BANK"));
        assertFalse(SectorValidator.isCompatibleWithCounterpartyType("BANKING", "INVALID"));
    }

    @Test
    @DisplayName("Should get all valid sectors")
    void shouldGetAllValidSectors() {
        Set<String> sectors = SectorValidator.getAllValidSectors();
        
        assertNotNull(sectors);
        assertFalse(sectors.isEmpty());
        assertTrue(sectors.contains("BANKING"));
        assertTrue(sectors.contains("MANUFACTURING"));
        assertTrue(sectors.contains("SOVEREIGN"));
        assertTrue(sectors.contains("RETAIL_INDIVIDUAL"));
    }

    @Test
    @DisplayName("Should get all valid counterparty types")
    void shouldGetAllValidCounterpartyTypes() {
        Set<String> types = SectorValidator.getAllValidCounterpartyTypes();
        
        assertNotNull(types);
        assertFalse(types.isEmpty());
        assertTrue(types.contains("BANK"));
        assertTrue(types.contains("CORPORATE"));
        assertTrue(types.contains("INDIVIDUAL"));
        assertTrue(types.contains("SOVEREIGN"));
    }

    @Test
    @DisplayName("Should return immutable sets")
    void shouldReturnImmutableSets() {
        Set<String> sectors = SectorValidator.getAllValidSectors();
        Set<String> types = SectorValidator.getAllValidCounterpartyTypes();
        
        assertThrows(UnsupportedOperationException.class, () -> sectors.add("NEW_SECTOR"));
        assertThrows(UnsupportedOperationException.class, () -> types.add("NEW_TYPE"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"BANKING", "banking", " Banking ", "BANKING  "})
    @DisplayName("Should handle various sector formats")
    void shouldHandleVariousSectorFormats(String sector) {
        assertTrue(SectorValidator.isValidSector(sector));
        assertEquals("BANKING", SectorValidator.normalizeSector(sector));
    }
}
