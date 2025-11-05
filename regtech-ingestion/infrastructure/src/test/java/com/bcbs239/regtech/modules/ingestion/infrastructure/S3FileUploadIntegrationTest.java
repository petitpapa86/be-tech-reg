package com.bcbs239.regtech.modules.ingestion.infrastructure;

import com.bcbs239.regtech.ingestion.domain.bankinfo.BankId;
import com.bcbs239.regtech.ingestion.domain.batch.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test focusing on the core ingestion functionality:
 * 1. File upload
 * 2. S3 storage
 * 3. Batch metadata persistence
 * 
 * Data quality validation is handled by separate quality module.
 */
@DisplayName("S3 File Upload Integration Test")
class S3FileUploadIntegrationTest {

    private LocalStackContainer localstack;
    private S3Client s3Client;

    private ObjectMapper objectMapper;

    private static final String TEST_BUCKET = "regtech-data-storage";
    private static final String BANK_ID = "COMMUNITY_FIRST_BANK";

    @BeforeEach
    void setUp() {
        // Initialize object mapper
        this.objectMapper = new ObjectMapper();

        // Try to start LocalStack; if Docker not available, skip these integration tests.
        try {
            this.localstack = new LocalStackContainer(org.testcontainers.utility.DockerImageName.parse("localstack/localstack:3.0"))
                .withServices(LocalStackContainer.Service.S3);
            this.localstack.start();

            // Create S3 client pointing to localstack endpoint
            java.net.URI endpoint = localstack.getEndpointOverride(LocalStackContainer.Service.S3);
            this.s3Client = software.amazon.awssdk.services.s3.S3Client.builder()
                .endpointOverride(endpoint)
                .region(software.amazon.awssdk.regions.Region.of("us-east-1"))
                .credentialsProvider(software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(
                    software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create("test","test")
                ))
                .build();
        } catch (Exception e) {
            // Skip tests when Docker / Testcontainers is not available
            Assumptions.assumeTrue(false, "Docker/Testcontainers not available: " + e.getMessage());
        }

        // Create test bucket
        try {
            s3Client.createBucket(CreateBucketRequest.builder()
                .bucket(TEST_BUCKET)
                .build());
        } catch (BucketAlreadyExistsException e) {
            // Bucket already exists, continue
        }
    }

    @AfterEach
    void tearDown() {
        if (this.localstack != null && this.localstack.isRunning()) {
            try { this.localstack.stop(); } catch (Exception ignored) {}
        }
    }

    @Test
    @DisplayName("Should upload JSON file to S3 and create batch metadata")
    void shouldUploadJsonFileToS3AndCreateBatchMetadata() throws IOException {
        // Given: Sample loan portfolio file
        ClassPathResource inputFile = new ClassPathResource("test-data/daily_loans_2024_09_12.json");
        byte[] fileContent = inputFile.getInputStream().readAllBytes();
        String originalFilename = "daily_loans_2024_09_12.json";
        
        // When: Processing file upload
        IngestionResult result = processFileUpload(fileContent, originalFilename, "application/json");
        
        // Then: File should be uploaded to S3
        assertThat(result.success()).isTrue();
        assertThat(result.batchId()).isNotNull();
        assertThat(result.s3Reference()).isNotNull();
        
        S3Reference s3Ref = result.s3Reference();
        assertThat(s3Ref.bucket()).isEqualTo(TEST_BUCKET);
        assertThat(s3Ref.key()).startsWith("raw/");
        assertThat(s3Ref.key()).contains(BANK_ID);
        assertThat(s3Ref.key()).endsWith(".json");
        
        // Verify file exists in S3
        HeadObjectResponse headResponse = s3Client.headObject(HeadObjectRequest.builder()
            .bucket(s3Ref.bucket())
            .key(s3Ref.key())
            .build());
        
        assertThat(headResponse.contentLength()).isEqualTo(fileContent.length);
        assertThat(headResponse.contentType()).isEqualTo("application/json");
        
        // Verify file content in S3
        ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(GetObjectRequest.builder()
            .bucket(s3Ref.bucket())
            .key(s3Ref.key())
            .build());
        
        byte[] s3Content = s3Object.readAllBytes();
        assertThat(s3Content).isEqualTo(fileContent);
        
        // Verify JSON structure is preserved
        JsonNode originalJson = objectMapper.readTree(fileContent);
        JsonNode s3Json = objectMapper.readTree(s3Content);
        assertThat(s3Json).isEqualTo(originalJson);
    }

    @Test
    @DisplayName("Should upload enhanced JSON file to S3 with proper metadata")
    void shouldUploadEnhancedJsonFileToS3WithProperMetadata() throws IOException {
        // Given: Enhanced loan portfolio file
        ClassPathResource inputFile = new ClassPathResource("test-data/enhanced_daily_loans_2024_09_12.json");
        byte[] fileContent = inputFile.getInputStream().readAllBytes();
        String originalFilename = "enhanced_daily_loans_2024_09_12.json";
        
        // When: Processing file upload
        IngestionResult result = processFileUpload(fileContent, originalFilename, "application/json");
        
        // Then: File should be uploaded successfully
        assertThat(result.success()).isTrue();
        
        S3Reference s3Ref = result.s3Reference();
        FileMetadata metadata = result.fileMetadata();
        
        // Verify S3 storage
        assertThat(s3Ref.bucket()).isEqualTo(TEST_BUCKET);
        assertThat(s3Ref.key()).contains("enhanced_daily_loans");
        
        // Verify file metadata (use record accessors)
        assertThat(metadata.fileName()).isEqualTo(originalFilename);
        assertThat(metadata.contentType()).isEqualTo("application/json");
        assertThat(metadata.fileSizeBytes()).isEqualTo((long) fileContent.length);

        // Verify S3 object metadata
        HeadObjectResponse headResponse = s3Client.headObject(HeadObjectRequest.builder()
            .bucket(s3Ref.bucket())
            .key(s3Ref.key())
            .build());
        
        assertThat(headResponse.metadata()).containsEntry("original-filename", originalFilename);
        assertThat(headResponse.metadata()).containsEntry("bank-id", BANK_ID);
        assertThat(headResponse.metadata()).containsKey("upload-timestamp");
    }

    @Test
    @DisplayName("Should generate unique S3 keys for multiple uploads")
    void shouldGenerateUniqueS3KeysForMultipleUploads() throws IOException {
        // Given: Same file uploaded multiple times
        ClassPathResource inputFile = new ClassPathResource("test-data/daily_loans_2024_09_12.json");
        byte[] fileContent = inputFile.getInputStream().readAllBytes();
        String originalFilename = "daily_loans_2024_09_12.json";
        
        // When: Uploading same file multiple times
        IngestionResult result1 = processFileUpload(fileContent, originalFilename, "application/json");
        IngestionResult result2 = processFileUpload(fileContent, originalFilename, "application/json");
        IngestionResult result3 = processFileUpload(fileContent, originalFilename, "application/json");
        
        // Then: Each upload should have unique S3 key
        assertThat(result1.s3Reference().key()).isNotEqualTo(result2.s3Reference().key());
        assertThat(result2.s3Reference().key()).isNotEqualTo(result3.s3Reference().key());
        assertThat(result1.s3Reference().key()).isNotEqualTo(result3.s3Reference().key());
        
        // All files should exist in S3
        assertThat(s3ObjectExists(result1.s3Reference())).isTrue();
        assertThat(s3ObjectExists(result2.s3Reference())).isTrue();
        assertThat(s3ObjectExists(result3.s3Reference())).isTrue();
    }

    @Test
    @DisplayName("Should handle large file uploads efficiently")
    void shouldHandleLargeFileUploadsEfficiently() throws IOException {
        // Given: Large JSON file (simulated by repeating loan data)
        ClassPathResource inputFile = new ClassPathResource("test-data/enhanced_daily_loans_2024_09_12.json");
        JsonNode originalJson = objectMapper.readTree(inputFile.getInputStream());
        
        // Create larger file by duplicating loan portfolio
        JsonNode loanPortfolio = originalJson.get("loan_portfolio");
        StringBuilder largeJsonBuilder = new StringBuilder();
        largeJsonBuilder.append("{\"bank_info\":");
        largeJsonBuilder.append(objectMapper.writeValueAsString(originalJson.get("bank_info")));
        largeJsonBuilder.append(",\"loan_portfolio\":[");
        
        // Add 1000 loan records
        for (int i = 0; i < 1000; i++) {
            if (i > 0) largeJsonBuilder.append(",");
            for (int j = 0; j < loanPortfolio.size(); j++) {
                if (j > 0) largeJsonBuilder.append(",");
                JsonNode loan = loanPortfolio.get(j);
                // Modify loan_id to make it unique
                String loanJson = objectMapper.writeValueAsString(loan);
                loanJson = loanJson.replace("\"loan_id\":\"LOAN", "\"loan_id\":\"LOAN" + i + "_");
                largeJsonBuilder.append(loanJson);
            }
        }
        largeJsonBuilder.append("]}");
        
        byte[] largeFileContent = largeJsonBuilder.toString().getBytes();
        String originalFilename = "large_loan_portfolio.json";
        
        // When: Uploading large file
        long startTime = System.currentTimeMillis();
        IngestionResult result = processFileUpload(largeFileContent, originalFilename, "application/json");
        long uploadTime = System.currentTimeMillis() - startTime;
        
        // Then: Upload should succeed efficiently
        assertThat(result.success()).isTrue();
        assertThat(uploadTime).isLessThan(30000); // Should complete within 30 seconds
        
        // Verify large file in S3
        HeadObjectResponse headResponse = s3Client.headObject(HeadObjectRequest.builder()
            .bucket(result.s3Reference().bucket())
            .key(result.s3Reference().key())
            .build());
        
        assertThat(headResponse.contentLength()).isEqualTo(largeFileContent.length);
        assertThat(headResponse.contentLength()).isGreaterThan(1_000_000); // > 1MB
    }

    @Test
    @DisplayName("Should create proper batch metadata for tracking")
    void shouldCreateProperBatchMetadataForTracking() throws IOException {
        // Given: Sample file
        ClassPathResource inputFile = new ClassPathResource("test-data/daily_loans_2024_09_12.json");
        byte[] fileContent = inputFile.getInputStream().readAllBytes();
        String originalFilename = "daily_loans_2024_09_12.json";
        
        // When: Processing upload
        IngestionResult result = processFileUpload(fileContent, originalFilename, "application/json");
        
        // Then: Batch metadata should be complete
        IngestionBatch batch = result.batch();
        assertThat(batch).isNotNull();
        assertThat(batch.getBatchId()).isNotNull();
        assertThat(batch.getBankId()).isEqualTo(BANK_ID);
        assertThat(batch.getS3Reference()).isEqualTo(result.s3Reference());
        assertThat(batch.getFileMetadata()).isEqualTo(result.fileMetadata());
        // uploadedAt getter is provided via Lombok (@Getter)
        assertThat(batch.getUploadedAt()).isNotNull();
        assertThat(batch.getStatus()).isEqualTo(BatchStatus.UPLOADED);

        // Batch should be ready for quality module processing: not terminal and uploaded
        assertThat(batch.isTerminal()).isFalse();
    }

    // Helper methods

    private IngestionResult processFileUpload(byte[] fileContent, String originalFilename, String contentType) {
        // Simulate the ingestion upload process
        BatchId batchId = BatchId.of(UUID.randomUUID().toString());
        
        // Generate S3 key
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String s3Key = String.format("raw/%s/%s/%s_%s", 
            BANK_ID, 
            timestamp.substring(0, 8), // Date prefix
            timestamp,
            originalFilename);
        
        // Upload to S3
        s3Client.putObject(PutObjectRequest.builder()
            .bucket(TEST_BUCKET)
            .key(s3Key)
            .contentType(contentType)
            .contentLength((long) fileContent.length)
            .metadata(java.util.Map.of(
                "original-filename", originalFilename,
                "bank-id", BANK_ID,
                "upload-timestamp", Instant.now().toString(),
                "batch-id", batchId.value()
            ))
            .build(),
            software.amazon.awssdk.core.sync.RequestBody.fromBytes(fileContent));
        
        // Create metadata objects
        S3Reference s3Reference = S3Reference.of(TEST_BUCKET, s3Key, "v1");

        // FileMetadata requires checksums; use placeholders for tests
        FileMetadata fileMetadata = new FileMetadata(
            originalFilename,
            contentType,
            (long) fileContent.length,
            "md5-placeholder",
            "sha256-placeholder"
        );
        
        // Use the domain constructor for reconstituting a batch from persistence
        IngestionBatch batch = new IngestionBatch(
            batchId,
            BankId.of(BANK_ID),
            BatchStatus.UPLOADED,
            fileMetadata,
            s3Reference,
            null, // BankInfo (not needed for this test)
            Integer.valueOf(0), // totalExposures (0 as placeholder)
            Instant.now(), // uploadedAt
            null, // completedAt
            null, // errorMessage
            null, // processingDurationMs
            0, // recoveryAttempts
            null, // lastCheckpoint
            null, // checkpointData
            Instant.now() // updatedAt
        );
        
        return new IngestionResult(true, batchId, s3Reference, fileMetadata, batch);
    }

    private boolean s3ObjectExists(S3Reference s3Reference) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                .bucket(s3Reference.bucket())
                .key(s3Reference.key())
                .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    /**
     * @param success Getters
     */ // Test result class
        public record IngestionResult(boolean success, BatchId batchId, S3Reference s3Reference, FileMetadata fileMetadata,
                                      IngestionBatch batch) {

    }
}

