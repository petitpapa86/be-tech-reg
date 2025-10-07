# Billing Context Design Document

## Overview

The Billing Context implements a comprehensive billing system using Domain-Driven Design principles, functional programming patterns, and the Saga pattern for distributed transaction management. The design follows the established RegTech architecture with closure-based dependency injection, Result types for error handling, and event-driven communication.

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Billing Context                              │
├─────────────────────────────────────────────────────────────────┤
│  API Layer                                                      │
│  ├── BillingController (Payment processing endpoints)           │
│  ├── SubscriptionController (Subscription management)           │
│  └── WebhookController (Stripe webhook handling)                │
├─────────────────────────────────────────────────────────────────┤
│  Application Layer                                              │
│  ├── ProcessPaymentCommandHandler                               │
│  ├── CreateSubscriptionCommandHandler                           │
│  ├── GenerateInvoiceCommandHandler                              │
│  ├── ProcessWebhookCommandHandler                               │
│  └── MonthlyBillingSaga                                         │
├─────────────────────────────────────────────────────────────────┤
│  Domain Layer                                                   │
│  ├── BillingAccount (Aggregate Root)                            │
│  ├── Subscription (Aggregate Root)                              │
│  ├── Invoice (Aggregate Root)                                   │
│  ├── DunningCase (Aggregate Root)                               │
│  └── Value Objects (Money, SubscriptionTier, etc.)             │
├─────────────────────────────────────────────────────────────────┤
│  Infrastructure Layer                                           │
│  ├── StripeService (External payment processing)                │
│  ├── JPA Repositories                                           │
│  ├── Event Publishers                                           │
│  └── Scheduled Jobs                                             │
└─────────────────────────────────────────────────────────────────┘
```

### Integration with Other Contexts

```
┌─────────────┐    Events    ┌─────────────┐    Saga     ┌─────────────┐
│     IAM     │◄────────────►│   Billing   │◄───────────►│  Ingestion  │
│   Context   │              │   Context   │             │   Context   │
└─────────────┘              └─────────────┘             └─────────────┘
      │                            │                           │
      │ UserRegistered            │ PaymentVerified           │ UsageMetrics
      │ UserActivated             │ InvoiceGenerated          │ ExposureCount
      └────────────────────────────┼───────────────────────────┘
                                   │
                            ┌─────────────┐
                            │   Stripe    │
                            │   Service   │
                            └─────────────┘
```

## Components and Interfaces

### Domain Layer

#### BillingAccount Aggregate Root

```java
public class BillingAccount {
    private BillingAccountId id;
    private UserId userId;
    private StripeCustomerId stripeCustomerId;
    private BillingAccountStatus status;
    private PaymentMethodId defaultPaymentMethodId;
    private Money accountBalance;
    private Instant createdAt;
    private Instant updatedAt;
    private long version;

    // Factory method
    public static BillingAccount create(UserId userId, StripeCustomerId stripeCustomerId) {
        BillingAccount account = new BillingAccount();
        account.id = BillingAccountId.generate();
        account.userId = userId;
        account.stripeCustomerId = stripeCustomerId;
        account.status = BillingAccountStatus.PENDING_VERIFICATION;
        account.accountBalance = Money.zero(Currency.EUR);
        account.createdAt = Instant.now();
        account.updatedAt = Instant.now();
        account.version = 0;
        return account;
    }

    // Business methods
    public Result<Void> activate(PaymentMethodId paymentMethodId) {
        if (this.status != BillingAccountStatus.PENDING_VERIFICATION) {
            return Result.failure(ErrorDetail.of("INVALID_STATUS_TRANSITION", 
                "Cannot activate account from status: " + this.status));
        }
        
        this.status = BillingAccountStatus.ACTIVE;
        this.defaultPaymentMethodId = paymentMethodId;
        this.updatedAt = Instant.now();
        this.version++;
        
        return Result.success(null);
    }

    public Result<Void> suspend(String reason) {
        if (this.status == BillingAccountStatus.CANCELLED) {
            return Result.failure(ErrorDetail.of("ACCOUNT_CANCELLED", 
                "Cannot suspend cancelled account"));
        }
        
        this.status = BillingAccountStatus.SUSPENDED;
        this.updatedAt = Instant.now();
        this.version++;
        
        return Result.success(null);
    }

    public boolean canCreateSubscription() {
        return this.status == BillingAccountStatus.ACTIVE;
    }
}
```

#### Subscription Aggregate Root

```java
public class Subscription {
    private SubscriptionId id;
    private BillingAccountId billingAccountId;
    private StripeSubscriptionId stripeSubscriptionId;
    private SubscriptionTier tier;
    private SubscriptionStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Instant createdAt;
    private Instant updatedAt;
    private long version;

    // Factory method
    public static Subscription create(BillingAccountId billingAccountId, 
                                    StripeSubscriptionId stripeSubscriptionId,
                                    SubscriptionTier tier) {
        Subscription subscription = new Subscription();
        subscription.id = SubscriptionId.generate();
        subscription.billingAccountId = billingAccountId;
        subscription.stripeSubscriptionId = stripeSubscriptionId;
        subscription.tier = tier;
        subscription.status = SubscriptionStatus.ACTIVE;
        subscription.startDate = LocalDate.now();
        subscription.createdAt = Instant.now();
        subscription.updatedAt = Instant.now();
        subscription.version = 0;
        return subscription;
    }

    // Business methods
    public Result<Void> cancel(LocalDate cancellationDate) {
        if (this.status == SubscriptionStatus.CANCELLED) {
            return Result.failure(ErrorDetail.of("ALREADY_CANCELLED", 
                "Subscription is already cancelled"));
        }
        
        this.status = SubscriptionStatus.CANCELLED;
        this.endDate = cancellationDate;
        this.updatedAt = Instant.now();
        this.version++;
        
        return Result.success(null);
    }

    public Money getMonthlyAmount() {
        return tier.getMonthlyPrice();
    }

    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE;
    }
}
```

#### Invoice Aggregate Root

```java
public class Invoice {
    private InvoiceId id;
    private BillingAccountId billingAccountId;
    private InvoiceNumber invoiceNumber;
    private StripeInvoiceId stripeInvoiceId;
    private InvoiceStatus status;
    private Money subscriptionAmount;
    private Money overageAmount;
    private Money totalAmount;
    private BillingPeriod billingPeriod;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private Instant paidAt;
    private Instant sentAt;
    private List<InvoiceLineItem> lineItems;
    private Instant createdAt;
    private Instant updatedAt;
    private long version;

    // Factory method
    public static Invoice create(BillingAccountId billingAccountId,
                               StripeInvoiceId stripeInvoiceId,
                               Money subscriptionAmount,
                               Money overageAmount,
                               BillingPeriod billingPeriod) {
        Invoice invoice = new Invoice();
        invoice.id = InvoiceId.generate();
        invoice.billingAccountId = billingAccountId;
        invoice.invoiceNumber = InvoiceNumber.generate();
        invoice.stripeInvoiceId = stripeInvoiceId;
        invoice.status = InvoiceStatus.PENDING;
        invoice.subscriptionAmount = subscriptionAmount;
        invoice.overageAmount = overageAmount;
        invoice.totalAmount = subscriptionAmount.add(overageAmount);
        invoice.billingPeriod = billingPeriod;
        invoice.issueDate = LocalDate.now();
        invoice.dueDate = LocalDate.now().plusDays(14);
        invoice.lineItems = new ArrayList<>();
        invoice.createdAt = Instant.now();
        invoice.updatedAt = Instant.now();
        invoice.version = 0;
        
        // Add line items
        invoice.addLineItem("Subscription - " + billingPeriod.toString(), 
                          subscriptionAmount, 1);
        if (overageAmount.isPositive()) {
            invoice.addLineItem("Usage Overage", overageAmount, 1);
        }
        
        return invoice;
    }

    // Business methods
    public Result<Void> markAsPaid(Instant paidAt) {
        if (this.status == InvoiceStatus.PAID) {
            return Result.failure(ErrorDetail.of("ALREADY_PAID", 
                "Invoice is already marked as paid"));
        }
        
        this.status = InvoiceStatus.PAID;
        this.paidAt = paidAt;
        this.updatedAt = Instant.now();
        this.version++;
        
        return Result.success(null);
    }

    public Result<Void> markAsOverdue() {
        if (this.status != InvoiceStatus.PENDING) {
            return Result.failure(ErrorDetail.of("INVALID_STATUS_TRANSITION", 
                "Cannot mark as overdue from status: " + this.status));
        }
        
        this.status = InvoiceStatus.OVERDUE;
        this.updatedAt = Instant.now();
        this.version++;
        
        return Result.success(null);
    }

    private void addLineItem(String description, Money amount, int quantity) {
        InvoiceLineItem lineItem = new InvoiceLineItem(
            InvoiceLineItemId.generate(),
            description,
            amount,
            quantity,
            amount.multiply(quantity)
        );
        this.lineItems.add(lineItem);
    }

    public boolean isOverdue() {
        return LocalDate.now().isAfter(dueDate) && status == InvoiceStatus.PENDING;
    }
}
```

#### Value Objects

```java
// Money Value Object
public record Money(BigDecimal amount, Currency currency) {
    
    public static Money of(BigDecimal amount, Currency currency) {
        if (amount == null) {
            return new Money(BigDecimal.ZERO, currency);
        }
        return new Money(amount.setScale(4, RoundingMode.HALF_UP), currency);
    }
    
    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }
    
    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add different currencies");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }
    
    public Money multiply(int multiplier) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)), this.currency);
    }
    
    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }
}

// Subscription Tier Value Object
public enum SubscriptionTier {
    STARTER(Money.of(new BigDecimal("500.00"), Currency.EUR), 10000);
    
    private final Money monthlyPrice;
    private final int exposureLimit;
    
    SubscriptionTier(Money monthlyPrice, int exposureLimit) {
        this.monthlyPrice = monthlyPrice;
        this.exposureLimit = exposureLimit;
    }
    
    public Money getMonthlyPrice() { return monthlyPrice; }
    public int getExposureLimit() { return exposureLimit; }
}

// Billing Period Value Object
public record BillingPeriod(LocalDate startDate, LocalDate endDate) {
    
    public static BillingPeriod forMonth(YearMonth yearMonth) {
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        return new BillingPeriod(start, end);
    }
    
    public int getDaysInPeriod() {
        return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }
    
    public Money calculateProRatedAmount(Money monthlyAmount, LocalDate startDate) {
        int totalDays = getDaysInPeriod();
        int remainingDays = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        
        BigDecimal proRationFactor = BigDecimal.valueOf(remainingDays)
            .divide(BigDecimal.valueOf(totalDays), 4, RoundingMode.HALF_UP);
            
        return new Money(monthlyAmount.amount().multiply(proRationFactor), monthlyAmount.currency());
    }
    
    @Override
    public String toString() {
        return startDate.format(DateTimeFormatter.ofPattern("MMM yyyy"));
    }
}
```

### Application Layer

#### ProcessPaymentCommandHandler

```java
@Component
public class ProcessPaymentCommandHandler {

    private final BillingAccountRepository billingAccountRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final CrossModuleEventBus eventBus;

    public ProcessPaymentCommandHandler(BillingAccountRepository billingAccountRepository,
                                      SubscriptionRepository subscriptionRepository,
                                      InvoiceRepository invoiceRepository,
                                      CrossModuleEventBus eventBus) {
        this.billingAccountRepository = billingAccountRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceRepository = invoiceRepository;
        this.eventBus = eventBus;
    }

    public Result<ProcessPaymentResponse> handle(ProcessPaymentCommand command) {
        return processPayment(
            command,
            billingAccountRepository.billingAccountSaver(),
            subscriptionRepository.subscriptionSaver(),
            invoiceRepository.invoiceSaver(),
            eventBus::publishEvent
        );
    }

    // Pure function for payment processing
    static Result<ProcessPaymentResponse> processPayment(
            ProcessPaymentCommand command,
            Function<BillingAccount, Result<BillingAccountId>> billingAccountSaver,
            Function<Subscription, Result<SubscriptionId>> subscriptionSaver,
            Function<Invoice, Result<InvoiceId>> invoiceSaver,
            Consumer<Object> eventPublisher) {

        // Extract user data from correlation ID via saga lookup
        Result<UserData> userDataResult = extractUserDataFromSaga(command.correlationId());
        if (userDataResult.isFailure()) {
            return Result.failure(userDataResult.getError().get());
        }
        UserData userData = userDataResult.getValue().get();

        // Create Stripe customer
        Result<StripeCustomer> stripeCustomerResult = createStripeCustomer(
            userData, command.paymentMethodId()
        );
        if (stripeCustomerResult.isFailure()) {
            return Result.failure(stripeCustomerResult.getError().get());
        }
        StripeCustomer stripeCustomer = stripeCustomerResult.getValue().get();

        // Create billing account
        BillingAccount billingAccount = BillingAccount.create(
            userData.userId(), 
            stripeCustomer.customerId()
        );
        
        Result<Void> activationResult = billingAccount.activate(command.paymentMethodId());
        if (activationResult.isFailure()) {
            return Result.failure(activationResult.getError().get());
        }

        Result<BillingAccountId> saveAccountResult = billingAccountSaver.apply(billingAccount);
        if (saveAccountResult.isFailure()) {
            return Result.failure(saveAccountResult.getError().get());
        }

        // Create subscription
        Result<StripeSubscription> stripeSubscriptionResult = createStripeSubscription(
            stripeCustomer.customerId(), SubscriptionTier.STARTER
        );
        if (stripeSubscriptionResult.isFailure()) {
            return Result.failure(stripeSubscriptionResult.getError().get());
        }
        StripeSubscription stripeSubscription = stripeSubscriptionResult.getValue().get();

        Subscription subscription = Subscription.create(
            billingAccount.getId(),
            stripeSubscription.subscriptionId(),
            SubscriptionTier.STARTER
        );

        Result<SubscriptionId> saveSubscriptionResult = subscriptionSaver.apply(subscription);
        if (saveSubscriptionResult.isFailure()) {
            return Result.failure(saveSubscriptionResult.getError().get());
        }

        // Generate first invoice
        Result<Invoice> invoiceResult = generateProRatedInvoice(
            billingAccount.getId(),
            stripeSubscription.invoiceId(),
            SubscriptionTier.STARTER
        );
        if (invoiceResult.isFailure()) {
            return Result.failure(invoiceResult.getError().get());
        }
        Invoice invoice = invoiceResult.getValue().get();

        Result<InvoiceId> saveInvoiceResult = invoiceSaver.apply(invoice);
        if (saveInvoiceResult.isFailure()) {
            return Result.failure(saveInvoiceResult.getError().get());
        }

        // Publish events
        eventPublisher.accept(new PaymentVerifiedEvent(
            userData.userId(),
            billingAccount.getId(),
            command.correlationId()
        ));

        eventPublisher.accept(new InvoiceGeneratedEvent(
            invoice.getId(),
            billingAccount.getId(),
            invoice.getTotalAmount()
        ));

        return Result.success(new ProcessPaymentResponse(
            billingAccount.getId(),
            subscription.getId(),
            invoice.getId(),
            invoice.getTotalAmount()
        ));
    }

    private static Result<UserData> extractUserDataFromSaga(String correlationId) {
        // Implementation to extract user data from saga context
        // This would typically query the saga repository or event store
        return Result.success(new UserData(
            UserId.fromString("extracted-user-id"),
            "user@example.com",
            "John Doe"
        ));
    }

    private static Result<StripeCustomer> createStripeCustomer(UserData userData, PaymentMethodId paymentMethodId) {
        // Implementation to create Stripe customer
        // This would call the Stripe API through StripeService
        return Result.success(new StripeCustomer(
            StripeCustomerId.fromString("cus_stripe_id"),
            userData.email()
        ));
    }

    private static Result<StripeSubscription> createStripeSubscription(StripeCustomerId customerId, SubscriptionTier tier) {
        // Implementation to create Stripe subscription
        return Result.success(new StripeSubscription(
            StripeSubscriptionId.fromString("sub_stripe_id"),
            StripeInvoiceId.fromString("in_stripe_id")
        ));
    }

    private static Result<Invoice> generateProRatedInvoice(BillingAccountId billingAccountId,
                                                         StripeInvoiceId stripeInvoiceId,
                                                         SubscriptionTier tier) {
        BillingPeriod currentPeriod = BillingPeriod.forMonth(YearMonth.now());
        Money monthlyAmount = tier.getMonthlyPrice();
        Money proRatedAmount = currentPeriod.calculateProRatedAmount(monthlyAmount, LocalDate.now());
        
        Invoice invoice = Invoice.create(
            billingAccountId,
            stripeInvoiceId,
            proRatedAmount,
            Money.zero(Currency.EUR),
            currentPeriod
        );
        
        return Result.success(invoice);
    }
}
```

#### MonthlyBillingSaga

```java
public class MonthlyBillingSagaData extends SagaData {
    private String userId;
    private String billingPeriodId; // "2024-01"
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalExposures;
    private Money subscriptionCharges;
    private Money overageCharges;
    private InvoiceId generatedInvoiceId;
    private BillingStep currentStep = BillingStep.GATHER_METRICS;

    public enum BillingStep {
        GATHER_METRICS,
        CALCULATE_CHARGES,
        GENERATE_INVOICE,
        FINALIZE_BILLING
    }

    // Getters, setters, and business logic methods
}

@Component
public class MonthlyBillingSaga implements Saga<MonthlyBillingSagaData> {

    // Closure dependencies
    private final SagaClosures.MessagePublisher messagePublisher;
    private final SagaClosures.Logger logger;
    private final Function<String, Result<UsageMetrics>> usageMetricsQuery;
    private final Function<Invoice, Result<InvoiceId>> invoiceGenerator;

    public MonthlyBillingSaga(
            SagaClosures.MessagePublisher messagePublisher,
            SagaClosures.Logger logger,
            Function<String, Result<UsageMetrics>> usageMetricsQuery,
            Function<Invoice, Result<InvoiceId>> invoiceGenerator) {
        this.messagePublisher = messagePublisher;
        this.logger = logger;
        this.usageMetricsQuery = usageMetricsQuery;
        this.invoiceGenerator = invoiceGenerator;
    }

    @Override
    public SagaResult execute(MonthlyBillingSagaData sagaData) {
        try {
            switch (sagaData.getCurrentStep()) {
                case GATHER_METRICS:
                    return gatherUsageMetrics(sagaData);
                case CALCULATE_CHARGES:
                    return calculateCharges(sagaData);
                case GENERATE_INVOICE:
                    return generateInvoice(sagaData);
                case FINALIZE_BILLING:
                    return finalizeBilling(sagaData);
                default:
                    return SagaResult.failure("Unknown billing step: " + sagaData.getCurrentStep());
            }
        } catch (Exception e) {
            logger.log("error", "Monthly billing saga failed: {}", e.getMessage());
            return SagaResult.failure("Monthly billing failed: " + e.getMessage());
        }
    }

    private SagaResult gatherUsageMetrics(MonthlyBillingSagaData sagaData) {
        // Query ingestion context for usage metrics
        Result<UsageMetrics> metricsResult = usageMetricsQuery.apply(sagaData.getUserId());
        if (metricsResult.isFailure()) {
            return SagaResult.failure("Failed to gather usage metrics: " + 
                metricsResult.getError().get().getMessage());
        }

        UsageMetrics metrics = metricsResult.getValue().get();
        sagaData.setTotalExposures(metrics.totalExposures());
        sagaData.setCurrentStep(MonthlyBillingSagaData.BillingStep.CALCULATE_CHARGES);

        logger.log("info", "Gathered usage metrics for user {}: {} exposures", 
            sagaData.getUserId(), metrics.totalExposures());

        return SagaResult.success();
    }

    private SagaResult calculateCharges(MonthlyBillingSagaData sagaData) {
        // Calculate subscription and overage charges
        Money subscriptionAmount = SubscriptionTier.STARTER.getMonthlyPrice();
        Money overageAmount = Money.zero(Currency.EUR);

        int exposureLimit = SubscriptionTier.STARTER.getExposureLimit();
        if (sagaData.getTotalExposures() > exposureLimit) {
            int overageExposures = sagaData.getTotalExposures() - exposureLimit;
            // €0.05 per exposure over limit
            BigDecimal overageRate = new BigDecimal("0.05");
            overageAmount = Money.of(
                overageRate.multiply(BigDecimal.valueOf(overageExposures)),
                Currency.EUR
            );
        }

        sagaData.setSubscriptionCharges(subscriptionAmount);
        sagaData.setOverageCharges(overageAmount);
        sagaData.setCurrentStep(MonthlyBillingSagaData.BillingStep.GENERATE_INVOICE);

        logger.log("info", "Calculated charges for user {}: subscription={}, overage={}", 
            sagaData.getUserId(), subscriptionAmount, overageAmount);

        return SagaResult.success();
    }

    private SagaResult generateInvoice(MonthlyBillingSagaData sagaData) {
        // Generate invoice through Stripe and store locally
        BillingPeriod billingPeriod = new BillingPeriod(sagaData.getStartDate(), sagaData.getEndDate());
        
        Invoice invoice = Invoice.create(
            BillingAccountId.fromString("billing-account-id"), // Would be looked up
            StripeInvoiceId.fromString("stripe-invoice-id"), // Would be created via Stripe
            sagaData.getSubscriptionCharges(),
            sagaData.getOverageCharges(),
            billingPeriod
        );

        Result<InvoiceId> invoiceResult = invoiceGenerator.apply(invoice);
        if (invoiceResult.isFailure()) {
            return SagaResult.failure("Failed to generate invoice: " + 
                invoiceResult.getError().get().getMessage());
        }

        sagaData.setGeneratedInvoiceId(invoiceResult.getValue().get());
        sagaData.setCurrentStep(MonthlyBillingSagaData.BillingStep.FINALIZE_BILLING);

        logger.log("info", "Generated invoice {} for user {}", 
            invoiceResult.getValue().get(), sagaData.getUserId());

        return SagaResult.success();
    }

    private SagaResult finalizeBilling(MonthlyBillingSagaData sagaData) {
        // Publish events and complete saga
        messagePublisher.publish(new SagaMessage(
            sagaData.getSagaId(),
            "billing.invoice-generated",
            "billing-saga",
            "iam-context",
            Map.of(
                "userId", sagaData.getUserId(),
                "invoiceId", sagaData.getGeneratedInvoiceId().toString(),
                "totalAmount", sagaData.getSubscriptionCharges().add(sagaData.getOverageCharges())
            )
        ));

        logger.log("info", "Completed monthly billing for user {}", sagaData.getUserId());
        return SagaResult.success();
    }

    @Override
    public SagaResult handleMessage(MonthlyBillingSagaData sagaData, SagaMessage message) {
        // Handle any incoming messages during billing process
        return SagaResult.success();
    }

    @Override
    public SagaResult compensate(MonthlyBillingSagaData sagaData) {
        // Implement compensation logic
        logger.log("warn", "Compensating monthly billing saga for user {}", sagaData.getUserId());
        
        // Reverse any partial operations
        if (sagaData.getGeneratedInvoiceId() != null) {
            // Cancel/void the generated invoice
            logger.log("info", "Cancelling invoice {} during compensation", 
                sagaData.getGeneratedInvoiceId());
        }
        
        return SagaResult.success();
    }

    @Override
    public String getSagaType() {
        return "monthly-billing";
    }
}
```

### Infrastructure Layer

#### Repository Implementations with Closures

```java
@Repository
@Transactional
public class BillingAccountRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Function<BillingAccountId, Maybe<BillingAccount>> billingAccountFinder() {
        return id -> {
            try {
                BillingAccount account = entityManager.find(BillingAccount.class, id.getValue());
                return account != null ? Maybe.some(account) : Maybe.none();
            } catch (Exception e) {
                return Maybe.none();
            }
        };
    }

    public Function<UserId, Maybe<BillingAccount>> billingAccountByUserFinder() {
        return userId -> {
            try {
                BillingAccount account = entityManager.createQuery(
                    "SELECT ba FROM BillingAccount ba WHERE ba.userId = :userId", BillingAccount.class)
                    .setParameter("userId", userId.getValue())
                    .getSingleResult();
                return Maybe.some(account);
            } catch (NoResultException e) {
                return Maybe.none();
            }
        };
    }

    public Function<BillingAccount, Result<BillingAccountId>> billingAccountSaver() {
        return account -> {
            try {
                if (account.getId() == null) {
                    entityManager.persist(account);
                } else {
                    account = entityManager.merge(account);
                }
                return Result.success(account.getId());
            } catch (Exception e) {
                return Result.failure(ErrorDetail.of("BILLING_ACCOUNT_SAVE_FAILED",
                    "Failed to save billing account: " + e.getMessage()));
            }
        };
    }
}

@Repository
@Transactional
public class SubscriptionRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Function<BillingAccountId, Maybe<Subscription>> activeSubscriptionFinder() {
        return billingAccountId -> {
            try {
                Subscription subscription = entityManager.createQuery(
                    "SELECT s FROM Subscription s WHERE s.billingAccountId = :accountId AND s.status = :status",
                    Subscription.class)
                    .setParameter("accountId", billingAccountId.getValue())
                    .setParameter("status", SubscriptionStatus.ACTIVE)
                    .getSingleResult();
                return Maybe.some(subscription);
            } catch (NoResultException e) {
                return Maybe.none();
            }
        };
    }

    public Function<Subscription, Result<SubscriptionId>> subscriptionSaver() {
        return subscription -> {
            try {
                if (subscription.getId() == null) {
                    entityManager.persist(subscription);
                } else {
                    subscription = entityManager.merge(subscription);
                }
                return Result.success(subscription.getId());
            } catch (Exception e) {
                return Result.failure(ErrorDetail.of("SUBSCRIPTION_SAVE_FAILED",
                    "Failed to save subscription: " + e.getMessage()));
            }
        };
    }
}
```

#### Stripe Service Integration

```java
@Service
public class StripeService {

    private final Stripe stripeClient;

    public StripeService(@Value("${stripe.secret-key}") String secretKey) {
        Stripe.apiKey = secretKey;
        this.stripeClient = new Stripe();
    }

    public Result<StripeCustomer> createCustomer(String email, String name, String paymentMethodId) {
        try {
            CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(email)
                .setName(name)
                .setPaymentMethod(paymentMethodId)
                .setInvoiceSettings(
                    CustomerCreateParams.InvoiceSettings.builder()
                        .setDefaultPaymentMethod(paymentMethodId)
                        .build()
                )
                .build();

            Customer customer = Customer.create(params);
            
            return Result.success(new StripeCustomer(
                StripeCustomerId.fromString(customer.getId()),
                customer.getEmail()
            ));
        } catch (StripeException e) {
            return Result.failure(ErrorDetail.of("STRIPE_CUSTOMER_CREATION_FAILED",
                "Failed to create Stripe customer: " + e.getMessage()));
        }
    }

    public Result<StripeSubscription> createSubscription(StripeCustomerId customerId, SubscriptionTier tier) {
        try {
            SubscriptionCreateParams params = SubscriptionCreateParams.builder()
                .setCustomer(customerId.getValue())
                .addItem(
                    SubscriptionCreateParams.Item.builder()
                        .setPrice(tier.getStripePriceId())
                        .build()
                )
                .setBillingCycleAnchor(getNextMonthStart())
                .setProrationBehavior(SubscriptionCreateParams.ProrationBehavior.CREATE_PRORATIONS)
                .build();

            com.stripe.model.Subscription subscription = com.stripe.model.Subscription.create(params);
            
            return Result.success(new StripeSubscription(
                StripeSubscriptionId.fromString(subscription.getId()),
                StripeInvoiceId.fromString(subscription.getLatestInvoice())
            ));
        } catch (StripeException e) {
            return Result.failure(ErrorDetail.of("STRIPE_SUBSCRIPTION_CREATION_FAILED",
                "Failed to create Stripe subscription: " + e.getMessage()));
        }
    }

    private long getNextMonthStart() {
        return LocalDate.now().plusMonths(1).withDayOfMonth(1)
            .atStartOfDay(ZoneOffset.UTC).toEpochSecond();
    }
}
```

## Data Models

### Database Schema

The database schema follows the provided SQL structure with the following tables:

1. **billing_accounts** - Core billing account information
2. **subscriptions** - Subscription details and status
3. **invoices** - Invoice records with amounts and status
4. **invoice_line_items** - Detailed line items for invoices
5. **dunning_cases** - Dunning process management
6. **dunning_actions** - Individual dunning actions
7. **processed_webhook_events** - Webhook idempotency tracking
8. **billing_domain_events** - Event outbox for reliable delivery
9. **saga_audit_log** - Saga execution audit trail

### JPA Entity Mappings

```java
@Entity
@Table(name = "billing_accounts", schema = "billing")
public class BillingAccountEntity {
    @Id
    private String id;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "stripe_customer_id", unique = true, nullable = false)
    private String stripeCustomerId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingAccountStatus status;
    
    @Column(name = "default_payment_method_id")
    private String defaultPaymentMethodId;
    
    @Column(name = "account_balance_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal accountBalanceAmount = BigDecimal.ZERO;
    
    @Column(name = "account_balance_currency", length = 3, nullable = false)
    private String accountBalanceCurrency = "EUR";
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Version
    private Long version = 0L;
    
    // Constructors, getters, setters
}

@Entity
@Table(name = "subscriptions", schema = "billing")
public class SubscriptionEntity {
    @Id
    private String id;
    
    @Column(name = "billing_account_id", nullable = false)
    private String billingAccountId;
    
    @Column(name = "stripe_subscription_id", unique = true, nullable = false)
    private String stripeSubscriptionId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionTier tier;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;
    
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Version
    private Long version = 0L;
    
    // Constructors, getters, setters
}
```

## Error Handling

All operations use the Result<T> pattern for functional error handling:

```java
public enum BillingErrorCode {
    BILLING_ACCOUNT_NOT_FOUND("BILLING_ACCOUNT_NOT_FOUND", "Billing account not found"),
    INVALID_PAYMENT_METHOD("INVALID_PAYMENT_METHOD", "Invalid payment method"),
    SUBSCRIPTION_CREATION_FAILED("SUBSCRIPTION_CREATION_FAILED", "Failed to create subscription"),
    INVOICE_GENERATION_FAILED("INVOICE_GENERATION_FAILED", "Failed to generate invoice"),
    STRIPE_API_ERROR("STRIPE_API_ERROR", "Stripe API error"),
    INSUFFICIENT_FUNDS("INSUFFICIENT_FUNDS", "Insufficient funds"),
    ACCOUNT_SUSPENDED("ACCOUNT_SUSPENDED", "Account is suspended"),
    INVALID_STATUS_TRANSITION("INVALID_STATUS_TRANSITION", "Invalid status transition");
    
    private final String code;
    private final String message;
    
    BillingErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public ErrorDetail toErrorDetail() {
        return ErrorDetail.of(code, message, code.toLowerCase().replace("_", "."));
    }
}
```

## Testing Strategy

### Unit Testing with Closures

```java
@DisplayName("Process Payment Command Handler Tests")
class ProcessPaymentCommandHandlerTest {

    private final AtomicReference<BillingAccount> savedAccount = new AtomicReference<>();
    private final AtomicReference<Subscription> savedSubscription = new AtomicReference<>();
    private final AtomicReference<Invoice> savedInvoice = new AtomicReference<>();
    private final List<Object> publishedEvents = new ArrayList<>();

    // Mock closures
    private final Function<BillingAccount, Result<BillingAccountId>> accountSaver = account -> {
        savedAccount.set(account);
        return Result.success(account.getId());
    };

    private final Function<Subscription, Result<SubscriptionId>> subscriptionSaver = subscription -> {
        savedSubscription.set(subscription);
        return Result.success(subscription.getId());
    };

    private final Function<Invoice, Result<InvoiceId>> invoiceSaver = invoice -> {
        savedInvoice.set(invoice);
        return Result.success(invoice.getId());
    };

    private final Consumer<Object> eventPublisher = event -> publishedEvents.add(event);

    @Test
    @DisplayName("Should process payment successfully with valid data")
    void shouldProcessPaymentSuccessfully() {
        // Given
        ProcessPaymentCommand command = new ProcessPaymentCommand(
            "correlation-123",
            PaymentMethodId.fromString("pm_test_123")
        );

        // When
        Result<ProcessPaymentResponse> result = ProcessPaymentCommandHandler.processPayment(
            command, accountSaver, subscriptionSaver, invoiceSaver, eventPublisher
        );

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(savedAccount.get()).isNotNull();
        assertThat(savedAccount.get().getStatus()).isEqualTo(BillingAccountStatus.ACTIVE);
        assertThat(savedSubscription.get()).isNotNull();
        assertThat(savedSubscription.get().getTier()).isEqualTo(SubscriptionTier.STARTER);
        assertThat(savedInvoice.get()).isNotNull();
        assertThat(publishedEvents).hasSize(2);
        assertThat(publishedEvents.get(0)).isInstanceOf(PaymentVerifiedEvent.class);
        assertThat(publishedEvents.get(1)).isInstanceOf(InvoiceGeneratedEvent.class);
    }
}
```

This design provides a comprehensive billing system that follows the established RegTech architecture patterns while implementing all the required functionality for payment processing, subscription management, and invoice generation using functional programming principles and the Saga pattern for distributed transactions.