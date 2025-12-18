package com.bcbs239.regtech.core.infrastructure.integration;

import com.bcbs239.regtech.core.domain.shared.dto.BatchDataDTO;
import com.bcbs239.regtech.core.domain.shared.dto.CreditRiskMitigationDTO;
import com.bcbs239.regtech.core.domain.shared.dto.ExposureDTO;
import com.bcbs239.regtech.core.domain.shared.dto.BankInfoDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JSON round-trip contract test for {@link BatchDataDTO}.
 *
 * This intentionally lives in Core and only validates the shared DTO JSON schema
 * (field names, serialization, deserialization) without depending on other modules,
 * to avoid introducing reactor dependency cycles.
 */
class IngestionToRiskCalculationIntegrationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void shouldPreserveDataThroughIngestionToRiskCalculationFlow() throws Exception {
        // Given
        BankInfoDTO bankInfoDTO = new BankInfoDTO(
            "Community First Bank",
            "08081",
            "815600D7623147C25D86",
            LocalDate.parse("2024-09-12"),
            2
        );

        ExposureDTO exposure1 = new ExposureDTO(
            "EXP_001_2024",
            "LOAN001",
            "LOAN",
            "Mike's Pizza Inc",
            "CORP12345",
            "549300ABCDEF1234567890",
            new BigDecimal("250000.0"),
            "EUR",
            "Business Loan",
            "CORPORATE",
            LocalDate.parse("2029-09-12"),
            "ON_BALANCE",
            "IT"
        );

        ExposureDTO exposure2 = new ExposureDTO(
            "EXP_002_2024",
            "LOAN002",
            "LOAN",
            "Tech Solutions Ltd",
            "CORP67890",
            "549300ZYXWVU0987654321",
            new BigDecimal("500000.0"),
            "EUR",
            "Corporate Loan",
            "CORPORATE_TECHNOLOGY",
            LocalDate.parse("2029-09-12"),
            "ON_BALANCE",
            "DE"
        );

        CreditRiskMitigationDTO mitigation1 = new CreditRiskMitigationDTO(
            "EXP_001_2024",
            "FINANCIAL_COLLATERAL",
            new BigDecimal("10000.0"),
            "EUR"
        );

        BatchDataDTO batchDataDTO = new BatchDataDTO(
            bankInfoDTO,
            List.of(exposure1, exposure2),
            List.of(mitigation1)
        );

        // Then: Verify DTO structure
        assertNotNull(batchDataDTO, "BatchDataDTO should not be null");
        assertNotNull(batchDataDTO.bankInfo(), "BankInfo should not be null");
        assertEquals(2, batchDataDTO.exposures().size(), "Should have 2 exposures");
        assertEquals(1, batchDataDTO.creditRiskMitigation().size(), "Should have 1 mitigation");

        // Verify bank info mapping
        BankInfoDTO bankInfoFromDto = batchDataDTO.bankInfo();
        assertEquals("Community First Bank", bankInfoFromDto.bankName());
        assertEquals("08081", bankInfoFromDto.abiCode());
        assertEquals("815600D7623147C25D86", bankInfoFromDto.leiCode());
        assertEquals(LocalDate.parse("2024-09-12"), bankInfoFromDto.reportDate());
        assertEquals(2, bankInfoFromDto.totalExposures());

        // When: Serialize to JSON
        String json = objectMapper.writeValueAsString(batchDataDTO);

        // Then: Verify JSON structure
        assertNotNull(json, "JSON should not be null");
        assertTrue(json.contains("bank_info"), "JSON should contain bank_info field");
        assertTrue(json.contains("exposures"), "JSON should contain exposures field");
        assertTrue(json.contains("credit_risk_mitigation"), "JSON should contain credit_risk_mitigation field");
        assertTrue(json.contains("Community First Bank"), "JSON should contain bank name");
        assertTrue(json.contains("EXP_001_2024"), "JSON should contain exposure ID");

        // When: Deserialize back to BatchDataDTO
        BatchDataDTO deserializedDTO = objectMapper.readValue(json, BatchDataDTO.class);

        // Then: Verify deserialized DTO matches original
        assertNotNull(deserializedDTO, "Deserialized DTO should not be null");
        assertNotNull(deserializedDTO.bankInfo(), "Deserialized bank info should not be null");
        assertEquals(2, deserializedDTO.exposures().size(), "Deserialized should have 2 exposures");
        assertEquals(1, deserializedDTO.creditRiskMitigation().size(), "Deserialized should have 1 mitigation");

        // Verify bank info preserved
        BankInfoDTO deserializedBankInfo = deserializedDTO.bankInfo();
        assertEquals(bankInfoFromDto.bankName(), deserializedBankInfo.bankName());
        assertEquals(bankInfoFromDto.abiCode(), deserializedBankInfo.abiCode());
        assertEquals(bankInfoFromDto.leiCode(), deserializedBankInfo.leiCode());
        assertEquals(bankInfoFromDto.reportDate(), deserializedBankInfo.reportDate());
        assertEquals(bankInfoFromDto.totalExposures(), deserializedBankInfo.totalExposures());

		// Verify exposure fields preserved
		ExposureDTO deserializedExposure1 = deserializedDTO.exposures().get(0);
		assertEquals(exposure1.exposureId(), deserializedExposure1.exposureId());
		assertEquals(exposure1.instrumentId(), deserializedExposure1.instrumentId());
		assertEquals(exposure1.counterpartyId(), deserializedExposure1.counterpartyId());
		assertEquals(exposure1.counterpartyName(), deserializedExposure1.counterpartyName());
		assertEquals(exposure1.counterpartyLei(), deserializedExposure1.counterpartyLei());
		assertEquals(exposure1.exposureAmount(), deserializedExposure1.exposureAmount());
		assertEquals(exposure1.currency(), deserializedExposure1.currency());
		assertEquals(exposure1.productType(), deserializedExposure1.productType());
		assertEquals(exposure1.balanceSheetType(), deserializedExposure1.balanceSheetType());
		assertEquals(exposure1.countryCode(), deserializedExposure1.countryCode());

		// Verify mitigation fields preserved
		CreditRiskMitigationDTO mitigationDTO = deserializedDTO.creditRiskMitigation().get(0);
		assertEquals(mitigation1.exposureId(), mitigationDTO.exposureId());
		assertEquals(mitigation1.mitigationType(), mitigationDTO.mitigationType());
		assertEquals(mitigation1.value(), mitigationDTO.value());
		assertEquals(mitigation1.currency(), mitigationDTO.currency());
    }

    @Test
    void shouldHandleEmptyExposuresAndMitigations() throws Exception {
        // Given
        BatchDataDTO batchDataDTO = new BatchDataDTO(
            new BankInfoDTO("Test Bank", "12345", "TEST123456789012345678", LocalDate.parse("2024-01-01"), 0),
            List.of(),
            List.of()
        );
        String json = objectMapper.writeValueAsString(batchDataDTO);

        // Then: Deserialize and verify
        BatchDataDTO deserializedDTO = objectMapper.readValue(json, BatchDataDTO.class);
        
        assertNotNull(deserializedDTO);
        assertNotNull(deserializedDTO.bankInfo());
        assertEquals(0, deserializedDTO.exposures().size(), "Should have no exposures");
        assertEquals(0, deserializedDTO.creditRiskMitigation().size(), "Should have no mitigations");
        
        // Verify bank info still preserved
        assertEquals("Test Bank", deserializedDTO.bankInfo().bankName());
        assertEquals("12345", deserializedDTO.bankInfo().abiCode());
    }

    @Test
    void shouldPreserveAllExposureFields() throws Exception {
        // Given
        ExposureDTO exposure = new ExposureDTO(
            "EXP_FULL_2024",
            "LOAN_FULL",
            "LOAN",
            "Full Data Corp",
            "FULL123",
            "LEI_FULL_12345678901234",
            new BigDecimal("1000000.0"),
            "USD",
            "Term Loan",
            "CORPORATE",
            LocalDate.parse("2029-09-12"),
            "OFF_BALANCE",
            "US"
        );

        BatchDataDTO dto = new BatchDataDTO(
            new BankInfoDTO("Bank", "123", "LEI12345678901234567", LocalDate.parse("2024-01-01"), 1),
            List.of(exposure),
            List.of()
        );

        // When
        String json = objectMapper.writeValueAsString(dto);
        BatchDataDTO deserializedDTO = objectMapper.readValue(json, BatchDataDTO.class);

        // Then
        ExposureDTO roundTripped = deserializedDTO.exposures().get(0);
        assertEquals(exposure.exposureId(), roundTripped.exposureId());
        assertEquals(exposure.instrumentId(), roundTripped.instrumentId());
        assertEquals(exposure.instrumentType(), roundTripped.instrumentType());
        assertEquals(exposure.counterpartyName(), roundTripped.counterpartyName());
        assertEquals(exposure.counterpartyId(), roundTripped.counterpartyId());
        assertEquals(exposure.counterpartyLei(), roundTripped.counterpartyLei());
        assertEquals(exposure.exposureAmount(), roundTripped.exposureAmount());
        assertEquals(exposure.currency(), roundTripped.currency());
        assertEquals(exposure.productType(), roundTripped.productType());
        assertEquals(exposure.sector(), roundTripped.sector());
        assertEquals(exposure.maturityDate(), roundTripped.maturityDate());
        assertEquals(exposure.balanceSheetType(), roundTripped.balanceSheetType());
        assertEquals(exposure.countryCode(), roundTripped.countryCode());
    }
}
