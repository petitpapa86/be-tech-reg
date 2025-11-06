package com.bcbs239.regtech.ingestion.infrastructure.monitoring;

import com.bcbs239.regtech.core.domain.logging.ILogger;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankId;
import com.bcbs239.regtech.ingestion.domain.batch.BatchId;
import com.bcbs239.regtech.ingestion.domain.batch.BatchStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for structured logging in the ingestion module.
 * Uses existing LoggingConfiguration patterns with ingestion-specific context.
 * Masks PII and provides detailed trace information for request flows.
 */
@Service
@RequiredArgsConstructor
public class IngestionLoggingService {
    private final ILogger asyncLogger;

    // File processing logging
    public void logFileUploadStarted(BatchId batchId, BankId bankId, String fileName, long fileSizeBytes, String contentType) {
        Map<String, Object> details = new HashMap<>();
        details.put("batchId", batchId.value());
        details.put("bankId", maskBankId(bankId.value()));
        details.put("fileName", maskFileName(fileName));
        details.put("fileSizeBytes", fileSizeBytes);
        details.put("fileSizeMB", Math.round(fileSizeBytes / (1024.0 * 1024.0) * 100.0) / 100.0);
        details.put("contentType", contentType);
        details.put("phase", "upload");
        details.put("status", "started");
        asyncLogger.asyncStructuredLog("FILE_UPLOAD_STARTED", details);
    }
    
    public void logFileUploadCompleted(BatchId batchId, BankId bankId, long durationMs) {
        Map<String, Object> details = new HashMap<>();
        details.put("batchId", batchId.value());
        details.put("bankId", maskBankId(bankId.value()));
        details.put("durationMs", durationMs);
        details.put("phase", "upload");
        details.put("status", "completed");
        asyncLogger.asyncStructuredLog("FILE_UPLOAD_COMPLETED", details);
    }
    
    public void logFileProcessingStarted(BatchId batchId, BankId bankId, BatchStatus fromStatus, BatchStatus toStatus) {
        Map<String, Object> details = new HashMap<>();
        details.put("batchId", batchId.value());
        details.put("bankId", maskBankId(bankId.value()));
        details.put("fromStatus", fromStatus.name());
        details.put("toStatus", toStatus.name());
        details.put("phase", "processing");
        details.put("status", "started");
       // details.put("correlationId", LoggingConfiguration.getCurrentCorrelationId());
        asyncLogger.asyncStructuredLog("FILE_PROCESSING_STARTED", details);
    }
    
    public void logFileProcessingCompleted(BatchId batchId, BankId bankId, BatchStatus status, 
                                         long durationMs, Integer exposureCount) {
        Map<String, Object> details = new HashMap<>();
        details.put("batchId", batchId.value());
        details.put("bankId", maskBankId(bankId.value()));
        details.put("status", status.name());
        details.put("durationMs", durationMs);
        details.put("phase", "processing");
        details.put("result", "completed");
       // details.put("correlationId", LoggingConfiguration.getCurrentCorrelationId());
        
        if (exposureCount != null) {
            details.put("exposureCount", exposureCount);
            if (durationMs > 0) {
                double exposuresPerSecond = exposureCount / (durationMs / 1000.0);
                details.put("exposuresPerSecond", Math.round(exposuresPerSecond * 100.0) / 100.0);
            }
        }
        asyncLogger.asyncStructuredLog("FILE_PROCESSING_COMPLETED", details);
    }
    
    public void logFileProcessingFailed(BatchId batchId, BankId bankId, String errorType, 
                                      String errorMessage, long durationMs, Throwable throwable) {
        Map<String, Object> details = new HashMap<>();
        details.put("batchId", batchId.value());
        details.put("bankId", maskBankId(bankId.value()));
        details.put("errorType", errorType);
        details.put("errorMessage", maskSensitiveData(errorMessage));
        details.put("durationMs", durationMs);
        details.put("phase", "processing");
        details.put("result", "failed");
      //  details.put("correlationId", LoggingConfiguration.getCurrentCorrelationId());
        
        LoggingConfiguration.logError("file_processing", errorType, errorMessage, throwable, details);
    }
    
    // File parsing logging
    public void logFileParsingStarted(BatchId batchId, String fileType, long fileSizeBytes) {
        Map<String, Object> details = new HashMap<>();
        details.put("batchId", batchId.value());
        details.put("fileType", fileType);
        details.put("fileSizeBytes", fileSizeBytes);
        details.put("phase", "parsing");
        details.put("status", "started");
        details.put("correlationId", LoggingConfiguration.getCurrentCorrelationId());
        
        LoggingConfiguration.logStructured("FILE_PARSING_STARTED", details);
    }
    
    public void logFileParsingCompleted(BatchId batchId, String fileType, int recordCount, 
                                      long durationMs, Map<String, Object> parseMetrics) {
        Map<String, Object> details = new HashMap<>();
        details.put("batchId", batchId.value());
        details.put("fileType", fileType);
        details.put("recordCount", recordCount);
        details.put("durationMs", durationMs);
        details.put("phase", "parsing");
        details.put("status", "completed");
        details.put("correlationId", LoggingConfiguration.getCurrentCorrelationId());
        
        if (parseMetrics != null) {
            details.putAll(parseMetrics);
        }
        
        if (durationMs > 0 && recordCount > 0) {
            double recordsPerSecond = recordCount / (durationMs / 1000.0);
            details.put("recordsPerSecond", Math.round(recordsPerSecond * 100.0) / 100.0);
        }
        
        LoggingConfiguration.logStructured("FILE_PARSING_COMPLETED", details);
    }
    
    public void logFileParsingFailed(BatchId batchId, String fileType, String errorType, 
                                   String errorMessage, long durationMs, Throwable throwable) {
        Map<String, Object> details = new HashMap<>();
        details.put("batchId", batchId.value());
        details.put("fileType", fileType);
        details.put("errorType", errorType);
        details.put("errorMessage", maskSensitiveData(errorMessage));
        details.put("durationMs", durationMs);
        details.put("phase", "parsing");
        details.put("status", "failed");
        details.put("correlationId", LoggingConfiguration.getCurrentCorrelationId());
        
        LoggingConfiguration.logError("file_parsing", errorType, errorMessage, throwable, details);
    }
    
    // File validation logging
    public void logFileValidationStarted(BatchId batchId, String validationType, int recordCount) {
        Map<String, Object> details = new HashMap<>();
        details.put("batchId", batchId.value());
        details.put("validationType", validationType);
        details.put("recordCount", recordCount);
        details.put("phase", "validation");
        details.put("status", "started");
        details.put("correlationId", LoggingConfiguration.getCurrentCorrelationId());
        
        LoggingConfiguration.logStructured("FILE_VALIDATION_STARTED", details);
    }
    
    public void logFileValidationCompleted(BatchId batchId, String validationType, int validRecords, 
                                         int invalidRecords, long durationMs) {
        Map<String, Object> details = new HashMap<>();
        details.put("batchId", batchId.value());
        details.put("validationType", validationType);
        details.put("validRecords", validRecords);
        details.put("invalidRecords", invalidRecords);
        details.put("totalRecords", validRecords + invalidRecords);
        details.put("validationSuccessRate", (validRecords + invalidRecords) > 0 ? 
            (double) validRecords / (validRecords + invalidRecords) : 1.0);
        details.put("durationMs", durationMs);
        details.put("phase", "validation");
        details.put("status", "completed");
        details.put("correlationId", LoggingConfiguration.getCurrentCorrelationId());
        
        LoggingConfiguration.logStructured("FILE_VALIDATION_COMPLETED", details);
    }
    
    public void logFileValidationFailed(BatchId batchId, String validationType, String errorType, 
                                      String errorMessage, long durationMs, Throwable throwable) {
        Map<String, Object> details = new HashMap<>();
        details.put("batchId", batchId.value());
        details.put("validationType", validationType);
        details.put("errorType", errorType);
        details.put("errorMessage", maskSensitiveData(errorMessage));
        details.put("durationMs", durationMs);
        details.put("phase", "validation");
        details.put("status", "failed");
        details.put("correlationId", LoggingConfiguration.getCurrentCorrelationId());
        
        LoggingConfiguration.logError("file_validation", errorType, errorMessage, throwable, details);
    }
    
    // S3 operations logging
    public void logS3OperationStarted(BatchId batchId, BankId bankId, String operation, 
                                    String s3Key, long fileSizeBytes) {
        Map<String, Object> details = new HashMap<>();
        details.put("batchId", batchId.value());
        details.put("bankId", maskBankId(bankId.value()));
        details.put("operation", operation);
        details.put("s3Key", maskS3Key(s3Key));
        details.put("fileSizeBytes", fileSizeBytes);
        details.put("phase", "s3_operation");
        details.put("status", "started");
        details.put("correlationId", LoggingConfiguration.getCurrentCorrelationId());
        
        LoggingConfiguration.logStructured("S3_OPERATION_STARTED", details);
    }
    
    public void logS3OperationCompleted(BatchId batchId, BankId bankId, String operation, 
                                      String s3Uri, long durationMs, String etag) {
        Map<String, Object> details = new HashMap<>();
        details.put("batchId", batchId.value());
        details.put("bankId", maskBankId(bankId.value()));
        details.put("operation", operation);
        details.put("s3Uri", maskS3Uri(s3Uri));
        details.put("durationMs", durationMs);
        details.put("etag", etag != null ? etag.substring(0, Math.min(8, etag.length())) + "..." : null);
        details.put("phase", "s3_operation");
        details.put("status", "completed");
        details.put("correlationId", LoggingConfiguration.getCurrentCorrelationId());
        
        LoggingConfiguration.logStructured("S3_OPERATION_COMPLETED", details);
    }
    
    public void logS3OperationFailed(BatchId batchId, BankId bankId, String operation, 
                                   String errorType, String errorMessage, long durationMs, Throwable throwable) {
        Map<String, Object> details = new HashMap<>();
        details.put("batchId", batchId.value());
        details.put("bankId", maskBankId(bankId.value()));
        details.put("operation", operation);
        details.put("errorType", errorType);
        details.put("errorMessage", maskSensitiveData(errorMessage));
        details.put("durationMs", durationMs);
        details.put("phase", "s3_operation");
        details.put("status", "failed");
        details.put("correlationId", LoggingConfiguration.getCurrentCorrelationId());
        
        LoggingConfiguration.logError("s3_operation", errorType, errorMessage, throwable, details);
    }
    
    // Database operations logging
    public void logDatabaseOperationStarted(String operation, String queryType, Map<String, Object> parameters) {
        Map<String, Object> details = new HashMap<>();
        details.put("operation", operation);
        details.put("queryType", queryType);
        details.put("phase", "database_operation");
        details.put("status", "started");
        details.put("correlationId", LoggingConfiguration.getCurrentCorrelationId());
        
        // Mask sensitive parameters
        if (parameters != null) {
            Map<String, Object> maskedParams = new HashMap<>();
            parameters.forEach((key, value) -> {
                if (key.toLowerCase().contains("password") || key.toLowerCase().contains("token")) {
                    maskedParams.put(key, "[MASKED]");
                } else {
                    maskedParams.put(key, value);
                }
            });
            details.put("parameters", maskedParams);
        }
        
        LoggingConfiguration.logStructured("DATABASE_OPERATION_STARTED", details);
    }
    
    public void logDatabaseOperationCompleted(String operation, String queryType, int recordCount, long durationMs) {
        Map<String, Object> details = new HashMap<>();
        details.put("operation", operation);
        details.put("queryType", queryType);
        details.put("recordCount", recordCount);
        details.put("durationMs", durationMs);
        details.put("phase", "database_operation");
        details.put("status", "completed");
        details.put("correlationId", LoggingConfiguration.getCurrentCorrelationId());
        
        LoggingConfiguration.logStructured("DATABASE_OPERATION_COMPLETED", details);
    }
    
    public void logDatabaseOperationFailed(String operation, String queryType, String errorType, 
                                         String errorMessage, long durationMs, Throwable throwable) {
        Map<String, Object> details = new HashMap<>();
        details.put("operation", operation);
        details.put("queryType", queryType);
        details.put("errorType", errorType);
        details.put("errorMessage", maskSensitiveData(errorMessage));
        details.put("durationMs", durationMs);
        details.put("phase", "database_operation");
        details.put("status", "failed");
        details.put("correlationId", LoggingConfiguration.getCurrentCorrelationId());
        
        LoggingConfiguration.logError("database_operation", errorType, errorMessage, throwable, details);
    }
    
    // Bank enrichment logging
    public void logBankEnrichmentStarted(BatchId batchId, BankId bankId, boolean useCache) {
        Map<String, Object> details = new HashMap<>();
        details.put("batchId", batchId.value());
        details.put("bankId", maskBankId(bankId.value()));
        details.put("useCache", useCache);
        details.put("phase", "bank_enrichment");
        details.put("status", "started");
        details.put("correlationId", LoggingConfiguration.getCurrentCorrelationId());
        
        LoggingConfiguration.logStructured("BANK_ENRICHMENT_STARTED", details);
    }
    
    public void logBankEnrichmentCompleted(BatchId batchId, BankId bankId, boolean fromCache, 
                                         String bankName, String bankCountry, long durationMs) {
        Map<String, Object> details = new HashMap<>();
        details.put("batchId", batchId.value());
        details.put("bankId", maskBankId(bankId.value()));
        details.put("fromCache", fromCache);
        details.put("bankName", maskBankName(bankName));
        details.put("bankCountry", bankCountry);
        details.put("durationMs", durationMs);
        details.put("phase", "bank_enrichment");
        details.put("status", "completed");
        details.put("correlationId", LoggingConfiguration.getCurrentCorrelationId());
        
        LoggingConfiguration.logStructured("BANK_ENRICHMENT_COMPLETED", details);
    }
    
    public void logBankEnrichmentFailed(BatchId batchId, BankId bankId, String errorType, 
                                      String errorMessage, long durationMs, Throwable throwable) {
        Map<String, Object> details = new HashMap<>();
        details.put("batchId", batchId.value());
        details.put("bankId", maskBankId(bankId.value()));
        details.put("errorType", errorType);
        details.put("errorMessage", maskSensitiveData(errorMessage));
        details.put("durationMs", durationMs);
        details.put("phase", "bank_enrichment");
        details.put("status", "failed");
        details.put("correlationId", LoggingConfiguration.getCurrentCorrelationId());
        
        LoggingConfiguration.logError("bank_enrichment", errorType, errorMessage, throwable, details);
    }
    
    // Event publishing logging
    public void logEventPublishingStarted(BatchId batchId, String eventType, Map<String, Object> eventPayload) {
        Map<String, Object> details = new HashMap<>();
        details.put("batchId", batchId.value());
        details.put("eventType", eventType);
        details.put("phase", "event_publishing");
        details.put("status", "started");
        details.put("correlationId", LoggingConfiguration.getCurrentCorrelationId());
        
        // Include non-sensitive payload information
        if (eventPayload != null) {
            Map<String, Object> maskedPayload = new HashMap<>();
            eventPayload.forEach((key, value) -> {
                if (key.equals("bankId")) {
                    maskedPayload.put(key, maskBankId(String.valueOf(value)));
                } else if (key.equals("s3Uri")) {
                    maskedPayload.put(key, maskS3Uri(String.valueOf(value)));
                } else {
                    maskedPayload.put(key, value);
                }
            });
            details.put("eventPayload", maskedPayload);
        }
        
        LoggingConfiguration.logStructured("EVENT_PUBLISHING_STARTED", details);
    }
    
    public void logEventPublishingCompleted(BatchId batchId, String eventType, String outboxMessageId, long durationMs) {
        Map<String, Object> details = new HashMap<>();
        details.put("batchId", batchId.value());
        details.put("eventType", eventType);
        details.put("outboxMessageId", outboxMessageId);
        details.put("durationMs", durationMs);
        details.put("phase", "event_publishing");
        details.put("status", "completed");
        details.put("correlationId", LoggingConfiguration.getCurrentCorrelationId());
        
        LoggingConfiguration.logStructured("EVENT_PUBLISHING_COMPLETED", details);
    }
    
    public void logEventPublishingFailed(BatchId batchId, String eventType, String errorType, 
                                       String errorMessage, long durationMs, Throwable throwable) {
        Map<String, Object> details = new HashMap<>();
        details.put("batchId", batchId.value());
        details.put("eventType", eventType);
        details.put("errorType", errorType);
        details.put("errorMessage", maskSensitiveData(errorMessage));
        details.put("durationMs", durationMs);
        details.put("phase", "event_publishing");
        details.put("status", "failed");
        details.put("correlationId", LoggingConfiguration.getCurrentCorrelationId());
        
        LoggingConfiguration.logError("event_publishing", errorType, errorMessage, throwable, details);
    }
    
    // Request flow tracing
    public void logRequestFlowStep(String step, String operation, Map<String, Object> context) {
        Map<String, Object> details = new HashMap<>();
        details.put("step", step);
        details.put("operation", operation);
        details.put("timestamp", Instant.now().toString());
        details.put("correlationId", LoggingConfiguration.getCurrentCorrelationId());
        
        if (context != null) {
            // Mask sensitive context data
            Map<String, Object> maskedContext = new HashMap<>();
            context.forEach((key, value) -> {
                if (key.equals("bankId")) {
                    maskedContext.put(key, maskBankId(String.valueOf(value)));
                } else if (key.toLowerCase().contains("password") || key.toLowerCase().contains("token")) {
                    maskedContext.put(key, "[MASKED]");
                } else {
                    maskedContext.put(key, value);
                }
            });
            details.put("context", maskedContext);
        }
        
        LoggingConfiguration.logStructured("REQUEST_FLOW_STEP", details);
    }
    
    // PII masking methods
    private String maskBankId(String bankId) {
        if (bankId == null || bankId.length() <= 4) {
            return bankId;
        }
        return bankId.substring(0, 2) + "***" + bankId.substring(bankId.length() - 2);
    }
    
    private String maskFileName(String fileName) {
        if (fileName == null) {
            return null;
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            String name = fileName.substring(0, dotIndex);
            String extension = fileName.substring(dotIndex);
            if (name.length() <= 6) {
                return name + extension;
            }
            return name.substring(0, 3) + "***" + name.substring(name.length() - 3) + extension;
        }
        if (fileName.length() <= 6) {
            return fileName;
        }
        return fileName.substring(0, 3) + "***" + fileName.substring(fileName.length() - 3);
    }
    
    private String maskBankName(String bankName) {
        if (bankName == null || bankName.length() <= 6) {
            return bankName;
        }
        return bankName.substring(0, 3) + "***" + bankName.substring(bankName.length() - 3);
    }
    
    private String maskS3Key(String s3Key) {
        if (s3Key == null) {
            return null;
        }
        // Show bucket and first/last parts of key
        String[] parts = s3Key.split("/");
        if (parts.length <= 2) {
            return s3Key;
        }
        return parts[0] + "/***/" + parts[parts.length - 1];
    }
    
    private String maskS3Uri(String s3Uri) {
        if (s3Uri == null) {
            return null;
        }
        // Extract key from URI and mask it
        if (s3Uri.contains("/")) {
            String[] parts = s3Uri.split("/");
            if (parts.length > 3) {
                return parts[0] + "//" + parts[2] + "/***/" + parts[parts.length - 1];
            }
        }
        return s3Uri;
    }
    
    private String maskSensitiveData(String data) {
        if (data == null) {
            return null;
        }
        // Mask potential sensitive information in error messages
        return data.replaceAll("(?i)(password|token|key|secret)=[^\\s,]+", "$1=[MASKED]")
                  .replaceAll("(?i)(bankId|bank_id)=[^\\s,]+", "$1=[MASKED]");
    }
}



