package com.bcbs239.regtech.modules.dataquality.domain.specifications;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.Specification;
import com.bcbs239.regtech.dataquality.domain.specifications.AccuracySpecifications;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

@DisplayName("Accuracy Specifications Tests")
class AccuracySpecificationsTest {

    @Nested
    @DisplayName("hasPositiveAmount() tests")
    class HasPositiveAmountTests {

        @Test
        @DisplayName("Should pass for positive amount")
        void shouldPassForPositiveAmount() {
            // Given
            ExposureRecord exposure = ExposureRecord.builder()
                .exposureId("EXP001")
                .amount(new BigDecimal("1000.00"))
                .build();
            
            Specification<ExposureRecord> spec = AccuracySpecifications.hasPositiveAmount();
            
            // When
            Result<Void> result = spec.isSatisfiedBy(exposure);
            
            // Then
            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("Should fail for zero amount")
        void shouldFailForZeroAmount() {
            // Given
            ExposureRecord exposure = ExposureRecord.builder()
                .exposureId("EXP001")
                .amount(BigDecimal.ZERO)
                .build();
            
            Specification<ExposureRecord> spec = AccuracySpecifications.hasPositiveAmount();
            
            // When
            Result<Void> result = spec.isSatisfiedBy(exposure);
            
            // Then
            assertTrue(result.isFailure());
            assertEquals("ACCURACY_AMOUNT_NOT_POSITIVE", result.getError().get().getCode());
            assertEquals("Amount must be positive", result.getError().get().getMessage());
        }

        @Test
        @DisplayName("Should fail for negative amount")
        void shouldFailForNegativeAmount() {
            // Given
            ExposureRecord exposure = ExposureRecord.builder()
                .exposureId("EXP001")
                .amount(new BigDecimal("-1000.00"))
                .build();
            
            Specification<ExposureRecord> spec = AccuracySpecifications.hasPositiveAmount();
            
            // When
            Result<Void> result = spec.isSatisfiedBy(exposure);
            
            // Then
            assertTrue(result.isFailure());
            assertEquals("ACCURACY_AMOUNT_NOT_POSITIVE", result.getError().get().getCode());
        }

        @Test
        @DisplayName("Should pass for null amount")
        void shouldPassForNullAmount() {
            // Given
            ExposureRecord exposure = ExposureRecord.builder()
                .exposureId("EXP001")
                .amount(null)
                .build();
            
            Specification<ExposureRecord> spec = AccuracySpecifications.hasPositiveAmount();
            
            // When
            Result<Void> result = spec.isSatisfiedBy(exposure);
            
            // Then
            assertTrue(result.isSuccess());
        }
    }

    @Nested
    @DisplayName("hasValidCurrency() tests")
    class HasValidCurrencyTests {

        @Test
        @DisplayName("Should pass for valid ISO 4217 currency codes")
        void shouldPassForValidCurrencyCodes() {
            String[] validCurrencies = {"USD", "EUR", "GBP", "JPY", "CHF", "CAD"};
            
            for (String currency : validCurrencies) {
                // Given
                ExposureRecord exposure = ExposureRecord.builder()
                    .exposureId("EXP001")
                    .currency(currency)
                    .build();
                
                Specification<ExposureRecord> spec = AccuracySpecifications.hasValidCurrency();
                
                // When
                Result<Void> result = spec.isSatisfiedBy(exposure);
                
                // Then
                assertTrue(result.isSuccess(), "Currency " + currency + " should be valid");
            }
        }

        @Test
        @DisplayName("Should pass for valid currency codes in lowercase")
        void shouldPassForLowercaseCurrencyCodes() {
            // Given
            ExposureRecord exposure = ExposureRecord.builder()
                .exposureId("EXP001")
                .currency("usd")
                .build();
            
            Specification<ExposureRecord> spec = AccuracySpecifications.hasValidCurrency();
            
            // When
            Result<Void> result = spec.isSatisfiedBy(exposure);
            
            // Then
            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("Should fail for invalid currency codes")
        void shouldFailForInvalidCurrencyCodes() {
            String[] invalidCurrencies = {"XXX", "INVALID", "US", "EURO", "123"};
            
            for (String currency : invalidCurrencies) {
                // Given
                ExposureRecord exposure = ExposureRecord.builder()
                    .exposureId("EXP001")
                    .currency(currency)
                    .build();
                
                Specification<ExposureRecord> spec = AccuracySpecifications.hasValidCurrency();
                
                // When
                Result<Void> result = spec.isSatisfiedBy(exposure);
                
                // Then
                assertTrue(result.isFailure(), "Currency " + currency + " should be invalid");
                assertEquals("ACCURACY_INVALID_CURRENCY", result.getError().get().getCode());
            }
        }

        @Test
        @DisplayName("Should pass for null currency")
        void shouldPassForNullCurrency() {
            // Given
            ExposureRecord exposure = ExposureRecord.builder()
                .exposureId("EXP001")
                .currency(null)
                .build();
            
            Specification<ExposureRecord> spec = AccuracySpecifications.hasValidCurrency();
            
            // When
            Result<Void> result = spec.isSatisfiedBy(exposure);
            
            // Then
            assertTrue(result.isSuccess());
        }
    }

    @Nested
    @DisplayName("hasValidCountry() tests")
    class HasValidCountryTests {

        @Test
        @DisplayName("Should pass for valid ISO 3166-1 alpha-2 country codes")
        void shouldPassForValidCountryCodes() {
            String[] validCountries = {"US", "GB", "DE", "FR", "IT", "ES", "JP", "CN"};
            
            for (String country : validCountries) {
                // Given
                ExposureRecord exposure = ExposureRecord.builder()
                    .exposureId("EXP001")
                    .country(country)
                    .build();
                
                Specification<ExposureRecord> spec = AccuracySpecifications.hasValidCountry();
                
                // When
                Result<Void> result = spec.isSatisfiedBy(exposure);
                
                // Then
                assertTrue(result.isSuccess(), "Country " + country + " should be valid");
            }
        }

        @Test
        @DisplayName("Should pass for valid country codes in lowercase")
        void shouldPassForLowercaseCountryCodes() {
            // Given
            ExposureRecord exposure = ExposureRecord.builder()
                .exposureId("EXP001")
                .country("us")
                .build();
            
            Specification<ExposureRecord> spec = AccuracySpecifications.hasValidCountry();
            
            // When
            Result<Void> result = spec.isSatisfiedBy(exposure);
            
            // Then
            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("Should fail for invalid country codes")
        void shouldFailForInvalidCountryCodes() {
            String[] invalidCountries = {"XX", "USA", "GERMANY", "123", "A"};
            
            for (String country : invalidCountries) {
                // Given
                ExposureRecord exposure = ExposureRecord.builder()
                    .exposureId("EXP001")
                    .country(country)
                    .build();
                
                Specification<ExposureRecord> spec = AccuracySpecifications.hasValidCountry();
                
                // When
                Result<Void> result = spec.isSatisfiedBy(exposure);
                
                // Then
                assertTrue(result.isFailure(), "Country " + country + " should be invalid");
                assertEquals("ACCURACY_INVALID_COUNTRY", result.getError().get().getCode());
            }
        }

        @Test
        @DisplayName("Should pass for null country")
        void shouldPassForNullCountry() {
            // Given
            ExposureRecord exposure = ExposureRecord.builder()
                .exposureId("EXP001")
                .country(null)
                .build();
            
            Specification<ExposureRecord> spec = AccuracySpecifications.hasValidCountry();
            
            // When
            Result<Void> result = spec.isSatisfiedBy(exposure);
            
            // Then
            assertTrue(result.isSuccess());
        }
    }

    @Nested
    @DisplayName("hasValidLeiFormat() tests")
    class HasValidLeiFormatTests {

        @Test
        @DisplayName("Should pass for valid LEI format")
        void shouldPassForValidLeiFormat() {
            String[] validLeis = {
                "ABCDEFGHIJKLMNOPQRST",
                "1234567890ABCDEFGHIJ",
                "ZYXWVUTSRQPONMLKJIHG"
            };
            
            for (String lei : validLeis) {
                // Given
                ExposureRecord exposure = ExposureRecord.builder()
                    .exposureId("EXP001")
                    .leiCode(lei)
                    .build();
                
                Specification<ExposureRecord> spec = AccuracySpecifications.hasValidLeiFormat();
                
                // When
                Result<Void> result = spec.isSatisfiedBy(exposure);
                
                // Then
                assertTrue(result.isSuccess(), "LEI " + lei + " should be valid");
            }
        }

        @Test
        @DisplayName("Should pass for valid LEI format in lowercase")
        void shouldPassForLowercaseLeiFormat() {
            // Given
            ExposureRecord exposure = ExposureRecord.builder()
                .exposureId("EXP001")
                .leiCode("abcdefghijklmnopqrst")
                .build();
            
            Specification<ExposureRecord> spec = AccuracySpecifications.hasValidLeiFormat();
            
            // When
            Result<Void> result = spec.isSatisfiedBy(exposure);
            
            // Then
            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("Should fail for invalid LEI formats")
        void shouldFailForInvalidLeiFormats() {
            String[] invalidLeis = {
                "ABCDEFGHIJKLMNOPQRS",    // 19 characters
                "ABCDEFGHIJKLMNOPQRSTU",  // 21 characters
                "ABCDEFGHIJKLMNOPQR@T",   // contains special character
                "ABCDEFGHIJKLMNOPQR T",   // contains space
                "",                       // empty string
                "123"                     // too short
            };
            
            for (String lei : invalidLeis) {
                // Given
                ExposureRecord exposure = ExposureRecord.builder()
                    .exposureId("EXP001")
                    .leiCode(lei)
                    .build();
                
                Specification<ExposureRecord> spec = AccuracySpecifications.hasValidLeiFormat();
                
                // When
                Result<Void> result = spec.isSatisfiedBy(exposure);
                
                // Then
                assertTrue(result.isFailure(), "LEI " + lei + " should be invalid");
                assertEquals("ACCURACY_INVALID_LEI_FORMAT", result.getError().get().getCode());
            }
        }

        @Test
        @DisplayName("Should pass for null LEI")
        void shouldPassForNullLei() {
            // Given
            ExposureRecord exposure = ExposureRecord.builder()
                .exposureId("EXP001")
                .leiCode(null)
                .build();
            
            Specification<ExposureRecord> spec = AccuracySpecifications.hasValidLeiFormat();
            
            // When
            Result<Void> result = spec.isSatisfiedBy(exposure);
            
            // Then
            assertTrue(result.isSuccess());
        }
    }

    @Nested
    @DisplayName("hasReasonableAmount() tests")
    class HasReasonableAmountTests {

        @Test
        @DisplayName("Should pass for reasonable amounts")
        void shouldPassForReasonableAmounts() {
            BigDecimal[] reasonableAmounts = {
                new BigDecimal("1000.00"),
                new BigDecimal("1000000.00"),      // 1 million
                new BigDecimal("1000000000.00"),   // 1 billion
                new BigDecimal("9999999999.99")    // Just under 10 billion
            };
            
            for (BigDecimal amount : reasonableAmounts) {
                // Given
                ExposureRecord exposure = ExposureRecord.builder()
                    .exposureId("EXP001")
                    .amount(amount)
                    .build();
                
                Specification<ExposureRecord> spec = AccuracySpecifications.hasReasonableAmount();
                
                // When
                Result<Void> result = spec.isSatisfiedBy(exposure);
                
                // Then
                assertTrue(result.isSuccess(), "Amount " + amount + " should be reasonable");
            }
        }

        @Test
        @DisplayName("Should fail for unreasonable amounts")
        void shouldFailForUnreasonableAmounts() {
            BigDecimal[] unreasonableAmounts = {
                new BigDecimal("10000000000.00"),   // Exactly 10 billion
                new BigDecimal("10000000000.01"),   // Just over 10 billion
                new BigDecimal("100000000000.00"),  // 100 billion
                new BigDecimal("1000000000000.00")  // 1 trillion
            };
            
            for (BigDecimal amount : unreasonableAmounts) {
                // Given
                ExposureRecord exposure = ExposureRecord.builder()
                    .exposureId("EXP001")
                    .amount(amount)
                    .build();
                
                Specification<ExposureRecord> spec = AccuracySpecifications.hasReasonableAmount();
                
                // When
                Result<Void> result = spec.isSatisfiedBy(exposure);
                
                // Then
                assertTrue(result.isFailure(), "Amount " + amount + " should be unreasonable");
                assertEquals("ACCURACY_AMOUNT_UNREASONABLE", result.getError().get().getCode());
                assertEquals("Amount exceeds reasonable bounds (10B EUR)", result.getError().get().getMessage());
            }
        }

        @Test
        @DisplayName("Should pass for null amount")
        void shouldPassForNullAmount() {
            // Given
            ExposureRecord exposure = ExposureRecord.builder()
                .exposureId("EXP001")
                .amount(null)
                .build();
            
            Specification<ExposureRecord> spec = AccuracySpecifications.hasReasonableAmount();
            
            // When
            Result<Void> result = spec.isSatisfiedBy(exposure);
            
            // Then
            assertTrue(result.isSuccess());
        }
    }

    @Nested
    @DisplayName("Specification composition tests")
    class SpecificationCompositionTests {

        @Test
        @DisplayName("Should compose multiple accuracy specifications with AND")
        void shouldComposeMultipleSpecificationsWithAnd() {
            // Given
            ExposureRecord validExposure = ExposureRecord.builder()
                .exposureId("EXP001")
                .amount(new BigDecimal("1000.00"))
                .currency("USD")
                .country("US")
                .leiCode("ABCDEFGHIJKLMNOPQRST")
                .build();
            
            Specification<ExposureRecord> composedSpec = 
                AccuracySpecifications.hasPositiveAmount()
                    .and(AccuracySpecifications.hasValidCurrency())
                    .and(AccuracySpecifications.hasValidCountry())
                    .and(AccuracySpecifications.hasValidLeiFormat())
                    .and(AccuracySpecifications.hasReasonableAmount());
            
            // When
            Result<Void> result = composedSpec.isSatisfiedBy(validExposure);
            
            // Then
            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("Should fail composed specification when one rule fails")
        void shouldFailComposedSpecificationWhenOneRuleFails() {
            // Given - invalid currency
            ExposureRecord invalidExposure = ExposureRecord.builder()
                .exposureId("EXP001")
                .amount(new BigDecimal("1000.00"))
                .currency("INVALID")
                .country("US")
                .leiCode("ABCDEFGHIJKLMNOPQRST")
                .build();
            
            Specification<ExposureRecord> composedSpec = 
                AccuracySpecifications.hasPositiveAmount()
                    .and(AccuracySpecifications.hasValidCurrency())
                    .and(AccuracySpecifications.hasValidCountry())
                    .and(AccuracySpecifications.hasValidLeiFormat())
                    .and(AccuracySpecifications.hasReasonableAmount());
            
            // When
            Result<Void> result = composedSpec.isSatisfiedBy(invalidExposure);
            
            // Then
            assertTrue(result.isFailure());
            assertEquals("ACCURACY_INVALID_CURRENCY", result.getError().get().getCode());
        }
    }
}

