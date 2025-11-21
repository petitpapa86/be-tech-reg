package com.bcbs239.regtech.reportgeneration.infrastructure.templates;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Configuration for Thymeleaf template engine used for HTML report generation.
 * 
 * This configuration:
 * - Sets up template resolver for classpath:/templates/reports/
 * - Enables caching in production for performance
 * - Disables caching in development for easier template editing
 * - Configures UTF-8 encoding for international characters
 * 
 * Requirements: 6.1
 */
@Configuration
public class ThymeleafConfiguration {
    
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;
    
    /**
     * Creates and configures the template resolver.
     * 
     * Template resolver configuration:
     * - Prefix: classpath:/templates/reports/
     * - Suffix: .html
     * - Template mode: HTML
     * - Character encoding: UTF-8
     * - Cacheable: true in production, false in development
     * 
     * @return configured ClassLoaderTemplateResolver
     */
    @Bean
    public ClassLoaderTemplateResolver templateResolver() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        
        // Set template location
        templateResolver.setPrefix("templates/reports/");
        templateResolver.setSuffix(".html");
        
        // Set template mode to HTML5
        templateResolver.setTemplateMode(TemplateMode.HTML);
        
        // Set character encoding
        templateResolver.setCharacterEncoding("UTF-8");
        
        // Enable caching in production, disable in development
        boolean isProduction = "prod".equalsIgnoreCase(activeProfile) || 
                              "production".equalsIgnoreCase(activeProfile);
        templateResolver.setCacheable(isProduction);
        
        // Set cache TTL to 1 hour in production
        if (isProduction) {
            templateResolver.setCacheTTLMs(3600000L); // 1 hour
        }
        
        // Set order for template resolution
        templateResolver.setOrder(1);
        
        return templateResolver;
    }
    
    /**
     * Creates and configures the Spring template engine.
     * 
     * The template engine is configured with:
     * - Custom template resolver
     * - Spring integration for message resolution
     * - Expression dialect for Thymeleaf expressions
     * 
     * @param templateResolver the configured template resolver
     * @return configured SpringTemplateEngine
     */
    @Bean
    public SpringTemplateEngine templateEngine(ClassLoaderTemplateResolver templateResolver) {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        
        // Set template resolver
        templateEngine.setTemplateResolver(templateResolver);
        
        // Enable Spring EL compiler for better performance
        templateEngine.setEnableSpringELCompiler(true);
        
        return templateEngine;
    }
}
