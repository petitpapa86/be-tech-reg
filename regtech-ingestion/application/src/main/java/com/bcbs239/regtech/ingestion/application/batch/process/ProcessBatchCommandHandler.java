package com.bcbs239.regtech.ingestion.application.batch.process;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.ingestion.application.model.ParsedFileData;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankId;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankInfo;
import com.bcbs239.regtech.ingestion.domain.batch.FileMetadata;
import com.bcbs239.regtech.ingestion.domain.batch.IIngestionBatchRepository;
import com.bcbs239.regtech.ingestion.domain.batch.IngestionBatch;
import com.bcbs239.regtech.ingestion.domain.batch.S3Reference;
import com.bcbs239.regtech.ingestion.domain.integrationevents.BatchIngestedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Instant;

/**
 * Command handler for processing batches asynchronously.
 * Handles the complete processing pipeline: parsing, validation, enrichment, storage, and event publishing.
 */
@Component
public class ProcessBatchCommandHandler {
    
    private final IIngestionBatchRepository ingestionBatchRepository;
    private final FileParsingService fileParsingService;
    private final FileValidationService fileValidationService;
    private final BankInfoEnrichmentService bankInfoEnrichmentService;
    private final S3StorageService s3StorageService;
    private final IngestionOutboxEventPublisher eventPublisher;
    
    public ProcessBatchCommandHandler(
            IIngestionBatchRepository ingestionBatchRepository,
            FileParsingService fileParsingService,
            FileValidationService fileValidationService,
            BankInfoEnrichmentService bankInfoEnrichmentService,
            S3StorageService s3StorageService,
            IngestionOutboxEventPublisher eventPublisher) {
        this.ingestionBatchRepository = ingestionBatchRepository;
        this.fileParsingService = fileParsingService;
        this.fileValidationService = fileValidationService;
        this.bankInfoEnrichmentService = bankInfoEnrichmentService;
        this.s3StorageService = s3StorageService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<Void> handle(ProcessBatchCommand command) {
        try {
            // 1. Load batch from repository
            IngestionBatch batch = ingestionBatchRepository.findByBatchId(command.batchId())
                .orElse(null);
            
            if (batch == null) {
                return Result.failure(ErrorDetail.of("BATCH_NOT_FOUND", ErrorType.NOT_FOUND_ERROR, "Batch not found: " + command.batchId().value(), "batch.not.found"));
            }
            
            // 2. Start processing - update batch status
            Result<Void> startResult = batch.startProcessing();
            if (startResult.isFailure()) {
                return startResult;
            }
            
            // Save the status update
            Result<IngestionBatch> saveResult = ingestionBatchRepository.save(batch);
            if (saveResult.isFailure()) {
                return Result.failure(saveResult.getError().orElse(
                    ErrorDetail.of("DATABASE_ERROR", ErrorType.SYSTEM_ERROR, "Failed to update batch status to PARSING", "database.error")));
            }
            
            // 3. Parse file content
            Result<ParsedFileData> parseResult = parseFile(command, batch);
            if (parseResult.isFailure()) {
                batch.markAsFailed(parseResult.getError().orElseThrow().getMessage());
                ingestionBatchRepository.save(batch);
                return Result.failure(parseResult.getError().orElseThrow());
            }
            
            ParsedFileData parsedData = parseResult.getValue().orElseThrow();
            
            // 4. Validate structure and business rules
            Result<ValidationResult> validationResult = validateFile(parsedData);
            if (validationResult.isFailure()) {
                batch.markAsFailed(validationResult.getError().orElseThrow().getMessage());
                ingestionBatchRepository.save(batch);
                return Result.failure(validationResult.getError().orElseThrow());
            }
            
            // 5. Mark as validated and update exposure count
            ValidationResult validation = validationResult.getValue().orElseThrow();
            Result<Void> validatedResult = batch.markAsValidated(validation.totalExposures());
            if (validatedResult.isFailure()) {
                return validatedResult;
            }
            
            // Save validation status
            saveResult = ingestionBatchRepository.save(batch);
            if (saveResult.isFailure()) {
                return Result.failure(saveResult.getError().orElse(
                    ErrorDetail.of("DATABASE_ERROR", ErrorType.SYSTEM_ERROR, "Failed to update batch status to VALIDATED", "database.error")));
            }
            
            // 6. Enrich with bank information
            Result<BankInfo> bankInfoResult = bankInfoEnrichmentService.enrichBankInfo(batch.getBankId());
            if (bankInfoResult.isFailure()) {
                batch.markAsFailed("Bank information enrichment failed: " + 
                    bankInfoResult.getError().orElseThrow().getMessage());
                ingestionBatchRepository.save(batch);
                return Result.failure(bankInfoResult.getError().orElseThrow());
            }
            
            BankInfo bankInfo = bankInfoResult.getValue().orElseThrow();
            
            // Validate bank status
            Result<Void> bankStatusResult = bankInfoEnrichmentService.validateBankStatus(bankInfo);
            if (bankStatusResult.isFailure()) {
                batch.markAsFailed("Bank status validation failed: " + 
                    bankStatusResult.getError().orElseThrow().getMessage());
                ingestionBatchRepository.save(batch);
                return Result.failure(bankStatusResult.getError().orElseThrow());
            }
            
            // Attach bank info to batch
            Result<Void> attachResult = batch.attachBankInfo(bankInfo);
            if (attachResult.isFailure()) {
                return attachResult;
            }
            
            // 7. Store in S3 with enterprise features
            Result<S3Reference> s3Result = s3StorageService.storeFile(
                command.fileStream(),
                batch.getFileMetadata(),
                batch.getBatchId().value(),
                batch.getBankId().value(),
                validation.totalExposures()
            );
            
            if (s3Result.isFailure()) {
                batch.markAsFailed("S3 storage failed: " + 
                    s3Result.getError().orElseThrow().getMessage());
                ingestionBatchRepository.save(batch);
                return Result.failure(s3Result.getError().orElseThrow());
            }
            
            S3Reference s3Reference = s3Result.getValue().orElseThrow();
            
            // 8. Record S3 storage in batch
            Result<Void> storageResult = batch.recordS3Storage(s3Reference);
            if (storageResult.isFailure()) {
                return storageResult;
            }
            
            // 9. Complete ingestion
            Result<Void> completeResult = batch.completeIngestion();
            if (completeResult.isFailure()) {
                return completeResult;
            }
            
            // 10. Save final batch state
            saveResult = ingestionBatchRepository.save(batch);
            if (saveResult.isFailure()) {
                return Result.failure(saveResult.getError().orElse(
                    ErrorDetail.of("DATABASE_ERROR", ErrorType.SYSTEM_ERROR, "Failed to save completed batch", "database.error")));
            }
            
            // 11. Publish events using existing CrossModuleEventBus
            BatchIngestedEvent event = new BatchIngestedEvent(
                batch.getBatchId().value(),
                batch.getBankId().value(),
                s3Reference.uri(),
                validation.totalExposures(),
                batch.getFileMetadata().fileSizeBytes(),
                Instant.now()
            );
            
            eventPublisher.publishBatchIngestedEvent(event);
            
            return Result.success(null);
            
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of("PROCESS_HANDLER_ERROR", ErrorType.SYSTEM_ERROR, "Unexpected error during batch processing: " + e.getMessage(), "process.handler.error"));
        }
    }
    
    private Result<ParsedFileData> parseFile(ProcessBatchCommand command, IngestionBatch batch) {
        String fileName = batch.getFileMetadata().fileName();
        String contentType = batch.getFileMetadata().contentType();
        
        return switch (contentType) {
            case "application/json" -> fileParsingService.parseJsonFile(command.fileStream(), fileName);
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> fileParsingService.parseExcelFile(command.fileStream(), fileName);
            default -> Result.failure(ErrorDetail.of("UNSUPPORTED_CONTENT_TYPE", ErrorType.VALIDATION_ERROR, "Unsupported content type for parsing: " + contentType, "unsupported.content.type"));
        };
    }
    
    private Result<ValidationResult> validateFile(ParsedFileData parsedData) {
        // First validate structure
        Result<ValidationResult> structureResult = fileValidationService.validateStructure(parsedData);
        if (structureResult.isFailure()) {
            return structureResult;
        }
        
        // Then validate business rules
        return fileValidationService.validateBusinessRules(parsedData);
    }
    
    // These services will be migrated to infrastructure layer
    // For now, creating placeholder interfaces
    public interface FileParsingService {
        Result<ParsedFileData> parseJsonFile(InputStream fileStream, String fileName);
        Result<ParsedFileData> parseExcelFile(InputStream fileStream, String fileName);
    }
    
    public interface FileValidationService {
        Result<ValidationResult> validateStructure(ParsedFileData parsedData);
        Result<ValidationResult> validateBusinessRules(ParsedFileData parsedData);
    }
    
    public interface BankInfoEnrichmentService {
        Result<BankInfo> enrichBankInfo(BankId bankId);
        Result<Void> validateBankStatus(BankInfo bankInfo);
    }
    
    public interface S3StorageService {
        Result<S3Reference> storeFile(InputStream fileStream, 
                                    FileMetadata fileMetadata,
                                    String batchId, String bankId, int exposureCount);
    }
    
    public interface IngestionOutboxEventPublisher {
        void publishBatchIngestedEvent(BatchIngestedEvent event);
    }

    public record ValidationResult(int totalExposures) {
    }
}

