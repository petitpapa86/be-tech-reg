package com.bcbs239.regtech.dataquality.infrastructure.deprecated.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

/**
 * S3 configuration for the data-quality module.
 * Provides an S3Client bean for S3 storage operations.
 */
@Deprecated
public class S3Config {

    @Value("${ingestion.s3.region:us-east-1}")
    private String region;

    @Value("${ingestion.s3.access-key:}")
    private String accessKey;

    @Value("${ingestion.s3.secret-key:}")
    private String secretKey;

    @Value("${ingestion.s3.endpoint:}")
    private String endpoint;

    /**
     * Creates an S3Client bean for data quality operations.
     * 
     * <p>Configuration priority:
     * <ol>
     *   <li>Explicit credentials (access-key and secret-key from properties)</li>
     *   <li>AWS default credential provider chain (environment variables, IAM roles, etc.)</li>
     * </ol>
     * 
     * <p>If an endpoint is specified, it will be used (useful for LocalStack, MinIO testing).
     * 
     * @return configured S3Client instance
     */
    // Deprecated: bean defined in infrastructure.config.S3Config
    // Intentionally not annotated with @Bean to avoid duplicate bean registration.
    public S3Client s3Client() {
        var builder = S3Client.builder()
                .region(Region.of(region));

        // Configure credentials only if both access key and secret key are provided
        if (accessKey != null && !accessKey.trim().isEmpty() &&
            secretKey != null && !secretKey.trim().isEmpty()) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
        }
        // If no explicit credentials provided, AWS SDK will use default credential provider chain
        // (environment variables, system properties, credential files, IAM roles, etc.)

        // Configure endpoint if provided (for local testing with MinIO, LocalStack, etc.)
        if (endpoint != null && !endpoint.trim().isEmpty()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        return builder.build();
    }
}
