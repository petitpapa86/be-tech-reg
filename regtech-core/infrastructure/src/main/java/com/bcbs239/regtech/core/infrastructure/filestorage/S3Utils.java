package com.bcbs239.regtech.core.infrastructure.filestorage;

import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

import java.util.Map;
import java.util.Optional;

/**
 * Small S3 helper utilities to centralize common S3 behaviors (encryption, metadata normalization).
 * Keep this class minimal and dependency-free so other modules can rely on consistent behavior.
 */
public final class S3Utils {

    private S3Utils() {}

    /**
     * Build a PutObjectRequest with sensible defaults for server-side encryption and metadata.
     * If a KMS key id is provided it will configure AWS_KMS with that key, otherwise AES256 is used.
     */
    public static PutObjectRequest buildPutObjectRequest(
            String bucket,
            String key,
            String contentType,
            Long contentLength,
            Map<String, String> metadata,
            String kmsKeyId
    ) {
        PutObjectRequest.Builder builder = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key);

        if (contentType != null && !contentType.isBlank()) {
            builder.contentType(contentType);
        }

        if (contentLength != null && contentLength >= 0) {
            builder.contentLength(contentLength);
        }

        if (metadata != null && !metadata.isEmpty()) {
            builder.metadata(metadata);
        }

        if (kmsKeyId != null && !kmsKeyId.isBlank()) {
            builder.serverSideEncryption(ServerSideEncryption.AWS_KMS)
                    .ssekmsKeyId(kmsKeyId);
        } else {
            builder.serverSideEncryption(ServerSideEncryption.AES256);
        }

        return builder.build();
    }

    /**
     * Simple holder for parsed S3 bucket/key.
     */
    public record S3BucketKey(String bucket, String key) {}

    /**
     * Parse an S3 URI like s3://bucket/path/to/object and return bucket/key if valid.
     * Uses a simple manual parser to be robust across JVM URI parsing differences.
     */
    public static Optional<S3BucketKey> parseS3Uri(String s3Uri) {
        if (s3Uri == null) return Optional.empty();
        try {
            if (!s3Uri.startsWith("s3://")) return Optional.empty();

            String withoutPrefix = s3Uri.substring(5); // after s3://
            int idx = withoutPrefix.indexOf('/');
            if (idx <= 0) return Optional.empty();

            String bucket = withoutPrefix.substring(0, idx);
            String key = withoutPrefix.substring(idx + 1);

            if (bucket.isBlank() || key.isBlank()) return Optional.empty();
            return Optional.of(new S3BucketKey(bucket, key));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
