package com.bcbs239.regtech.core.infrastructure.storage;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.storage.StorageResult;
import com.bcbs239.regtech.core.domain.storage.StorageUri;
import com.bcbs239.regtech.core.infrastructure.filestorage.CoreS3Service;
import com.bcbs239.regtech.core.infrastructure.s3.S3Properties;
import org.junit.jupiter.api.*;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Manual integration tests for StorageServiceAdapter.
 * <p>
 * These tests assume LocalStack is running on localhost:4566.
 * To run these tests:
 * 1. Start LocalStack: docker run -p 4566:4566 localstack/localstack
 * 2. Run tests: mvnw test -Dtest=StorageServiceManualTest
 * </p>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StorageServiceManualTest {
    
    private static final String LOCALSTACK_ENDPOINT = "http://localhost:4566";
    private static final String TEST_BUCKET = "regtech-test-bucket";
    private static final String TEST_REGION = "us-east-1";
    
    private static S3Client s3Client;
    private static S3Presigner s3Presigner;
    private static CoreS3Service coreS3Service;
    private static JsonStorageHelper jsonStorageHelper;
    private static StorageServiceAdapter storageService;
    
    @BeforeAll
    static void setUpClass() {
        // Create S3 client for LocalStack
        s3Client = S3Client.builder()
            .endpointOverride(URI.create(LOCALSTACK_ENDPOINT))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create("test", "test")))
            .region(Region.US_EAST_1)
            .forcePathStyle(true)
            .build();
        
        // Create S3 presigner for LocalStack
        s3Presigner = S3Presigner.builder()
            .endpointOverride(URI.create(LOCALSTACK_ENDPOINT))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create("test", "test")))
            .region(Region.US_EAST_1)
            .build();
        
        // Create S3 properties
        S3Properties s3Properties = new S3Properties();
        s3Properties.setEndpoint(LOCALSTACK_ENDPOINT);
        s3Properties.setRegion(TEST_REGION);
        s3Properties.setAccessKey("test");
        s3Properties.setSecretKey("test");
        
        // Create services
        coreS3Service = new CoreS3Service(s3Properties, s3Client, s3Presigner);
        jsonStorageHelper = new JsonStorageHelper(new com.fasterxml.jackson.databind.ObjectMapper());
        storageService = new StorageServiceAdapter(coreS3Service, jsonStorageHelper);
        
        // Create test bucket
        try {
            s3Client.createBucket(CreateBucketRequest.builder()
                .bucket(TEST_BUCKET)
                .build());
            System.out.println("✓ Created test bucket: " + TEST_BUCKET);
        } catch (BucketAlreadyOwnedByYouException | BucketAlreadyExistsException e) {
            System.out.println("✓ Test bucket already exists: " + TEST_BUCKET);
        } catch (Exception e) {
            System.err.println("✗ Failed to create test bucket. Is LocalStack running?");
            System.err.println("  Run: docker run -p 4566:4566 localstack/localstack");
            throw e;
        }
    }
    
    @AfterAll
    static void tearDownClass() {
        // Clean up test objects
        try {
            ListObjectsV2Response response = s3Client.listObjectsV2(
                ListObjectsV2Request.builder().bucket(TEST_BUCKET).build());
            
            for (S3Object object : response.contents()) {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(TEST_BUCKET)
                    .key(object.key())
                    .build());
            }
            System.out.println("✓ Cleaned up test objects");
        } catch (Exception e) {
            System.err.println("✗ Failed to clean up test objects: " + e.getMessage());
        }
        
        if (s3Client != null) {
            s3Client.close();
        }
        if (s3Presigner != null) {
            s3Presigner.close();
        }
    }
    
    @Test
    @Order(1)
    void shouldUploadJsonToS3() throws Exception {
        // Arrange
        StorageUri uri = StorageUri.parse("s3://" + TEST_BUCKET + "/test.json");
        String jsonContent = "{\"test\": \"integration\", \"number\": 42}";
        Map<String, String> metadata = Map.of("x-batch-id", "batch-123");
        
        // Act
        Result<StorageResult> result = storageService.upload(jsonContent, uri, metadata);
        
        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValueOrThrow().uri()).isEqualTo(uri);
        assertThat(result.getValueOrThrow().sizeBytes()).isGreaterThan(0);
        
        System.out.println("✓ JSON uploaded: " + result.getValueOrThrow().sizeBytes() + " bytes");
    }
    
    @Test
    @Order(2)
    void shouldDownloadJsonFromS3() throws Exception {
        // Arrange - upload first
        StorageUri uri = StorageUri.parse("s3://" + TEST_BUCKET + "/test.json");
        String originalContent = "{\"test\": \"integration\", \"number\": 42}";
        storageService.upload(originalContent, uri, Map.of());
        
        // Act
        Result<String> result = storageService.download(uri);
        
        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValueOrThrow()).isEqualTo(originalContent);
        
        System.out.println("✓ JSON downloaded: " + result.getValueOrThrow());
    }
    
    @Test
    @Order(3)
    void shouldUploadBinaryToS3() throws Exception {
        // Arrange
        StorageUri uri = StorageUri.parse("s3://" + TEST_BUCKET + "/test.bin");
        byte[] binaryContent = "Binary test data".getBytes(StandardCharsets.UTF_8);
        Map<String, String> metadata = Map.of("x-file-type", "binary");
        
        // Act
        Result<StorageResult> result = storageService.uploadBytes(
            binaryContent, uri, "application/octet-stream", metadata);
        
        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValueOrThrow().sizeBytes()).isEqualTo(binaryContent.length);
        
        System.out.println("✓ Binary uploaded: " + result.getValueOrThrow().sizeBytes() + " bytes");
    }
    
    @Test
    @Order(4)
    void shouldDownloadBinaryFromS3() throws Exception {
        // Arrange - upload first
        StorageUri uri = StorageUri.parse("s3://" + TEST_BUCKET + "/test.bin");
        byte[] originalContent = "Binary test data".getBytes(StandardCharsets.UTF_8);
        storageService.uploadBytes(originalContent, uri, "application/octet-stream", Map.of());
        
        // Act
        Result<byte[]> result = storageService.downloadBytes(uri);
        
        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValueOrThrow()).isEqualTo(originalContent);
        
        System.out.println("✓ Binary downloaded: " + result.getValueOrThrow().length + " bytes");
    }
    
    @Test
    @Order(5)
    void shouldHandleLargeJsonFile() throws Exception {
        // Arrange - generate 10MB JSON content
        StringBuilder largeJson = new StringBuilder("{\"records\":[");
        for (int i = 0; i < 100000; i++) {
            if (i > 0) largeJson.append(",");
            largeJson.append(String.format(
                "{\"id\":%d,\"name\":\"record_%d\",\"value\":%d}",
                i, i, i * 100
            ));
        }
        largeJson.append("]}");
        
        String largeContent = largeJson.toString();
        StorageUri uri = StorageUri.parse("s3://" + TEST_BUCKET + "/large-test.json");
        
        // Act - upload
        long uploadStart = System.currentTimeMillis();
        Result<StorageResult> uploadResult = storageService.upload(largeContent, uri, Map.of());
        long uploadDuration = System.currentTimeMillis() - uploadStart;
        
        // Assert upload
        assertThat(uploadResult.isSuccess()).isTrue();
        System.out.println("✓ Large JSON uploaded: " + uploadResult.getValueOrThrow().sizeBytes() + 
            " bytes in " + uploadDuration + "ms");
        
        // Act - download
        long downloadStart = System.currentTimeMillis();
        Result<String> downloadResult = storageService.download(uri);
        long downloadDuration = System.currentTimeMillis() - downloadStart;
        
        // Assert download
        assertThat(downloadResult.isSuccess()).isTrue();
        assertThat(downloadResult.getValueOrThrow()).isEqualTo(largeContent);
        System.out.println("✓ Large JSON downloaded in " + downloadDuration + "ms");
    }
    
    @Test
    @Order(6)
    void shouldReturnFailureForNonExistentFile() throws Exception {
        // Arrange
        StorageUri uri = StorageUri.parse("s3://" + TEST_BUCKET + "/nonexistent.json");
        
        // Act
        Result<String> result = storageService.download(uri);
        
        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isPresent();
        
        System.out.println("✓ Correctly handled non-existent file");
    }
}
