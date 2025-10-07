package com.bcbs239.regtech.billing;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Billing Module Configuration
 *
 * This module provides comprehensive billing capabilities including:
 * - Payment processing through Stripe integration
 * - Subscription tier management (STARTER tier with €500.00/month)
 * - Invoice generation and management
 * - Usage tracking and overage calculation
 * - Monthly billing saga orchestration
 * - Dunning process management for overdue payments
 * - Webhook event processing for Stripe integration
 * - Audit trail and compliance reporting
 *
 * The module is organized using Domain-Driven Design principles with
 * the following layer structure:
 * - API Layer: REST controllers and DTOs
 * - Application Layer: Command handlers and saga orchestration
 * - Domain Layer: Aggregate roots, entities, and value objects
 * - Infrastructure Layer: External service integrations and persistence
 *
 * Key Features:
 * - Functional programming patterns with Result<T> and Maybe<T>
 * - Closure-based dependency injection for testability
 * - Event-driven architecture with domain events
 * - Saga pattern for distributed transaction management
 * - Comprehensive error handling without exceptions
 */
@Configuration
@ComponentScan(basePackages = "com.bcbs239.regtech.billing")
@EnableScheduling
public class BillingModule {
}