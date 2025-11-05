package com.bcbs239.regtech.billing.infrastructure.security;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.net.Webhook;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to validate Stripe webhook signatures.
 * 
 * This filter validates the Stripe-Signature header against the webhook payload
 * using the configured endpoint secret. This ensures that webhook requests
 * are actually coming from Stripe and haven't been tampered with.
 */
public class WebhookSignatureValidationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(WebhookSignatureValidationFilter.class);
    
    private final String webhookSecret;

    public WebhookSignatureValidationFilter(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        // Only validate webhook endpoints
        if (!request.getRequestURI().startsWith("/api/v1/billing/webhooks/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip validation for health check endpoints
        if (request.getRequestURI().endsWith("/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip validation if webhook secret is not configured (development mode)
        if (!StringUtils.hasText(webhookSecret)) {
            logger.warn("Webhook secret not configured - skipping signature validation");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Get the signature header
            String signatureHeader = request.getHeader("Stripe-Signature");
            if (!StringUtils.hasText(signatureHeader)) {
                logger.warn("Missing Stripe-Signature header for webhook request");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\":\"Missing Stripe-Signature header\"}");
                return;
            }

            // Read the request body
            String payload = request.getReader().lines()
                .reduce("", (accumulator, actual) -> accumulator + actual);

            // Validate the signature
            try {
                Webhook.constructEvent(payload, signatureHeader, webhookSecret);
                logger.debug("Webhook signature validation successful");
            } catch (SignatureVerificationException e) {
                logger.warn("Webhook signature validation failed: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\":\"Invalid webhook signature\"}");
                return;
            }

            // Create a wrapper to allow re-reading the request body
            CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request, payload);
            filterChain.doFilter(wrappedRequest, response);

        } catch (Exception e) {
            logger.error("Error during webhook signature validation", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"Internal server error during signature validation\"}");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only filter webhook endpoints
        return !request.getRequestURI().startsWith("/api/v1/billing/webhooks/");
    }
}
