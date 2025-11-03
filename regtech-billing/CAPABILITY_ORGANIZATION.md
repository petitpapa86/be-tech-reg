# Billing Module - Capability-Based Organization

## Overview
Reorganizing the regtech-billing module from a mixed technical/business structure to a pure capability-based structure that aligns with business functionality.

## Current Structure Analysis
The billing module currently has:
- **API Layer**: Mixed business-focused controllers (subscriptions, billing, webhooks, monitoring)
- **Application Layer**: Business-focused commands but scattered across individual folders
- **Domain Layer**: Well-organized by business concepts (billing, subscriptions, invoices, dunning)
- **Infrastructure Layer**: Technical pattern-based organization

## Identified Business Capabilities

### 1. Subscription Management
**Purpose**: Managing subscription lifecycle (create, cancel, modify)
- Commands: CreateSubscription, CancelSubscription, GetSubscription
- Domain: Subscription aggregate, SubscriptionTier, SubscriptionStatus
- API: SubscriptionController

### 2. Payment Processing  
**Purpose**: Processing payments and managing payment methods
- Commands: ProcessPayment
- Domain: Payment-related value objects, PaymentMethodId
- Integration: Stripe payment processing

### 3. Invoice Management
**Purpose**: Generating and managing invoices
- Commands: GenerateInvoice
- Domain: Invoice aggregate, InvoiceLineItem, InvoiceStatus
- Policies: Monthly billing saga

### 4. Dunning Management
**Purpose**: Handling overdue payments and collection processes
- Domain: DunningCase, DunningAction, DunningStep
- Jobs: DunningProcessScheduler, DunningActionExecutor
- Policies: Dunning workflows

### 5. Integration
**Purpose**: External integrations and cross-module communication
- Commands: ProcessWebhook
- Infrastructure: Stripe integration, messaging, outbox
- API: WebhookController

### 6. Monitoring
**Purpose**: Health checks, metrics, and observability
- Infrastructure: Health indicators, performance metrics
- API: BillingMonitoringController

## Reorganization Plan

### Application Layer Reorganization
Transform from command-based folders to capability-based organization:

**Before:**
```
application/
├── cancelsubscription/
├── createsubscription/
├── generateinvoice/
├── getsubscription/
├── processpayment/
├── processwebhook/
├── handlers/
├── policies/
└── shared/
```

**After:**
```
application/
├── subscriptions/          # Subscription Management
├── payments/               # Payment Processing  
├── invoicing/              # Invoice Management
├── dunning/                # Dunning Management
├── integration/            # External Integration
├── monitoring/             # Monitoring & Health
└── shared/                 # Shared utilities
```

### API Layer Reorganization
The API layer is already well-organized by business capabilities, minimal changes needed.

### Infrastructure Layer Reorganization
Transform from technical patterns to capability-aligned organization.

This reorganization will provide:
- Better business alignment
- Improved team ownership
- Clearer capability boundaries
- Easier maintenance and evolution