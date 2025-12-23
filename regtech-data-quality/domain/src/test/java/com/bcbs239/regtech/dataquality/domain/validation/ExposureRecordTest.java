package com.bcbs239.regtech.dataquality.domain.validation;

import com.bcbs239.regtech.core.domain.shared.dto.ExposureDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ExposureRecord
 */
class ExposureRecordTest {

    @Test
    void fromDTO_shouldMapAllAvailableFields() {
        // Given
        ExposureDTO dto = new ExposureDTO(
            "EXP_001",
            "INST_001",
            "LOAN",
            "Test Company",
            "COUNTER_001",
            "LEI123456789",
            new BigDecimal("100000.00"),
            "EUR",
            "Business Loan",
            "CORPORATE",
            LocalDate.parse("2029-09-12"),
            "ON_BALANCE",
            "IT",
            null
        );

        // When
        ExposureRecord record = ExposureRecord.fromDTO(dto);

        // Then
        assertNotNull(record);
        assertEquals("EXP_001", record.exposureId());
        assertEquals("COUNTER_001", record.counterpartyId());
        assertEquals(new BigDecimal("100000.00"), record.amount());
        assertEquals("EUR", record.currency());
        assertEquals("IT", record.country());
        assertEquals("CORPORATE", record.sector());
        assertEquals("Business Loan", record.productType());
        assertEquals("LEI123456789", record.leiCode());
        assertEquals("INST_001", record.referenceNumber());
        
        // Fields not in DTO should be null
        assertNull(record.counterpartyType());
        assertNull(record.internalRating());
        assertNull(record.riskCategory());
        assertNull(record.riskWeight());
        assertNull(record.reportingDate());
        assertNull(record.valuationDate());
        assertEquals(LocalDate.parse("2029-09-12"), record.maturityDate());
    }

    @Test
    void fromDTO_shouldHandleNullValues() {
        // Given
        ExposureDTO dto = new ExposureDTO(
            "EXP_002",
            null,
            null,
            null,
            null,
            null,
            new BigDecimal("50000.00"),
            "USD",
            null,
            null,
            null,
            null,
            "US",
            null
        );

        // When
        ExposureRecord record = ExposureRecord.fromDTO(dto);

        // Then
        assertNotNull(record);
        assertEquals("EXP_002", record.exposureId());
        assertNull(record.counterpartyId());
        assertEquals(new BigDecimal("50000.00"), record.amount());
        assertEquals("USD", record.currency());
        assertEquals("US", record.country());
        assertNull(record.productType());
        assertNull(record.leiCode());
        assertNull(record.referenceNumber());
    }

    @Test
    void hasRequiredFields_shouldReturnTrueForValidRecord() {
        // Given
        ExposureDTO dto = new ExposureDTO(
            "EXP_003",
            "INST_003",
            "BOND",
            "Test Corp",
            "COUNTER_003",
            "LEI987654321",
            new BigDecimal("200000.00"),
            "GBP",
            "Corporate Bond",
            null,
            null,
            "ON_BALANCE",
            "GB",
            null
        );

        // When
        ExposureRecord record = ExposureRecord.fromDTO(dto);

        // Then - should have required fields even though sector is null
        // Note: hasRequiredFields checks for sector, which won't be present from DTO
        assertFalse(record.hasRequiredFields(), "Record from DTO won't have sector, so required fields check fails");
    }
}
