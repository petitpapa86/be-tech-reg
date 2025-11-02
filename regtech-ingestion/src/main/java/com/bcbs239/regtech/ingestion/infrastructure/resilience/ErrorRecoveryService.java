package com.bcbs239.regtech.ingestion.infrastructure.resilience;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.ingestion.domain.model.BatchId;
import com.bcbs239.regtech.ingestion.domain.model.BatchStatus;
import com.bcbs239.regtech.ingestion.domain.repository.IngestionBatchRepository;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for comprehensive error handling and recovery mechanisms.
 * Provides detailed error analysis, recovery strategies, and error enrichment.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ErrorRecoveryService {
    
    private final IngestionBatchRepository batchRepository;
    
    private static final Pattern JSON_ERROR_PATTERN = Pattern.compile(
        "at line (\\d+), column (\\d+)|line (\\d+)|column (\\d+)"
    );
    
    /**
     * Enriches JSON parsing errors with detailed line and column information.
     */
    public Result<Void> enrichJsonParsingError(Exception originalException, String fileName, BatchId batchId) {
        log.debug("Enriching JSON parsing error for file: {} batch: {}", fileName, batchId.value());
        
        List<ErrorDetail> enrichedErrors = new ArrayList<>();
        
        if (originalException instanceof JsonParseException jsonException) {
            JsonLocation location = jsonException.getLocation();
            
            String detailedMessage = String.format(
                "JSON parsing failed in file '%s' at line %d, column %d: %s",
                fileName,
                location.getLineNr(),
                location.getColumnNr(),
                jsonException.getOriginalMessage()
            );
            
            enrichedErrors.add(ErrorDetail.of("JSON_PARSE_ERROR", detailedMessage));
            
            // Add specific guidance based on common JSON errors
            String guidance = getJsonErrorGuidance(jsonException.getOriginalMessage());
            if (guidance != null) {
                enrichedErrors.add(ErrorDetail.of("JSON_ERROR_GUIDANCE", guidance));
            }
            
        } else if (originalException instanceof IOException ioException) {
            // Try to extract line information from IO exception message
            String message = ioException.getMessage();
            Matcher matcher = JSON_ERROR_PATTERN.matcher(message);
            
            if (matcher.find()) {
                String lineStr = matcher.group(1) != null ? matcher.group(1) : matcher.group(3);
                String columnStr = matcher.group(2) != null ? matcher.group(2) : matcher.group(4);
                
                String detailedMessage = String.format(
                    "JSON parsing failed in file '%s'%s%s: %s",
                    fileName,
                    lineStr != null ? " at line " + lineStr : "",
                    columnStr != null ? ", column " + columnStr : "",
                    message
                );
                
                enrichedErrors.add(ErrorDetail.of("JSON_IO_ERROR", detailedMessage));
            } else {
                enrichedErrors.add(ErrorDetail.of("JSON_IO_ERROR", 
                    String.format("JSON parsing failed in file '%s': %s", fileName, message)));
            }
        } else {
            enrichedErrors.add(ErrorDetail.of("JSON_PARSING_ERROR", 
                String.format("Unexpected error parsing JSON file '%s': %s", 
                             fileName, originalException.getMessage())));
        }
        
        // Record the enriched error in the batch
        recordBatchError(batchId, enrichedErrors);
        
        return Result.failure(enrichedErrors);
    }
    
    /**
     * Provides specific guidance for common JSON parsing errors.
     */
    private String getJsonErrorGuidance(String errorMessage) {
        String lowerMessage = errorMessage.toLowerCase();
        
        if (lowerMessage.contains("unexpected character") || lowerMessage.contains("invalid character")) {
            return "Check for invalid characters, ensure proper UTF-8 encoding, and verify quotes are properly escaped";
        } else if (lowerMessage.contains("unexpected end-of-input") || lowerMessage.contains("truncated")) {
            return "The JSON file appears to be incomplete or truncated. Please verify the file was uploaded completely";
        } else if (lowerMessage.contains("duplicate field") || lowerMessage.contains("duplicate key")) {
            return "Remove duplicate field names within the same JSON object";
        } else if (lowerMessage.contains("expected") && lowerMessage.contains("but found")) {
            return "Check JSON structure - ensure proper use of brackets [], braces {}, and commas";
        } else if (lowerMessage.contains("unrecognized token")) {
            return "Verify that string values are properly quoted and numeric values are valid";
        }
        
        return null;
    }
    
    /**
     * Enriches Excel parsing errors with detailed row and column information.
     */
    public Result<Void> enrichExcelParsingError(Exception originalException, String fileName, 
                                               BatchId batchId, Optional<Integer> rowNumber) {
        log.debug("Enriching Excel parsing error for file: {} batch: {}", fileName, batchId.value());
        
        List<ErrorDetail> enrichedErrors = new ArrayList<>();
        
        String baseMessage = String.format("Excel parsing failed in file '%s'", fileName);
        if (rowNumber.isPresent()) {
            baseMessage += String.format(" at row %d", rowNumber.get());
        }
        baseMessage += ": " + originalException.getMessage();
        
        enrichedErrors.add(ErrorDetail.of("EXCEL_PARSE_ERROR", baseMessage));
        
        // Add specific guidance based on common Excel errors
        String guidance = getExcelErrorGuidance(originalException);
        if (guidance != null) {
            enrichedErrors.add(ErrorDetail.of("EXCEL_ERROR_GUIDANCE", guidance));
        }
        
        // Record the enriched error in the batch
        recordBatchError(batchId, enrichedErrors);
        
        return Result.failure(enrichedErrors);
    }
    
    /**
     * Provides specific guidance for common Excel parsing errors.
     */
    private String getExcelErrorGuidance(Exception exception) {
        String message = exception.getMessage().toLowerCase();
        
        if (message.contains("invalid header signature") || message.contains("not a valid excel file")) {
            return "Ensure the file is a valid Excel (.xlsx) format and not corrupted";
        } else if (message.contains("missing header") || message.contains("required column")) {
            return "Verify that the first row contains all required column headers: exposure_id, amount, currency, country, sector";
        } else if (message.contains("number format") || message.contains("invalid amount")) {
            return "Check that amount values are valid positive numbers without special characters";
        } else if (message.contains("empty") || message.contains("missing")) {
            return "Ensure all required fields have values and there are no empty cells in required columns";
        }
        
        return "Please verify the Excel file format and data integrity";
    }
    
    /**
     * Handles Bank Registry service unavailability with cached fallback strategy.
     */
    public Result<String> handleBankRegistryUnavailability(String bankId, Exception originalException) {
        log.warn("Bank Registry service unavailable for bank {}: {}", bankId, originalException.getMessage());
        
        // This would typically check for cached bank info
        // For now, we'll provide a structured error with fallback guidance
        
        List<ErrorDetail> errors = new ArrayList<>();
        
        errors.add(ErrorDetail.of("BANK_REGISTRY_UNAVAILABLE", 
            String.format("Bank Registry service is currently unavailable: %s", originalException.getMessage())));
        
        errors.add(ErrorDetail.of("BANK_REGISTRY_FALLBACK_GUIDANCE", 
            "The system will attempt to use cached bank information if available. " +
            "If no cached data exists, please retry the upload after the service is restored."));
        
        // Add retry guidance
        errors.add(ErrorDetail.of("RETRY_GUIDANCE", 
            "This is typically a temporary issue. Please wait a few minutes and retry your upload."));
        
        return Result.failure(errors);
    }
    
    /**
     * Analyzes S3 upload failures and provides recovery strategies.
     */
    public Result<String> analyzeS3UploadFailure(Exception originalException, String batchId, int attemptNumber) {
        log.error("S3 upload failure analysis for batch {} attempt {}: {}", 
                 batchId, attemptNumber, originalException.getMessage());
        
        List<ErrorDetail> errors = new ArrayList<>();
        
        String errorType = classifyS3Error(originalException);
        errors.add(ErrorDetail.of("S3_UPLOAD_FAILURE_TYPE", errorType));
        
        String recoveryStrategy = getS3RecoveryStrategy(originalException, attemptNumber);
        errors.add(ErrorDetail.of("S3_RECOVERY_STRATEGY", recoveryStrategy));
        
        // Add specific error details
        errors.add(ErrorDetail.of("S3_UPLOAD_ERROR", 
            String.format("S3 upload failed for batch %s on attempt %d: %s", 
                         batchId, attemptNumber, originalException.getMessage())));
        
        return Result.failure(errors);
    }
    
    /**
     * Classifies S3 errors into categories for better handling.
     */
    private String classifyS3Error(Exception exception) {
        String message = exception.getMessage().toLowerCase();
        
        if (message.contains("timeout") || message.contains("connection")) {
            return "NETWORK_CONNECTIVITY_ISSUE";
        } else if (message.contains("access denied") || message.contains("forbidden")) {
            return "AUTHENTICATION_AUTHORIZATION_ISSUE";
        } else if (message.contains("bucket") && message.contains("not found")) {
            return "BUCKET_CONFIGURATION_ISSUE";
        } else if (message.contains("checksum") || message.contains("etag")) {
            return "DATA_INTEGRITY_ISSUE";
        } else if (message.contains("service unavailable") || message.contains("throttling")) {
            return "SERVICE_AVAILABILITY_ISSUE";
        } else {
            return "UNKNOWN_S3_ERROR";
        }
    }
    
    /**
     * Provides recovery strategies based on S3 error type and attempt number.
     */
    private String getS3RecoveryStrategy(Exception exception, int attemptNumber) {
        String errorType = classifyS3Error(exception);
        
        return switch (errorType) {
            case "NETWORK_CONNECTIVITY_ISSUE" -> 
                String.format("Network connectivity issue detected. Retry attempt %d will use exponential backoff. " +
                             "If this persists, check network connectivity and firewall settings.", attemptNumber + 1);
            
            case "AUTHENTICATION_AUTHORIZATION_ISSUE" -> 
                "Authentication or authorization issue detected. This error is not retryable. " +
                "Please verify AWS credentials and S3 bucket permissions.";
            
            case "BUCKET_CONFIGURATION_ISSUE" -> 
                "S3 bucket configuration issue detected. This error is not retryable. " +
                "Please verify the bucket exists and is properly configured.";
            
            case "DATA_INTEGRITY_ISSUE" -> 
                "Data integrity issue detected (checksum mismatch). This error is not retryable. " +
                "The file may be corrupted during upload. Please try uploading the file again.";
            
            case "SERVICE_AVAILABILITY_ISSUE" -> 
                String.format("S3 service availability issue detected. Retry attempt %d will use exponential backoff. " +
                             "This is typically temporary and should resolve automatically.", attemptNumber + 1);
            
            default -> 
                String.format("Unknown S3 error detected. Retry attempt %d will be attempted with exponential backoff.", 
                             attemptNumber + 1);
        };
    }
    
    /**
     * Records detailed error information in the batch record.
     */
    private void recordBatchError(BatchId batchId, List<ErrorDetail> errors) {
        try {
            // Combine all error messages into a detailed error description
            StringBuilder errorMessage = new StringBuilder();
            for (int i = 0; i < errors.size(); i++) {
                ErrorDetail error = errors.get(i);
                errorMessage.append(String.format("[%s] %s", error.getCode(), error.getMessage()));
                if (i < errors.size() - 1) {
                    errorMessage.append(" | ");
                }
            }
            
            // Update batch status to FAILED with detailed error message
            batchRepository.findByBatchId(batchId).ifPresent(batch -> {
                batch.markAsFailed(errorMessage.toString());
                batchRepository.save(batch);
                log.info("Recorded detailed error information for batch {}", batchId.value());
            });
            
        } catch (Exception e) {
            log.error("Failed to record error information for batch {}: {}", batchId.value(), e.getMessage());
        }
    }
    
    /**
     * Determines if an error is recoverable and should be retried.
     */
    public boolean isRecoverableError(Exception exception) {
        if (exception == null) {
            return false;
        }
        
        String message = exception.getMessage();
        if (message == null) {
            return false;
        }
        
        String lowerMessage = message.toLowerCase();
        
        // Non-recoverable errors
        if (lowerMessage.contains("access denied") ||
            lowerMessage.contains("authentication") ||
            lowerMessage.contains("authorization") ||
            lowerMessage.contains("bucket not found") ||
            lowerMessage.contains("checksum mismatch") ||
            lowerMessage.contains("validation") ||
            lowerMessage.contains("invalid format") ||
            lowerMessage.contains("parse error")) {
            return false;
        }
        
        // Recoverable errors
        return lowerMessage.contains("timeout") ||
               lowerMessage.contains("connection") ||
               lowerMessage.contains("service unavailable") ||
               lowerMessage.contains("throttling") ||
               lowerMessage.contains("temporary") ||
               lowerMessage.contains("retry");
    }
    
    /**
     * Provides user-friendly error messages with actionable guidance.
     */
    public String getUserFriendlyErrorMessage(List<ErrorDetail> errors) {
        if (errors == null || errors.isEmpty()) {
            return "An unknown error occurred during processing.";
        }
        
        StringBuilder message = new StringBuilder();
        message.append("Processing failed with the following issues:\n\n");
        
        for (int i = 0; i < errors.size(); i++) {
            ErrorDetail error = errors.get(i);
            message.append(String.format("%d. %s\n", i + 1, error.getMessage()));
            
            // Add guidance if available
            if (error.getCode().endsWith("_GUIDANCE")) {
                message.append("   → ").append(error.getMessage()).append("\n");
            }
        }
        
        message.append("\nPlease address these issues and try uploading your file again.");
        
        return message.toString();
    }
}