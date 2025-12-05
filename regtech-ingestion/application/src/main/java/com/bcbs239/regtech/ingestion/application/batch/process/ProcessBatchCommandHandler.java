package com.bcbs239.regtech.ingestion.application.batch.process;

import com.bcbs239.regtech.core.application.BaseUnitOfWork;
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
import com.bcbs239.regtech.ingestion.domain.model.ParsedFileData;
import com.bcbs239.regtech.ingestion.domain.parsing.FileParsingService;
import com.bcbs239.regtech.ingestion.domain.services.FileStorageService;
import com.bcbs239.regtech.ingestion.domain.validation.FileContentValidationService;
import com.bcbs239.regtech.ingestion.application.serialization.ParsedDataSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.Map;
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
    private final BaseUnitOfWork unitOfWork;
    private final TemporaryFileStorageService temporaryFileStorage;
    private final ParsedDataSerializer parsedDataSerializer;
    
    public ProcessBatchCommandHandler(
            IIngestionBatchRepository ingestionBatchRepository,
            FileParsingService fileParsingService,
            FileContentValidationService fileContentValidationService,
            IBankInfoRepository bankInfoRepository,
            FileStorageService fileStorageService,
            BaseUnitOfWork unitOfWork,
            TemporaryFileStorageService temporaryFileStorage,
            ParsedDataSerializer parsedDataSerializer) {
        this.ingestionBatchRepository = ingestionBatchRepository;
        this.fileParsingService = fileParsingService;
        this.fileContentValidationService = fileContentValidationService;
        this.bankInfoRepository = bankInfoRepository;
        this.fileStorageService = fileStorageService;
        this.unitOfWork = unitOfWork;
        this.temporaryFileStorage = temporaryFileStorage;
        this.parsedDataSerializer = parsedDataSerializer;
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
            
            // If still not found, this is an error - bank must exist
            if (bankInfoOpt.isEmpty()) {
                String errorMessage = "Bank not found: " + batch.getBankId().value();
                batch.markAsFailed(errorMessage);
                ingestionBatchRepository.save(batch);
                
                ErrorDetail error = ErrorDetail.of(
                    "BANK_NOT_FOUND",
                    ErrorType.VALIDATION_ERROR,
                    errorMessage,
                    "bank.not.found"
                );
                
                log.error("Bank not found during batch processing; details={}", Map.of(
                    "batchId", batch.getBatchId().value(),
                    "bankId", batch.getBankId().value()
                ));
                
                return Result.failure(error);
            }
            
            BankInfo bankInfo = bankInfoOpt.get();
            
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
            
            // 7. Serialize ParsedFileData to JSON via BatchDataDTO
            InputStream serializedStream;
            try {
                serializedStream = parsedDataSerializer.serializeToInputStream(parsedData);
                log.info("Successfully serialized ParsedFileData to JSON for batch {} - bank: {}, exposures: {}",
                    batch.getBatchId().value(),
                    bankInfo.bankName(),
                    validation.totalExposures());
            } catch (Exception e) {
                String errorMessage = "Failed to serialize parsed data to JSON: " + e.getMessage();
                log.error("Serialization failed for batch {}: {}", batch.getBatchId().value(), errorMessage, e);
                batch.markAsFailed(errorMessage);
                ingestionBatchRepository.save(batch);
                return Result.failure(ErrorDetail.of(
                    "SERIALIZATION_ERROR",
                    ErrorType.SYSTEM_ERROR,
                    errorMessage,
                    "serialization.error"
                ));
            }
            
            // 8. Store serialized JSON in S3/local storage
            Result<S3Reference> s3Result = fileStorageService.storeFile(
                serializedStream,
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
            
            // 9. Record S3 storage in batch
            Result<Void> storageResult = batch.recordS3Storage(s3Reference);
            if (storageResult.isFailure()) {
                return storageResult;
            }
            
            // 10. Complete ingestion
            Result<Void> completeResult = batch.completeIngestion();
            if (completeResult.isFailure()) {
                return completeResult;
            }
            
            // 11. Save final batch state
            saveResult = ingestionBatchRepository.save(batch);
            if (saveResult.isFailure()) {
                return Result.failure(saveResult.getError().orElse(
                    ErrorDetail.of("DATABASE_ERROR", ErrorType.SYSTEM_ERROR, "Failed to save completed batch", "database.error")));
            }
            
            // 12. save events through unit of work to outbox pattern
            unitOfWork.registerEntity(batch);
            unitOfWork.saveChanges();
            
            // 13. Cleanup temporary file storage
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
