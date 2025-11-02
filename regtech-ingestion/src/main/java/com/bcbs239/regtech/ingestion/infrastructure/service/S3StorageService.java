package com.bcbs239.regtech.ingestion.infrastructure.service;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.ingestion.domain.model.FileMetadata;
import com.bcbs239.regtech.ingestion.domain.model.S3Reference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

/**
 * Service for storing files in S3 with enterprise-grade features including
 * encryption, checksums, metadata, and multipart uploads.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3StorageService {
    
    private final S3Client s3Client;
    
    @Value("${regtech.s3.bucket:regtech-data-storage}")
    private String bucketName;
    
    @Value("${regtech.s3.prefix:raw/}")
    private String keyPrefix;
    
    private static final long MULTIPART_THRESHOLD = 100L * 1024 * 1024; // 100MB
    private static final String SERVER_SIDE_ENCRYPTION = "AES256";
    
    /**
     * Stores a file in S3 with production features including encryption,
     * checksums, and metadata.
     */
    public Result<S3Reference> storeFile(InputStream fileStream, FileMetadata fileMetadata, 
                                       String batchId, String bankId, int exposureCount) {
        try {
            log.info("Starting S3 upload for batch {} with file size {} bytes", 
                    batchId, fileMetadata.fileSizeBytes());
            
            // Generate S3 key with prefix
            String s3Key = generateS3Key(batchId, fileMetadata.fileName());
            
            // Calculate checksums from file content
            byte[] fileContent = fileStream.readAllBytes();
            String calculatedMd5 = calculateMd5(fileContent);
            String calculatedSha256 = calculateSha256(fileContent);
            
            // Verify checksums match metadata
            if (!calculatedMd5.equals(fileMetadata.md5Checksum())) {
                log.error("MD5 checksum mismatch for batch {}: expected {}, calculated {}", 
                         batchId, fileMetadata.md5Checksum(), calculatedMd5);
                return Result.failure(ErrorDetail.of("S3_CHECKSUM_MISMATCH", 
                    "File MD5 checksum does not match expected value"));
            }
            
            if (!calculatedSha256.equals(fileMetadata.sha256Checksum())) {
                log.error("SHA-256 checksum mismatch for batch {}: expected {}, calculated {}", 
                         batchId, fileMetadata.sha256Checksum(), calculatedSha256);
                return Result.failure(ErrorDetail.of("S3_CHECKSUM_MISMATCH", 
                    "File SHA-256 checksum does not match expected value"));
            }
            
            // Prepare metadata
            Map<String, String> metadata = createMetadata(batchId, bankId, exposureCount, fileMetadata);
            
            // Choose upload method based on file size
            if (fileMetadata.fileSizeBytes() > MULTIPART_THRESHOLD) {
                return performMultipartUpload(fileContent, s3Key, metadata, calculatedMd5);
            } else {
                return performSingleUpload(fileContent, s3Key, metadata, calculatedMd5);
            }
            
        } catch (Exception e) {
            log.error("Failed to store file in S3 for batch {}: {}", batchId, e.getMessage(), e);
            return Result.failure(ErrorDetail.of("S3_UPLOAD_FAILED", 
                "Failed to upload file to S3: " + e.getMessage()));
        }
    }
    
    /**
     * Performs a single-part upload for smaller files.
     */
    private Result<S3Reference> performSingleUpload(byte[] fileContent, String s3Key, 
                                                   Map<String, String> metadata, String md5Checksum) {
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .serverSideEncryption(ServerSideEncryption.AES256)
                    .metadata(metadata)
                    .contentMD5(md5Checksum)
                    .build();
            
            PutObjectResponse response = s3Client.putObject(putRequest, 
                    RequestBody.fromBytes(fileContent));
            
            // Verify ETag matches MD5 checksum
            String etag = response.eTag().replace("\"", "");
            if (!etag.equals(md5Checksum)) {
                log.error("ETag verification failed: expected {}, got {}", md5Checksum, etag);
                return Result.failure(ErrorDetail.of("S3_ETAG_MISMATCH", 
                    "S3 ETag does not match calculated MD5 checksum"));
            }
            
            S3Reference s3Reference = S3Reference.of(bucketName, s3Key, response.versionId());
            
            log.info("Successfully uploaded file to S3: {}", s3Reference.uri());
            return Result.success(s3Reference);
            
        } catch (S3Exception e) {
            log.error("S3 single upload failed for key {}: {}", s3Key, e.getMessage(), e);
            return Result.failure(ErrorDetail.of("S3_UPLOAD_FAILED", 
                "S3 upload failed: " + e.awsErrorDetails().errorMessage()));
        }
    }
    
    /**
     * Performs a multipart upload for larger files (>100MB).
     */
    private Result<S3Reference> performMultipartUpload(byte[] fileContent, String s3Key, 
                                                      Map<String, String> metadata, String md5Checksum) {
        try {
            // Create multipart upload
            CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .serverSideEncryption(ServerSideEncryption.AES256)
                    .metadata(metadata)
                    .build();
            
            CreateMultipartUploadResponse createResponse = s3Client.createMultipartUpload(createRequest);
            String uploadId = createResponse.uploadId();
            
            try {
                // Split file into parts (5MB minimum part size for S3)
                int partSize = 5 * 1024 * 1024; // 5MB
                int totalParts = (int) Math.ceil((double) fileContent.length / partSize);
                
                log.info("Starting multipart upload with {} parts for key {}", totalParts, s3Key);
                
                List<CompletedPart> completedParts = new ArrayList<>();
                
                for (int partNumber = 1; partNumber <= totalParts; partNumber++) {
                    int startPos = (partNumber - 1) * partSize;
                    int endPos = Math.min(startPos + partSize, fileContent.length);
                    byte[] partData = Arrays.copyOfRange(fileContent, startPos, endPos);
                    
                    UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                            .bucket(bucketName)
                            .key(s3Key)
                            .uploadId(uploadId)
                            .partNumber(partNumber)
                            .build();
                    
                    UploadPartResponse uploadPartResponse = s3Client.uploadPart(uploadPartRequest, 
                            RequestBody.fromBytes(partData));
                    
                    CompletedPart completedPart = CompletedPart.builder()
                            .partNumber(partNumber)
                            .eTag(uploadPartResponse.eTag())
                            .build();
                    
                    completedParts.add(completedPart);
                    
                    log.debug("Completed upload of part {} of {} for key {}", 
                             partNumber, totalParts, s3Key);
                }
                
                // Complete multipart upload
                CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .uploadId(uploadId)
                        .multipartUpload(CompletedMultipartUpload.builder()
                                .parts(completedParts)
                                .build())
                        .build();
                
                CompleteMultipartUploadResponse completeResponse = s3Client.completeMultipartUpload(completeRequest);
                
                // Note: For multipart uploads, ETag is not the MD5 of the entire file
                // It's a composite ETag, so we skip the ETag verification for multipart uploads
                log.info("Multipart upload ETag: {} (composite, not MD5)", completeResponse.eTag());
                
                S3Reference s3Reference = S3Reference.of(bucketName, s3Key, completeResponse.versionId());
                
                log.info("Successfully completed multipart upload to S3: {}", s3Reference.uri());
                return Result.success(s3Reference);
                
            } catch (Exception e) {
                // Abort multipart upload on failure
                AbortMultipartUploadRequest abortRequest = AbortMultipartUploadRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .uploadId(uploadId)
                        .build();
                
                s3Client.abortMultipartUpload(abortRequest);
                throw e;
            }
            
        } catch (S3Exception e) {
            log.error("S3 multipart upload failed for key {}: {}", s3Key, e.getMessage(), e);
            return Result.failure(ErrorDetail.of("S3_MULTIPART_UPLOAD_FAILED", 
                "S3 multipart upload failed: " + e.awsErrorDetails().errorMessage()));
        }
    }
    
    /**
     * Generates S3 key with proper prefix structure.
     */
    private String generateS3Key(String batchId, String fileName) {
        return keyPrefix + batchId + "/" + fileName;
    }
    
    /**
     * Creates metadata map for S3 object.
     */
    private Map<String, String> createMetadata(String batchId, String bankId, 
                                             int exposureCount, FileMetadata fileMetadata) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("batch-id", batchId);
        metadata.put("bank-id", bankId);
        metadata.put("exposure-count", String.valueOf(exposureCount));
        metadata.put("upload-timestamp", Instant.now().toString());
        metadata.put("file-size-bytes", String.valueOf(fileMetadata.fileSizeBytes()));
        metadata.put("content-type", fileMetadata.contentType());
        metadata.put("md5-checksum", fileMetadata.md5Checksum());
        metadata.put("sha256-checksum", fileMetadata.sha256Checksum());
        return metadata;
    }
    
    /**
     * Calculates MD5 checksum of file content.
     */
    private String calculateMd5(byte[] content) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] hash = md5.digest(content);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }
    
    /**
     * Calculates SHA-256 checksum of file content.
     */
    private String calculateSha256(byte[] content) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] hash = sha256.digest(content);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}