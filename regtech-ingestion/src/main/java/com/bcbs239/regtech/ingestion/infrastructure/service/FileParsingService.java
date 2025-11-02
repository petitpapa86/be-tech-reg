package com.bcbs239.regtech.ingestion.infrastructure.service;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service for parsing JSON and Excel files containing exposure data.
 * Implements streaming parsing for large files to avoid memory issues.
 */
@Service
@Slf4j
public class FileParsingService {

    private static final String[] REQUIRED_FIELDS = {"exposure_id", "amount", "currency", "country", "sector"};
    private static final int MAX_EXPOSURES_PER_FILE = 1_000_000;

    /**
     * Parse JSON file with streaming to handle large files efficiently
     */
    public Result<ParsedFileData> parseJsonFile(InputStream fileStream, String fileName) {
        log.info("Starting JSON file parsing for file: {}", fileName);
        
        try {
            JsonFactory factory = new JsonFactory();
            JsonParser parser = factory.createParser(fileStream);
            
            List<ParsedFileData.ExposureRecord> exposures = new ArrayList<>();
            Set<String> seenExposureIds = new HashSet<>();
            int lineNumber = 1;
            
            // Expect array of objects
            if (parser.nextToken() != JsonToken.START_ARRAY) {
                return Result.failure(ErrorDetail.of("INVALID_JSON_FORMAT", 
                    "JSON file must contain an array of exposure objects"));
            }
            
            while (parser.nextToken() == JsonToken.START_OBJECT) {
                lineNumber++;
                
                if (exposures.size() >= MAX_EXPOSURES_PER_FILE) {
                    return Result.failure(ErrorDetail.of("FILE_TOO_LARGE", 
                        String.format("File contains more than %d exposures. Please split the file.", MAX_EXPOSURES_PER_FILE)));
                }
                
                Result<ParsedFileData.ExposureRecord> recordResult = parseJsonExposureRecord(parser, lineNumber);
                if (recordResult.isFailure()) {
                    return Result.failure(recordResult.getError().orElse(
                        ErrorDetail.of("PARSING_ERROR", "Failed to parse exposure record")));
                }
                
                ParsedFileData.ExposureRecord record = recordResult.getValue().orElseThrow();
                
                // Check for duplicate exposure_id
                if (seenExposureIds.contains(record.getExposureId())) {
                    return Result.failure(ErrorDetail.of("DUPLICATE_EXPOSURE_ID", 
                        String.format("Duplicate exposure_id '%s' found at line %d", record.getExposureId(), lineNumber)));
                }
                seenExposureIds.add(record.getExposureId());
                
                exposures.add(record);
            }
            
            if (exposures.isEmpty()) {
                return Result.failure(ErrorDetail.of("EMPTY_FILE", "File contains no exposure records"));
            }
            
            ParsedFileData result = ParsedFileData.builder()
                .exposures(exposures)
                .totalCount(exposures.size())
                .fileName(fileName)
                .contentType("application/json")
                .build();
                
            log.info("Successfully parsed JSON file: {} with {} exposures", fileName, exposures.size());
            return Result.success(result);
            
        } catch (IOException e) {
            log.error("IO error parsing JSON file: {}", fileName, e);
            return Result.failure(ErrorDetail.of("JSON_PARSING_ERROR", 
                "Failed to parse JSON file: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error parsing JSON file: {}", fileName, e);
            return Result.failure(ErrorDetail.of("PARSING_ERROR", 
                "Unexpected error parsing file: " + e.getMessage()));
        }
    }

    /**
     * Parse Excel file reading only the first worksheet
     */
    public Result<ParsedFileData> parseExcelFile(InputStream fileStream, String fileName) {
        log.info("Starting Excel file parsing for file: {}", fileName);
        
        try (Workbook workbook = new XSSFWorkbook(fileStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            
            if (workbook.getNumberOfSheets() > 1) {
                log.warn("Excel file {} has {} sheets, processing only the first sheet", 
                    fileName, workbook.getNumberOfSheets());
            }
            
            List<ParsedFileData.ExposureRecord> exposures = new ArrayList<>();
            Set<String> seenExposureIds = new HashSet<>();
            
            // Find header row and validate required columns
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return Result.failure(ErrorDetail.of("MISSING_HEADER", "Excel file must have a header row"));
            }
            
            Result<int[]> columnMappingResult = mapExcelColumns(headerRow);
            if (columnMappingResult.isFailure()) {
                return columnMappingResult.map(ignored -> null);
            }
            
            int[] columnMapping = columnMappingResult.getValue().orElseThrow();
            
            // Process data rows
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isEmptyRow(row)) {
                    continue;
                }
                
                if (exposures.size() >= MAX_EXPOSURES_PER_FILE) {
                    return Result.failure(ErrorDetail.of("FILE_TOO_LARGE", 
                        String.format("File contains more than %d exposures. Please split the file.", MAX_EXPOSURES_PER_FILE)));
                }
                
                Result<ParsedFileData.ExposureRecord> recordResult = parseExcelExposureRecord(row, columnMapping, rowIndex + 1);
                if (recordResult.isFailure()) {
                    return Result.failure(recordResult.getError().orElse(
                        ErrorDetail.of("PARSING_ERROR", "Failed to parse exposure record")));
                }
                
                ParsedFileData.ExposureRecord record = recordResult.getValue().orElseThrow();
                
                // Check for duplicate exposure_id
                if (seenExposureIds.contains(record.getExposureId())) {
                    return Result.failure(ErrorDetail.of("DUPLICATE_EXPOSURE_ID", 
                        String.format("Duplicate exposure_id '%s' found at row %d", record.getExposureId(), rowIndex + 1)));
                }
                seenExposureIds.add(record.getExposureId());
                
                exposures.add(record);
            }
            
            if (exposures.isEmpty()) {
                return Result.failure(ErrorDetail.of("EMPTY_FILE", "File contains no exposure records"));
            }
            
            ParsedFileData result = ParsedFileData.builder()
                .exposures(exposures)
                .totalCount(exposures.size())
                .fileName(fileName)
                .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .build();
                
            log.info("Successfully parsed Excel file: {} with {} exposures", fileName, exposures.size());
            return Result.success(result);
            
        } catch (IOException e) {
            log.error("IO error parsing Excel file: {}", fileName, e);
            return Result.failure(ErrorDetail.of("EXCEL_PARSING_ERROR", 
                "Failed to parse Excel file: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error parsing Excel file: {}", fileName, e);
            return Result.failure(ErrorDetail.of("PARSING_ERROR", 
                "Unexpected error parsing file: " + e.getMessage()));
        }
    }

    private Result<ParsedFileData.ExposureRecord> parseJsonExposureRecord(JsonParser parser, int lineNumber) throws IOException {
        String exposureId = null;
        BigDecimal amount = null;
        String currency = null;
        String country = null;
        String sector = null;
        
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = parser.getCurrentName();
            parser.nextToken();
            
            switch (fieldName) {
                case "exposure_id":
                    exposureId = parser.getValueAsString();
                    break;
                case "amount":
                    try {
                        amount = new BigDecimal(parser.getValueAsString());
                        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                            return Result.failure(ErrorDetail.of("INVALID_AMOUNT", 
                                String.format("Amount must be positive at line %d", lineNumber)));
                        }
                    } catch (NumberFormatException e) {
                        return Result.failure(ErrorDetail.of("INVALID_AMOUNT_FORMAT", 
                            String.format("Invalid amount format at line %d", lineNumber)));
                    }
                    break;
                case "currency":
                    currency = parser.getValueAsString();
                    break;
                case "country":
                    country = parser.getValueAsString();
                    break;
                case "sector":
                    sector = parser.getValueAsString();
                    break;
                default:
                    // Skip unknown fields
                    parser.skipChildren();
                    break;
            }
        }
        
        // Validate required fields
        if (exposureId == null || exposureId.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("MISSING_EXPOSURE_ID", 
                String.format("Missing or empty exposure_id at line %d", lineNumber)));
        }
        if (amount == null) {
            return Result.failure(ErrorDetail.of("MISSING_AMOUNT", 
                String.format("Missing amount at line %d", lineNumber)));
        }
        if (currency == null || currency.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("MISSING_CURRENCY", 
                String.format("Missing currency at line %d", lineNumber)));
        }
        if (country == null || country.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("MISSING_COUNTRY", 
                String.format("Missing country at line %d", lineNumber)));
        }
        if (sector == null || sector.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("MISSING_SECTOR", 
                String.format("Missing sector at line %d", lineNumber)));
        }
        
        return Result.success(ParsedFileData.ExposureRecord.builder()
            .exposureId(exposureId.trim())
            .amount(amount)
            .currency(currency.trim().toUpperCase())
            .country(country.trim().toUpperCase())
            .sector(sector.trim().toUpperCase())
            .lineNumber(lineNumber)
            .build());
    }

    private Result<int[]> mapExcelColumns(Row headerRow) {
        int[] columnMapping = new int[REQUIRED_FIELDS.length];
        boolean[] found = new boolean[REQUIRED_FIELDS.length];
        
        for (Cell cell : headerRow) {
            String headerValue = getCellValueAsString(cell).toLowerCase().trim();
            
            for (int i = 0; i < REQUIRED_FIELDS.length; i++) {
                if (REQUIRED_FIELDS[i].equals(headerValue)) {
                    columnMapping[i] = cell.getColumnIndex();
                    found[i] = true;
                    break;
                }
            }
        }
        
        // Check if all required columns are present
        for (int i = 0; i < REQUIRED_FIELDS.length; i++) {
            if (!found[i]) {
                return Result.failure(ErrorDetail.of("MISSING_COLUMN", 
                    String.format("Required column '%s' not found in Excel header", REQUIRED_FIELDS[i])));
            }
        }
        
        return Result.success(columnMapping);
    }

    private Result<ParsedFileData.ExposureRecord> parseExcelExposureRecord(Row row, int[] columnMapping, int rowNumber) {
        try {
            String exposureId = getCellValueAsString(row.getCell(columnMapping[0])).trim();
            String amountStr = getCellValueAsString(row.getCell(columnMapping[1])).trim();
            String currency = getCellValueAsString(row.getCell(columnMapping[2])).trim().toUpperCase();
            String country = getCellValueAsString(row.getCell(columnMapping[3])).trim().toUpperCase();
            String sector = getCellValueAsString(row.getCell(columnMapping[4])).trim().toUpperCase();
            
            // Validate required fields
            if (exposureId.isEmpty()) {
                return Result.failure(ErrorDetail.of("MISSING_EXPOSURE_ID", 
                    String.format("Missing or empty exposure_id at row %d", rowNumber)));
            }
            if (amountStr.isEmpty()) {
                return Result.failure(ErrorDetail.of("MISSING_AMOUNT", 
                    String.format("Missing amount at row %d", rowNumber)));
            }
            if (currency.isEmpty()) {
                return Result.failure(ErrorDetail.of("MISSING_CURRENCY", 
                    String.format("Missing currency at row %d", rowNumber)));
            }
            if (country.isEmpty()) {
                return Result.failure(ErrorDetail.of("MISSING_COUNTRY", 
                    String.format("Missing country at row %d", rowNumber)));
            }
            if (sector.isEmpty()) {
                return Result.failure(ErrorDetail.of("MISSING_SECTOR", 
                    String.format("Missing sector at row %d", rowNumber)));
            }
            
            // Parse and validate amount
            BigDecimal amount;
            try {
                amount = new BigDecimal(amountStr);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    return Result.failure(ErrorDetail.of("INVALID_AMOUNT", 
                        String.format("Amount must be positive at row %d", rowNumber)));
                }
            } catch (NumberFormatException e) {
                return Result.failure(ErrorDetail.of("INVALID_AMOUNT_FORMAT", 
                    String.format("Invalid amount format at row %d: %s", rowNumber, amountStr)));
            }
            
            return Result.success(ParsedFileData.ExposureRecord.builder()
                .exposureId(exposureId)
                .amount(amount)
                .currency(currency)
                .country(country)
                .sector(sector)
                .lineNumber(rowNumber)
                .build());
                
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of("ROW_PARSING_ERROR", 
                String.format("Error parsing row %d: %s", rowNumber, e.getMessage())));
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                } else {
                    // Format numeric values to avoid scientific notation
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == Math.floor(numericValue)) {
                        yield String.valueOf((long) numericValue);
                    } else {
                        yield String.valueOf(numericValue);
                    }
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    private boolean isEmptyRow(Row row) {
        for (Cell cell : row) {
            if (cell != null && !getCellValueAsString(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}