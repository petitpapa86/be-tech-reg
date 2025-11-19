package com.bcbs239.regtech.reportgeneration.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * Configuration for AWS S3 client and presigner.
 * Supports both production (AWS) and development (LocalStack) environments.
 */
@Configuration
@Slf4j
public class S3Configuration {
    
    @Value("${aws.region:eu-west-1}")
    private String awsRegion;
    
    @Value("${aws.s3.endpoint:}")
    private String s3Endpoint;
    
    @Value("${aws.access-key-id:}")
    private String accessKeyId;
    
    @Value("${aws.secret-access-key:}")
    private String secretAccessKey;
    
    /**
     * Create S3 client bean.
     * Uses LocalStack endpoint in development, AWS in production.
     */
    @Bean
    public S3Client s3Client() {
        log.info("Initializing S3 client [region:{},endpoint:{}]", 
                awsRegion, s3Endpoint.isEmpty() ? "AWS" : s3Endpoint);
        
        var builder = S3Client.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(credentialsProvider());
        
        // Use custom endpoint for LocalStack in development
        if (!s3Endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(s3Endpoint));
            log.info("Using custom S3 endpoint: {}", s3Endpoint);
        }
        
        return builder.build();
    }
    
    /**
     * Create S3 presigner bean for generating presigned URLs.
     */
    @Bean
    public S3Presigner s3Presigner() {
        log.info("Initializing S3 presigner [region:{},endpoint:{}]", 
                awsRegion, s3Endpoint.isEmpty() ? "AWS" : s3Endpoint);
        
        var builder = S3Presigner.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(credentialsProvider());
        
        // Use custom endpoint for LocalStack in development
        if (!s3Endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(s3Endpoint));
            log.info("Using custom S3 presigner endpoint: {}", s3Endpoint);
        }
        
        return builder.build();
    }
    
    /**
     * Create AWS credentials provider.
     * Uses static credentials if provided, otherwise uses default chain.
     */
    private AwsCredentialsProvider credentialsProvider() {
        if (!accessKeyId.isEmpty() && !secretAccessKey.isEmpty()) {
            log.debug("Using static AWS credentials");
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey)
            );
        }
        
        log.debug("Using default AWS credentials provider chain");
        return DefaultCredentialsProvider.create();
    }
}
