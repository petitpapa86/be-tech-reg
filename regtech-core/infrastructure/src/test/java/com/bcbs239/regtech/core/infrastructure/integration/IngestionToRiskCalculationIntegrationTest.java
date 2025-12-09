package com.bcbs239.regtech.core.infrastructure.integration;

import com.bcbs239.regtech.core.domain.shared.dto.BatchDataDTO;
import com.bcbs239.regtech.core.domain.shared.dto.BankInfoDTO;
import com.bcbs239.regtech.core.domain.shared.dto.CreditRiskMitigationDTO;
import com.bcbs239.regtech.core.domain.shared.dto.ExposureDTO;
import com.bcbs239.regtech.ingestion.domain.model.BankInfoModel;
import com.bcbs239.regtech.ingestion.domain.model.CreditRiskMitigation;
import com.bcbs239.regtech.ingestion.domain.model.LoanExposure;
import com.bcbs239.regtech.ingestion.domain.model.ParsedFileData;
import com.bcbs239.regtech.riskcalculation.domain.exposure.ExposureRecording;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for data format alignment between Ingestion and Risk Calculation modules.
 * 
 * Tests the complete flow:
 * 1. Ingestion creates ParsedFileData
 * 2. Converts to BatchDataDTO using toDTO()
 * 3. Serializes to JSON
 * 4. Deserializes back to BatchDataDTO
 * 5. Risk Calculation converts to ExposureRecording using fromDTO()
 * 
 * Validates Requirements 7.1, 7.2:
 * - 7.1: WHEN running integration tests THEN the system SHALL verify that Ingestion output can be deserialized to BatchDataDTO
 * - 7.2: WHEN running integration tests THEN the system SHALL verify that Risk Calculation can read BatchDataDTO from JSON
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
        // Given: Ingestion module creates ParsedFileData with sample data
        BankInfoModel bankInfo = new BankInfoModel(
            "Community First Bank",
            "08081",
            "815600D7623147C25D86",
            "2024-09-12",
            2
        );

        LoanExposure exposure1 = new LoanExposure(
            "LOAN001",                          // loanId
            "EXP_001_2024",                     // exposureId
            "Mike's Pizza Inc",                 // borrowerName
            "CORP12345",                        // borrowerId
            "549300ABCDEF1234567890",           // counterpartyLei
            250000.0,                           // loanAmount
            250000.0,                           // grossExposureAmount
            240000.0,                           // netExposureAmount
            "EUR",                              // currency
            "Business Loan",                    // loanType
            "Retail",                           // sector
            "ON_BALANCE",                       // exposureType
            "Italy",                            // borrowerCountry
            "IT"                                // countryCode
        );

        LoanExposure exposure2 = new LoanExposure(
            "LOAN002",                          // loanId
            "EXP_002_2024",                     // exposureId
            "Tech Solutions Ltd",               // borrowerName
            "CORP67890",                        // borrowerId
            "549300ZYXWVU0987654321",           // counterpartyLei
            500000.0,                           // loanAmount
            500000.0,                           // grossExposureAmount
            480000.0,                           // netExposureAmount
            "EUR",                              // currency
            "Corporate Loan",                   // loanType
            "Technology",                       // sector
            "ON_BALANCE",                       // exposureType
            "Germany",                          // borrowerCountry
            "DE"                                // countryCode
        );

        CreditRiskMitigation mitigation1 = new CreditRiskMitigation(
            "EXP_001_2024",                     // exposureId
            "FINANCIAL_COLLATERAL",             // collateralType
            10000.0,                            // collateralValue
            "EUR"                               // collateralCurrency
        );

        ParsedFileData parsedFileData = new ParsedFileData(
            bankInfo,
            List.of(exposure1, exposure2),
            List.of(mitigation1),
            Map.of("source", "test")
        );

        // When: Convert to DTO
        BatchDataDTO batchDataDTO = parsedFileData.toDTO();

        // Then: Verify DTO structure
        assertNotNull(batchDataDTO, "BatchDataDTO should not be null");
        assertNotNull(batchDataDTO.bankInfo(), "BankInfo should not be null");
        assertEquals(2, batchDataDTO.exposures().size(), "Should have 2 exposures");
        assertEquals(1, batchDataDTO.creditRiskMitigation().size(), "Should have 1 mitigation");

        // Verify bank info mapping
        BankInfoDTO bankInfoDTO = batchDataDTO.bankInfo();
        assertEquals("Community First Bank", bankInfoDTO.bankName());
        assertEquals("08081", bankInfoDTO.abiCode());
        assertEquals("815600D7623147C25D86", bankInfoDTO.leiCode());
        assertEquals(LocalDate.parse("2024-09-12"), bankInfoDTO.reportDate());
        assertEquals(2, bankInfoDTO.totalExposures());

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
        assertEquals(bankInfoDTO.bankName(), deserializedBankInfo.bankName());
        assertEquals(bankInfoDTO.abiCode(), deserializedBankInfo.abiCode());
        assertEquals(bankInfoDTO.leiCode(), deserializedBankInfo.leiCode());
        assertEquals(bankInfoDTO.reportDate(), deserializedBankInfo.reportDate());
        assertEquals(bankInfoDTO.totalExposures(), deserializedBankInfo.totalExposures());

        // When: Risk Calculation converts exposures using fromDTO()
        List<ExposureRecording> exposureRecordings = deserializedDTO.exposures().stream()
            .map(ExposureRecording::fromDTO)
            .toList();

        // Then: Verify all exposures converted successfully
        assertEquals(2, exposureRecordings.size(), "Should have 2 exposure recordings");

        // Verify first exposure data preserved
        ExposureRecording recording1 = exposureRecordings.get(0);
        assertNotNull(recording1, "First exposure recording should not be null");
        assertEquals("EXP_001_2024", recording1.id().value(), "Exposure ID should be preserved");
        assertEquals("LOAN001", recording1.instrumentId().value(), "Instrument ID should be preserved");
        assertEquals("CORP12345", recording1.counterparty().counterpartyId(), "Counterparty ID should be preserved");
        assertEquals("Mike's Pizza Inc", recording1.counterparty().counterpartyName(), "Counterparty name should be preserved");
        assertEquals("549300ABCDEF1234567890", recording1.counterparty().leiCode(), "LEI code should be preserved");
        assertEquals(new BigDecimal("250000.0"), recording1.exposureAmount().amount(), "Exposure amount should be preserved");
        assertEquals("EUR", recording1.exposureAmount().currency(), "Currency should be preserved");
        assertEquals("Business Loan", recording1.classification().productType(), "Product type should be preserved");
        assertEquals("IT", recording1.classification().countryCode(), "Country code should be preserved");

        // Verify second exposure data preserved
        ExposureRecording recording2 = exposureRecordings.get(1);
        assertNotNull(recording2, "Second exposure recording should not be null");
        assertEquals("EXP_002_2024", recording2.id().value(), "Exposure ID should be preserved");
        assertEquals("LOAN002", recording2.instrumentId().value(), "Instrument ID should be preserved");
        assertEquals("CORP67890", recording2.counterparty().counterpartyId(), "Counterparty ID should be preserved");
        assertEquals("Tech Solutions Ltd", recording2.counterparty().counterpartyName(), "Counterparty name should be preserved");
        assertEquals(new BigDecimal("500000.0"), recording2.exposureAmount().amount(), "Exposure amount should be preserved");
        assertEquals("EUR", recording2.exposureAmount().currency(), "Currency should be preserved");
        assertEquals("Corporate Loan", recording2.classification().productType(), "Product type should be preserved");
        assertEquals("DE", recording2.classification().countryCode(), "Country code should be preserved");

        // Verify credit risk mitigation preserved
        CreditRiskMitigationDTO mitigationDTO = deserializedDTO.creditRiskMitigation().get(0);
        assertEquals("EXP_001_2024", mitigationDTO.exposureId(), "Mitigation exposure ID should be preserved");
        assertEquals("FINANCIAL_COLLATERAL", mitigationDTO.mitigationType(), "Mitigation type should be preserved");
        assertEquals(new BigDecimal("10000.0"), mitigationDTO.value(), "Mitigation value should be preserved");
        assertEquals("EUR", mitigationDTO.currency(), "Mitigation currency should be preserved");
    }

    @Test
    void shouldHandleEmptyExposuresAndMitigations() throws Exception {
        // Given: ParsedFileData with no exposures or mitigations
        BankInfoModel bankInfo = new BankInfoModel(
            "Test Bank",
            "12345",
            "TEST123456789012345678",
            "2024-01-01",
            0
        );

        ParsedFileData parsedFileData = new ParsedFileData(
            bankInfo,
            List.of(),
            List.of(),
            Map.of()
        );

        // When: Convert to DTO and serialize
        BatchDataDTO batchDataDTO = parsedFileData.toDTO();
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
        // Given: Exposure with all fields populated
        LoanExposure exposure = new LoanExposure(
            "LOAN_FULL",
            "EXP_FULL_2024",
            "Full Data Corp",
            "FULL123",
            "LEI_FULL_12345678901234",
            1000000.0,
            1000000.0,
            950000.0,
            "USD",
            "Term Loan",
            "Manufacturing",
            "OFF_BALANCE",
            "United States",
            "US"
        );

        ParsedFileData parsedFileData = new ParsedFileData(
            new BankInfoModel("Bank", "123", "LEI123", "2024-01-01", 1),
            List.of(exposure),
            List.of(),
            Map.of()
        );

        // When: Full round trip
        BatchDataDTO dto = parsedFileData.toDTO();
        String json = objectMapper.writeValueAsString(dto);
        BatchDataDTO deserializedDTO = objectMapper.readValue(json, BatchDataDTO.class);
        ExposureRecording recording = ExposureRecording.fromDTO(deserializedDTO.exposures().get(0));

        // Then: Verify all fields preserved
        assertEquals("EXP_FULL_2024", recording.id().value());
        assertEquals("LOAN_FULL", recording.instrumentId().value());
        assertEquals("FULL123", recording.counterparty().counterpartyId());
        assertEquals("Full Data Corp", recording.counterparty().counterpartyName());
        assertEquals("LEI_FULL_12345678901234", recording.counterparty().leiCode());
        assertEquals(new BigDecimal("1000000.0"), recording.exposureAmount().amount());
        assertEquals("USD", recording.exposureAmount().currency());
        assertEquals("Term Loan", recording.classification().productType());
        assertEquals("US", recording.classification().countryCode());
    }
}
