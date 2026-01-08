package com.bcbs239.regtech.core.domain.storage;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Represents a storage location URI that can be S3 or local filesystem.
 * 
 * <p>Supported formats:
 * <ul>
 *   <li>S3: {@code s3://bucket-name/path/to/file.json}</li>
 *   <li>Local absolute: {@code file:///absolute/path/to/file.json} or {@code /absolute/path}</li>
 *   <li>Local relative: {@code relative/path/to/file.json} or {@code ./relative/path}</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * StorageUri uri = StorageUri.parse("s3://my-bucket/data/batch123.json");
 * 
 * if (uri.getType() == StorageType.S3) {
 *     String bucket = uri.getBucket();  // "my-bucket"
 *     String key = uri.getKey();        // "data/batch123.json"
 * }
 * }</pre>
 * 
 * @param uri The raw URI string
 */
public record StorageUri(@NonNull String uri) {
    
    public StorageUri {
        Objects.requireNonNull(uri, "URI cannot be null");
        if (uri.isBlank()) {
            throw new IllegalArgumentException("URI cannot be blank");
        }
    }
    
    /**
     * Parses a storage URI string into a StorageUri object
     * 
     * @param uriString The URI string to parse
     * @return A StorageUri instance
     * @throws IllegalArgumentException if the URI format is invalid
     */
    public static StorageUri parse(@NonNull String uriString) {
        StorageUri result = new StorageUri(uriString);
        
        // Validate S3 URIs have a bucket
        if (result.getType() == StorageType.S3) {
            String bucket = result.getBucket();
            if (bucket == null || bucket.isBlank()) {
                throw new IllegalArgumentException("S3 URI must include a bucket name");
            }
        }
        
        // Validate scheme is supported (but skip validation for Windows absolute paths like "C:/..." or "C:\...")
        // Windows paths don't have URI schemes and should be treated as local file paths
        boolean isWindowsPath = uriString.length() >= 2 && 
                                Character.isLetter(uriString.charAt(0)) && 
                                uriString.charAt(1) == ':';
        
        if (!isWindowsPath) {
            try {
                java.net.URI parsed = java.net.URI.create(uriString);
                String scheme = parsed.getScheme();
                if (scheme != null && !scheme.equals("s3") && !scheme.equals("file") && !scheme.equals("memory")) {
                    throw new IllegalArgumentException("Unsupported URI scheme: " + scheme);
                }
            } catch (IllegalArgumentException e) {
                // If it's already an IAE from validation above, re-throw it
                if (e.getMessage().startsWith("S3 URI") || e.getMessage().startsWith("Unsupported")) {
                    throw e;
                }
                // Otherwise, it's a malformed URI - wrap it (but only if not a Windows path with backslashes)
                boolean hasBackslashes = uriString.contains("\\");
                if (!hasBackslashes) {
                    throw new IllegalArgumentException("Invalid URI format: " + e.getMessage(), e);
                }
                // Windows paths with backslashes are acceptable - they'll be normalized by StorageUri
            }
        }
        
        return result;
    }
    
    /**
     * Creates an S3 storage URI
     * 
     * @param bucket S3 bucket name
     * @param key S3 object key
     * @return A StorageUri instance
     */
    public static StorageUri s3(@NonNull String bucket, @NonNull String key) {
        Objects.requireNonNull(bucket, "Bucket cannot be null");
        Objects.requireNonNull(key, "Key cannot be null");
        return new StorageUri("s3://" + bucket + "/" + key);
    }
    
    /**
     * Creates a local filesystem storage URI
     * 
     * @param path Local filesystem path
     * @return A StorageUri instance
     */
    public static StorageUri local(@NonNull String path) {
        Objects.requireNonNull(path, "Path cannot be null");
        
        // If already has file:// prefix, use as-is
        if (path.startsWith("file://")) {
            return new StorageUri(path);
        }
        
        // If absolute path, add file:// prefix
        Path filePath = Paths.get(path);
        if (filePath.isAbsolute()) {
            return new StorageUri("file://" + path);
        }
        
        // Relative path
        return new StorageUri(path);
    }
    
    /**
     * Determines the storage type from the URI
     * 
     * @return The storage type
     */
    public StorageType getType() {
        if (uri.startsWith("s3://")) {
            return StorageType.S3;
        }
        
        if (uri.startsWith("file://")) {
            return StorageType.LOCAL_ABSOLUTE;
        }
        
        if (uri.startsWith("memory://")) {
            return StorageType.MEMORY;
        }
        
        // Check if it's an absolute path (Windows or Unix)
        if (uri.startsWith("/") || uri.matches("^[A-Za-z]:[/\\\\].*")) {
            return StorageType.LOCAL_ABSOLUTE;
        }
        
        // Default to relative path
        return StorageType.LOCAL_RELATIVE;
    }
    
    /**
     * Extracts the S3 bucket name from an S3 URI
     * 
     * @return The bucket name, or null if not an S3 URI
     */
    public @Nullable String getBucket() {
        if (getType() != StorageType.S3) {
            return null;
        }
        
        try {
            URI parsed = URI.create(uri);
            return parsed.getHost();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Extracts the S3 object key from an S3 URI
     * 
     * @return The object key, or null if not an S3 URI
     */
    public @Nullable String getKey() {
        if (getType() != StorageType.S3) {
            return null;
        }
        
        try {
            URI parsed = URI.create(uri);
            String path = parsed.getPath();
            // Remove leading slash
            return path.startsWith("/") ? path.substring(1) : path;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Extracts the filesystem path from a local URI
     * 
     * @return The filesystem path, or null if not a local URI
     */
    public @Nullable String getFilePath() {
        StorageType type = getType();
        
        if (type == StorageType.LOCAL_ABSOLUTE) {
            if (uri.startsWith("file://")) {
                return uri.substring(7); // Remove "file://"
            }
            return uri;
        }
        
        if (type == StorageType.LOCAL_RELATIVE) {
            return uri;
        }
        
        return null;
    }
    
    /**
     * Gets the filename from the URI (last path segment)
     * 
     * @return The filename
     */
    public String getFilename() {
        String path = switch (getType()) {
            case S3 -> getKey();
            case LOCAL_ABSOLUTE, LOCAL_RELATIVE -> getFilePath();
            case MEMORY -> uri;
        };
        
        if (path == null) {
            return "unknown";
        }
        
        int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }
    
    @Override
    public String toString() {
        return uri;
    }
}
