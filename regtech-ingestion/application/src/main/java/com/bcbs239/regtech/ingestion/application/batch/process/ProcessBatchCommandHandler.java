package com.bcbs239.regtech.ingestion.application.batch.process;

import com.bcbs239.regtech.core.domain.events.IIntegrationEventBus;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.ingestion.application.common.TemporaryFileStorageService;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankInfo;
import com.bcbs239.regtech.ingestion.domain.bankinfo.IBankInfoRepository;
import com.bcbs239.regtech.ingestion.domain.batch.IIngestionBatchRepository;
import com.bcbs239.regtech.ingestion.domain.batch.IngestionBatch;
import com.bcbs239.regtech.ingestion.domain.batch.S3Reference;
import com.bcbs239.regtech.ingestion.domain.file.FileContent;
import com.bcbs239.regtech.ingestion.domain.integrationevents.BatchIngestedEvent;
import com.bcbs239.regtech.ingestion.domain.model.ParsedFileData;
import com.bcbs239.regtech.ingestion.domain.parsing.FileParsingService;
import com.bcbs239.regtech.ingestion.domain.services.FileStorageService;
import com.bcbs239.regtech.ingestion.domain.validation.FileContentValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Command handler for processing batches asynchronously.
 * Handles the complete processing pipeline: parsing, validation, enrichment, storage, and event publishing.
 */
@Component
@Slf4j
public class ProcessBatchCommandHandler {
    
    private final IIngestionBatchRepository ingestionBatchRepository;
    private final FileParsingService fileParsingService;
    private final FileContentValidationService fileContentValidationService;
    private final IBankInfoRepository bankInfoRepository;
    private final FileStorageService fileStorageService;
    private final IIntegrationEventBus eventPublisher;
    private final TemporaryFileStorageService temporaryFileStorage;
    
    public ProcessBatchCommandHandler(
            IIngestionBatchRepository ingestionBatchRepository,
            FileParsingService fileParsingService,
            FileContentValidationService fileContentValidationService,
            IBankInfoRepository bankInfoRepository,
            FileStorageService fileStorageService,
            IIntegrationEventBus eventPublisher,
            TemporaryFileStorageService temporaryFileStorage) {
        this.ingestionBatchRepository = ingestionBatchRepository;
        this.fileParsingService = fileParsingService;
        this.fileContentValidationService = fileContentValidationService;
        this.bankInfoRepository = bankInfoRepository;
        this.fileStorageService = fileStorageService;
        this.eventPublisher = eventPublisher;
        this.temporaryFileStorage = temporaryFileStorage;
    }

    @Transactional
    public Result<Void> handle(ProcessBatchCommand command) {
        try {
            // 0. Retrieve file data from temporary storage
            Result<TemporaryFileStorageService.FileData> fileDataResult =
                temporaryFileStorage.retrieveFile(command.tempFileKey());
            if (fileDataResult.isFailure()) {
                return Result.failure(fileDataResult.getError().orElseThrow());
            }
            
            TemporaryFileStorageService.FileData fileData = 
                fileDataResult.getValue().orElseThrow();
            
            // Log file data for debugging
            log.info("Retrieved temporary file - key: {}, fileName: {}, contentType: {}, fileSize: {}, dataLength: {}", 
                command.tempFileKey(), fileData.fileName(), fileData.contentType(), 
                fileData.fileSize(), fileData.data() != null ? fileData.data().length : 0);
            
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
            
            // 3. Parse file content (use fresh InputStream)
            FileContent fileContent = FileContent.of(
                fileData.getInputStream(),
                batch.getFileMetadata().fileName(),
                batch.getFileMetadata().contentType()
            );
            
            Result<ParsedFileData> parseResult = fileParsingService.parseFileContent(fileContent);
            if (parseResult.isFailure()) {
                batch.markAsFailed(parseResult.getError().orElseThrow().getMessage());
                ingestionBatchRepository.save(batch);
                return Result.failure(parseResult.getError().orElseThrow());
            }
            
            ParsedFileData parsedData = parseResult.getValue().orElseThrow();
            
            // 4. Validate structure and business rules
            Result<FileContentValidationService.ValidationResult> validationResult = validateFile(parsedData);
            if (validationResult.isFailure()) {
                batch.markAsFailed(validationResult.getError().orElseThrow().getMessage());
                ingestionBatchRepository.save(batch);
                return Result.failure(validationResult.getError().orElseThrow());
            }
            
            // 5. Mark as validated and update exposure count
            FileContentValidationService.ValidationResult validation = validationResult.getValue().orElseThrow();
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
            
            // 6. Fetch bank information from repository
            Optional<BankInfo> bankInfoOpt = bankInfoRepository.findFreshBankInfo(batch.getBankId());
            
            // If not fresh, try to get any cached version
            if (bankInfoOpt.isEmpty()) {
                bankInfoOpt = bankInfoRepository.findByBankId(batch.getBankId());
            }
            
            // If still not found, create and cache mock bank info (in production, would call external service)
            BankInfo bankInfo;
            if (bankInfoOpt.isEmpty()) {
                bankInfo = new BankInfo(
                    batch.getBankId(),
                    "Bank Name for " + batch.getBankId().value(),
                    "US",
                    BankInfo.BankStatus.ACTIVE,
                    Instant.now()
                );
                bankInfoRepository.save(bankInfo);
            } else {
                bankInfo = bankInfoOpt.get();
            }
            
            // Validate bank eligibility - Let the domain object do the validation
            Result<Void> eligibilityResult = bankInfo.validateEligibilityForProcessing();
            if (eligibilityResult.isFailure()) {
                batch.markAsFailed("Bank eligibility validation failed: " + 
                    eligibilityResult.getError().orElseThrow().getMessage());
                ingestionBatchRepository.save(batch);
                return Result.failure(eligibilityResult.getError().orElseThrow());
            }
            
            // Attach bank info to batch
            Result<Void> attachResult = batch.attachBankInfo(bankInfo);
            if (attachResult.isFailure()) {
                return attachResult;
            }
            
            // 7. Store in S3 with enterprise features (use fresh InputStream)
            Result<S3Reference> s3Result = fileStorageService.storeFile(
                fileData.getInputStream(),
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
            
            eventPublisher.publish(event);
            
            // 12. Cleanup temporary file storage
            temporaryFileStorage.removeFile(command.tempFileKey());
            
            return Result.success(null);
            
        } catch (Exception e) {
            // Cleanup on error
            try {
                temporaryFileStorage.removeFile(command.tempFileKey());
            } catch (Exception cleanupEx) {
                // Log but don't fail - already have an error
            }
            return Result.failure(ErrorDetail.of("PROCESS_HANDLER_ERROR", ErrorType.SYSTEM_ERROR, "Unexpected error during batch processing: " + e.getMessage(), "process.handler.error"));
        }
    }
    
    private Result<FileContentValidationService.ValidationResult> validateFile(ParsedFileData parsedData) {
        // First validate structure
        Result<FileContentValidationService.ValidationResult> structureResult = fileContentValidationService.validateStructure(parsedData);
        if (structureResult.isFailure()) {
            return structureResult;
        }
        
        // Then validate business rules
        return fileContentValidationService.validateBusinessRules(parsedData);
    }

}
