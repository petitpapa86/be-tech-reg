package com.bcbs239.regtech.ingestion.infrastructure.fileparsing;


import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.ingestion.domain.file.FileContent;
import com.bcbs239.regtech.ingestion.domain.model.LoanExposure;
import com.bcbs239.regtech.ingestion.domain.model.ParsedFileData;
import com.bcbs239.regtech.ingestion.domain.parsing.FileParsingService;
import com.bcbs239.regtech.ingestion.infrastructure.configuration.IngestionProperties;
import com.bcbs239.regtech.ingestion.infrastructure.performance.FileProcessingPerformanceOptimizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DefaultFileParsingService implements FileParsingService {

    private static final Logger log = LoggerFactory.getLogger(DefaultFileParsingService.class);

    private final FileToLoanExposureParser parser;
    private final FileProcessingPerformanceOptimizer optimizer;
    private final int defaultMaxRecords;

    public DefaultFileParsingService(FileToLoanExposureParser parser,
                                    FileProcessingPerformanceOptimizer optimizer,
                                    IngestionProperties ingestionProperties) {
        this.parser = parser;
        this.optimizer = optimizer;
        this.defaultMaxRecords = ingestionProperties.parser().defaultMaxRecords();
    }

    @Override
    public Result<ParsedFileData> parseFileContent(FileContent fileContent) {
        log.info("Parsing file content: {}", fileContent);
        
        if (!fileContent.isSupportedFormat()) {
            return Result.failure(ErrorDetail.of(
                "UNSUPPORTED_FORMAT", 
                ErrorType.VALIDATION_ERROR,
                "Unsupported file format: " + fileContent.getFormat(), 
                "file.format.unsupported"
            ));
        }
        
        return switch (fileContent.getFormat()) {
            case TRANSACTION_JSON -> parseJsonFile(fileContent.getStream(), fileContent.getFileName().value());
            case BANK_STATEMENT_EXCEL -> parseExcelFile(fileContent.getStream(), fileContent.getFileName().value());
            default -> Result.failure(ErrorDetail.of(
                "UNSUPPORTED_FORMAT", 
                ErrorType.VALIDATION_ERROR, 
                "Format not yet implemented: " + fileContent.getFormat(), 
                "file.format.not.implemented"
            ));
        };
    }

    private Result<ParsedFileData> parseJsonFile(InputStream fileStream, String fileName) {
        try {
            int maxRecords = decideMaxRecords();
            
            // Parse bank info, loan exposures, and credit risk mitigations in a single pass
            // This is necessary because an InputStream can only be read once
            FileToLoanExposureParser.JsonParsingResult result = 
                parser.parseJsonToBothArrays(fileStream, maxRecords);
            
            Map<String,Object> metadata = new HashMap<>();
            metadata.put("sourceFileName", fileName);
            metadata.put("parsedRecordsLimit", maxRecords);

            ParsedFileData data = new ParsedFileData(result.bankInfo(), result.exposures(), result.crms(), metadata);
            return Result.success(data);
        } catch (Exception e) {
            log.error("Failed to parse JSON file {}: {}", fileName, e.getMessage(), e);
            return Result.failure(ErrorDetail.of("PARSING_ERROR", ErrorType.SYSTEM_ERROR, "Failed to parse JSON file: " + e.getMessage(), "file.parse.json.failed"));
        }
    }

    private Result<ParsedFileData> parseExcelFile(InputStream fileStream, String fileName) {
        try {
            int maxRecords = decideMaxRecords();
            List<LoanExposure> exposures = parser.parseExcelToLoanExposures(fileStream, maxRecords);

            Map<String,Object> metadata = new HashMap<>();
            metadata.put("sourceFileName", fileName);
            metadata.put("parsedRecordsLimit", maxRecords);

            ParsedFileData data = new ParsedFileData(null, exposures, List.of(), metadata);
            return Result.success(data);
        } catch (Exception e) {
            log.error("Failed to parse Excel file {}: {}", fileName, e.getMessage(), e);
            return Result.failure(ErrorDetail.of("PARSING_ERROR", ErrorType.SYSTEM_ERROR, "Failed to parse Excel file: " + e.getMessage(), "file.parse.excel.failed"));
        }
    }

    private int decideMaxRecords() {
        // If optimizer indicates we can accept more files, be generous; otherwise use defaultLimit/2
        if (optimizer.canAcceptMoreFiles()) {
            return defaultMaxRecords;
        } else {
            return Math.max(100, defaultMaxRecords / 10);
        }
    }
}
