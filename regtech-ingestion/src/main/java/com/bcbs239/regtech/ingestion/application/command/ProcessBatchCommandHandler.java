package com.bcbs239.regtech.ingestion.application.command;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.ingestion.application.service.BankInfoEnrichmentService;
import com.bcbs239.regtech.ingestion.domain.model.BatchId;
import com.bcbs239.regtech.ingestion.domain.model.BankInfo;
import com.bcbs239.regtech.ingestion.domain.model.IngestionBatch;
import com.bcbs239.regtech.ingestion.domain.model.S3Reference;
import com.bcbs239.regtech.ingestion.domain.repository.IngestionBatchRepository;
import com.bcbs239.regtech.ingestion.domain.events.BatchIngestedEvent;
import com.bcbs239.regtech.ingestion.infrastructure.events.IngestionOutboxEventPublisher;
import com.bcbs239.regtech.ingestion.infrastructure.service.FileParsingService;
import com.bcbs239.regtech.ingestion.infrastructure.service.FileValidationService;
import com.bcbs239.regtech.ingestion.infrastructure.service.ParsedFileData;
import com.bcbs239.regtech.ingestion.infrastructure.service.S3StorageService;
import com.bcbs239.regtech.ingestion.infrastructure.service.ValidationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Command handler for processing batches asynchronously.
 * Handles the complete processing pipeline: parsing, validation, enrichment, storage, and event publishing.
 */
@Component
public class ProcessBatchCommandHandler {
    
    private final FileParsingService fileParsingService;
    private final FileValidationService fileValidationService;
    private final BankInfoEnrichmentService bankInfoEnrichmentService;
    private final S3StorageService s3StorageService;
    private final IngestionBatchRepository ingestionBatchRepository;
    private final IngestionOutboxEventPublisher eventPublisher;
    
    @Autowired
    public ProcessBatchCommandHandler(
            FileParsingService fileParsingService,
            FileValidationService fileValidationService,
            BankInfoEnrichmentService bankInfoEnrichmentService,
            S3StorageService s3StorageService,
            IngestionBatchRepository ingestionBatchRepository,
            IngestionOutboxEventPublisher eventPublisher) {
        this.fileParsingService = fileParsingService;
        this.fileValidationService = fileValidationService;
        this.bankInfoEnrichmentService = bankInfoEnrichmentService;
        this.s3StorageService = s3StorageService;
        this.ingestionBatchRepository = ingestionBatchRepository;
        this.eventPublisher = eventPublisher;
    }
    
    /**
     * Handle the process batch command.
     * 1. Load batch from repository
     * 2. Parse file content using FileParsingService
     * 3. Validate structure and business rules
     * 4. Enrich with bank information
     * 5. Store in S3 with enterprise features
     * 6. Update batch record
     * 7. Publish events using existing CrossModuleEventBus
     */
    @Transactional
    public Result<Void> handle(ProcessBatchCommand command) {
        try {
            // 1. Load batch from repository
            IngestionBatch batch = ingestionBatchRepository.findByBatchId(command.batchId())
                .orElse(null);
            
            if (batch == null) {
                return Result.failure(ErrorDetail.businessRuleViolation("BATCH_NOT_FOUND", 
                    "Batch not found: " + command.batchId().value()));
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
                    ErrorDetail.infrastructureError("DATABASE", "Failed to update batch status to PARSING")));
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
            Result<Void> validatedResult = batch.markAsValidated(validation.getTotalExposures());
            if (validatedResult.isFailure()) {
                return validatedResult;
            }
            
            // Save validation status
            saveResult = ingestionBatchRepository.save(batch);
            if (saveResult.isFailure()) {
                return Result.failure(saveResult.getError().orElse(
                    ErrorDetail.infrastructureError("DATABASE", "Failed to update batch status to VALIDATED")));
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
                validation.getTotalExposures()
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
                    ErrorDetail.infrastructureError("DATABASE", "Failed to save completed batch")));
            }
            
            // 11. Publish events using existing CrossModuleEventBus
            BatchIngestedEvent event = new BatchIngestedEvent(
                batch.getBatchId().value(),
                batch.getBankId().value(),
                s3Reference.uri(),
                validation.getTotalExposures(),
                batch.getFileMetadata().fileSizeBytes(),
                Instant.now()
            );
            
            eventPublisher.publishBatchIngestedEvent(event);
            
            return Result.success(null);
            
        } catch (Exception e) {
            return Result.failure(ErrorDetail.infrastructureError("PROCESS_HANDLER", 
                "Unexpected error during batch processing: " + e.getMessage()));
        }
    }
    
    private Result<ParsedFileData> parseFile(ProcessBatchCommand command, IngestionBatch batch) {
        String fileName = batch.getFileMetadata().fileName();
        String contentType = batch.getFileMetadata().contentType();
        
        if (contentType.equals("application/json")) {
            return fileParsingService.parseJsonFile(command.fileStream(), fileName);
        } else if (contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
            return fileParsingService.parseExcelFile(command.fileStream(), fileName);
        } else {
            return Result.failure(ErrorDetail.validationError("contentType", contentType, 
                "Unsupported content type for parsing"));
        }
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
}