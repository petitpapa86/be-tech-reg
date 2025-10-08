package com.bcbs239.regtech.billing.infrastructure.validation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration class for billing validation setup.
 * Ensures proper validation configuration for the billing module.
 */
@Configuration
public class BillingValidationConfiguration implements WebMvcConfigurer {

    private final InputSanitizer inputSanitizer;

    public BillingValidationConfiguration(InputSanitizer inputSanitizer) {
        this.inputSanitizer = inputSanitizer;
    }

    /**
     * Configure the validator factory bean for custom validation
     */
    @Bean
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }

    /**
     * Enable method-level validation for service classes
     */
    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
        processor.setValidator(validator());
        return processor;
    }

    /**
     * Input sanitization interceptor for additional security
     */
    @Bean
    public InputSanitizationInterceptor inputSanitizationInterceptor() {
        return new InputSanitizationInterceptor(inputSanitizer);
    }

    /**
     * Register the input sanitization interceptor
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(inputSanitizationInterceptor())
                .addPathPatterns("/api/billing/**")
                .addPathPatterns("/api/webhooks/**");
    }
}