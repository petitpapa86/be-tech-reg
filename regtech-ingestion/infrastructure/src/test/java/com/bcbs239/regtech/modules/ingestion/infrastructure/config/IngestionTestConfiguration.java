package com.bcbs239.regtech.modules.ingestion.infrastructure.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

/**
 * Test configuration for integration tests.
 * Provides test containers and mock services for testing.
 */
@TestConfiguration
@Profile("test")
public class IngestionTestConfiguration {
    
    @Bean
    @Primary
    public LocalStackContainer localStackContainer() {
        LocalStackContainer localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.0"))
                .withServices(LocalStackContainer.Service.S3);
        localStack.start();
        return localStack;
    }
    
    @Bean
    @Primary
    public S3Client testS3Client(LocalStackContainer localStackContainer) {
        return S3Client.builder()
                .endpointOverride(localStackContainer.getEndpoint())
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test", "test")))
                .region(Region.US_EAST_1)
                .forcePathStyle(true)
                .build();
    }
    
    @Bean
    @Primary
    public PostgreSQLContainer<?> postgreSQLContainer() {
        PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
                .withDatabaseName("ingestion_test")
                .withUsername("test")
                .withPassword("test");
        postgres.start();
        return postgres;
    }
}