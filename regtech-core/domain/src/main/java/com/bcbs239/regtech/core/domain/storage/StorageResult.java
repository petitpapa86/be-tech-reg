package com.bcbs239.regtech.core.domain.storage;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Result of a storage operation (upload/download)
 * 
 * @param uri The storage URI where the data was stored
 * @param contentType The MIME type of the stored content
 * @param sizeBytes The size of the stored content in bytes
 * @param metadata Additional metadata about the stored content
 * @param uploadedAt The timestamp when the content was uploaded
 * @param etag The entity tag (for S3 versioning/caching)
 */
public record StorageResult(
    @NonNull StorageUri uri,
    @Nullable String contentType,
    long sizeBytes,
    @NonNull Map<String, String> metadata,
    @NonNull Instant uploadedAt,
    @Nullable String etag
) {
    
    /**
     * Creates a builder for StorageResult
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Gets a metadata value by key
     * 
     * @param key The metadata key
     * @return The metadata value wrapped in Optional
     */
    public Optional<String> getMetadata(String key) {
        return Optional.ofNullable(metadata.get(key));
    }
    
    /**
     * Builder for StorageResult
     */
    public static class Builder {
        private StorageUri uri;
        private String contentType;
        private long sizeBytes;
        private Map<String, String> metadata = Map.of();
        private Instant uploadedAt = Instant.now();
        private String etag;
        
        public Builder uri(StorageUri uri) {
            this.uri = uri;
            return this;
        }
        
        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }
        
        public Builder sizeBytes(long sizeBytes) {
            this.sizeBytes = sizeBytes;
            return this;
        }
        
        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
            return this;
        }
        
        public Builder uploadedAt(Instant uploadedAt) {
            this.uploadedAt = uploadedAt;
            return this;
        }
        
        public Builder etag(String etag) {
            this.etag = etag;
            return this;
        }
        
        public StorageResult build() {
            if (uri == null) {
                throw new IllegalStateException("URI is required");
            }
            return new StorageResult(uri, contentType, sizeBytes, metadata, uploadedAt, etag);
        }
    }
}
