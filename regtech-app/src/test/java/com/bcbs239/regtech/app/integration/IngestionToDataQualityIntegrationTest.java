package com.bcbs239.regtech.app.integration;

import com.bcbs239.regtech.core.domain.shared.dto.BatchDataDTO;
import com.bcbs239.regtech.core.domain.shared.dto.BankInfoDTO;
import com.bcbs239.regtech.core.domain.shared.dto.CreditRiskMitigationDTO;
import com.bcbs239.regtech.core.domain.shared.dto.ExposureDTO;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import com.bcbs239.regtech.ingestion.domain.model.BankInfoModel;
import com.bcbs239.regtech.ingestion.domain.model.CreditRiskMitigation;
import com.bcbs239.regtech.ingestion.domain.model.LoanExposure;
import com.bcbs239.regtech.ingestion.domain.model.ParsedFileData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for data format alignment between Ingestion and Data Quality modules.
 * 
 * Tests the complete flow:
 * 1. Ingestion creates ParsedFileData
 * 2. Convert to BatchDataDTO
 * 3. Serialize to JSON
 * 4. Deserialize using Data Quality parsing logic
 * 5. Data Quality creates ExposureRecord from DTO
 * 6. Verify all exposures are parsed correctly
 * 
 * Requirements: 7.1, 7.3
 */
@SpringBootTest
class IngestionToDataQualityIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldPreserveDataThroughIngestionToDataQualityFlow() throws Exception {
        // Step 1: Create ParsedFileData (Ingestion domain model)
        BankInfoModel bankInfo = new BankInfoModel(
            "Community First Bank",
            "08081",
            "815600D7623147C25D86",
            "2024-09-12",
            3
        );


        LoanExposure exposure1 = new LoanExposure(
            "LOAN001",
            "EXP_001_2024",
            "Mike's Pizza Inc",
            "CORP12345",
            "549300ABCDEF1234567890",
            250000.0,
            250000.0,
            240000.0,
            "EUR",
            "Business Loan",
            "Retail",
            "ON_BALANCE",
            "Italy",
            "IT"
        );

        LoanExposure exposure2 = new LoanExposure(
            "LOAN002",
            "EXP_002_2024",
            "Tech Solutions Ltd",
            "CORP67890",
            "549300ZYXWVU9876543210",
            500000.0,
            500000.0,
            480000.0,
            "USD",
            "Corporate Loan",
            "Technology",
            "ON_BALANCE",
            "United States",
            "US"
        );

        LoanExposure exposure3 = new LoanExposure(
            "LOAN003",
            "EXP_003_2024",
            "Green Energy Corp",
            "CORP11111",
            "549300GREENERGY123456",
            750000.0,
            750000.0,
            720000.0,
            "GBP",
            "Project Finance",
            "Energy",
            "ON_BALANCE",
            "United Kingdom",
            "GB"
        );

        CreditRiskMitigation mitigation1 = new CreditRiskMitigation(
            "EXP_001_2024",
            "FINANCIAL_COLLATERAL",
            BigDecimal.valueOf(10000.00),
            "EUR"
        );

        CreditRiskMitigation mitigation2 = new CreditRiskMitigation(
            "EXP_003_2024",
            "GUARANTEE",
            BigDecimal.valueOf(50000.00),
            "GBP"
        );

        ParsedFileData parsedFileData = new ParsedFileData(
            bankInfo,
            List.of(exposure1, exposure2, exposure3),
            List.of(mitigation1, mitigation2),
            Map.of("source", "test", "format", "json")
        );

        // Step 2: Convert to BatchDataDTO
        BatchDataDTO batchDataDTO = parsedFileData.toDTO();
        
        // Verify DTO structure
        assertNotNull(batchDataDTO, "BatchDataDTO should not be null");
        assertNotNull(batchDataDTO.bankInfo(), "Bank info should not be null");
        assertEquals(3, batchDataDTO.exposures().size(), "Should have 3 exposures");
        assertEquals(2, batchDataDTO.creditRiskMitigation().size(), "Should have 2 mitigations");

        // Step 3: Serialize BatchDataDTO to JSON
        String json = objectMapper.writeValueAsString(batchDataDTO);
        
        // Verify JSON is not empty
        assertNotNull(json, "JSON should not be null");
        assertFalse(json.isEmpty(), "JSON should not be empty");
        
        // Verify JSON contains expected snake_case fields
        assertTrue(json.contains("\"bank_info\""), "JSON should contain bank_info field");
        assertTrue(json.contains("\"exposures\""), "JSON should contain exposures field");
        assertTrue(json.contains("\"credit_risk_mitigation\""), "JSON should contain credit_risk_mitigation field");
        assertTrue(json.contains("\"exposure_id\""), "JSON should contain exposure_id field");
        assertTrue(json.contains("\"counterparty_lei\""), "JSON should contain counterparty_lei field");
        assertTrue(json.contains("\"exposure_amount\""), "JSON should contain exposure_amount field");

        // Step 4: Deserialize JSON using Data Quality parsing logic (simulating what S3StorageServiceImpl does)
        BatchDataDTO deserializedDTO = objectMapper.readValue(json, BatchDataDTO.class);
        
        // Verify deserialized DTO
        assertNotNull(deserializedDTO, "Deserialized DTO should not be null");
        assertNotNull(deserializedDTO.bankInfo(), "Deserialized bank info should not be null");
        assertEquals(3, deserializedDTO.exposures().size(), "Deserialized should have 3 exposures");
        assertEquals(2, deserializedDTO.creditRiskMitigation().size(), "Deserialized should have 2 mitigations");

        // Step 5: Verify bank info is preserved
        BankInfoDTO deserializedBankInfo = deserializedDTO.bankInfo();
        assertEquals("Community First Bank", deserializedBankInfo.bankName());
        assertEquals("08081", deserializedBankInfo.abiCode());
        assertEquals("815600D7623147C25D86", deserializedBankInfo.leiCode());
        assertEquals(LocalDate.parse("2024-09-12"), deserializedBankInfo.reportDate());
        assertEquals(3, deserializedBankInfo.totalExposures());

        // Step 6: Call ExposureRecord.fromDTO() for each exposure (Data Quality domain model)
        List<ExposureRecord> exposureRecords = deserializedDTO.exposures().stream()
            .map(ExposureRecord::fromDTO)
            .toList();
        
        assertEquals(3, exposureRecords.size(), "Should have 3 exposure records");

        // Step 7: Verify first exposure data is preserved
        ExposureRecord record1 = exposureRecords.get(0);
        assertNotNull(record1, "First exposure record should not be null");
        assertEquals("EXP_001_2024", record1.exposureId());
        assertEquals("CORP12345", record1.counterpartyId());
        assertEquals(0, BigDecimal.valueOf(250000.0).compareTo(record1.amount()));
        assertEquals("EUR", record1.currency());
        assertEquals("IT", record1.country());
        assertEquals("Business Loan", record1.productType());
        assertEquals("549300ABCDEF1234567890", record1.leiCode());
        assertEquals("LOAN001", record1.referenceNumber()); // instrumentId mapped to referenceNumber

        // Step 8: Verify second exposure data is preserved
        ExposureRecord record2 = exposureRecords.get(1);
        assertNotNull(record2, "Second exposure record should not be null");
        assertEquals("EXP_002_2024", record2.exposureId());
        assertEquals("CORP67890", record2.counterpartyId());
        assertEquals(0, BigDecimal.valueOf(500000.0).compareTo(record2.amount()));
        assertEquals("USD", record2.currency());
        assertEquals("US", record2.country());
        assertEquals("Corporate Loan", record2.productType());
        assertEquals("549300ZYXWVU9876543210", record2.leiCode());
        assertEquals("LOAN002", record2.referenceNumber());

        // Step 9: Verify third exposure data is preserved
        ExposureRecord record3 = exposureRecords.get(2);
        assertNotNull(record3, "Third exposure record should not be null");
        assertEquals("EXP_003_2024", record3.exposureId());
        assertEquals("CORP11111", record3.counterpartyId());
        assertEquals(0, BigDecimal.valueOf(750000.0).compareTo(record3.amount()));
        assertEquals("GBP", record3.currency());
        assertEquals("GB", record3.country());
        assertEquals("Project Finance", record3.productType());
        assertEquals("549300GREENERGY123456", record3.leiCode());
        assertEquals("LOAN003", record3.referenceNumber());

        // Step 10: Verify credit risk mitigation is preserved
        CreditRiskMitigationDTO deserializedMitigation1 = deserializedDTO.creditRiskMitigation().get(0);
        assertEquals("EXP_001_2024", deserializedMitigation1.exposureId());
        assertEquals("FINANCIAL_COLLATERAL", deserializedMitigation1.mitigationType());
        assertEquals(0, BigDecimal.valueOf(10000.00).compareTo(deserializedMitigation1.value()));
        assertEquals("EUR", deserializedMitigation1.currency());

        CreditRiskMitigationDTO deserializedMitigation2 = deserializedDTO.creditRiskMitigation().get(1);
        assertEquals("EXP_003_2024", deserializedMitigation2.exposureId());
        assertEquals("GUARANTEE", deserializedMitigation2.mitigationType());
        assertEquals(0, BigDecimal.valueOf(50000.00).compareTo(deserializedMitigation2.value()));
        assertEquals("GBP", deserializedMitigation2.currency());
    }


    @Test
    void shouldHandleEmptyExposuresList() throws Exception {
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

        BatchDataDTO batchDataDTO = parsedFileData.toDTO();
        String json = objectMapper.writeValueAsString(batchDataDTO);
        BatchDataDTO deserializedDTO = objectMapper.readValue(json, BatchDataDTO.class);
        
        // Convert to ExposureRecords
        List<ExposureRecord> exposureRecords = deserializedDTO.exposures().stream()
            .map(ExposureRecord::fromDTO)
            .toList();
        
        assertNotNull(exposureRecords);
        assertTrue(exposureRecords.isEmpty(), "Should have no exposure records");
        assertNotNull(deserializedDTO.creditRiskMitigation());
        assertTrue(deserializedDTO.creditRiskMitigation().isEmpty(), "Should have no mitigations");
    }

    @Test
    void shouldHandleNullBankInfo() throws Exception {
        LoanExposure exposure = new LoanExposure(
            "LOAN001",
            "EXP_001_2024",
            "Test Company",
            "CORP99999",
            "549300TEST1234567890",
            100000.0,
            100000.0,
            95000.0,
            "EUR",
            "Test Loan",
            "Test Sector",
            "ON_BALANCE",
            "Germany",
            "DE"
        );

        ParsedFileData parsedFileData = new ParsedFileData(
            null, // null bank info
            List.of(exposure),
            List.of(),
            Map.of()
        );

        BatchDataDTO batchDataDTO = parsedFileData.toDTO();
        String json = objectMapper.writeValueAsString(batchDataDTO);
        BatchDataDTO deserializedDTO = objectMapper.readValue(json, BatchDataDTO.class);
        
        // Verify bank info is null
        assertNull(deserializedDTO.bankInfo(), "Bank info should be null");
        
        // Verify exposures are still parsed correctly
        List<ExposureRecord> exposureRecords = deserializedDTO.exposures().stream()
            .map(ExposureRecord::fromDTO)
            .toList();
        
        assertEquals(1, exposureRecords.size());
        assertEquals("EXP_001_2024", exposureRecords.get(0).exposureId());
    }

    @Test
    void shouldPreserveAllExposureFields() throws Exception {
        // Create exposure with all fields populated
        LoanExposure exposure = new LoanExposure(
            "LOAN_FULL",
            "EXP_FULL_2024",
            "Full Data Company",
            "CORP_FULL",
            "549300FULLDATA12345678",
            1000000.0,
            1000000.0,
            950000.0,
            "CHF",
            "Full Product Type",
            "Full Sector",
            "OFF_BALANCE",
            "Switzerland",
            "CH"
        );

        BankInfoModel bankInfo = new BankInfoModel(
            "Full Bank",
            "99999",
            "FULLBANK123456789012",
            "2024-12-05",
            1
        );

        ParsedFileData parsedFileData = new ParsedFileData(
            bankInfo,
            List.of(exposure),
            List.of(),
            Map.of()
        );

        // Convert and serialize
        BatchDataDTO batchDataDTO = parsedFileData.toDTO();
        String json = objectMapper.writeValueAsString(batchDataDTO);
        
        // Deserialize and convert to ExposureRecord
        BatchDataDTO deserializedDTO = objectMapper.readValue(json, BatchDataDTO.class);
        ExposureDTO exposureDTO = deserializedDTO.exposures().get(0);
        ExposureRecord record = ExposureRecord.fromDTO(exposureDTO);
        
        // Verify all fields are preserved
        assertEquals("EXP_FULL_2024", record.exposureId());
        assertEquals("CORP_FULL", record.counterpartyId());
        assertEquals(0, BigDecimal.valueOf(1000000.0).compareTo(record.amount()));
        assertEquals("CHF", record.currency());
        assertEquals("CH", record.country());
        assertEquals("Full Product Type", record.productType());
        assertEquals("549300FULLDATA12345678", record.leiCode());
        assertEquals("LOAN_FULL", record.referenceNumber());
        
        // Verify ExposureDTO has all fields
        assertEquals("EXP_FULL_2024", exposureDTO.exposureId());
        assertEquals("LOAN_FULL", exposureDTO.instrumentId());
        assertEquals("Full Data Company", exposureDTO.counterpartyName());
        assertEquals("CORP_FULL", exposureDTO.counterpartyId());
        assertEquals("549300FULLDATA12345678", exposureDTO.counterpartyLei());
        assertEquals(0, BigDecimal.valueOf(1000000.0).compareTo(exposureDTO.exposureAmount()));
        assertEquals("CHF", exposureDTO.currency());
        assertEquals("Full Product Type", exposureDTO.productType());
        assertEquals("OFF_BALANCE", exposureDTO.balanceSheetType());
        assertEquals("CH", exposureDTO.countryCode());
    }
}
