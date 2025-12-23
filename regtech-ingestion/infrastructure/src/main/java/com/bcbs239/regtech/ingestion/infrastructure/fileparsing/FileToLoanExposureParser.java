package com.bcbs239.regtech.ingestion.infrastructure.fileparsing;

import com.bcbs239.regtech.ingestion.domain.model.*;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Unified parser that accepts JSON and Excel input streams and maps them to domain objects.
 * Capacity controls: callers provide a maxRecords limit to avoid unbounded memory usage.
 * The parser focuses on the architecture-level concern of capacity and streaming rather than
 * low-level technical plumbing; it exposes simple APIs that can be composed into batch processors.
 */
@Component
public class FileToLoanExposureParser {

    private static final Logger log = LoggerFactory.getLogger(FileToLoanExposureParser.class);

    private final ObjectMapper objectMapper;

    public FileToLoanExposureParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parse loan exposures from a JSON input stream. Expects a top-level object with a "loan_portfolio" array.
     * Stops after maxRecords to limit memory consumption.
     */
    public List<LoanExposure> parseJsonToLoanExposures(InputStream is, int maxRecords) throws IOException {
        JsonFactory jf = objectMapper.getFactory();
        try (JsonParser jp = jf.createParser(is)) {
            // Traverse until we find the loan_portfolio field
            while (jp.nextToken() != null) {
                if (jp.currentToken() == JsonToken.FIELD_NAME && "loan_portfolio".equals(jp.currentName())) {
                    jp.nextToken(); // move to START_ARRAY
                    if (jp.currentToken() != JsonToken.START_ARRAY) {
                        log.warn("Expected loan_portfolio to be an array");
                        return List.of();
                    }

                    List<LoanExposure> results = new ArrayList<>();
                    while (jp.nextToken() != JsonToken.END_ARRAY) {
                        // Deserialize current object into DTO using ObjectMapper and map to domain
                        ExposureDto dto = objectMapper.readValue(jp, ExposureDto.class);
                        results.add(DomainMapper.toLoanExposure(dto));
                        if (results.size() >= maxRecords) {
                            log.info("Reached maxRecords limit ({}) while parsing JSON", maxRecords);
                            break;
                        }
                    }

                    return results;
                }
            }
        }
        return List.of();
    }

    /**
     * Parse credit risk mitigation entries from JSON top-level "credit_risk_mitigation" array.
     */
    public List<CreditRiskMitigation> parseJsonToCreditRiskMitigations(InputStream is, int maxRecords) throws IOException {
        JsonFactory jf = objectMapper.getFactory();
        try (JsonParser jp = jf.createParser(is)) {
            while (jp.nextToken() != null) {
                if (jp.currentToken() == JsonToken.FIELD_NAME && "credit_risk_mitigation".equals(jp.currentName())) {
                    jp.nextToken(); // move to START_ARRAY
                    if (jp.currentToken() != JsonToken.START_ARRAY) {
                        log.warn("Expected credit_risk_mitigation to be an array");
                        return List.of();
                    }

                    List<CreditRiskMitigation> results = new ArrayList<>();
                    while (jp.nextToken() != JsonToken.END_ARRAY) {
                        CreditRiskMitigationDto dto = objectMapper.readValue(jp, CreditRiskMitigationDto.class);
                        results.add(DomainMapper.toCrm(dto));
                        if (results.size() >= maxRecords) {
                            log.info("Reached maxRecords limit ({}) while parsing CRM JSON", maxRecords);
                            break;
                        }
                    }

                    return results;
                }
            }
        }
        return List.of();
    }

    /**
     * Result holder for combined JSON parsing.
     */
    public record JsonParsingResult(BankInfoModel bankInfo, List<LoanExposure> exposures, List<CreditRiskMitigation> crms) {}

    /**
     * Parse bank info, loan exposures, and credit risk mitigations from a JSON input stream in a single pass.
     * This is more efficient than parsing the same stream multiple times (which is actually impossible since
     * InputStreams can only be read once).
     * 
     * Supports both old format (loan_portfolio) and new format (exposures) for backward compatibility.
     */
    public JsonParsingResult parseJsonToBothArrays(InputStream is, int maxRecords) throws IOException {
        JsonFactory jf = objectMapper.getFactory();
        BankInfoModel bankInfo = null;
        List<LoanExposure> exposures = new ArrayList<>();
        List<CreditRiskMitigation> crms = new ArrayList<>();
        
        try (JsonParser jp = jf.createParser(is)) {
            // Traverse the entire JSON document
            while (jp.nextToken() != null) {
                if (jp.currentToken() == JsonToken.FIELD_NAME) {
                    String fieldName = jp.currentName();
                    
                    switch (fieldName) {
                        // Parse bank_info object
                        case "bank_info" -> {
                            jp.nextToken(); // move to START_OBJECT
                            if (jp.currentToken() == JsonToken.START_OBJECT) {
                                BankInfoDto dto = objectMapper.readValue(jp, BankInfoDto.class);
                                bankInfo = DomainMapper.toBankInfoModel(dto);
                                log.debug("Parsed bank_info: {}", bankInfo);
                            } else {
                                log.warn("Expected bank_info to be an object");
                            }
                        }
                        // Parse exposures array (new format)
                        case "exposures" -> {
                            jp.nextToken(); // move to START_ARRAY
                            if (jp.currentToken() == JsonToken.START_ARRAY) {
                                while (jp.nextToken() != JsonToken.END_ARRAY) {
                                    ExposureDto dto = objectMapper.readValue(jp, ExposureDto.class);
                                    exposures.add(DomainMapper.toLoanExposure(dto));
                                    if (exposures.size() >= maxRecords) {
                                        log.info("Reached maxRecords limit ({}) while parsing exposures", maxRecords);
                                        // Skip remaining elements in this array
                                        jp.skipChildren();
                                        break;
                                    }
                                }
                            } else {
                                log.warn("Expected exposures to be an array");
                            }
                        }
                        // Parse loan_portfolio array (old format - backward compatibility)
                        case "loan_portfolio" -> {
                            jp.nextToken(); // move to START_ARRAY
                            if (jp.currentToken() == JsonToken.START_ARRAY) {
                                while (jp.nextToken() != JsonToken.END_ARRAY) {
                                    ExposureDto dto = objectMapper.readValue(jp, ExposureDto.class);
                                    exposures.add(DomainMapper.toLoanExposure(dto));
                                    if (exposures.size() >= maxRecords) {
                                        log.info("Reached maxRecords limit ({}) while parsing loan_portfolio", maxRecords);
                                        // Skip remaining elements in this array
                                        jp.skipChildren();
                                        break;
                                    }
                                }
                            } else {
                                log.warn("Expected loan_portfolio to be an array");
                            }
                        }
                        // Parse credit_risk_mitigation array
                        case "credit_risk_mitigation" -> {
                            jp.nextToken(); // move to START_ARRAY
                            if (jp.currentToken() == JsonToken.START_ARRAY) {
                                while (jp.nextToken() != JsonToken.END_ARRAY) {
                                    CreditRiskMitigationDto dto = objectMapper.readValue(jp, CreditRiskMitigationDto.class);
                                    crms.add(DomainMapper.toCrm(dto));
                                    if (crms.size() >= maxRecords) {
                                        log.info("Reached maxRecords limit ({}) while parsing credit_risk_mitigation", maxRecords);
                                        // Skip remaining elements in this array
                                        jp.skipChildren();
                                        break;
                                    }
                                }
                            } else {
                                log.warn("Expected credit_risk_mitigation to be an array");
                            }
                        }
                    }
                }
            }
        }
        
        log.info("Parsed JSON file: bank_info={}, {} exposures, {} credit risk mitigations", 
                 bankInfo != null ? bankInfo.bankName() : "null", exposures.size(), crms.size());
        return new JsonParsingResult(bankInfo, exposures, crms);
    }

    /**
     * Parse loan exposures from an Excel (XLSX) input stream. Stops after maxRecords.
     * This implementation uses a simple in-memory XSSF read — for very large files the
     * architecture should use the streaming (SAX) API or offload to a specialized processor.
     */
    public List<LoanExposure> parseExcelToLoanExposures(InputStream is, int maxRecords) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(is)) {
            XSSFSheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            List<LoanExposure> results = new ArrayList<>();

            // Assume header row at index 0
            int lastRow = sheet.getLastRowNum();
            for (int r = 1; r <= lastRow; r++) {
                if (results.size() >= maxRecords) {
                    log.info("Reached maxRecords limit ({}) while parsing Excel", maxRecords);
                    break;
                }
                Row row = sheet.getRow(r);
                if (row == null) continue;

                // Expected Excel columns (by index):
                // 0 loan_id, 1 exposure_id, 2 borrower_name, 3 borrower_id, 4 counterparty_lei,
                // 5 loan_amount, 6 gross_exposure_amount, 7 net_exposure_amount, 8 currency,
                // 9 loan_type, 10 sector, 11 exposure_type, 12 borrower_country, 13 country_code, 14 internal_rating
                String instrumentId = formatter.formatCellValue(row.getCell(0));
                String exposureId = formatter.formatCellValue(row.getCell(1));
                String instrumentType = "LOAN";
                String counterpartyName = formatter.formatCellValue(row.getCell(2));
                String counterpartyId = formatter.formatCellValue(row.getCell(3));
                String counterpartyLei = formatter.formatCellValue(row.getCell(4));
                double exposureAmount = parseDoubleSafe(formatter.formatCellValue(row.getCell(5)));
                String currency = formatter.formatCellValue(row.getCell(8));
                String productType = formatter.formatCellValue(row.getCell(9));
                String sector = formatter.formatCellValue(row.getCell(10));
                String balanceSheetType = formatter.formatCellValue(row.getCell(11));
                String countryCode = formatter.formatCellValue(row.getCell(13));
                String internalRating = formatter.formatCellValue(row.getCell(14));

                ExposureDto dto = new ExposureDto(
                    exposureId,
                    instrumentId,
                    instrumentType,
                    counterpartyName,
                    counterpartyId,
                    counterpartyLei,
                    exposureAmount,
                    currency,
                    productType,
                    sector,
                    null,
                    balanceSheetType,
                    countryCode,
                    internalRating
                );

                results.add(DomainMapper.toLoanExposure(dto));
            }

            return results;
        }
    }

    private double parseDoubleSafe(String v) {
        if (v == null || v.isBlank()) return 0.0;
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }
}
