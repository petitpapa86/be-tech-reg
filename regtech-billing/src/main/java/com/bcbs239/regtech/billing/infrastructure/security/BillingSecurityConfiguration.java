package com.bcbs239.regtech.billing.infrastructure.security;

import com.bcbs239.regtech.core.security.SecurityConfigurationRegistry;
import com.bcbs239.regtech.core.security.SecurityConfigurationRegistry.ModuleSecurityConfiguration;
import com.bcbs239.regtech.core.security.authorization.Permission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.annotation.PostConstruct;

/**
 * Billing module security configuration.
 * Handles billing-specific security concerns like webhook validation and rate limiting.
 */
@Configuration
public class BillingSecurityConfiguration implements ModuleSecurityConfiguration {

    @Autowired
    private SecurityConfigurationRegistry securityConfigurationRegistry;

    @Value("${billing.webhook.stripe.endpoint-secret:}")
    private String stripeWebhookSecret;

    @Value("${billing.security.rate-limit.payment.requests-per-minute:10}")
    private int paymentRateLimit;

    @PostConstruct
    public void registerSecurityConfiguration() {
        securityConfigurationRegistry.registerModuleSecurityConfiguration("billing", this);
    }

    @Override
    public String[] getPathPatterns() {
        return new String[]{"/api/v1/billing/**", "/api/v1/subscriptions/**"};
    }

    @Override
    public int getOrder() {
        return 2; // After IAM (order 1)
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                // Webhook endpoints - signature validation only
                .requestMatchers("/api/v1/billing/webhooks/**").permitAll()

                // Health endpoints - public
                .requestMatchers("/api/v1/billing/health/**").permitAll()

                // Payment processing - requires billing payment permission
                .requestMatchers("/api/v1/billing/process-payment").hasAuthority(Permission.BILLING_PROCESS_PAYMENT)

                // Subscription management - requires billing subscription permission
                .requestMatchers("/api/v1/subscriptions/**").hasAuthority(Permission.BILLING_MANAGE_SUBSCRIPTIONS)

                // Invoice viewing - requires billing read permission
                .requestMatchers("/api/v1/billing/invoices/**").hasAuthority(Permission.BILLING_VIEW_INVOICES)

                // Monitoring endpoints - requires billing admin permission
                .requestMatchers("/api/v1/billing/monitoring/**").hasAuthority(Permission.BILLING_ADMIN)

                // Scheduling endpoints - requires billing admin permission
                .requestMatchers("/api/v1/billing/scheduling/**").hasAuthority(Permission.BILLING_ADMIN)

                // Webhook management - requires webhook management permission
                .requestMatchers("/api/v1/billing/webhook-config/**").hasAuthority(Permission.BILLING_WEBHOOK_MANAGE)

                .anyRequest().authenticated()
            )
            .csrf(csrf -> csrf
                // Disable CSRF for webhook endpoints (they use signature validation)
                .ignoringRequestMatchers("/api/v1/billing/webhooks/**")
            )
            .addFilterBefore(webhookSignatureValidationFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(billingRateLimitingFilter(), UsernamePasswordAuthenticationFilter.class);
    }

    @Bean
    public WebhookSignatureValidationFilter webhookSignatureValidationFilter() {
        return new WebhookSignatureValidationFilter(stripeWebhookSecret);
    }

    @Bean
    public BillingRateLimitingFilter billingRateLimitingFilter() {
        return new BillingRateLimitingFilter(paymentRateLimit);
    }
}
