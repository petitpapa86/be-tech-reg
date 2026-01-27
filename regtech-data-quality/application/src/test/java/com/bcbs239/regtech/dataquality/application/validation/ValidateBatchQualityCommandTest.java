package com.bcbs239.regtech.dataquality.application.validation;

import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.core.domain.shared.valueobjects.BatchId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ValidateBatchQualityCommand Unit Tests")
class ValidateBatchQualityCommandTest {

    @Test
    @DisplayName("Should create command with required fields")
    void shouldCreateCommandWithRequiredFields() {
        // Arrange
        BatchId batchId = BatchId.of("batch_batch-1");
        BankId bankId = BankId.of("bank-1");
        String s3Uri = "s3://bucket/path/file.json";
        
        // Act
        ValidateBatchQualityCommand command = ValidateBatchQualityCommand.of(
            batchId, bankId, s3Uri, 100, "test.json", 1024L, "json"
        );
        
        // Assert
        assertNotNull(command);
        assertEquals(batchId, command.batchId());
        assertEquals(bankId, command.bankId());
        assertEquals(s3Uri, command.s3Uri());
        assertEquals(100, command.expectedExposureCount());
        assertEquals(1024L, command.fileSize());
        assertEquals("json", command.fileFormat());
    }
    
    @Test
    @DisplayName("Should validate successfully with valid parameters")
    void shouldValidateSuccessfullyWithValidParameters() {
        // Arrange
        ValidateBatchQualityCommand command = ValidateBatchQualityCommand.of(
            BatchId.of("batch_batch-1"),
            BankId.of("bank-1"),
            "s3://bucket/path/file.json",
            50,
            "test.json",
            2048L,
            "json"
        );
        
        // Act & Assert
        assertDoesNotThrow(command::validate);
    }
    
    @Test
    @DisplayName("Should throw exception for null batch ID")
    void shouldThrowExceptionForNullBatchId() {
        // Arrange
        ValidateBatchQualityCommand command = new ValidateBatchQualityCommand(
            null,
            BankId.of("bank-1"),
            "s3://bucket/path/file.json",
            50,
            null,null,"test.json", 100L, "json"
        );
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, command::validate);
    }
    
    @Test
    @DisplayName("Should throw exception for null bank ID")
    void shouldThrowExceptionForNullBankId() {
        // Arrange
        ValidateBatchQualityCommand command = new ValidateBatchQualityCommand(
            BatchId.of("batch_batch-1"),
            null,
            "s3://bucket/path/file.json",
            50,
            null,null,"test.json", 100L, "json"
        );
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, command::validate);
    }
    
    @Test
    @DisplayName("Should throw exception for null S3 URI")
    void shouldThrowExceptionForNullS3Uri() {
        // Arrange
        ValidateBatchQualityCommand command = new ValidateBatchQualityCommand(
            BatchId.of("batch_batch-1"),
            BankId.of("bank-1"),
            null,
            50,
            null,null,"test.json", 100L, "json"
        );
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, command::validate);
    }
    
    @Test
    @DisplayName("Should throw exception for negative exposure count")
    void shouldThrowExceptionForNegativeExposureCount() {
        // Arrange
        ValidateBatchQualityCommand command = new ValidateBatchQualityCommand(
            BatchId.of("batch_batch-1"),
            BankId.of("bank-1"),
            "s3://bucket/path/file.json",
            -1,
            null,null,"test.json"
        );
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, command::validate);
    }
    
    @Test
    @DisplayName("Should extract S3 bucket from URI")
    void shouldExtractS3BucketFromUri() {
        // Arrange
        ValidateBatchQualityCommand command = ValidateBatchQualityCommand.of(
            BatchId.of("batch_batch-1"),
            BankId.of("bank-1"),
            "s3://my-bucket/path/to/file.json",
            0,
            "test.json"
        );
        
        // Act
        String bucket = command.getS3Bucket();
        
        // Assert
        assertEquals("my-bucket", bucket);
    }
    
    @Test
    @DisplayName("Should extract S3 key from URI")
    void shouldExtractS3KeyFromUri() {
        // Arrange
        ValidateBatchQualityCommand command = ValidateBatchQualityCommand.of(
            BatchId.of("batch_batch-1"),
            BankId.of("bank-1"),
            "s3://my-bucket/path/to/file.json",
            0,
            "test.json"
        );
        
        // Act
        String key = command.getS3Key();
        
        // Assert
        assertEquals("path/to/file.json", key);
    }
}
