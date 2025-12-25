package com.bcbs239.regtech.app.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Health indicator for S3-based file storage.
 *
 * Enabled only when:
 * - management.health.external-services.file-storage.type=s3
 */
@Component("s3Storage")
@ConditionalOnProperty(
        prefix = "management.health.external-services.file-storage",
        name = "type",
        havingValue = "s3"
)
public class S3HealthIndicator implements HealthIndicator {

    private final S3Client s3Client;
    private final String bucketName;

    public S3HealthIndicator(
            S3Client s3Client,
            @Value("${management.health.external-services.file-storage.s3.bucket:}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    @Override
    public Health health() {
        if (bucketName == null || bucketName.isBlank()) {
            return Health.unknown().withDetail("reason", "S3 bucket not configured").build();
        }

        try {
            s3Client.headBucket(b -> b.bucket(bucketName));
            return Health.up().withDetail("bucket", bucketName).build();
        } catch (Exception e) {
            return Health.down().withDetail("bucket", bucketName).withException(e).build();
        }
    }
}
