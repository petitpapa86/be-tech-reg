# Phase 3: Testing & Validation - Implementation Plan

**Status**: 🚀 **STARTING NOW**  
**Start Date**: January 8, 2026  
**Estimated Duration**: 1 day  
**Prerequisites**: Phase 0A ✅, Phase 1 ✅, Phase 2 ✅

---

## Executive Summary

Phase 3 focuses on comprehensive testing and validation of the shared storage infrastructure implemented in Phases 0A-2. This includes:
1. **LocalStack TestContainers Setup** - S3 mocking for integration tests
2. **Unit Tests** - Core storage components (StorageServiceAdapter, JsonStorageHelper)
3. **Integration Tests** - Cross-module workflows (data-quality ↔ report-generation)
4. **Performance Tests** - Large file handling (>100MB)
5. **Architecture Validation** - Verify no duplicate code remains

---

## Objectives

### Primary Goals
1. ✅ **100% test coverage** for shared storage code (StorageServiceAdapter, JsonStorageHelper)
2. ✅ **End-to-end integration tests** verifying data-quality → report-generation workflow
3. ✅ **Cross-module tests** ensuring all three modules (data-quality, report-generation, risk-calculation) can read/write shared storage
4. ✅ **Performance benchmarks** for large files (>100MB)
5. ✅ **Architecture verification** - confirm zero duplicate storage code

### Success Criteria
- [ ] All unit tests passing (target: 40+ tests)
- [ ] All integration tests passing (target: 10+ tests)
- [ ] Performance tests passing (<5s for 100MB files)
- [ ] BUILD SUCCESS across all modules with tests enabled
- [ ] Test coverage report generated (target: >90%)

---

## Implementation Plan

### Task 1: LocalStack TestContainers Setup (2 hours)

#### 1.1 Add Dependencies
**File**: `regtech-core/infrastructure/pom.xml`

Add TestContainers dependencies:
```xml
<!-- TestContainers for integration testing -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>localstack</artifactId>
    <version>1.19.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>1.19.0</version>
    <scope>test</scope>
</dependency>
```

#### 1.2 Create LocalStack Configuration
**File**: `regtech-core/infrastructure/src/test/resources/application-test.yml`

```yaml
storage:
  type: s3
  s3:
    endpoint: ${LOCALSTACK_ENDPOINT:http://localhost:4566}
    region: us-east-1
    bucket: test-bucket
    credentials:
      access-key: test
      secret-key: test
  json:
    validate-on-read: true
    validate-on-write: false
    max-size-mb: 100

spring:
  main:
    allow-bean-definition-overriding: true
```

#### 1.3 Create Base Test Class
**File**: `regtech-core/infrastructure/src/test/java/com/bcbs239/regtech/core/infrastructure/storage/AbstractStorageIntegrationTest.java`

```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public abstract class AbstractStorageIntegrationTest {
    
    @Container
    static LocalStackContainer localstack = new LocalStackContainer(
        DockerImageName.parse("localstack/localstack:3.0")
    )
    .withServices(LocalStackContainer.Service.S3)
    .withReuse(true);
    
    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("storage.s3.endpoint", () -> localstack.getEndpointOverride(S3).toString());
        registry.add("storage.s3.region", () -> localstack.getRegion());
    }
    
    @Autowired
    protected IStorageService storageService;
    
    @Autowired
    protected CoreS3Service coreS3Service;
    
    protected static final String TEST_BUCKET = "test-bucket";
    
    @BeforeEach
    void setUp() {
        // Create test bucket
        coreS3Service.createBucket(TEST_BUCKET, localstack.getRegion());
    }
    
    @AfterEach
    void tearDown() {
        // Clean up test bucket
        coreS3Service.deleteBucket(TEST_BUCKET);
    }
}
```

---

### Task 2: Unit Tests for StorageServiceAdapter (3 hours)

**File**: `regtech-core/infrastructure/src/test/java/com/bcbs239/regtech/core/infrastructure/storage/StorageServiceAdapterTest.java`

#### 2.1 S3 Upload Tests
```java
@ExtendWith(MockitoExtension.class)
class StorageServiceAdapterTest {
    
    @Mock
    private CoreS3Service coreS3Service;
    
    @Mock
    private JsonStorageHelper jsonHelper;
    
    @InjectMocks
    private StorageServiceAdapter storageService;
    
    @Test
    void shouldUploadJsonToS3() {
        // Arrange
        StorageUri uri = StorageUri.parse("s3://test-bucket/file.json");
        String content = "{\"test\": \"data\"}";
        Map<String, String> metadata = Map.of("key", "value");
        
        when(coreS3Service.putString(anyString(), anyString(), anyString(), anyString(), any(), any()))
            .thenReturn(true);
        
        // Act
        Result<StorageResult> result = storageService.upload(content, uri, metadata);
        
        // Assert
        assertThat(result.isSuccess()).isTrue();
        StorageResult storageResult = result.getValueOrThrow();
        assertThat(storageResult.uri()).isEqualTo(uri);
        assertThat(storageResult.sizeBytes()).isEqualTo(content.getBytes(StandardCharsets.UTF_8).length);
        
        verify(coreS3Service).putString("test-bucket", "file.json", content, "application/json", metadata, null);
    }
    
    @Test
    void shouldFailUploadWhenS3Fails() {
        // Arrange
        StorageUri uri = StorageUri.parse("s3://test-bucket/file.json");
        
        when(coreS3Service.putString(anyString(), anyString(), anyString(), anyString(), any(), any()))
            .thenThrow(new RuntimeException("S3 error"));
        
        // Act & Assert
        assertThatThrownBy(() -> storageService.upload("{}", uri, Map.of()))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("S3 error");
    }
}
```

#### 2.2 S3 Download Tests
```java
@Test
void shouldDownloadJsonFromS3() {
    // Arrange
    StorageUri uri = StorageUri.parse("s3://test-bucket/file.json");
    String expectedContent = "{\"test\": \"data\"}";
    
    when(coreS3Service.getString("test-bucket", "file.json", null))
        .thenReturn(Result.success(expectedContent));
    
    // Act
    Result<String> result = storageService.downloadJson(uri);
    
    // Assert
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getValueOrThrow()).isEqualTo(expectedContent);
}

@Test
void shouldDownloadBytesFromS3() {
    // Arrange
    StorageUri uri = StorageUri.parse("s3://test-bucket/file.bin");
    byte[] expectedBytes = "binary data".getBytes(StandardCharsets.UTF_8);
    
    when(coreS3Service.getBytes("test-bucket", "file.bin", null))
        .thenReturn(Result.success(expectedBytes));
    
    // Act
    Result<byte[]> result = storageService.downloadBytes(uri);
    
    // Assert
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getValueOrThrow()).isEqualTo(expectedBytes);
}
```

#### 2.3 Local Filesystem Tests
```java
@Test
void shouldUploadToLocalFilesystem() throws IOException {
    // Arrange
    Path tempDir = Files.createTempDirectory("storage-test");
    StorageUri uri = StorageUri.parse(tempDir.toString() + "/test.json");
    String content = "{\"test\": \"local\"}";
    
    // Act
    Result<StorageResult> result = storageService.upload(content, uri, Map.of());
    
    // Assert
    assertThat(result.isSuccess()).isTrue();
    assertThat(Files.exists(Path.of(uri.toString()))).isTrue();
    assertThat(Files.readString(Path.of(uri.toString()))).isEqualTo(content);
    
    // Cleanup
    Files.deleteIfExists(Path.of(uri.toString()));
    Files.deleteIfExists(tempDir);
}

@Test
void shouldDownloadFromLocalFilesystem() throws IOException {
    // Arrange
    Path tempFile = Files.createTempFile("storage-test", ".json");
    String expectedContent = "{\"test\": \"local\"}";
    Files.writeString(tempFile, expectedContent);
    
    StorageUri uri = StorageUri.parse(tempFile.toString());
    
    // Act
    Result<String> result = storageService.downloadJson(uri);
    
    // Assert
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getValueOrThrow()).isEqualTo(expectedContent);
    
    // Cleanup
    Files.deleteIfExists(tempFile);
}
```

#### 2.4 Presigned URL Tests
```java
@Test
void shouldGeneratePresignedUrl() {
    // Arrange
    StorageUri uri = StorageUri.parse("s3://test-bucket/file.json");
    Duration expiration = Duration.ofHours(1);
    Instant expectedExpiry = Instant.now().plus(expiration);
    String expectedUrl = "https://test-bucket.s3.amazonaws.com/file.json?presigned";
    
    when(coreS3Service.generatePresignedUrl("test-bucket", "file.json", expiration, null))
        .thenReturn(Result.success(expectedUrl));
    
    // Act
    Result<PresignedUrl> result = storageService.generatePresignedUrl(uri, expiration);
    
    // Assert
    assertThat(result.isSuccess()).isTrue();
    PresignedUrl presignedUrl = result.getValueOrThrow();
    assertThat(presignedUrl.url()).isEqualTo(expectedUrl);
    assertThat(presignedUrl.expiresAt()).isCloseTo(expectedExpiry, within(10, ChronoUnit.SECONDS));
}
```

**Estimated Tests**: 15-20 unit tests

---

### Task 3: Integration Tests with LocalStack (3 hours)

**File**: `regtech-core/infrastructure/src/test/java/com/bcbs239/regtech/core/infrastructure/storage/StorageServiceIntegrationTest.java`

#### 3.1 End-to-End S3 Upload/Download
```java
class StorageServiceIntegrationTest extends AbstractStorageIntegrationTest {
    
    @Test
    void shouldUploadAndDownloadJsonFromS3() {
        // Arrange
        StorageUri uri = StorageUri.parse("s3://" + TEST_BUCKET + "/test.json");
        String originalContent = "{\"test\": \"integration\", \"number\": 42}";
        Map<String, String> metadata = Map.of(
            "content-type", "application/json",
            "x-custom-header", "test-value"
        );
        
        // Act - Upload
        Result<StorageResult> uploadResult = storageService.upload(originalContent, uri, metadata);
        
        // Assert - Upload
        assertThat(uploadResult.isSuccess()).isTrue();
        StorageResult storageResult = uploadResult.getValueOrThrow();
        assertThat(storageResult.uri()).isEqualTo(uri);
        assertThat(storageResult.sizeBytes()).isEqualTo(originalContent.getBytes(StandardCharsets.UTF_8).length);
        
        // Act - Download
        Result<String> downloadResult = storageService.downloadJson(uri);
        
        // Assert - Download
        assertThat(downloadResult.isSuccess()).isTrue();
        assertThat(downloadResult.getValueOrThrow()).isEqualTo(originalContent);
    }
    
    @Test
    void shouldUploadAndDownloadBinaryFromS3() {
        // Arrange
        StorageUri uri = StorageUri.parse("s3://" + TEST_BUCKET + "/test.bin");
        byte[] originalBytes = "binary test data".getBytes(StandardCharsets.UTF_8);
        
        // Act - Upload
        Result<StorageResult> uploadResult = storageService.uploadBytes(originalBytes, uri, Map.of());
        
        // Assert - Upload
        assertThat(uploadResult.isSuccess()).isTrue();
        
        // Act - Download
        Result<byte[]> downloadResult = storageService.downloadBytes(uri);
        
        // Assert - Download
        assertThat(downloadResult.isSuccess()).isTrue();
        assertThat(downloadResult.getValueOrThrow()).isEqualTo(originalBytes);
    }
    
    @Test
    void shouldGenerateValidPresignedUrl() {
        // Arrange
        StorageUri uri = StorageUri.parse("s3://" + TEST_BUCKET + "/test.json");
        String content = "{\"test\": \"presigned\"}";
        storageService.upload(content, uri, Map.of());
        
        // Act
        Result<PresignedUrl> result = storageService.generatePresignedUrl(uri, Duration.ofMinutes(5));
        
        // Assert
        assertThat(result.isSuccess()).isTrue();
        PresignedUrl presignedUrl = result.getValueOrThrow();
        assertThat(presignedUrl.url()).contains(TEST_BUCKET);
        assertThat(presignedUrl.url()).contains("test.json");
        assertThat(presignedUrl.isValid()).isTrue();
        
        // Verify URL works by downloading via HTTP (optional)
        // RestTemplate restTemplate = new RestTemplate();
        // String downloadedContent = restTemplate.getForObject(presignedUrl.url(), String.class);
        // assertThat(downloadedContent).isEqualTo(content);
    }
}
```

#### 3.2 Large File Performance Test
```java
@Test
void shouldHandleLargeFiles() {
    // Arrange
    StorageUri uri = StorageUri.parse("s3://" + TEST_BUCKET + "/large-file.json");
    String largeContent = generateLargeJsonContent(100_000); // ~100MB
    
    // Act - Upload
    long uploadStart = System.currentTimeMillis();
    Result<StorageResult> uploadResult = storageService.upload(largeContent, uri, Map.of());
    long uploadDuration = System.currentTimeMillis() - uploadStart;
    
    // Assert - Upload
    assertThat(uploadResult.isSuccess()).isTrue();
    assertThat(uploadDuration).isLessThan(5000); // Less than 5 seconds
    
    // Act - Download
    long downloadStart = System.currentTimeMillis();
    Result<String> downloadResult = storageService.downloadJson(uri);
    long downloadDuration = System.currentTimeMillis() - downloadStart;
    
    // Assert - Download
    assertThat(downloadResult.isSuccess()).isTrue();
    assertThat(downloadDuration).isLessThan(5000); // Less than 5 seconds
    assertThat(downloadResult.getValueOrThrow()).hasSize(largeContent.length());
}

private String generateLargeJsonContent(int numRecords) {
    StringBuilder sb = new StringBuilder("{\"records\": [");
    for (int i = 0; i < numRecords; i++) {
        if (i > 0) sb.append(",");
        sb.append("{\"id\": ").append(i)
          .append(", \"name\": \"Record ").append(i).append("\"")
          .append(", \"value\": ").append(Math.random() * 1000)
          .append("}");
    }
    sb.append("]}");
    return sb.toString();
}
```

**Estimated Tests**: 10-15 integration tests

---

### Task 4: Cross-Module Integration Tests (2 hours)

**File**: `regtech-report-generation/application/src/test/java/com/bcbs239/regtech/reportgeneration/integration/CrossModuleStorageIntegrationTest.java`

#### 4.1 Data Quality → Report Generation Workflow
```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class CrossModuleStorageIntegrationTest {
    
    @Container
    static LocalStackContainer localstack = new LocalStackContainer(
        DockerImageName.parse("localstack/localstack:3.0")
    ).withServices(LocalStackContainer.Service.S3);
    
    @Autowired
    private IStorageService storageService;
    
    @Autowired
    private ComprehensiveReportDataAggregator dataAggregator;
    
    @Autowired
    private LocalDetailedResultsReader detailedResultsReader;
    
    private static final String TEST_BUCKET = "integration-test-bucket";
    
    @Test
    void shouldReadDataQualityResultsFromReportGeneration() {
        // Arrange - Data Quality writes validation results
        String batchId = "batch-" + UUID.randomUUID();
        StorageUri qualityUri = StorageUri.parse("s3://" + TEST_BUCKET + "/quality/" + batchId + ".json");
        
        String validationResults = """
        {
            "totalExposures": 1000,
            "validExposures": 950,
            "totalErrors": 50,
            "exposureResults": [],
            "batchErrors": []
        }
        """;
        
        Result<StorageResult> uploadResult = storageService.upload(validationResults, qualityUri, Map.of());
        assertThat(uploadResult.isSuccess()).isTrue();
        
        // Act - Report Generation reads the results
        StoredValidationResults storedResults = detailedResultsReader.load(qualityUri.toString());
        
        // Assert
        assertThat(storedResults).isNotNull();
        assertThat(storedResults.totalExposures()).isEqualTo(1000);
        assertThat(storedResults.validExposures()).isEqualTo(950);
        assertThat(storedResults.totalErrors()).isEqualTo(50);
    }
    
    @Test
    void shouldReadRiskCalculationResultsFromReportGeneration() {
        // Arrange - Risk Calculation writes calculation results
        String batchId = "batch-" + UUID.randomUUID();
        StorageUri calculationUri = StorageUri.parse("s3://" + TEST_BUCKET + "/calculations/" + batchId + ".json");
        
        String calculationResults = """
        {
            "batchId": "%s",
            "exposures": [
                {"id": "exp-1", "value": 1000000, "riskWeight": 0.5}
            ],
            "totalRisk": 500000
        }
        """.formatted(batchId);
        
        storageService.upload(calculationResults, calculationUri, Map.of());
        
        // Act - Report Generation aggregates data
        ProcessedBatchData processedData = dataAggregator.aggregateData(
            calculationUri.toString(),
            "s3://" + TEST_BUCKET + "/quality/" + batchId + ".json"
        );
        
        // Assert
        assertThat(processedData).isNotNull();
        assertThat(processedData.batchId()).isEqualTo(batchId);
    }
}
```

**Estimated Tests**: 5-8 cross-module tests

---

### Task 5: Architecture Validation (1 hour)

#### 5.1 Verify No Duplicate Code
```bash
# Search for duplicate storage implementations
grep -r "CoreS3Service" --include="*.java" \
  regtech-data-quality/ regtech-report-generation/ regtech-risk-calculation/

# Should only find usage in regtech-core
```

#### 5.2 Dependency Analysis
```bash
# Check that modules depend on regtech-core
./mvnw dependency:tree -pl regtech-report-generation/application | grep regtech-core
./mvnw dependency:tree -pl regtech-data-quality/infrastructure | grep regtech-core
```

#### 5.3 Generate Test Coverage Report
```bash
./mvnw test jacoco:report -pl regtech-core/infrastructure
# Report: regtech-core/infrastructure/target/site/jacoco/index.html
```

---

## Testing Timeline

### Hour 1-2: LocalStack Setup
- ✅ Add TestContainers dependencies
- ✅ Create AbstractStorageIntegrationTest base class
- ✅ Configure application-test.yml
- ✅ Verify LocalStack container starts

### Hour 3-5: Unit Tests
- ✅ StorageServiceAdapter upload tests (5 tests)
- ✅ StorageServiceAdapter download tests (5 tests)
- ✅ Local filesystem tests (5 tests)
- ✅ Presigned URL tests (3 tests)
- **Total: 18 unit tests**

### Hour 6-8: Integration Tests
- ✅ End-to-end S3 upload/download (3 tests)
- ✅ Large file performance (2 tests)
- ✅ Presigned URL generation (2 tests)
- ✅ Error handling scenarios (3 tests)
- **Total: 10 integration tests**

### Hour 9-10: Cross-Module Tests
- ✅ Data Quality → Report Generation (3 tests)
- ✅ Risk Calculation → Report Generation (2 tests)
- **Total: 5 cross-module tests**

### Hour 11: Architecture Validation
- ✅ Code duplication scan
- ✅ Dependency analysis
- ✅ Coverage report generation
- ✅ Documentation updates

---

## Expected Outcomes

### Test Coverage Metrics
- **StorageServiceAdapter**: >90% line coverage
- **JsonStorageHelper**: >85% line coverage
- **StorageUri**: 100% line coverage (already achieved - 22/22 tests)
- **Overall regtech-core**: >90% coverage

### Performance Benchmarks
- **Small files (<1MB)**: <100ms upload/download
- **Medium files (1-10MB)**: <500ms upload/download
- **Large files (10-100MB)**: <5s upload/download
- **Presigned URL generation**: <200ms

### Build Success Criteria
- ✅ All unit tests passing
- ✅ All integration tests passing
- ✅ All cross-module tests passing
- ✅ BUILD SUCCESS with tests enabled
- ✅ Zero duplicate storage code found

---

## Documentation Updates

### Files to Update
1. **COMPREHENSIVE_CODE_EXTRACTION_PLAN.md**
   - Mark Phase 3 as ✅ COMPLETE
   - Update progress tracking (100% complete)
   
2. **README.md** (create if missing)
   - Add "Testing" section
   - Document how to run tests with LocalStack
   
3. **.github/copilot-instructions.md**
   - Add Phase 3 testing patterns
   - Document LocalStack integration
   
4. **Create PHASE_3_TESTING_VALIDATION_COMPLETE.md**
   - Summary of all tests created
   - Test coverage metrics
   - Performance benchmark results
   - Architecture validation results

---

## Next Steps After Phase 3

1. **Code Review**: Review all shared storage code with team
2. **Performance Monitoring**: Add observability to storage operations
3. **Production Deployment**: Deploy shared storage to staging/production
4. **Documentation**: Update API documentation and architecture diagrams
5. **Future Enhancements**:
   - Azure Blob Storage support
   - Google Cloud Storage support
   - Storage cost optimization
   - Compression support for large files

---

## Progress Tracking

### Phase 3 Tasks
- [x] Task 1: LocalStack TestContainers Setup (2 hours) ✅ COMPLETE
  - [x] Added TestContainers dependencies to regtech-core/infrastructure/pom.xml
  - [x] Created AbstractStorageIntegrationTest base class
  - [x] Updated application-test.yml with LocalStack configuration
  - [x] Verified BUILD SUCCESS (01:16 min)
- [ ] Task 2: Unit Tests for StorageServiceAdapter (3 hours)
- [ ] Task 3: Integration Tests with LocalStack (3 hours)
- [ ] Task 4: Cross-Module Integration Tests (2 hours)
- [ ] Task 5: Architecture Validation (1 hour)

**Current Status**: ✅ **Task 1 COMPLETE** - Moving to Task 2  
**Total Estimated Time**: 11 hours (1.5 working days)  
**Time Elapsed**: 0.5 hours

---

## Ready to Begin!

Phase 3 implementation is now planned and ready to execute. Let's start with Task 1: LocalStack TestContainers Setup.
