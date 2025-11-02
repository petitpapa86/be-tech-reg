package com.bcbs239.regtech.ingestion.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/**
 * Configuration for AWS S3 client with support for both production and local development.
 */
@Configuration
@Slf4j
public class S3ClientConfiguration {
    
    @Value("${regtech.s3.region:us-east-1}")
    private String region;
    
    @Value("${regtech.s3.endpoint:}")
    private String endpoint;
    
    @Value("${regtech.s3.path-style-access:false}")
    private boolean pathStyleAccess;
    
    @Bean
    public S3Client s3Client() {
        var builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(pathStyleAccess)
                        .build());
        
        // Configure endpoint for local development (localstack)
        if (endpoint != null && !endpoint.trim().isEmpty()) {
            log.info("Configuring S3 client with custom endpoint: {}", endpoint);
            builder.endpointOverride(URI.create(endpoint));
        }
        
        S3Client client = builder.build();
        log.info("S3 client configured for region: {}", region);
        
        return client;
    }
}