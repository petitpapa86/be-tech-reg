package com.bcbs239.regtech.riskcalculation.domain.classification;

import com.bcbs239.regtech.riskcalculation.domain.shared.enums.GeographicRegion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ExposureClassifier domain service
 * Tests geographic and sector classification business rules
 */
class ExposureClassifierTest {
    
    private ExposureClassifier classifier;
    
    @BeforeEach
    void setUp() {
        classifier = new ExposureClassifier();
    }
    
    // Geographic Classification Tests
    
    @Test
    @DisplayName("Should classify IT as ITALY")
    void shouldClassifyItalyAsHomeCountry() {
        GeographicRegion result = classifier.classifyRegion("IT");
        assertEquals(GeographicRegion.ITALY, result);
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"DE", "FR", "ES", "NL", "BE", "AT", "PT", "GR", "IE", "FI"})
    @DisplayName("Should classify EU countries as EU_OTHER")
    void shouldClassifyEuCountriesAsEuOther(String countryCode) {
        GeographicRegion result = classifier.classifyRegion(countryCode);
        assertEquals(GeographicRegion.EU_OTHER, result);
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"US", "GB", "CH", "JP", "CN", "BR", "IN", "AU", "CA", "RU"})
    @DisplayName("Should classify non-EU countries as NON_EUROPEAN")
    void shouldClassifyNonEuCountriesAsNonEuropean(String countryCode) {
        GeographicRegion result = classifier.classifyRegion(countryCode);
        assertEquals(GeographicRegion.NON_EUROPEAN, result);
    }
    
    @Test
    @DisplayName("Should throw NullPointerException for null country code")
    void shouldThrowExceptionForNullCountryCode() {
        assertThrows(NullPointerException.class, () -> classifier.classifyRegion(null));
    }
    
    // Sector Classification Tests
    
    @ParameterizedTest
    @CsvSource({
        "Residential Mortgage, RETAIL_MORTGAGE",
        "Home Mortgage Loan, RETAIL_MORTGAGE",
        "MORTGAGE, RETAIL_MORTGAGE",
        "mortgage, RETAIL_MORTGAGE"
    })
    @DisplayName("Should classify mortgage products as RETAIL_MORTGAGE")
    void shouldClassifyMortgageProducts(String productType, EconomicSector expected) {
        EconomicSector result = classifier.classifySector(productType);
        assertEquals(expected, result);
    }
    
    @ParameterizedTest
    @CsvSource({
        "Government Bond, SOVEREIGN",
        "Treasury Bill, SOVEREIGN",
        "GOVERNMENT, SOVEREIGN",
        "treasury, SOVEREIGN"
    })
    @DisplayName("Should classify government products as SOVEREIGN")
    void shouldClassifyGovernmentProducts(String productType, EconomicSector expected) {
        EconomicSector result = classifier.classifySector(productType);
        assertEquals(expected, result);
    }
    
    @ParameterizedTest
    @CsvSource({
        "Interbank Loan, BANKING",
        "INTERBANK, BANKING",
        "interbank, BANKING"
    })
    @DisplayName("Should classify interbank products as BANKING")
    void shouldClassifyInterbankProducts(String productType, EconomicSector expected) {
        EconomicSector result = classifier.classifySector(productType);
        assertEquals(expected, result);
    }
    
    @ParameterizedTest
    @CsvSource({
        "Business Loan, CORPORATE",
        "Equipment Financing, CORPORATE",
        "Credit Line, CORPORATE",
        "BUSINESS, CORPORATE",
        "equipment, CORPORATE",
        "credit line, CORPORATE"
    })
    @DisplayName("Should classify corporate products as CORPORATE")
    void shouldClassifyCorporateProducts(String productType, EconomicSector expected) {
        EconomicSector result = classifier.classifySector(productType);
        assertEquals(expected, result);
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"Personal Loan", "Credit Card", "Unknown Product", "Other"})
    @DisplayName("Should classify unmatched products as OTHER")
    void shouldClassifyUnmatchedProductsAsOther(String productType) {
        EconomicSector result = classifier.classifySector(productType);
        assertEquals(EconomicSector.OTHER, result);
    }
    
    @Test
    @DisplayName("Should throw NullPointerException for null product type")
    void shouldThrowExceptionForNullProductType() {
        assertThrows(NullPointerException.class, () -> classifier.classifySector(null));
    }
    
    @Test
    @DisplayName("Should handle empty product type")
    void shouldHandleEmptyProductType() {
        EconomicSector result = classifier.classifySector("");
        assertEquals(EconomicSector.OTHER, result);
    }
    
    @Test
    @DisplayName("Should handle whitespace-only product type")
    void shouldHandleWhitespaceProductType() {
        EconomicSector result = classifier.classifySector("   ");
        assertEquals(EconomicSector.OTHER, result);
    }
    
    @Test
    @DisplayName("Should return immutable set of EU countries")
    void shouldReturnImmutableEuCountries() {
        var euCountries = classifier.getEuCountries();
        assertNotNull(euCountries);
        assertFalse(euCountries.isEmpty());
        assertFalse(euCountries.contains("IT")); // Italy should not be in EU_OTHER set
        assertTrue(euCountries.contains("DE"));
        assertTrue(euCountries.contains("FR"));
        
        // Verify immutability
        assertThrows(UnsupportedOperationException.class, () -> euCountries.add("XX"));
    }
}
