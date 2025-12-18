package com.bcbs239.regtech.riskcalculation.domain.exposure;

import com.bcbs239.regtech.core.domain.shared.dto.ExposureDTO;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExposureId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ExposureRecording Aggregate Root Tests")
class ExposureRecordingTest {

    private ExposureId exposureId;
    private InstrumentId instrumentId;
    private CounterpartyRef counterparty;
    private MonetaryAmount exposureAmount;
    private ExposureClassification classification;
    private Instant recordedAt;

    @BeforeEach
    void setUp() {
        exposureId = ExposureId.of("EXP-12345");
        instrumentId = InstrumentId.of("LOAN-67890");
        counterparty = CounterpartyRef.of("CP-001", "Test Bank", "213800TESTBANK123");
        exposureAmount = MonetaryAmount.of(new BigDecimal("1000000.00"), "EUR");
        classification = ExposureClassification.of(
            "Corporate Loan",
            InstrumentType.LOAN,
            BalanceSheetType.ON_BALANCE,
            "IT"
        );
        recordedAt = Instant.now();
    }

    @Test
    @DisplayName("Should create valid exposure recording with all parameters")
    void shouldCreateValidExposureRecording() {
        // When
        ExposureRecording exposureRecording = new ExposureRecording(
            exposureId,
            instrumentId,
            counterparty,
            exposureAmount,
            classification,
            recordedAt
        );

        // Then
        assertEquals(exposureId, exposureRecording.id());
        assertEquals(instrumentId, exposureRecording.instrumentId());
        assertEquals(counterparty, exposureRecording.counterparty());
        assertEquals(exposureAmount, exposureRecording.exposureAmount());
        assertEquals(classification, exposureRecording.classification());
        assertEquals(recordedAt, exposureRecording.recordedAt());
    }

    @Test
    @DisplayName("Should create exposure recording using create factory method")
    void shouldCreateExposureRecordingUsingCreateFactory() {
        // When
        ExposureRecording exposureRecording = ExposureRecording.create(
            exposureId,
            instrumentId,
            counterparty,
            exposureAmount,
            classification
        );

        // Then
        assertEquals(exposureId, exposureRecording.id());
        assertEquals(instrumentId, exposureRecording.instrumentId());
        assertEquals(counterparty, exposureRecording.counterparty());
        assertEquals(exposureAmount, exposureRecording.exposureAmount());
        assertEquals(classification, exposureRecording.classification());
        assertNotNull(exposureRecording.recordedAt());
        assertTrue(exposureRecording.recordedAt().isBefore(Instant.now().plusSeconds(1)));
    }

    @Test
    @DisplayName("Should reconstitute exposure recording from persistence")
    void shouldReconstituteExposureRecordingFromPersistence() {
        // Given
        Instant pastTime = Instant.now().minusSeconds(3600);

        // When
        ExposureRecording exposureRecording = ExposureRecording.reconstitute(
            exposureId,
            instrumentId,
            counterparty,
            exposureAmount,
            classification,
            pastTime
        );

        // Then
        assertEquals(exposureId, exposureRecording.id());
        assertEquals(instrumentId, exposureRecording.instrumentId());
        assertEquals(counterparty, exposureRecording.counterparty());
        assertEquals(exposureAmount, exposureRecording.exposureAmount());
        assertEquals(classification, exposureRecording.classification());
        assertEquals(pastTime, exposureRecording.recordedAt());
    }

    @Test
    @DisplayName("Should create exposure recording from DTO")
    void shouldCreateExposureRecordingFromDTO() {
        // Given
        ExposureDTO dto = new ExposureDTO(
            "EXP-DTO-001",
            "LOAN-DTO-001",
            "LOAN",
            "DTO Bank",
            "CP-DTO-001",
            "213800DTOBANK123",
            new BigDecimal("500000.00"),
            "USD",
            "Retail Loan",
            "RETAIL",
            null,
            "ON_BALANCE",
            "US"
        );

        // When
        ExposureRecording exposureRecording = ExposureRecording.fromDTO(dto);

        // Then
        assertEquals("EXP-DTO-001", exposureRecording.id().value());
        assertEquals("LOAN-DTO-001", exposureRecording.instrumentId().value());
        assertEquals("CP-DTO-001", exposureRecording.counterparty().counterpartyId());
        assertEquals("DTO Bank", exposureRecording.counterparty().name());
        assertTrue(exposureRecording.counterparty().leiCode().isPresent());
        assertEquals("213800DTOBANK123", exposureRecording.counterparty().leiCode().get());
        assertEquals(new BigDecimal("500000.00"), exposureRecording.exposureAmount().amount());
        assertEquals("USD", exposureRecording.exposureAmount().currencyCode());
        assertEquals("Retail Loan", exposureRecording.classification().productType());
        assertEquals(InstrumentType.LOAN, exposureRecording.classification().instrumentType());
        assertEquals(BalanceSheetType.ON_BALANCE, exposureRecording.classification().balanceSheetType());
        assertEquals("US", exposureRecording.classification().countryCode());
        assertNotNull(exposureRecording.recordedAt());
    }

    @Test
    @DisplayName("Should handle unknown instrument type in DTO by defaulting to OTHER")
    void shouldHandleUnknownInstrumentTypeInDTO() {
        // Given
        ExposureDTO dto = new ExposureDTO(
            "EXP-UNKNOWN-001",
            "UNKNOWN-001",
            "UNKNOWN_TYPE",
            "Test Bank",
            "CP-001",
            null,
            new BigDecimal("100000.00"),
            "EUR",
            "Unknown Product",
            null,
            null,
            "ON_BALANCE",
            "IT"
        );

        // When
        ExposureRecording exposureRecording = ExposureRecording.fromDTO(dto);

        // Then
        assertEquals(InstrumentType.OTHER, exposureRecording.classification().instrumentType());
    }

    @Test
    @DisplayName("Should handle unknown balance sheet type in DTO by defaulting to ON_BALANCE")
    void shouldHandleUnknownBalanceSheetTypeInDTO() {
        // Given
        ExposureDTO dto = new ExposureDTO(
            "EXP-UNKNOWN-002",
            "LOAN-002",
            "LOAN",
            "Test Bank",
            "CP-002",
            null,
            new BigDecimal("100000.00"),
            "EUR",
            "Test Product",
            null,
            null,
            "UNKNOWN_BALANCE_TYPE",
            "IT"
        );

        // When
        ExposureRecording exposureRecording = ExposureRecording.fromDTO(dto);

        // Then
        assertEquals(BalanceSheetType.ON_BALANCE, exposureRecording.classification().balanceSheetType());
    }

    @Test
    @DisplayName("Should handle null LEI in DTO")
    void shouldHandleNullLeiInDTO() {
        // Given
        ExposureDTO dto = new ExposureDTO(
            "EXP-NO-LEI-001",
            "LOAN-001",
            "LOAN",
            "Bank Without LEI",
            "CP-001",
            null,
            new BigDecimal("100000.00"),
            "EUR",
            "Corporate Loan",
            null,
            null,
            "ON_BALANCE",
            "IT"
        );

        // When
        ExposureRecording exposureRecording = ExposureRecording.fromDTO(dto);

        // Then
        assertTrue(exposureRecording.counterparty().leiCode().isEmpty());
    }

    @Test
    @DisplayName("Should reject null exposure ID")
    void shouldRejectNullExposureId() {
        // When & Then
        assertThrows(NullPointerException.class, () ->
            new ExposureRecording(
                null,
                instrumentId,
                counterparty,
                exposureAmount,
                classification,
                recordedAt
            ));
    }

    @Test
    @DisplayName("Should reject null instrument ID")
    void shouldRejectNullInstrumentId() {
        // When & Then
        assertThrows(NullPointerException.class, () ->
            new ExposureRecording(
                exposureId,
                null,
                counterparty,
                exposureAmount,
                classification,
                recordedAt
            ));
    }

    @Test
    @DisplayName("Should reject null counterparty")
    void shouldRejectNullCounterparty() {
        // When & Then
        assertThrows(NullPointerException.class, () ->
            new ExposureRecording(
                exposureId,
                instrumentId,
                null,
                exposureAmount,
                classification,
                recordedAt
            ));
    }

    @Test
    @DisplayName("Should reject null exposure amount")
    void shouldRejectNullExposureAmount() {
        // When & Then
        assertThrows(NullPointerException.class, () ->
            new ExposureRecording(
                exposureId,
                instrumentId,
                counterparty,
                null,
                classification,
                recordedAt
            ));
    }

    @Test
    @DisplayName("Should reject null classification")
    void shouldRejectNullClassification() {
        // When & Then
        assertThrows(NullPointerException.class, () ->
            new ExposureRecording(
                exposureId,
                instrumentId,
                counterparty,
                exposureAmount,
                null,
                recordedAt
            ));
    }

    @Test
    @DisplayName("Should reject null recorded timestamp")
    void shouldRejectNullRecordedTimestamp() {
        // When & Then
        assertThrows(NullPointerException.class, () ->
            new ExposureRecording(
                exposureId,
                instrumentId,
                counterparty,
                exposureAmount,
                classification,
                null
            ));
    }

    @Test
    @DisplayName("Should reject null DTO in fromDTO method")
    void shouldRejectNullDTOInFromDTOMethod() {
        // When & Then
        assertThrows(NullPointerException.class, () ->
            ExposureRecording.fromDTO(null));
    }

    @Test
    @DisplayName("Should be equal when same exposure ID")
    void shouldBeEqualWhenSameExposureId() {
        // Given
        ExposureRecording exposureRecording1 = new ExposureRecording(
            exposureId,
            instrumentId,
            counterparty,
            exposureAmount,
            classification,
            recordedAt
        );

        ExposureRecording exposureRecording2 = new ExposureRecording(
            exposureId,
            InstrumentId.of("DIFFERENT-INSTRUMENT"),
            CounterpartyRef.of("DIFFERENT-CP", "Different Bank"),
            MonetaryAmount.of(new BigDecimal("999999.99"), "USD"),
            ExposureClassification.of("Different Product", InstrumentType.BOND, BalanceSheetType.OFF_BALANCE, "US"),
            Instant.now().minusSeconds(1000)
        );

        // When & Then
        assertEquals(exposureRecording1, exposureRecording2);
        assertEquals(exposureRecording1.hashCode(), exposureRecording2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when different exposure ID")
    void shouldNotBeEqualWhenDifferentExposureId() {
        // Given
        ExposureRecording exposureRecording1 = new ExposureRecording(
            exposureId,
            instrumentId,
            counterparty,
            exposureAmount,
            classification,
            recordedAt
        );

        ExposureRecording exposureRecording2 = new ExposureRecording(
            ExposureId.of("DIFFERENT-EXP-ID"),
            instrumentId,
            counterparty,
            exposureAmount,
            classification,
            recordedAt
        );

        // When & Then
        assertNotEquals(exposureRecording1, exposureRecording2);
    }

    @Test
    @DisplayName("Should not be equal to null")
    void shouldNotBeEqualToNull() {
        // Given
        ExposureRecording exposureRecording = new ExposureRecording(
            exposureId,
            instrumentId,
            counterparty,
            exposureAmount,
            classification,
            recordedAt
        );

        // When & Then
        assertNotEquals(exposureRecording, null);
    }

    @Test
    @DisplayName("Should not be equal to different class")
    void shouldNotBeEqualToDifferentClass() {
        // Given
        ExposureRecording exposureRecording = new ExposureRecording(
            exposureId,
            instrumentId,
            counterparty,
            exposureAmount,
            classification,
            recordedAt
        );

        // When & Then
        assertNotEquals(exposureRecording, "not an exposure recording");
    }

    @Test
    @DisplayName("Should have meaningful toString representation")
    void shouldHaveMeaningfulToStringRepresentation() {
        // Given
        ExposureRecording exposureRecording = new ExposureRecording(
            exposureId,
            instrumentId,
            counterparty,
            exposureAmount,
            classification,
            recordedAt
        );

        // When
        String toString = exposureRecording.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("ExposureRecording"));
        assertTrue(toString.contains(exposureId.toString()));
        assertTrue(toString.contains(instrumentId.toString()));
    }

    @Test
    @DisplayName("Should handle case insensitive instrument type in DTO")
    void shouldHandleCaseInsensitiveInstrumentTypeInDTO() {
        // Given
        ExposureDTO dto = new ExposureDTO(
            "EXP-CASE-001",
            "LOAN-001",
            "loan", // lowercase
            "Test Bank",
            "CP-001",
            null,
            new BigDecimal("100000.00"),
            "EUR",
            "Test Product",
            null,
            null,
            "ON_BALANCE",
            "IT"
        );

        // When
        ExposureRecording exposureRecording = ExposureRecording.fromDTO(dto);

        // Then
        assertEquals(InstrumentType.LOAN, exposureRecording.classification().instrumentType());
    }

    @Test
    @DisplayName("Should handle case insensitive balance sheet type in DTO")
    void shouldHandleCaseInsensitiveBalanceSheetTypeInDTO() {
        // Given
        ExposureDTO dto = new ExposureDTO(
            "EXP-CASE-002",
            "LOAN-001",
            "LOAN",
            "Test Bank",
            "CP-001",
            null,
            new BigDecimal("100000.00"),
            "EUR",
            "Test Product",
            null,
            null,
            "off_balance", // lowercase
            "IT"
        );

        // When
        ExposureRecording exposureRecording = ExposureRecording.fromDTO(dto);

        // Then
        assertEquals(BalanceSheetType.OFF_BALANCE, exposureRecording.classification().balanceSheetType());
    }

    @Test
    @DisplayName("Should handle different instrument types from DTO")
    void shouldHandleDifferentInstrumentTypesFromDTO() {
        // Test each instrument type
        for (InstrumentType instrumentType : InstrumentType.values()) {
            // Given
            ExposureDTO dto = new ExposureDTO(
                "EXP-TYPE-" + instrumentType.name(),
                "INST-001",
                instrumentType.name(),
                "Test Bank",
                "CP-001",
                null,
                new BigDecimal("100000.00"),
                "EUR",
                "Test Product",
                null,
                null,
                "ON_BALANCE",
                "IT"
            );

            // When
            ExposureRecording exposureRecording = ExposureRecording.fromDTO(dto);

            // Then
            assertEquals(instrumentType, exposureRecording.classification().instrumentType(),
                "Failed for instrument type: " + instrumentType);
        }
    }

    @Test
    @DisplayName("Should handle different balance sheet types from DTO")
    void shouldHandleDifferentBalanceSheetTypesFromDTO() {
        // Test each balance sheet type
        for (BalanceSheetType balanceSheetType : BalanceSheetType.values()) {
            // Given
            ExposureDTO dto = new ExposureDTO(
                "EXP-BALANCE-" + balanceSheetType.name(),
                "INST-001",
                "LOAN",
                "Test Bank",
                "CP-001",
                null,
                new BigDecimal("100000.00"),
                "EUR",
                "Test Product",
                null,
                null,
                balanceSheetType.name(),
                "IT"
            );

            // When
            ExposureRecording exposureRecording = ExposureRecording.fromDTO(dto);

            // Then
            assertEquals(balanceSheetType, exposureRecording.classification().balanceSheetType(),
                "Failed for balance sheet type: " + balanceSheetType);
        }
    }
}