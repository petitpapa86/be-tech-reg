package com.bcbs239.regtech.core.infrastructure.s3;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * Central S3 configuration exposed from core infrastructure.
 * Creates an S3Client and S3Presigner only when another bean is not already defined.
 */
@Configuration
@ConditionalOnProperty(name = "ingestion.s3.enabled", havingValue = "true", matchIfMissing = true)
public class S3Config {

    private final S3Properties properties;

    public S3Config(S3Properties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean(S3Client.class)
    public S3Client s3Client() {
        var builder = S3Client.builder()
                .region(Region.of(properties.getRegion()));

        if (properties.getAccessKey() != null && !properties.getAccessKey().trim().isEmpty()
                && properties.getSecretKey() != null && !properties.getSecretKey().trim().isEmpty()) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey());
            builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
        }

        if (properties.getEndpoint() != null && !properties.getEndpoint().trim().isEmpty()) {
            builder.endpointOverride(URI.create(properties.getEndpoint()));
        }

        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean(S3Presigner.class)
    public S3Presigner s3Presigner() {
        var builder = S3Presigner.builder().region(Region.of(properties.getRegion()));
        if (properties.getAccessKey() != null && !properties.getAccessKey().trim().isEmpty()
                && properties.getSecretKey() != null && !properties.getSecretKey().trim().isEmpty()) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey());
            builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
        }
        if (properties.getEndpoint() != null && !properties.getEndpoint().trim().isEmpty()) {
            builder.endpointOverride(URI.create(properties.getEndpoint()));
        }
        return builder.build();
    }
}
