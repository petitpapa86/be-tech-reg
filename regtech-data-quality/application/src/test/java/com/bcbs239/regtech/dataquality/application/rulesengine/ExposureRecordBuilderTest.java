package com.bcbs239.regtech.dataquality.application.rulesengine;

import com.bcbs239.regtech.dataquality.application.validation.ValidationResults;
import com.bcbs239.regtech.dataquality.domain.rules.RuleViolation;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple unit test to verify ExposureRecord can be created and validated.
 * This tests the basic API without requiring database or Spring context.
 */
@DisplayName("ExposureRecord Builder Verification")
class ExposureRecordBuilderTest {

    @Test
    @DisplayName("Should successfully build ExposureRecord with all fields")
    void shouldBuildExposureRecordWithAllFields() {
        // Arrange & Act
        ExposureRecord exposure = ExposureRecord.builder()
            .exposureId("EXP_001_2024")
            .counterpartyId("CP_12345")
            .exposureAmount(new BigDecimal("2000000"))
            .currency("EUR")
            .countryCode("IT")
            .sector("RETAIL")
            .counterpartyType("CORPORATE")
            .productType("LOAN")
            .counterpartyLei("815600D7623147C25D86")
            .internalRating("BBB")
            .riskCategory("STANDARD")
            .riskWeight(new BigDecimal("0.75"))
            .reportingDate(LocalDate.now().minusDays(10))
            .valuationDate(LocalDate.now().minusDays(5))
            .maturityDate(LocalDate.now().plusYears(1))
            .referenceNumber("REF_001")
            .build();

        // Assert
        assertThat(exposure).isNotNull();
        assertThat(exposure.exposureId()).isEqualTo("EXP_001_2024");
        assertThat(exposure.counterpartyId()).isEqualTo("CP_12345");
        assertThat(exposure.exposureAmount()).isEqualByComparingTo(new BigDecimal("2000000"));
        assertThat(exposure.currency()).isEqualTo("EUR");
        assertThat(exposure.countryCode()).isEqualTo("IT");
        assertThat(exposure.productType()).isEqualTo("LOAN");
        assertThat(exposure.internalRating()).isEqualTo("BBB");
    }

    @Test
    @DisplayName("Should build ExposureRecord with minimal fields (nulls allowed)")
    void shouldBuildExposureRecordWithMinimalFields() {
        // Arrange & Act
        ExposureRecord exposure = ExposureRecord.builder()
            .exposureId("EXP_002")
            .exposureAmount(new BigDecimal("1000"))
            .currency("USD")
            .build();

        // Assert
        assertThat(exposure).isNotNull();
        assertThat(exposure.exposureId()).isEqualTo("EXP_002");
        assertThat(exposure.counterpartyId()).isNull();
        assertThat(exposure.countryCode()).isNull();
    }

    @Test
    @DisplayName("ExposureRecord should be a Java record with expected fields")
    void shouldBeJavaRecordWithExpectedFields() {
        // Arrange
        ExposureRecord exposure = ExposureRecord.builder()
            .exposureId("EXP_003")
            .counterpartyId("CP_789")
            .exposureAmount(new BigDecimal("5000000"))
            .currency("GBP")
            .build();

        // Assert - test record accessor methods
        assertThat(exposure.exposureId()).isEqualTo("EXP_003");
        assertThat(exposure.counterpartyId()).isEqualTo("CP_789");
        assertThat(exposure.exposureAmount()).isEqualByComparingTo(new BigDecimal("5000000"));
        assertThat(exposure.currency()).isEqualTo("GBP");
        
        // Test toString() method (records have built-in toString)
        String toString = exposure.toString();
        assertThat(toString).contains("EXP_003");
        assertThat(toString).contains("CP_789");
        assertThat(toString).contains("5000000");
        assertThat(toString).contains("GBP");
    }

    @Test
    @DisplayName("Two ExposureRecords with same values should be equal (record equality)")
    void shouldSupportRecordEquality() {
        // Arrange
        ExposureRecord exposure1 = ExposureRecord.builder()
            .exposureId("EXP_004")
            .exposureAmount(new BigDecimal("1000"))
            .currency("EUR")
            .build();

        ExposureRecord exposure2 = ExposureRecord.builder()
            .exposureId("EXP_004")
            .exposureAmount(new BigDecimal("1000"))
            .currency("EUR")
            .build();

        // Assert - Java records provide structural equality
        assertThat(exposure1).isEqualTo(exposure2);
        assertThat(exposure1.hashCode()).isEqualTo(exposure2.hashCode());
    }
}
