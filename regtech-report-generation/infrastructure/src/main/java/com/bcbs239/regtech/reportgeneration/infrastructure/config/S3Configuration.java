package com.bcbs239.regtech.reportgeneration.infrastructure.config;

import org.springframework.context.annotation.Configuration;

/**
 * Placeholder S3Configuration kept for module compatibility.
 * Actual S3 client and presigner beans are provided by `regtech-core` module
 * via `com.bcbs239.regtech.core.infrastructure.s3.S3Config`.
 */
@Configuration
@Deprecated
public class S3Configuration {
    // Intentionally empty. Use core S3Config for S3 beans.
}
