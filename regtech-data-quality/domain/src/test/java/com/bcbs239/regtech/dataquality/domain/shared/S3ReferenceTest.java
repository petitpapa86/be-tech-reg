package com.bcbs239.regtech.dataquality.domain.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("S3Reference Tests")
class S3ReferenceTest {

    @Test
    @DisplayName("Should create valid S3Reference")
    void shouldCreateValidS3Reference() {
        S3Reference ref = new S3Reference(
            "my-bucket", 
            "path/to/file.json", 
            "v123", 
            "s3://my-bucket/path/to/file.json"
        );
        
        assertEquals("my-bucket", ref.bucket());
        assertEquals("path/to/file.json", ref.key());
        assertEquals("v123", ref.versionId());
        assertEquals("s3://my-bucket/path/to/file.json", ref.uri());
    }

    @Test
    @DisplayName("Should create S3Reference using factory method")
    void shouldCreateS3ReferenceUsingFactoryMethod() {
        S3Reference ref = S3Reference.of("my-bucket", "path/to/file.json", "v123");
        
        assertEquals("my-bucket", ref.bucket());
        assertEquals("path/to/file.json", ref.key());
        assertEquals("v123", ref.versionId());
        assertEquals("s3://my-bucket/path/to/file.json", ref.uri());
    }

    @Test
    @DisplayName("Should construct correct URI in factory method")
    void shouldConstructCorrectUriInFactoryMethod() {
        S3Reference ref = S3Reference.of("test-bucket", "folder/subfolder/file.txt", "v456");
        
        assertEquals("s3://test-bucket/folder/subfolder/file.txt", ref.uri());
    }

    @Test
    @DisplayName("Should reject null bucket")
    void shouldRejectNullBucket() {
        assertThrows(NullPointerException.class, 
            () -> new S3Reference(null, "key", "version", "uri"));
        assertThrows(NullPointerException.class, 
            () -> S3Reference.of(null, "key", "version"));
    }

    @Test
    @DisplayName("Should reject null key")
    void shouldRejectNullKey() {
        assertThrows(NullPointerException.class, 
            () -> new S3Reference("bucket", null, "version", "uri"));
        assertThrows(NullPointerException.class, 
            () -> S3Reference.of("bucket", null, "version"));
    }

    @Test
    @DisplayName("Should reject null version ID")
    void shouldRejectNullVersionId() {
        assertThrows(NullPointerException.class, 
            () -> new S3Reference("bucket", "key", null, "uri"));
        assertThrows(NullPointerException.class, 
            () -> S3Reference.of("bucket", "key", null));
    }

    @Test
    @DisplayName("Should reject null URI")
    void shouldRejectNullUri() {
        assertThrows(NullPointerException.class, 
            () -> new S3Reference("bucket", "key", "version", null));
    }

    @Test
    @DisplayName("Should reject empty bucket")
    void shouldRejectEmptyBucket() {
        assertThrows(IllegalArgumentException.class, 
            () -> new S3Reference("", "key", "version", "uri"));
        assertThrows(IllegalArgumentException.class, 
            () -> new S3Reference("   ", "key", "version", "uri"));
        assertThrows(IllegalArgumentException.class, 
            () -> S3Reference.of("", "key", "version"));
    }

    @Test
    @DisplayName("Should reject empty key")
    void shouldRejectEmptyKey() {
        assertThrows(IllegalArgumentException.class, 
            () -> new S3Reference("bucket", "", "version", "uri"));
        assertThrows(IllegalArgumentException.class, 
            () -> new S3Reference("bucket", "   ", "version", "uri"));
        assertThrows(IllegalArgumentException.class, 
            () -> S3Reference.of("bucket", "", "version"));
    }

    @Test
    @DisplayName("Should reject empty version ID")
    void shouldRejectEmptyVersionId() {
        assertThrows(IllegalArgumentException.class, 
            () -> new S3Reference("bucket", "key", "", "uri"));
        assertThrows(IllegalArgumentException.class, 
            () -> new S3Reference("bucket", "key", "   ", "uri"));
        assertThrows(IllegalArgumentException.class, 
            () -> S3Reference.of("bucket", "key", ""));
    }

    @Test
    @DisplayName("Should reject empty URI")
    void shouldRejectEmptyUri() {
        assertThrows(IllegalArgumentException.class, 
            () -> new S3Reference("bucket", "key", "version", ""));
        assertThrows(IllegalArgumentException.class, 
            () -> new S3Reference("bucket", "key", "version", "   "));
    }

    @Test
    @DisplayName("Should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
        S3Reference ref1 = S3Reference.of("bucket", "key", "v1");
        S3Reference ref2 = S3Reference.of("bucket", "key", "v1");
        S3Reference ref3 = S3Reference.of("bucket", "key", "v2");
        S3Reference ref4 = S3Reference.of("other-bucket", "key", "v1");
        
        assertEquals(ref1, ref2);
        assertNotEquals(ref1, ref3); // Different version
        assertNotEquals(ref1, ref4); // Different bucket
    }

    @Test
    @DisplayName("Should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly() {
        S3Reference ref1 = S3Reference.of("bucket", "key", "v1");
        S3Reference ref2 = S3Reference.of("bucket", "key", "v1");
        
        assertEquals(ref1.hashCode(), ref2.hashCode());
    }

    @Test
    @DisplayName("Should handle complex S3 paths")
    void shouldHandleComplexS3Paths() {
        S3Reference ref = S3Reference.of(
            "my-bucket-name", 
            "data/quality/reports/2023/11/15/report-123.json", 
            "abc123"
        );
        
        assertEquals("my-bucket-name", ref.bucket());
        assertEquals("data/quality/reports/2023/11/15/report-123.json", ref.key());
        assertEquals("s3://my-bucket-name/data/quality/reports/2023/11/15/report-123.json", ref.uri());
    }

    @Test
    @DisplayName("Should be usable as map key")
    void shouldBeUsableAsMapKey() {
        S3Reference key1 = S3Reference.of("bucket", "key", "v1");
        S3Reference key2 = S3Reference.of("bucket", "key", "v1");
        S3Reference key3 = S3Reference.of("bucket", "key", "v2");
        
        java.util.Map<S3Reference, String> map = new java.util.HashMap<>();
        map.put(key1, "Value1");
        
        assertEquals("Value1", map.get(key2)); // Same S3Reference should retrieve value
        assertNull(map.get(key3)); // Different version should not
    }
}
