package com.bcbs239.regtech.core.infrastructure.storage;

import com.bcbs239.regtech.core.domain.storage.StorageType;
import com.bcbs239.regtech.core.domain.storage.StorageUri;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for StorageUri parsing and validation.
 * 
 * <p>Tests cover all storage types:
 * <ul>
 *   <li>S3 URIs (s3://bucket/key)</li>
 *   <li>Absolute local paths (file:///path, /path, C:/path)</li>
 *   <li>Relative local paths (data/file.json)</li>
 *   <li>Invalid URIs (null, empty, malformed)</li>
 * </ul>
 */
@DisplayName("StorageUri - URI Parsing and Validation")
class StorageUriTest {
    
    // ========================================================================
    // S3 URI Tests
    // ========================================================================
    
    @Test
    @DisplayName("Should parse valid S3 URI with bucket and key")
    void shouldParseValidS3Uri() {
        // Given
        String uri = "s3://my-bucket/path/to/file.json";
        
        // When
        StorageUri storageUri = StorageUri.parse(uri);
        
        // Then
        assertThat(storageUri.getType()).isEqualTo(StorageType.S3);
        assertThat(storageUri.getBucket()).isEqualTo("my-bucket");
        assertThat(storageUri.getKey()).isEqualTo("path/to/file.json");
        assertThat(storageUri.getFilePath()).isNull();
    }
    
    @Test
    @DisplayName("Should parse S3 URI with nested path")
    void shouldParseS3UriWithNestedPath() {
        // Given
        String uri = "s3://regtech-storage/reports/2024/january/report-001.json";
        
        // When
        StorageUri storageUri = StorageUri.parse(uri);
        
        // Then
        assertThat(storageUri.getType()).isEqualTo(StorageType.S3);
        assertThat(storageUri.getBucket()).isEqualTo("regtech-storage");
        assertThat(storageUri.getKey()).isEqualTo("reports/2024/january/report-001.json");
    }
    
    @Test
    @DisplayName("Should parse S3 URI with bucket only (no key)")
    void shouldParseS3UriWithBucketOnly() {
        // Given
        String uri = "s3://my-bucket/";
        
        // When
        StorageUri storageUri = StorageUri.parse(uri);
        
        // Then
        assertThat(storageUri.getType()).isEqualTo(StorageType.S3);
        assertThat(storageUri.getBucket()).isEqualTo("my-bucket");
        assertThat(storageUri.getKey()).isEqualTo("");
    }
    
    @Test
    @DisplayName("Should parse S3 URI with special characters in key")
    void shouldParseS3UriWithSpecialCharacters() {
        // Given
        String uri = "s3://my-bucket/path%20with%20spaces/file-name_123.json";
        
        // When
        StorageUri storageUri = StorageUri.parse(uri);
        
        // Then
        assertThat(storageUri.getType()).isEqualTo(StorageType.S3);
        assertThat(storageUri.getBucket()).isEqualTo("my-bucket");
        // Note: URI.getPath() automatically decodes percent-encoded characters
        assertThat(storageUri.getKey()).isEqualTo("path with spaces/file-name_123.json");
    }
    
    // ========================================================================
    // Absolute Local Path Tests (file:// scheme)
    // ========================================================================
    
    @Test
    @DisplayName("Should parse absolute local path with file:// scheme (Unix)")
    void shouldParseAbsoluteLocalPathWithFileSchemeUnix() {
        // Given
        String uri = "file:///var/data/storage/file.json";
        
        // When
        StorageUri storageUri = StorageUri.parse(uri);
        
        // Then
        assertThat(storageUri.getType()).isEqualTo(StorageType.LOCAL_ABSOLUTE);
        assertThat(storageUri.getFilePath()).isEqualTo("/var/data/storage/file.json");
        assertThat(storageUri.getBucket()).isNull();
        assertThat(storageUri.getKey()).isNull();
    }
    
    @Test
    @DisplayName("Should parse absolute local path with file:// scheme (Windows)")
    void shouldParseAbsoluteLocalPathWithFileSchemeWindows() {
        // Given
        String uri = "file:///C:/Users/data/file.json";
        
        // When
        StorageUri storageUri = StorageUri.parse(uri);
        
        // Then
        assertThat(storageUri.getType()).isEqualTo(StorageType.LOCAL_ABSOLUTE);
        // Note: file:// URIs follow RFC 8089 - Windows paths retain leading slash after file:// prefix
        assertThat(storageUri.getFilePath()).isEqualTo("/C:/Users/data/file.json");
        assertThat(storageUri.getBucket()).isNull();
        assertThat(storageUri.getKey()).isNull();
    }
    
    // ========================================================================
    // Absolute Local Path Tests (no scheme)
    // ========================================================================
    
    @Test
    @DisplayName("Should parse absolute local path starting with / (Unix)")
    void shouldParseAbsoluteLocalPathUnix() {
        // Given
        String uri = "/var/data/storage/file.json";
        
        // When
        StorageUri storageUri = StorageUri.parse(uri);
        
        // Then
        assertThat(storageUri.getType()).isEqualTo(StorageType.LOCAL_ABSOLUTE);
        assertThat(storageUri.getFilePath()).isEqualTo("/var/data/storage/file.json");
    }
    
    @Test
    @DisplayName("Should parse absolute local path with drive letter (Windows)")
    void shouldParseAbsoluteLocalPathWindows() {
        // Given
        String uri = "C:/Users/data/file.json";
        
        // When
        StorageUri storageUri = StorageUri.parse(uri);
        
        // Then
        assertThat(storageUri.getType()).isEqualTo(StorageType.LOCAL_ABSOLUTE);
        assertThat(storageUri.getFilePath()).isEqualTo("C:/Users/data/file.json");
    }
    
    @Test
    @DisplayName("Should parse absolute local path with backslashes (Windows)")
    void shouldParseAbsoluteLocalPathWindowsBackslashes() {
        // Given
        String uri = "C:\\Users\\data\\file.json";
        
        // When
        StorageUri storageUri = StorageUri.parse(uri);
        
        // Then
        assertThat(storageUri.getType()).isEqualTo(StorageType.LOCAL_ABSOLUTE);
        // Note: StorageUri preserves the original path format (backslashes on Windows)
        assertThat(storageUri.getFilePath()).isEqualTo("C:\\Users\\data\\file.json");
        assertThat(storageUri.getBucket()).isNull();
        assertThat(storageUri.getKey()).isNull();
    }
    
    // ========================================================================
    // Relative Local Path Tests
    // ========================================================================
    
    @Test
    @DisplayName("Should parse relative local path")
    void shouldParseRelativeLocalPath() {
        // Given
        String uri = "data/storage/file.json";
        
        // When
        StorageUri storageUri = StorageUri.parse(uri);
        
        // Then
        assertThat(storageUri.getType()).isEqualTo(StorageType.LOCAL_RELATIVE);
        assertThat(storageUri.getFilePath()).isEqualTo("data/storage/file.json");
    }
    
    @Test
    @DisplayName("Should parse relative local path with ./")
    void shouldParseRelativeLocalPathWithDotSlash() {
        // Given
        String uri = "./data/file.json";
        
        // When
        StorageUri storageUri = StorageUri.parse(uri);
        
        // Then
        assertThat(storageUri.getType()).isEqualTo(StorageType.LOCAL_RELATIVE);
        assertThat(storageUri.getFilePath()).isEqualTo("./data/file.json");
    }
    
    @Test
    @DisplayName("Should parse relative local path with ../")
    void shouldParseRelativeLocalPathWithParentDirectory() {
        // Given
        String uri = "../data/file.json";
        
        // When
        StorageUri storageUri = StorageUri.parse(uri);
        
        // Then
        assertThat(storageUri.getType()).isEqualTo(StorageType.LOCAL_RELATIVE);
        assertThat(storageUri.getFilePath()).isEqualTo("../data/file.json");
    }
    
    @Test
    @DisplayName("Should parse single filename as relative path")
    void shouldParseSingleFilenameAsRelativePath() {
        // Given
        String uri = "file.json";
        
        // When
        StorageUri storageUri = StorageUri.parse(uri);
        
        // Then
        assertThat(storageUri.getType()).isEqualTo(StorageType.LOCAL_RELATIVE);
        assertThat(storageUri.getFilePath()).isEqualTo("file.json");
    }
    
    // ========================================================================
    // Invalid URI Tests
    // ========================================================================
    
    @Test
    @DisplayName("Should throw exception for null URI")
    void shouldThrowExceptionForNullUri() {
        // Note: Objects.requireNonNull() throws NullPointerException (Java standard behavior)
        assertThatThrownBy(() -> StorageUri.parse(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("URI cannot be null");
    }
    
    @Test
    @DisplayName("Should throw exception for empty URI")
    void shouldThrowExceptionForEmptyUri() {
        // Note: Domain uses "blank" (more precise - covers empty + whitespace)
        assertThatThrownBy(() -> StorageUri.parse(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("URI cannot be blank");
    }
    
    @Test
    @DisplayName("Should throw exception for blank URI")
    void shouldThrowExceptionForBlankUri() {
        // Note: Domain uses "blank" (more precise - covers empty + whitespace)
        assertThatThrownBy(() -> StorageUri.parse("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("URI cannot be blank");
    }

    @Test
    @DisplayName("Should throw exception for S3 URI without bucket")
    void shouldThrowExceptionForS3UriWithoutBucket() {
        assertThatThrownBy(() -> StorageUri.parse("s3://"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("S3 URI must include a bucket name");
    }
    
    @Test
    @DisplayName("Should throw exception for unsupported URI scheme")
    void shouldThrowExceptionForUnsupportedScheme() {
        assertThatThrownBy(() -> StorageUri.parse("http://example.com/file.json"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported URI scheme");
    }
    
    // ========================================================================
    // Edge Case Tests
    // ========================================================================
    
    @Test
    @DisplayName("Should handle S3 URI with query parameters")
    void shouldHandleS3UriWithQueryParameters() {
        // Given
        String uri = "s3://my-bucket/file.json?versionId=abc123";
        
        // When
        StorageUri storageUri = StorageUri.parse(uri);
        
        // Then
        assertThat(storageUri.getType()).isEqualTo(StorageType.S3);
        assertThat(storageUri.getBucket()).isEqualTo("my-bucket");
        // Query parameters are part of the key
        assertThat(storageUri.getKey()).contains("file.json");
    }
    
    @Test
    @DisplayName("Should handle very long paths")
    void shouldHandleVeryLongPaths() {
        // Given
        String longPath = "data/" + "very-long-directory-name/".repeat(50) + "file.json";
        
        // When
        StorageUri storageUri = StorageUri.parse(longPath);
        
        // Then
        assertThat(storageUri.getType()).isEqualTo(StorageType.LOCAL_RELATIVE);
        assertThat(storageUri.getFilePath()).isEqualTo(longPath);
        assertThat(storageUri.getFilePath().length()).isGreaterThan(1000);
    }
    
    @Test
    @DisplayName("Should preserve original URI string")
    void shouldPreserveOriginalUriString() {
        // Given
        String uri = "s3://my-bucket/path/to/file.json";
        
        // When
        StorageUri storageUri = StorageUri.parse(uri);
        
        // Then
        assertThat(storageUri.uri()).isEqualTo(uri);
    }
    
    // ========================================================================
    // toString() Tests
    // ========================================================================
    
    @Test
    @DisplayName("Should return original URI in toString()")
    void shouldReturnOriginalUriInToString() {
        // Given
        String uri = "s3://my-bucket/file.json";
        StorageUri storageUri = StorageUri.parse(uri);
        
        // When
        String result = storageUri.toString();
        
        // Then
        assertThat(result).contains("s3://my-bucket/file.json");
    }
}
