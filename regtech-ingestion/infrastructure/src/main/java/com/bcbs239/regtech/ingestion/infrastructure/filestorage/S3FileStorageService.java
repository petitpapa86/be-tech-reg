package com.bcbs239.regtech.ingestion.infrastructure.filestorage;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.ingestion.domain.batch.FileMetadata;
import com.bcbs239.regtech.ingestion.domain.batch.S3Reference;
import com.bcbs239.regtech.ingestion.domain.services.FileStorageService;
import com.bcbs239.regtech.core.infrastructure.filestorage.CoreS3FileStorageService;
import com.bcbs239.regtech.core.infrastructure.filestorage.CoreS3Reference;
import com.bcbs239.regtech.ingestion.infrastructure.fileparsing.FileToLoanExposureParser;
import com.bcbs239.regtech.core.infrastructure.filestorage.CoreS3Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter implementation of the FileStorageService that delegates to the
 * central CoreS3FileStorageService. Keeps ingestion domain types as API.
 */
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "s3", matchIfMissing = true)
public class S3FileStorageService implements FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(S3FileStorageService.class);

    private final CoreS3FileStorageService coreS3FileStorageService;
    private final CoreS3Service coreS3Service;
    private final FileToLoanExposureParser fileParser;

    public S3FileStorageService(CoreS3FileStorageService coreS3FileStorageService, CoreS3Service coreS3Service, FileToLoanExposureParser fileParser) {
        this.coreS3FileStorageService = coreS3FileStorageService;
        this.coreS3Service = coreS3Service;
        this.fileParser = fileParser;
    }

    @Override
    public Result<com.bcbs239.regtech.ingestion.domain.batch.S3Reference> storeFile(InputStream fileStream, FileMetadata fileMetadata,
                                                                                     String batchId, String bankId, int exposureCount) {
        // Delegate to core service
        Result<CoreS3Reference> coreResult = coreS3FileStorageService.storeFile(fileStream, fileMetadata.fileName(), batchId, bankId, exposureCount);
        if (coreResult.isFailure()) {
            // propagate failure (reuse the same ErrorDetail list)
            return Result.failure(coreResult.errors());
        }

        CoreS3Reference coreRef = coreResult.getValueOrThrow();
        // Map CoreS3Reference -> ingestion.domain.batch.S3Reference (record expects bucket,key,versionId,uri)
        S3Reference domainRef = new S3Reference(coreRef.bucket(), coreRef.key(), coreRef.versionId(), coreRef.uri());
        logger.info("Stored file {} for batch {} in S3 (bucket={}, key={})", fileMetadata.fileName(), batchId, coreRef.bucket(), coreRef.key());
        return Result.success(domainRef);
    }

    @Override
    public Result<Boolean> checkServiceHealth() {
        return coreS3FileStorageService.checkServiceHealth();
    }

    /**
     * Download exposures from either a local file URI (file://) or S3 URI (s3://bucket/key)
     * Uses FileToLoanExposureParser for JSON parsing and CoreS3Service for S3 streaming.
     */
    public Result<List<com.bcbs239.regtech.ingestion.domain.model.LoanExposure>> downloadExposures(String s3Uri) {
        try {
            List<com.bcbs239.regtech.ingestion.domain.model.LoanExposure> exposures = new ArrayList<>();

            if (s3Uri != null && s3Uri.startsWith("file://")) {
                String filePath = s3Uri.replace("file:///", "").replace("file://", "");
                filePath = URLDecoder.decode(filePath, StandardCharsets.UTF_8.name());

                try (InputStream is = Files.newInputStream(Paths.get(filePath))) {
                    exposures = fileParser.parseJsonToLoanExposures(is, Integer.MAX_VALUE);
                }

            } else {
                var parsed = com.bcbs239.regtech.core.infrastructure.filestorage.S3Utils.parseS3Uri(s3Uri);
                if (parsed.isEmpty()) {
                    return Result.failure(ErrorDetail.of("S3_URI_INVALID", ErrorType.VALIDATION_ERROR, "Invalid S3 URI format: " + s3Uri, "s3_uri"));
                }

                try (ResponseInputStream<GetObjectResponse> s3Object = coreS3Service.getObjectStream(parsed.get().bucket(), parsed.get().key())) {
                    exposures = fileParser.parseJsonToLoanExposures(s3Object, Integer.MAX_VALUE);
                }
            }

            return Result.success(exposures);
        } catch (Exception e) {
            logger.error("Error downloading exposures from {}: {}", s3Uri, e.getMessage(), e);
            return Result.failure(ErrorDetail.of("S3_DOWNLOAD_ERROR", ErrorType.SYSTEM_ERROR, "Failed to download or parse exposures: " + e.getMessage(), "s3_download"));
        }
    }
}
