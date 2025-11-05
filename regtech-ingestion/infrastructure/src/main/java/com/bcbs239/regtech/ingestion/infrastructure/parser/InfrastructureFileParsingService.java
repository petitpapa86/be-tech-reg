package com.bcbs239.regtech.ingestion.infrastructure.parser;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.ingestion.application.batch.process.ProcessBatchCommandHandler;
import com.bcbs239.regtech.ingestion.application.model.ParsedFileData;
import com.bcbs239.regtech.ingestion.domain.model.CreditRiskMitigation;
import com.bcbs239.regtech.ingestion.domain.model.LoanExposure;
import com.bcbs239.regtech.ingestion.infrastructure.performance.FileProcessingPerformanceOptimizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InfrastructureFileParsingService implements ProcessBatchCommandHandler.FileParsingService {

    private static final Logger log = LoggerFactory.getLogger(InfrastructureFileParsingService.class);

    private final FileToLoanExposureParser parser;
    private final FileProcessingPerformanceOptimizer optimizer;

    // capacity policy: default max records to parse per file if optimizer indicates limited capacity
    @Value("${ingestion.parser.default-max-records:10000}")
    private int defaultMaxRecords;

    public InfrastructureFileParsingService(FileToLoanExposureParser parser,
                                           FileProcessingPerformanceOptimizer optimizer) {
        this.parser = parser;
        this.optimizer = optimizer;
    }

    @Override
    public Result<ParsedFileData> parseJsonFile(InputStream fileStream, String fileName) {
        try {
            int maxRecords = decideMaxRecords();
            List<LoanExposure> exposures = parser.parseLoanExposuresFromJson(fileStream, maxRecords);
            List<CreditRiskMitigation> crms = parser.parseCreditRiskMitigationFromJson(fileStream, maxRecords);

            Map<String,Object> metadata = new HashMap<>();
            metadata.put("sourceFileName", fileName);
            metadata.put("parsedRecordsLimit", maxRecords);

            ParsedFileData data = new ParsedFileData(exposures, crms, metadata);
            return Result.success(data);
        } catch (Exception e) {
            log.error("Failed to parse JSON file {}: {}", fileName, e.getMessage(), e);
            return Result.failure(new ErrorDetail("PARSING_ERROR", "Failed to parse JSON file: " + e.getMessage()));
        }
    }

    @Override
    public Result<ParsedFileData> parseExcelFile(InputStream fileStream, String fileName) {
        try {
            int maxRecords = decideMaxRecords();
            List<LoanExposure> exposures = parser.parseLoanExposuresFromExcel(fileStream, maxRecords);

            Map<String,Object> metadata = new HashMap<>();
            metadata.put("sourceFileName", fileName);
            metadata.put("parsedRecordsLimit", maxRecords);

            ParsedFileData data = new ParsedFileData(exposures, List.of(), metadata);
            return Result.success(data);
        } catch (Exception e) {
            log.error("Failed to parse Excel file {}: {}", fileName, e.getMessage(), e);
            return Result.failure(new ErrorDetail("PARSING_ERROR", "Failed to parse Excel file: " + e.getMessage()));
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


