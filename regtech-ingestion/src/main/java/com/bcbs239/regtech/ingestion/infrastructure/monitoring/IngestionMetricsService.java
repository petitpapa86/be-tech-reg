package com.bcbs239.regtech.ingestion.infrastructure.monitoring;

import com.bcbs239.regtech.core.config.LoggingConfiguration;
import com.bcbs239.regtech.ingestion.web.constants.Tags;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Tag;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;

/**
 * Service for collecting and emitting ingestion-specific metrics.
 * Tracks file processing performance, S3 operations, and database operations.
 */
@Service
public class IngestionMetricsService {

    private final MeterRegistry meterRegistry;
    
    // File processing metrics
    private final Counter filesUploadedCounter;
    private final Counter filesProcessedCounter;
    private final Counter filesFailedCounter;
    private final Timer fileProcessingTimer;
    private final Timer fileParsingTimer;
    private final Timer fileValidationTimer;
    
    // S3 operation metrics
    private final Counter s3UploadsCounter;
    private final Counter s3UploadFailuresCounter;
    private final Timer s3UploadTimer;
    private final Counter s3DownloadsCounter;
    private final Counter s3DownloadFailuresCounter;
    private final Timer s3DownloadTimer;
    
    // Database operation metrics
    private final Timer databaseQueryTimer;
    private final Counter databaseConnectionFailuresCounter;
    
    // File size and throughput metrics
    private final AtomicLong totalFileSizeProcessed = new AtomicLong(0);
    private final AtomicLong totalExposuresProcessed = new AtomicLong(0);
    private final Map<String, AtomicLong> fileSizeByBank = new ConcurrentHashMap<>();
    
    public IngestionMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize file processing metrics with common tags
        this.filesUploadedCounter = Counter.builder("ingestion.files.uploaded")
                .description("Total number of files uploaded")
                .tag("operation", "upload")
                .tag("component", Tags.FILE_UPLOAD.toLowerCase())
                .register(meterRegistry);
                
        this.filesProcessedCounter = Counter.builder("ingestion.files.processed")
                .description("Total number of files successfully processed")
                .tag("operation", "process")
                .tag("component", Tags.BATCH_PROCESSING.toLowerCase())
                .register(meterRegistry);
                
        this.filesFailedCounter = Counter.builder("ingestion.files.failed")
                .description("Total number of files that failed processing")
                .tag("operation", "process")
                .tag("status", "failed")
                .register(meterRegistry);
                
        this.fileProcessingTimer = Timer.builder("ingestion.file.processing.duration")
                .description("Time taken to process files end-to-end")
                .tag("operation", "process")
                .register(meterRegistry);
                
        this.fileParsingTimer = Timer.builder("ingestion.file.parsing.duration")
                .description("Time taken to parse file content")
                .tag("operation", "parse")
                .register(meterRegistry);
                
        this.fileValidationTimer = Timer.builder("ingestion.file.validation.duration")
                .description("Time taken to validate file content")
                .tag("operation", "validate")
                .register(meterRegistry);
        
        // Initialize S3 operation metrics with common tags
        this.s3UploadsCounter = Counter.builder("ingestion.s3.operations")
                .description("Total number of S3 operations")
                .tag("operation", "upload")
                .tag("status", "success")
                .tag("component", "s3")
                .register(meterRegistry);
                
        this.s3UploadFailuresCounter = Counter.builder("ingestion.s3.operations")
                .description("Total number of failed S3 operations")
                .tag("operation", "upload")
                .tag("status", "failure")
                .tag("component", "s3")
                .register(meterRegistry);
                
        this.s3UploadTimer = Timer.builder("ingestion.s3.operation.duration")
                .description("Time taken for S3 operations")
                .tag("operation", "upload")
                .tag("component", "s3")
                .register(meterRegistry);
                
        this.s3DownloadsCounter = Counter.builder("ingestion.s3.operations")
                .description("Total number of S3 operations")
                .tag("operation", "download")
                .tag("status", "success")
                .tag("component", "s3")
                .register(meterRegistry);
                
        this.s3DownloadFailuresCounter = Counter.builder("ingestion.s3.operations")
                .description("Total number of failed S3 operations")
                .tag("operation", "download")
                .tag("status", "failure")
                .tag("component", "s3")
                .register(meterRegistry);
                
        this.s3DownloadTimer = Timer.builder("ingestion.s3.operation.duration")
                .description("Time taken for S3 operations")
                .tag("operation", "download")
                .tag("component", "s3")
                .register(meterRegistry);
        
        // Initialize database operation metrics with common tags
        this.databaseQueryTimer = Timer.builder("ingestion.database.operation.duration")
                .description("Time taken for database operations")
                .tag("operation", "query")
                .tag("component", "database")
                .register(meterRegistry);
                
        this.databaseConnectionFailuresCounter = Counter.builder("ingestion.database.operations")
                .description("Total number of database operations")
                .tag("operation", "connection")
                .tag("status", "failure")
                .tag("component", "database")
                .register(meterRegistry);
        
        // Register gauges for cumulative metrics
        Gauge.builder("ingestion.total.file.size.bytes")
                .description("Total size of all processed files in bytes")
                .register(meterRegistry, this, IngestionMetricsService::getTotalFileSizeProcessed);
                
        Gauge.builder("ingestion.total.exposures.processed")
                .description("Total number of exposures processed")
                .register(meterRegistry, this, IngestionMetricsService::getTotalExposuresProcessed);
    }
    
    // Helper method to create consistent tags
    private List<Tag> createCommonTags(String bankId, String operation, String status) {
        return List.of(
            Tag.of("bankId", bankId != null ? bankId : "unknown"),
            Tag.of("operation", operation),
            Tag.of("status", status)
        );
    }
    
    private List<Tag> createComponentTags(String component, String operation) {
        return List.of(
            Tag.of("component", component),
            Tag.of("operation", operation)
        );
    }
    
    // File processing metrics methods
    public void recordFileUploaded(String bankId, long fileSizeBytes, String contentType) {
        filesUploadedCounter.increment();
        totalFileSizeProcessed.addAndGet(fileSizeBytes);
        fileSizeByBank.computeIfAbsent(bankId, k -> new AtomicLong(0)).addAndGet(fileSizeBytes);
        
        LoggingConfiguration.logStructured("FILE_UPLOADED_METRIC", Map.of(
            "bankId", bankId,
            "fileSizeBytes", fileSizeBytes,
            "contentType", contentType,
            "totalFileSizeProcessed", totalFileSizeProcessed.get()
        ));
    }
    
    public void recordFileProcessed(String bankId, long processingDurationMs, int exposureCount) {
        filesProcessedCounter.increment();
        totalExposuresProcessed.addAndGet(exposureCount);
        
        LoggingConfiguration.logStructured("FILE_PROCESSED_METRIC", Map.of(
            "bankId", bankId,
            "processingDurationMs", processingDurationMs,
            "exposureCount", exposureCount,
            "totalExposuresProcessed", totalExposuresProcessed.get()
        ));
    }
    
    public void recordFileFailed(String bankId, String errorType, String errorMessage) {
        filesFailedCounter.increment();
        
        LoggingConfiguration.logStructured("FILE_FAILED_METRIC", Map.of(
            "bankId", bankId,
            "errorType", errorType,
            "errorMessage", errorMessage
        ));
    }
    
    public Timer.Sample startFileProcessingTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordFileProcessingTime(Timer.Sample sample, String bankId, String status) {
        sample.stop(Timer.builder("ingestion.file.processing.duration")
                .tags(createCommonTags(bankId, "process", status))
                .register(meterRegistry));
    }
    
    public Timer.Sample startFileParsingTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordFileParsingTime(Timer.Sample sample, String fileType, String status) {
        sample.stop(Timer.builder("ingestion.file.parsing.duration")
                .tags(createComponentTags("parser", "parse"))
                .tag("fileType", fileType)
                .tag("status", status)
                .register(meterRegistry));
    }
    
    public Timer.Sample startFileValidationTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordFileValidationTime(Timer.Sample sample, String validationType, String status) {
        sample.stop(Timer.builder("ingestion.file.validation.duration")
                .tags(createComponentTags("validator", "validate"))
                .tag("validationType", validationType)
                .tag("status", status)
                .register(meterRegistry));
    }
    
    // S3 operation metrics methods
    public void recordS3Upload(String bankId, long fileSizeBytes, long durationMs, boolean success) {
        if (success) {
            s3UploadsCounter.increment();
        } else {
            s3UploadFailuresCounter.increment();
        }
        
        LoggingConfiguration.logStructured("S3_UPLOAD_METRIC", Map.of(
            "bankId", bankId,
            "fileSizeBytes", fileSizeBytes,
            "durationMs", durationMs,
            "success", success
        ));
    }
    
    public Timer.Sample startS3UploadTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordS3UploadTime(Timer.Sample sample, String bankId, String status) {
        sample.stop(Timer.builder("ingestion.s3.operation.duration")
                .tags(createCommonTags(bankId, "upload", status))
                .tag("component", "s3")
                .register(meterRegistry));
    }
    
    public void recordS3Download(String bankId, long fileSizeBytes, long durationMs, boolean success) {
        if (success) {
            s3DownloadsCounter.increment();
        } else {
            s3DownloadFailuresCounter.increment();
        }
        
        LoggingConfiguration.logStructured("S3_DOWNLOAD_METRIC", Map.of(
            "bankId", bankId,
            "fileSizeBytes", fileSizeBytes,
            "durationMs", durationMs,
            "success", success
        ));
    }
    
    public Timer.Sample startS3DownloadTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordS3DownloadTime(Timer.Sample sample, String bankId, String status) {
        sample.stop(Timer.builder("ingestion.s3.operation.duration")
                .tags(createCommonTags(bankId, "download", status))
                .tag("component", "s3")
                .register(meterRegistry));
    }
    
    // Database operation metrics methods
    public Timer.Sample startDatabaseQueryTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordDatabaseQueryTime(Timer.Sample sample, String queryType, String status) {
        sample.stop(Timer.builder("ingestion.database.operation.duration")
                .tags(createComponentTags("database", "query"))
                .tag("queryType", queryType)
                .tag("status", status)
                .register(meterRegistry));
    }
    
    public void recordDatabaseConnectionFailure(String operation, String errorMessage) {
        databaseConnectionFailuresCounter.increment();
        
        LoggingConfiguration.logStructured("DATABASE_CONNECTION_FAILURE_METRIC", Map.of(
            "operation", operation,
            "errorMessage", errorMessage
        ));
    }
    
    // Success rate calculations
    public double getFileProcessingSuccessRate() {
        double total = filesProcessedCounter.count() + filesFailedCounter.count();
        return total > 0 ? filesProcessedCounter.count() / total : 1.0;
    }
    
    public double getS3UploadSuccessRate() {
        double total = s3UploadsCounter.count() + s3UploadFailuresCounter.count();
        return total > 0 ? s3UploadsCounter.count() / total : 1.0;
    }
    
    public double getS3DownloadSuccessRate() {
        double total = s3DownloadsCounter.count() + s3DownloadFailuresCounter.count();
        return total > 0 ? s3DownloadsCounter.count() / total : 1.0;
    }
    
    // Gauge accessor methods
    public long getTotalFileSizeProcessed() {
        return totalFileSizeProcessed.get();
    }
    
    public long getTotalExposuresProcessed() {
        return totalExposuresProcessed.get();
    }
    
    public long getFileSizeByBank(String bankId) {
        return fileSizeByBank.getOrDefault(bankId, new AtomicLong(0)).get();
    }
    
    // Performance metrics for monitoring dashboards
    public Map<String, Object> getPerformanceSnapshot() {
        return Map.of(
            "filesUploaded", filesUploadedCounter.count(),
            "filesProcessed", filesProcessedCounter.count(),
            "filesFailed", filesFailedCounter.count(),
            "fileProcessingSuccessRate", getFileProcessingSuccessRate(),
            "s3UploadSuccessRate", getS3UploadSuccessRate(),
            "s3DownloadSuccessRate", getS3DownloadSuccessRate(),
            "totalFileSizeProcessedMB", getTotalFileSizeProcessed() / (1024.0 * 1024.0),
            "totalExposuresProcessed", getTotalExposuresProcessed(),
            "avgFileProcessingTimeMs", fileProcessingTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS),
            "avgS3UploadTimeMs", s3UploadTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS)
        );
    }
}