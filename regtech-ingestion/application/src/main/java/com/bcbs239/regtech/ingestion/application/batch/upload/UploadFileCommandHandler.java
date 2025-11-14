package com.bcbs239.regtech.ingestion.application.batch.upload;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankInfo;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankId;
import com.bcbs239.regtech.ingestion.domain.bankinfo.IBankInfoRepository;
import com.bcbs239.regtech.ingestion.domain.batch.BatchId;
import com.bcbs239.regtech.ingestion.domain.batch.FileMetadata;
import com.bcbs239.regtech.ingestion.domain.batch.IIngestionBatchRepository;
import com.bcbs239.regtech.ingestion.domain.batch.IngestionBatch;
import com.bcbs239.regtech.ingestion.domain.file.ContentType;
import com.bcbs239.regtech.ingestion.domain.file.FileName;
import com.bcbs239.regtech.ingestion.domain.file.FileSize;
import com.bcbs239.regtech.core.domain.logging.ILogger;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Command handler for uploading files for ingestion processing.
 * Handles validation, batch creation, and initial storage.
 */
@Component
public class UploadFileCommandHandler {

    private final IIngestionBatchRepository ingestionBatchRepository;
    private final IBankInfoRepository bankInfoRepository;
    private final ILogger logger;

    public UploadFileCommandHandler(
            IIngestionBatchRepository ingestionBatchRepository,
            IBankInfoRepository bankInfoRepository,
            ILogger logger) {
        this.ingestionBatchRepository = ingestionBatchRepository;
        this.bankInfoRepository = bankInfoRepository;
        this.logger = logger;
    }

    /**
     * Handle the upload file command using railway pattern.
     * Chain validation and processing steps, where failure at any step
     * short-circuits the entire operation.
     */
    public Result<BatchId> handle(UploadFileCommand command) {
        long startTime = System.currentTimeMillis();
        BankId bankId = command.bankId();

        return loadAndValidateBank(bankId)
                .flatMap(bankInfo -> validateFileMetadata(command))
                .flatMap(validatedData -> createBatch(command, bankId))
                .flatMap(creationResult -> saveBatch(creationResult.batch(), creationResult.batchId(), bankId, startTime));
    }

    private Result<BankInfo> loadAndValidateBank(BankId bankId) {
        return bankInfoRepository.findByBankId(bankId)
                .filter(BankInfo::isActive)
                .map(Result::success)
                .orElse(Result.failure(ErrorDetail.of("BANK_NOT_FOUND", ErrorType.VALIDATION_ERROR,
                    String.format("Bank with ID '%s' not found or inactive", bankId.value()), "bank.not_found")));
    }

    private Result<ValidatedFileData> validateFileMetadata(UploadFileCommand command) {
        Result<FileName> fileNameResult = FileName.create(command.fileName());
        if (fileNameResult.isFailure()) return Result.failure(fileNameResult.getError().orElseThrow());

        Result<ContentType> contentTypeResult = ContentType.create(command.contentType());
        if (contentTypeResult.isFailure()) return Result.failure(contentTypeResult.getError().orElseThrow());

        Result<FileSize> fileSizeResult = FileSize.create(command.fileSizeBytes());
        if (fileSizeResult.isFailure()) return Result.failure(fileSizeResult.getError().orElseThrow());

        FileName fileName = fileNameResult.getValue().orElseThrow();
        ContentType contentType = contentTypeResult.getValue().orElseThrow();
        FileSize fileSize = fileSizeResult.getValue().orElseThrow();

        return validateFileExtensionAndLogWarnings(command, fileName, contentType, fileSize);
    }

    private Result<ValidatedFileData> validateFileExtensionAndLogWarnings(
            UploadFileCommand command, FileName fileName, ContentType contentType, FileSize fileSize) {

        // Validate file extension based on content type
        if (contentType.isJson() && !fileName.hasJsonExtension()) {
            return Result.failure(ErrorDetail.of("INVALID_FILE_EXTENSION", ErrorType.SYSTEM_ERROR,
                "JSON files must have .json extension", "generic.error"));
        } else if (contentType.isExcel() && !fileName.hasExcelExtension()) {
            return Result.failure(ErrorDetail.of("INVALID_FILE_EXTENSION", ErrorType.SYSTEM_ERROR,
                "Excel files must have .xlsx or .xls extension", "generic.error"));
        }

        // Log warnings for large files
        if (contentType.isJson() && fileSize.shouldWarnForJson()) {
            logger.asyncStructuredLog("Large JSON file detected",
                Map.of("fileName", command.fileName(), "fileSize", command.fileSizeBytes()));
        } else if (contentType.isExcel() && fileSize.shouldWarnForExcel()) {
            logger.asyncStructuredLog("Large Excel file detected",
                Map.of("fileName", command.fileName(), "fileSize", command.fileSizeBytes()));
        }

        return Result.success(new ValidatedFileData(fileName, contentType, fileSize));
    }

    private Result<BatchCreationResult> createBatch(UploadFileCommand command, BankId bankId) {
        BatchId batchId = BatchId.generate();

        String md5Checksum = calculateMD5Checksum(command.fileStream());

        FileMetadata fileMetadata = new FileMetadata(
                command.fileName(),
                command.contentType(),
                command.fileSizeBytes(),
                md5Checksum,
                null
        );

        IngestionBatch batch = new IngestionBatch(batchId, bankId, fileMetadata);

        logger.asyncStructuredLog("File upload started",
            Map.of("batchId", batchId.value(), "bankId", bankId.value(), "fileName", command.fileName(),
                   "fileSize", command.fileSizeBytes(), "contentType", command.contentType()));

        return Result.success(new BatchCreationResult(batch, batchId));
    }

    private Result<BatchId> saveBatch(IngestionBatch batch, BatchId batchId, BankId bankId, long startTime) {
        // Store initial batch record
        Result<IngestionBatch> saveResult = ingestionBatchRepository.save(batch);
        if (saveResult.isFailure()) {
            return Result.failure(saveResult.getError().orElse(
                    ErrorDetail.of("DATABASE_ERROR", ErrorType.SYSTEM_ERROR, "Failed to save batch record", "database.error")
            ));
        }

        long duration = System.currentTimeMillis() - startTime;
        logger.asyncStructuredLog("File upload completed",
            Map.of("batchId", batchId.value(), "bankId", bankId.value(), "duration", duration));

        return Result.success(batchId);
    }

    private String calculateMD5Checksum(java.io.InputStream inputStream) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int bytesRead;

            // Reset stream if possible
            if (inputStream.markSupported()) {
                inputStream.mark(Integer.MAX_VALUE);
            }

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }

            if (inputStream.markSupported()) {
                inputStream.reset();
            }

            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (NoSuchAlgorithmException | IOException e) {
            // Return a placeholder checksum if calculation fails
            // In production, this should be handled more robustly
            return "checksum_calculation_failed";
        }
    }

}


record ValidatedFileData(FileName fileName, ContentType contentType, FileSize fileSize) {
}

record BatchCreationResult(IngestionBatch batch, BatchId batchId) {
}

