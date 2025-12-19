# Clean Architecture Implementation Guide for LLMs

> **Purpose**: This document provides comprehensive instructions for implementing Clean Architecture with proper separation of concerns, event-driven design, and infrastructure-based observability.

---

## Table of Contents

1. [Core Architecture Principles](#1-core-architecture-principles)
2. [Layer Responsibilities](#2-layer-responsibilities)
3. [Package Structure](#3-package-structure)
4. [Domain Layer Implementation](#4-domain-layer-implementation)
5. [Application Layer Implementation](#5-application-layer-implementation)
6. [Infrastructure Layer Implementation](#6-infrastructure-layer-implementation)
7. [Presentation Layer Implementation](#7-presentation-layer-implementation)
8. [Result Pattern Implementation](#8-result-pattern-implementation)
9. [Event-Driven Architecture](#9-event-driven-architecture)
10. [Observability Implementation](#10-observability-implementation)
11. [Testing Strategy](#11-testing-strategy)
12. [Common Pitfalls](#12-common-pitfalls)

---

## 1. Core Architecture Principles

### The Golden Rules

```
✅ Domain = Business Rules (zero dependencies)
✅ Application = Orchestration (no implementation details)
✅ Infrastructure = Technical Details (all frameworks, logging, metrics)
✅ Presentation = Input/Output (HTTP, CLI, etc.)
```

### Dependency Direction

```
Presentation → Application → Domain
                    ↓
            Infrastructure (implements ports)
```

**Critical**: Dependencies flow inward. Inner layers NEVER depend on outer layers.

### Mental Model

| Layer | Question Answered | Contains | Must NOT Contain |
|-------|------------------|----------|------------------|
| **Domain** | What are the business rules? | Entities, Value Objects, Domain Events, Business Logic | Frameworks, Logging, Database, HTTP |
| **Application** | What can the system do? | Use Cases, Command/Query Handlers, Port Interfaces | Implementation details, Logging, Frameworks |
| **Infrastructure** | How is it technically done? | Repositories, Adapters, Logging, Metrics, External APIs | Business Logic |
| **Presentation** | How do users interact? | Controllers, DTOs, Input Validation | Business Logic, Database Access |

---

## 2. Layer Responsibilities

### 2.1 Domain Layer (Core Business)

**Purpose**: Pure business logic, independent of frameworks

**Contains**:
- Entities (with identity)
- Value Objects (immutable, no identity)
- Aggregates (consistency boundaries)
- Domain Events (something meaningful happened)
- Domain Services (multi-entity business rules)
- Enums
- Domain Exceptions

**Must NOT contain**:
- `@Entity`, `@Table`, `@Column` (JPA annotations)
- `Logger`, `log.info()` (logging)
- `@Transactional` (transactions)
- `@Service`, `@Component` (Spring annotations)
- Any framework imports

**Example**:
```java
// ✅ CORRECT - Pure domain
public class Order {
    private final OrderId id;
    private final CustomerId customerId;
    private OrderStatus status;
    private final List<OrderLine> lines;
    private final List<DomainEvent> domainEvents;
    
    public void cancel() {
        if (status == OrderStatus.COMPLETED) {
            throw new OrderCannotBeCancelledException(
                "Cannot cancel completed order"
            );
        }
        this.status = OrderStatus.CANCELLED;
        this.domainEvents.add(new OrderCancelled(id, Instant.now()));
    }
}

// ❌ WRONG - Infrastructure leakage
public class Order {
    private static final Logger log = LoggerFactory.getLogger(Order.class); // ❌
    
    @Id // ❌ JPA annotation
    private Long id;
    
    public void cancel() {
        log.info("Cancelling order"); // ❌ Logging
        this.status = "CANCELLED";
    }
}
```

### 2.2 Application Layer (Use Cases)

**Purpose**: Orchestrate business use cases

**Contains**:
- Use Cases / Application Services
- Command & Query objects (DTOs)
- Port Interfaces (repositories, gateways)
- Result types
- Application Events (integration triggers)

**Responsibilities**:
1. Validate intent (format, required fields)
2. Call domain logic
3. Coordinate repositories and services
4. Control transactions
5. Publish events

**Must NOT contain**:
- SQL queries
- HTTP clients
- Logging statements
- Framework-specific code
- Business rules (delegate to domain)

**The Orchestrator Pattern**:
```java
// ✅ CORRECT - Pure orchestration
@Service
@Transactional
public class PlaceOrderUseCase {
    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;
    private final EventPublisher eventPublisher;
    
    @Observed(name = "usecase.place.order")
    public Result<OrderResponse> execute(PlaceOrderCommand command) {
        // 1. Validate command
        Result<Void> validation = validateCommand(command);
        if (validation.isFailure()) {
            return Result.failure(validation.getError());
        }
        
        // 2. Execute domain logic
        Order order = Order.create(
            command.customerId(),
            command.orderLines()
        );
        
        // 3. Call external service (via port)
        paymentGateway.reserve(order.total());
        
        // 4. Persist
        Order saved = orderRepository.save(order);
        
        // 5. Publish events
        eventPublisher.publishAll(saved.pullDomainEvents());
        
        // 6. Return result
        return Result.success(OrderResponse.from(saved));
    }
}

// ❌ WRONG - Mixed concerns
@Service
public class PlaceOrderUseCase {
    private static final Logger log = LoggerFactory.getLogger(...); // ❌
    
    public OrderResponse execute(PlaceOrderCommand command) {
        log.info("Placing order"); // ❌ Logging
        
        // Direct SQL query ❌
        jdbcTemplate.query("SELECT * FROM orders WHERE id = ?", ...);
        
        // Business logic in application ❌
        if (order.total().isGreaterThan(Money.of(1000))) {
            order.applyDiscount(0.1);
        }
        
        return response;
    }
}
```

### 2.3 Infrastructure Layer (Technical Details)

**Purpose**: Implement technical concerns

**Contains**:
- Repository implementations (JPA, JDBC)
- External API clients
- Messaging (Kafka, RabbitMQ)
- File storage (S3, filesystem)
- Email/SMS services
- Caching
- **ALL Logging & Metrics (via AOP)**
- Database entities (`@Entity`)
- Mappers (Domain ↔ Entity)

**Examples**:
```java
// Repository implementation
@Repository
public class JpaOrderRepository implements OrderRepository {
    private final SpringDataOrderRepository springRepo;
    private final OrderMapper mapper;
    
    @Override
    public Order save(Order order) {
        OrderEntity entity = mapper.toEntity(order);
        OrderEntity saved = springRepo.save(entity);
        return mapper.toDomain(saved);
    }
}

// External gateway
@Component
public class StripePaymentGateway implements PaymentGateway {
    private final StripeClient stripeClient;
    
    @Override
    public PaymentResult charge(Money amount) {
        try {
            ChargeResponse response = stripeClient.charge(
                amount.value(),
                amount.currency()
            );
            return PaymentResult.success(response.transactionId());
        } catch (StripeException e) {
            return PaymentResult.failure(e.getMessage());
        }
    }
}
```

### 2.4 Presentation Layer (UI/API)

**Purpose**: Handle user interactions

**Contains**:
- REST Controllers
- GraphQL Resolvers
- CLI Commands
- Request/Response DTOs
- Input validation (format, required fields)
- Authentication/Authorization checks
- HTTP status codes

**Must NOT contain**:
- Business rules
- Database access
- Complex logic

**Example**:
```java
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final PlaceOrderUseCase placeOrderUseCase;
    
    @PostMapping
    public ResponseEntity<?> placeOrder(
        @Valid @RequestBody PlaceOrderRequest request
    ) {
        PlaceOrderCommand command = PlaceOrderCommand.from(request);
        Result<OrderResponse> result = placeOrderUseCase.execute(command);
        
        return result.isSuccess()
            ? ResponseEntity.status(CREATED).body(result.getValue())
            : toErrorResponse(result);
    }
    
    private ResponseEntity<?> toErrorResponse(Result<?> result) {
        return result.getError()
            .map(error -> ResponseEntity
                .status(toHttpStatus(error.type()))
                .body(new ErrorResponse(error.message()))
            )
            .orElse(ResponseEntity.internalServerError().build());
    }
}
```

---

## 3. Package Structure

### Recommended Organization

```
src/main/java/com/company/project/
├── domain/
│   ├── orders/
│   │   ├── Order.java                    # Aggregate root
│   │   ├── OrderId.java                  # Value object
│   │   ├── OrderLine.java                # Entity
│   │   ├── OrderStatus.java              # Enum
│   │   ├── OrderCreated.java             # Domain event
│   │   ├── OrderCancelled.java           # Domain event
│   │   └── PricingService.java           # Domain service
│   ├── customers/
│   │   ├── Customer.java
│   │   └── CustomerId.java
│   ├── common/
│   │   ├── DomainEvent.java              # Base event
│   │   ├── AggregateRoot.java            # Base aggregate
│   │   ├── Entity.java                   # Base entity
│   │   ├── ValueObject.java              # Base value object
│   │   └── Result.java                   # Result pattern
│   └── exceptions/
│       ├── DomainException.java
│       └── OrderException.java
│
├── application/
│   ├── orders/
│   │   ├── PlaceOrderUseCase.java        # Use case
│   │   ├── CancelOrderUseCase.java
│   │   ├── PlaceOrderCommand.java        # Input DTO
│   │   ├── OrderResponse.java            # Output DTO
│   │   └── OrderRepository.java          # Port interface
│   ├── payments/
│   │   ├── PayOrderUseCase.java
│   │   ├── PaymentCommand.java
│   │   └── PaymentGateway.java           # Port interface
│   └── common/
│       ├── EventPublisher.java           # Port interface
│       └── UseCase.java                  # Base use case
│
├── infrastructure/
│   ├── persistence/
│   │   ├── orders/
│   │   │   ├── OrderEntity.java          # JPA entity
│   │   │   ├── OrderLineEntity.java
│   │   │   ├── JpaOrderRepository.java   # Adapter
│   │   │   ├── SpringDataOrderRepo.java  # Spring Data
│   │   │   └── OrderMapper.java          # Domain ↔ Entity
│   │   └── config/
│   │       └── JpaConfig.java
│   ├── messaging/
│   │   ├── KafkaEventPublisher.java      # Adapter
│   │   └── config/
│   │       └── KafkaConfig.java
│   ├── external/
│   │   ├── StripePaymentGateway.java     # Adapter
│   │   └── S3FileStorage.java
│   └── observability/
│       ├── logging/
│       │   ├── UseCaseLoggingAspect.java
│       │   ├── DomainEventLoggingListener.java
│       │   ├── ExceptionLoggingAspect.java
│       │   └── SlowQueryDetectionAspect.java
│       ├── metrics/
│       │   ├── UseCaseMetricsAspect.java
│       │   ├── DomainEventMetricsListener.java
│       │   └── InfrastructureMetricsAspect.java
│       └── config/
│           ├── ObservabilityConfig.java
│           └── MdcConfig.java
│
└── presentation/
    ├── rest/
    │   ├── orders/
    │   │   ├── OrderController.java
    │   │   ├── PlaceOrderRequest.java    # HTTP request DTO
    │   │   └── OrderResponseDto.java     # HTTP response DTO
    │   └── common/
    │       ├── ErrorResponse.java
    │       └── GlobalExceptionHandler.java
    └── config/
        ├── SecurityConfig.java
        └── WebConfig.java
```

### Key Principles

1. **Organize by business capability** (orders, payments, documents)
2. **Each layer is independent** (can be in separate modules)
3. **Ports live in application layer** (interfaces)
4. **Adapters live in infrastructure layer** (implementations)
5. **Observability is 100% infrastructure**

---

## 4. Domain Layer Implementation

### 4.1 Entities

**Rules**:
- Have identity (ID)
- Mutable state
- Encapsulate business rules
- Raise domain events
- No framework annotations

```java
public class Order extends AggregateRoot<OrderId> {
    private final OrderId id;
    private final CustomerId customerId;
    private OrderStatus status;
    private final List<OrderLine> lines;
    private Money total;
    
    // Private constructor - use factory method
    private Order(OrderId id, CustomerId customerId, List<OrderLine> lines) {
        this.id = id;
        this.customerId = customerId;
        this.lines = new ArrayList<>(lines);
        this.status = OrderStatus.PENDING;
        this.total = calculateTotal();
    }
    
    // Factory method
    public static Order create(CustomerId customerId, List<OrderLine> lines) {
        if (lines.isEmpty()) {
            throw new EmptyOrderException("Order must have at least one line");
        }
        
        Order order = new Order(OrderId.generate(), customerId, lines);
        order.registerEvent(new OrderCreated(
            order.id,
            order.customerId,
            order.total,
            Instant.now()
        ));
        return order;
    }
    
    // Business methods
    public void cancel() {
        if (status == OrderStatus.COMPLETED) {
            throw new OrderCannotBeCancelledException(
                "Cannot cancel completed order"
            );
        }
        if (status == OrderStatus.CANCELLED) {
            throw new OrderAlreadyCancelledException(
                "Order is already cancelled"
            );
        }
        
        this.status = OrderStatus.CANCELLED;
        this.registerEvent(new OrderCancelled(id, Instant.now()));
    }
    
    public void markAsPaid() {
        if (status != OrderStatus.PENDING) {
            throw new InvalidOrderStatusException(
                "Only pending orders can be marked as paid"
            );
        }
        
        this.status = OrderStatus.PAID;
        this.registerEvent(new OrderPaid(id, total, Instant.now()));
    }
    
    public void complete() {
        if (status != OrderStatus.PAID) {
            throw new OrderNotPaidException("Order must be paid before completion");
        }
        
        this.status = OrderStatus.COMPLETED;
        this.registerEvent(new OrderCompleted(id, Instant.now()));
    }
    
    private Money calculateTotal() {
        return lines.stream()
            .map(OrderLine::subtotal)
            .reduce(Money.ZERO, Money::add);
    }
    
    // Getters only (no setters!)
    @Override
    public OrderId getId() {
        return id;
    }
    
    public CustomerId getCustomerId() {
        return customerId;
    }
    
    public OrderStatus getStatus() {
        return status;
    }
    
    public List<OrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }
    
    public Money getTotal() {
        return total;
    }
}
```

### 4.2 Value Objects

**Rules**:
- Immutable
- No identity
- Equality by value
- Self-validating
- No framework annotations

```java
public record Money(BigDecimal value, Currency currency) {
    
    public static final Money ZERO = new Money(BigDecimal.ZERO, Currency.EUR);
    
    // Compact constructor with validation
    public Money {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
        if (value.scale() > 2) {
            throw new IllegalArgumentException("Money cannot have more than 2 decimals");
        }
    }
    
    public static Money of(BigDecimal value, Currency currency) {
        return new Money(value, currency);
    }
    
    public static Money euros(double value) {
        return new Money(BigDecimal.valueOf(value), Currency.EUR);
    }
    
    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new CurrencyMismatchException(
                "Cannot add different currencies"
            );
        }
        return new Money(this.value.add(other.value), this.currency);
    }
    
    public Money multiply(BigDecimal factor) {
        return new Money(this.value.multiply(factor), this.currency);
    }
    
    public boolean isGreaterThan(Money other) {
        ensureSameCurrency(other);
        return this.value.compareTo(other.value) > 0;
    }
    
    private void ensureSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new CurrencyMismatchException(
                "Cannot compare different currencies"
            );
        }
    }
}
```

### 4.3 Domain Events

**Rules**:
- Immutable (use records)
- Past tense naming
- Contain timestamp
- Represent business facts

```java
public record OrderCreated(
    OrderId orderId,
    CustomerId customerId,
    Money total,
    Instant occurredOn
) implements DomainEvent {
    
    @Override
    public String eventType() {
        return "order.created";
    }
}

public record OrderCancelled(
    OrderId orderId,
    Instant occurredOn
) implements DomainEvent {
    
    @Override
    public String eventType() {
        return "order.cancelled";
    }
}

// Base interface
public interface DomainEvent {
    String eventType();
    Instant occurredOn();
}
```

### 4.4 Aggregate Root

**Rules**:
- Manages domain events
- Consistency boundary
- Single entry point for modifications

```java
public abstract class AggregateRoot<ID> {
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    
    protected void registerEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }
    
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = new ArrayList<>(this.domainEvents);
        this.domainEvents.clear();
        return events;
    }
    
    public abstract ID getId();
}
```

### 4.5 Domain Services

**When to use**: Business logic that doesn't naturally belong to a single entity

```java
public class PricingService {
    
    public Money calculateDiscount(Order order, Customer customer) {
        if (customer.isVip() && order.getTotal().isGreaterThan(Money.euros(1000))) {
            return order.getTotal().multiply(BigDecimal.valueOf(0.15));
        }
        if (customer.isVip()) {
            return order.getTotal().multiply(BigDecimal.valueOf(0.10));
        }
        if (order.getTotal().isGreaterThan(Money.euros(1000))) {
            return order.getTotal().multiply(BigDecimal.valueOf(0.05));
        }
        return Money.ZERO;
    }
}
```

---

## 5. Application Layer Implementation

### 5.1 Use Case Structure

```java
@Service
@Transactional
public class PlaceOrderUseCase {
    
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final PaymentGateway paymentGateway;
    private final EventPublisher eventPublisher;
    private final PricingService pricingService;
    
    public PlaceOrderUseCase(
        OrderRepository orderRepository,
        CustomerRepository customerRepository,
        PaymentGateway paymentGateway,
        EventPublisher eventPublisher,
        PricingService pricingService
    ) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.paymentGateway = paymentGateway;
        this.eventPublisher = eventPublisher;
        this.pricingService = pricingService;
    }
    
    @Observed(name = "usecase.place.order")
    public Result<OrderResponse> execute(PlaceOrderCommand command) {
        // 1. Validate command format
        Result<Void> validation = validateCommand(command);
        if (validation.isFailure()) {
            return Result.failure(validation.getError());
        }
        
        try {
            // 2. Load domain objects
            Customer customer = customerRepository.findById(command.customerId())
                .orElseThrow(() -> new CustomerNotFoundException(command.customerId()));
            
            // 3. Execute domain logic
            Order order = Order.create(command.customerId(), command.orderLines());
            
            // 4. Apply domain service
            Money discount = pricingService.calculateDiscount(order, customer);
            if (!discount.equals(Money.ZERO)) {
                order.applyDiscount(discount);
            }
            
            // 5. External interaction (via port)
            PaymentResult paymentResult = paymentGateway.reserve(order.getTotal());
            if (paymentResult.isFailure()) {
                return Result.failure(
                    "Payment reservation failed: " + paymentResult.getError(),
                    ErrorType.INFRASTRUCTURE_ERROR
                );
            }
            
            // 6. Persist
            Order savedOrder = orderRepository.save(order);
            
            // 7. Publish domain events
            eventPublisher.publishAll(savedOrder.pullDomainEvents());
            
            // 8. Return success
            return Result.success(OrderResponse.from(savedOrder));
            
        } catch (DomainException e) {
            return Result.failure(
                e.getMessage(),
                ErrorType.BUSINESS_RULE_VIOLATION
            );
        } catch (Exception e) {
            return Result.failure(
                "Failed to place order",
                ErrorType.INFRASTRUCTURE_ERROR,
                e
            );
        }
    }
    
    private Result<Void> validateCommand(PlaceOrderCommand command) {
        if (command.customerId() == null) {
            return Result.failure(
                "Customer ID is required",
                ErrorType.VALIDATION_ERROR
            );
        }
        if (command.orderLines() == null || command.orderLines().isEmpty()) {
            return Result.failure(
                "Order must have at least one line",
                ErrorType.VALIDATION_ERROR
            );
        }
        return Result.success(null);
    }
}
```

### 5.2 Port Interfaces

**Repository Port**:
```java
public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(OrderId id);
    List<Order> findByCustomerId(CustomerId customerId);
    void delete(OrderId id);
}
```

**External Gateway Port**:
```java
public interface PaymentGateway {
    PaymentResult reserve(Money amount);
    PaymentResult charge(Money amount);
    PaymentResult refund(Money amount);
}

public sealed interface PaymentResult {
    record Success(String transactionId) implements PaymentResult {}
    record Failure(String error) implements PaymentResult {}
    
    default boolean isSuccess() {
        return this instanceof Success;
    }
    
    default boolean isFailure() {
        return this instanceof Failure;
    }
    
    default String getError() {
        return this instanceof Failure f ? f.error() : null;
    }
}
```

**Event Publisher Port**:
```java
public interface EventPublisher {
    void publish(DomainEvent event);
    void publishAll(List<DomainEvent> events);
}
```

### 5.3 Commands and Queries

**Command** (write operation):
```java
public record PlaceOrderCommand(
    CustomerId customerId,
    List<OrderLineCommand> orderLines
) {
    public record OrderLineCommand(
        ProductId productId,
        int quantity,
        Money unitPrice
    ) {}
}
```

**Query** (read operation):
```java
public record GetOrderQuery(OrderId orderId) {}
```

**Response DTO**:
```java
public record OrderResponse(
    String orderId,
    String customerId,
    String status,
    BigDecimal total,
    String currency,
    List<OrderLineResponse> lines,
    Instant createdAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
            order.getId().value(),
            order.getCustomerId().value(),
            order.getStatus().name(),
            order.getTotal().value(),
            order.getTotal().currency().getCurrencyCode(),
            order.getLines().stream()
                .map(OrderLineResponse::from)
                .toList(),
            order.getCreatedAt()
        );
    }
    
    public record OrderLineResponse(
        String productId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
    ) {
        public static OrderLineResponse from(OrderLine line) {
            return new OrderLineResponse(
                line.getProductId().value(),
                line.getQuantity(),
                line.getUnitPrice().value(),
                line.subtotal().value()
            );
        }
    }
}
```

---

## 6. Infrastructure Layer Implementation

### 6.1 Repository Adapter

```java
@Repository
public class JpaOrderRepository implements OrderRepository {
    
    private final SpringDataOrderRepository springRepo;
    private final OrderMapper mapper;
    
    public JpaOrderRepository(
        SpringDataOrderRepository springRepo,
        OrderMapper mapper
    ) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }
    
    @Override
    public Order save(Order order) {
        OrderEntity entity = mapper.toEntity(order);
        OrderEntity saved = springRepo.save(entity);
        return mapper.toDomain(saved);
    }
    
    @Override
    public Optional<Order> findById(OrderId id) {
        return springRepo.findById(id.value())
            .map(mapper::toDomain);
    }
    
    @Override
    public List<Order> findByCustomerId(CustomerId customerId) {
        return springRepo.findByCustomerId(customerId.value())
            .stream()
            .map(mapper::toDomain)
            .toList();
    }
    
    @Override
    public void delete(OrderId id) {
        springRepo.deleteById(id.value());
    }
}
```

**JPA Entity**:
```java
@Entity
@Table(name = "orders")
public class OrderEntity {
    
    @Id
    private String id;
    
    @Column(name = "customer_id", nullable = false)
    private String customerId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatusEntity status;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal total;
    
    @Column(nullable = false, length = 3)
    private String currency;
    
    @OneToMany(
        mappedBy = "order",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    private List<OrderLineEntity> lines = new ArrayList<>();
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    // Getters and setters
}
```

**Mapper**:
```java
@Component
public class OrderMapper {
    
    public OrderEntity toEntity(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.setId(order.getId().value());
        entity.setCustomerId(order.getCustomerId().value());
        entity.setStatus(OrderStatusEntity.valueOf(order.getStatus().name()));
        entity.setTotal(order.getTotal().value());
        entity.setCurrency(order.getTotal().currency().getCurrencyCode());
        entity.setCreatedAt(order.getCreatedAt());
        
        List<OrderLineEntity> lineEntities = order.getLines().stream()
            .map(line -> toLineEntity(line, entity))
            .toList();
        entity.setLines(lineEntities);
        
        return entity;
    }
    
    public Order toDomain(OrderEntity entity) {
        List<OrderLine> lines = entity.getLines().stream()
            .map(this::toLineDomain)
            .toList();
        
        return Order.reconstitute(
            OrderId.of(entity.getId()),
            CustomerId.of(entity.getCustomerId()),
            OrderStatus.valueOf(entity.getStatus().name()),
            lines,
            Money.of(entity.getTotal(), Currency.getInstance(entity.getCurrency())),
            entity.getCreatedAt()
        );
    }
    
    private OrderLineEntity toLineEntity(OrderLine line, OrderEntity order) {
        OrderLineEntity entity = new OrderLineEntity();
        entity.setOrder(order);
        entity.setProductId(line.getProductId().value());
        entity.setQuantity(line.getQuantity());
        entity.setUnitPrice(line.getUnitPrice().value());
        return entity;
    }
    
    private OrderLine toLineDomain(OrderLineEntity entity) {
        return OrderLine.of(
            ProductId.of(entity.getProductId()),
            entity.getQuantity(),
            Money.euros(entity.getUnitPrice().doubleValue())
        );
    }
}
```

### 6.2 External Gateway Adapter

```java
@Component
public class StripePaymentGateway implements PaymentGateway {
    
    private final StripeClient stripeClient;
    
    public StripePaymentGateway(StripeClient stripeClient) {
        this.stripeClient = stripeClient;
    }
    
    @Override
    public PaymentResult reserve(Money amount) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(toStripeAmount(amount))
                .setCurrency(amount.currency().getCurrencyCode().toLowerCase())
                .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL)
                .build();
            
            PaymentIntent intent = PaymentIntent.create(params);
            
            return new PaymentResult.Success(intent.getId());
            
        } catch (StripeException e) {
            return new PaymentResult.Failure(e.getMessage());
        }
    }
    
    @Override
    public PaymentResult charge(Money amount) {
        try {
            // Capture the payment
            // ...implementation
            return new PaymentResult.Success("transaction-id");
        } catch (StripeException e) {
            return new PaymentResult.Failure(e.getMessage());
        }
    }
    
    private long toStripeAmount(Money amount) {
        // Stripe expects cents
        return amount.value()
            .multiply(BigDecimal.valueOf(100))
            .longValue();
    }
}
```

### 6.3 Event Publisher Adapter

```java
@Component
public class KafkaEventPublisher implements EventPublisher {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    @Override
    public void publish(DomainEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(event.eventType(), json);
        } catch (JsonProcessingException e) {
            throw new EventPublishingException(
                "Failed to publish event: " + event.eventType(),
                e
            );
        }
    }
    
    @Override
    public void publishAll(List<DomainEvent> events) {
        events.forEach(this::publish);
    }
}
```

---

## 7. Presentation Layer Implementation

### 7.1 REST Controller

```java
@RestController
@RequestMapping("/api/v1/orders")
@Validated
public class OrderController {
    
    private final PlaceOrderUseCase placeOrderUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final GetOrderQuery getOrderQuery;
    
    public OrderController(
        PlaceOrderUseCase placeOrderUseCase,
        CancelOrderUseCase cancelOrderUseCase,
        GetOrderQuery getOrderQuery
    ) {
        this.placeOrderUseCase = placeOrderUseCase;
        this.cancelOrderUseCase = cancelOrderUseCase;
        this.getOrderQuery = getOrderQuery;
    }
    
    @PostMapping
    public ResponseEntity<?> placeOrder(
        @Valid @RequestBody PlaceOrderRequest request
    ) {
        PlaceOrderCommand command = toCommand(request);
        Result<OrderResponse> result = placeOrderUseCase.execute(command);
        
        return result.isSuccess()
            ? ResponseEntity.status(HttpStatus.CREATED).body(result.getValue())
            : toErrorResponse(result);
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable String orderId) {
        Result<OrderResponse> result = getOrderQuery.execute(
            new GetOrderQuery(OrderId.of(orderId))
        );
        
        return result.isSuccess()
            ? ResponseEntity.ok(result.getValue())
            : toErrorResponse(result);
    }
    
    @DeleteMapping("/{orderId}")
    public ResponseEntity<?> cancelOrder(@PathVariable String orderId) {
        Result<Void> result = cancelOrderUseCase.execute(
            new CancelOrderCommand(OrderId.of(orderId))
        );
        
        return result.isSuccess()
            ? ResponseEntity.noContent().build()
            : toErrorResponse(result);
    }
    
    private PlaceOrderCommand toCommand(PlaceOrderRequest request) {
        List<OrderLineCommand> lines = request.lines().stream()
            .map(line -> new OrderLineCommand(
                ProductId.of(line.productId()),
                line.quantity(),
                Money.euros(line.unitPrice())
            ))
            .toList();
        
        return new PlaceOrderCommand(
            CustomerId.of(request.customerId()),
            lines
        );
    }
    
    private ResponseEntity<?> toErrorResponse(Result<?> result) {
        return result.getError()
            .map(error -> {
                HttpStatus status = switch (error.type()) {
                    case BUSINESS_RULE_VIOLATION -> HttpStatus.BAD_REQUEST;
                    case VALIDATION_ERROR -> HttpStatus.BAD_REQUEST;
                    case NOT_FOUND -> HttpStatus.NOT_FOUND;
                    case INFRASTRUCTURE_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
                };
                
                ErrorResponse errorResponse = new ErrorResponse(
                    error.message(),
                    error.code().orElse(null),
                    error.type().name()
                );
                
                return ResponseEntity.status(status).body(errorResponse);
            })
            .orElse(ResponseEntity.internalServerError().build());
    }
}
```

### 7.2 Request DTOs

```java
public record PlaceOrderRequest(
    @NotNull(message = "Customer ID is required")
    String customerId,
    
    @NotEmpty(message = "Order must have at least one line")
    @Valid
    List<OrderLineRequest> lines
) {
    public record OrderLineRequest(
        @NotNull(message = "Product ID is required")
        String productId,
        
        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity,
        
        @DecimalMin(value = "0.01", message = "Unit price must be positive")
        double unitPrice
    ) {}
}
```

### 7.3 Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
        MethodArgumentNotValidException ex
    ) {
        List<String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .toList();
        
        return ResponseEntity
            .badRequest()
            .body(new ErrorResponse(
                "Validation failed",
                "VALIDATION_ERROR",
                errors
            ));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericError(Exception ex) {
        log.error("Unexpected error", ex);
        
        return ResponseEntity
            .internalServerError()
            .body(new ErrorResponse(
                "An unexpected error occurred",
                "INTERNAL_ERROR"
            ));
    }
}
```

---

## 8. Result Pattern Implementation

### 8.1 Result Type

```java
public sealed interface Result<T> {
    
    // Factory methods
    static <T> Result<T> success(T value) {
        return new Success<>(value);
    }
    
    static <T> Result<T> failure(String message, ErrorType type) {
        return new Failure<>(new Error(message, type, Optional.empty(), Optional.empty()));
    }
    
    static <T> Result<T> failure(String message, ErrorType type, String code) {
        return new Failure<>(new Error(message, type, Optional.of(code), Optional.empty()));
    }
    
    static <T> Result<T> failure(String message, ErrorType type, Throwable cause) {
        return new Failure<>(new Error(message, type, Optional.empty(), Optional.of(cause)));
    }
    
    static <T> Result<T> failure(Error error) {
        return new Failure<>(error);
    }
    
    // Query methods
    boolean isSuccess();
    boolean isFailure();
    T getValue();
    Optional<Error> getError();
    
    // Railway-oriented programming
    <U> Result<U> map(Function<T, U> mapper);
    <U> Result<U> flatMap(Function<T, Result<U>> mapper);
    Result<T> onSuccess(Consumer<T> action);
    Result<T> onFailure(Consumer<Error> action);
    
    // Implementations
    record Success<T>(T value) implements Result<T> {
        @Override
        public boolean isSuccess() {
            return true;
        }
        
        @Override
        public boolean isFailure() {
            return false;
        }
        
        @Override
        public T getValue() {
            return value;
        }
        
        @Override
        public Optional<Error> getError() {
            return Optional.empty();
        }
        
        @Override
        public <U> Result<U> map(Function<T, U> mapper) {
            return Result.success(mapper.apply(value));
        }
        
        @Override
        public <U> Result<U> flatMap(Function<T, Result<U>> mapper) {
            return mapper.apply(value);
        }
        
        @Override
        public Result<T> onSuccess(Consumer<T> action) {
            action.accept(value);
            return this;
        }
        
        @Override
        public Result<T> onFailure(Consumer<Error> action) {
            return this;
        }
    }
    
    record Failure<T>(Error error) implements Result<T> {
        @Override
        public boolean isSuccess() {
            return false;
        }
        
        @Override
        public boolean isFailure() {
            return true;
        }
        
        @Override
        public T getValue() {
            throw new IllegalStateException("Cannot get value from failure");
        }
        
        @Override
        public Optional<Error> getError() {
            return Optional.of(error);
        }
        
        @Override
        public <U> Result<U> map(Function<T, U> mapper) {
            return Result.failure(error);
        }
        
        @Override
        public <U> Result<U> flatMap(Function<T, Result<U>> mapper) {
            return Result.failure(error);
        }
        
        @Override
        public Result<T> onSuccess(Consumer<T> action) {
            return this;
        }
        
        @Override
        public Result<T> onFailure(Consumer<Error> action) {
            action.accept(error);
            return this;
        }
    }
    
    // Error record
    record Error(
        String message,
        ErrorType type,
        Optional<String> code,
        Optional<Throwable> cause
    ) {
        public Error(String message, ErrorType type) {
            this(message, type, Optional.empty(), Optional.empty());
        }
    }
    
    // Error types
    enum ErrorType {
        BUSINESS_RULE_VIOLATION,
        VALIDATION_ERROR,
        NOT_FOUND,
        INFRASTRUCTURE_ERROR,
        UNAUTHORIZED,
        FORBIDDEN
    }
}
```

### 8.2 Railway-Oriented Programming

```java
public Result<OrderResponse> execute(PlaceOrderCommand command) {
    return validateCommand(command)
        .flatMap(__ -> loadCustomer(command.customerId()))
        .flatMap(customer -> createOrder(command, customer))
        .flatMap(this::applyDiscount)
        .flatMap(this::reservePayment)
        .flatMap(this::saveOrder)
        .flatMap(this::publishEvents)
        .map(OrderResponse::from);
}

private Result<Void> validateCommand(PlaceOrderCommand command) {
    if (command.customerId() == null) {
        return Result.failure("Customer ID is required", ErrorType.VALIDATION_ERROR);
    }
    return Result.success(null);
}

private Result<Customer> loadCustomer(CustomerId customerId) {
    return customerRepository.findById(customerId)
        .map(Result::success)
        .orElse(Result.failure("Customer not found", ErrorType.NOT_FOUND));
}

private Result<Order> createOrder(PlaceOrderCommand command, Customer customer) {
    try {
        Order order = Order.create(command.customerId(), command.orderLines());
        return Result.success(order);
    } catch (DomainException e) {
        return Result.failure(e.getMessage(), ErrorType.BUSINESS_RULE_VIOLATION);
    }
}
```

---

## 9. Event-Driven Architecture

### 9.1 Event Types

```
┌────────────────────────────────────────────────────────────────┐
│                       EVENT TYPES                               │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Domain Event (Domain Layer)                                   │
│  ├─ Purpose: Business fact that happened                       │
│  ├─ Scope: Within bounded context                              │
│  ├─ Example: OrderCreated, OrderCancelled                      │
│  └─ Storage: Aggregate, then outbox table                      │
│                                                                 │
│  Application Event (Application Layer)                         │
│  ├─ Purpose: Use case completed / integration trigger          │
│  ├─ Scope: Application coordination                            │
│  ├─ Example: OrderProcessingCompleted                          │
│  └─ Not stored, only emitted                                   │
│                                                                 │
│  Integration Event (Infrastructure Layer)                      │
│  ├─ Purpose: Communicate with external systems                 │
│  ├─ Scope: Cross-bounded context / microservices              │
│  ├─ Example: OrderPlacedEvent (to Kafka)                      │
│  └─ Published to message broker                                │
│                                                                 │
└────────────────────────────────────────────────────────────────┘
```

### 9.2 Event Flow

```
Domain Layer
  └─ Order.create() 
       └─ raises OrderCreated (domain event)
              │
              ▼
Application Layer
  └─ PlaceOrderUseCase.execute()
       ├─ Persists order
       ├─ Publishes domain events via EventPublisher
       └─ Domain events stored in outbox table
              │
              ▼
Infrastructure Layer
  └─ OutboxProcessor (scheduled job)
       ├─ Reads unpublished events from outbox
       ├─ Converts to integration events
       ├─ Publishes to Kafka
       └─ Marks events as published
```

### 9.3 Outbox Pattern

**Outbox Entity**:
```java
@Entity
@Table(name = "outbox_messages")
public class OutboxMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;
    
    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;
    
    @Column(name = "event_type", nullable = false)
    private String eventType;
    
    @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
    private String payload;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "processed_at")
    private Instant processedAt;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;
    
    @Column(name = "retry_count")
    private int retryCount;
}

enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
```

**Event Publisher with Outbox**:
```java
@Component
public class OutboxEventPublisher implements EventPublisher {
    
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    
    @Override
    @Transactional
    public void publish(DomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            
            OutboxMessage message = new OutboxMessage();
            message.setAggregateId(extractAggregateId(event));
            message.setAggregateType(extractAggregateType(event));
            message.setEventType(event.eventType());
            message.setPayload(payload);
            message.setCreatedAt(Instant.now());
            message.setStatus(OutboxStatus.PENDING);
            message.setRetryCount(0);
            
            outboxRepository.save(message);
            
        } catch (JsonProcessingException e) {
            throw new EventPublishingException("Failed to serialize event", e);
        }
    }
    
    @Override
    @Transactional
    public void publishAll(List<DomainEvent> events) {
        events.forEach(this::publish);
    }
}
```

**Outbox Processor**:
```java
@Component
public class OutboxProcessor {
    
    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    @Scheduled(fixedDelay = 5000) // Every 5 seconds
    @Transactional
    public void processOutboxMessages() {
        List<OutboxMessage> pendingMessages = outboxRepository
            .findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING, PageRequest.of(0, 100));
        
        for (OutboxMessage message : pendingMessages) {
            try {
                // Publish to Kafka
                kafkaTemplate.send(message.getEventType(), message.getPayload())
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            markAsPublished(message);
                        } else {
                            handleFailure(message, ex);
                        }
                    });
                    
            } catch (Exception e) {
                handleFailure(message, e);
            }
        }
    }
    
    private void markAsPublished(OutboxMessage message) {
        message.setStatus(OutboxStatus.PUBLISHED);
        message.setProcessedAt(Instant.now());
        outboxRepository.save(message);
    }
    
    private void handleFailure(OutboxMessage message, Throwable error) {
        message.setRetryCount(message.getRetryCount() + 1);
        
        if (message.getRetryCount() >= 3) {
            message.setStatus(OutboxStatus.FAILED);
        }
        
        outboxRepository.save(message);
    }
}
```

---

## 10. Observability Implementation

### 10.1 Logging Strategy

**Correlation ID Configuration**:
```java
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {
    
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";
    
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
```

**Use Case Logging Aspect**:
```java
@Aspect
@Component
public class UseCaseLoggingAspect {
    
    private static final Logger log = LoggerFactory.getLogger(UseCaseLoggingAspect.class);
    
    @Pointcut("within(com.company.project.application..*) && " +
              "execution(public * execute(..))")
    public void useCaseMethods() {}
    
    @Around("useCaseMethods()")
    public Object logUseCase(ProceedingJoinPoint pjp) throws Throwable {
        String useCaseName = extractUseCaseName(pjp);
        String correlationId = MDC.get("correlationId");
        
        log.info("▶ Executing {} | correlationId={}", useCaseName, correlationId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = pjp.proceed();
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("✓ Completed {} | duration={}ms", useCaseName, duration);
            
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗ Failed {} | duration={}ms | error={}", 
                useCaseName, duration, e.getMessage());
            throw e;
        }
    }
    
    private String extractUseCaseName(ProceedingJoinPoint pjp) {
        return pjp.getSignature().getDeclaringType().getSimpleName();
    }
}
```

**Domain Event Logging Listener**:
```java
@Component
public class DomainEventLoggingListener {
    
    private static final Logger log = LoggerFactory.getLogger(DomainEventLoggingListener.class);
    
    @EventListener
    public void handleDomainEvent(DomainEvent event) {
        String eventType = event.eventType();
        String correlationId = MDC.get("correlationId");
        
        log.info("📋 Domain event published | type={} | correlationId={} | event={}", 
            eventType, correlationId, toJson(event));
    }
    
    private String toJson(DomainEvent event) {
        // Serialize to JSON for structured logging
        return // ... implementation
    }
}
```

**Exception Logging Aspect**:
```java
@Aspect
@Component
public class ExceptionLoggingAspect {
    
    private static final Logger log = LoggerFactory.getLogger(ExceptionLoggingAspect.class);
    
    @AfterThrowing(
        pointcut = "within(com.company.project.application..*) || " +
                   "within(com.company.project.infrastructure..*)",
        throwing = "exception"
    )
    public void logException(JoinPoint jp, Throwable exception) {
        String className = jp.getSignature().getDeclaringType().getSimpleName();
        String methodName = jp.getSignature().getName();
        String correlationId = MDC.get("correlationId");
        
        if (exception instanceof DomainException) {
            log.warn("⚠ Business rule violation | class={} | method={} | error={} | correlationId={}",
                className, methodName, exception.getMessage(), correlationId);
        } else {
            log.error("🔥 Infrastructure error | class={} | method={} | correlationId={}",
                className, methodName, correlationId, exception);
        }
    }
}
```

**Result Logging Aspect** (for Result pattern):
```java
@Aspect
@Component
public class ResultLoggingAspect {
    
    private static final Logger log = LoggerFactory.getLogger(ResultLoggingAspect.class);
    
    @Pointcut("execution(Result+ *..*.*(..)) && within(com.company.project.application..*)")
    public void resultReturningMethods() {}
    
    @Around("resultReturningMethods()")
    public Object logResultFailures(ProceedingJoinPoint pjp) throws Throwable {
        String useCaseName = extractUseCaseName(pjp);
        Object result = pjp.proceed();
        
        if (result instanceof Result<?> r && r.isFailure()) {
            r.getError().ifPresent(error -> {
                String correlationId = MDC.get("correlationId");
                
                switch (error.type()) {
                    case BUSINESS_RULE_VIOLATION:
                        log.warn("⚠ Business rule violation in {} | message={} | correlationId={}",
                            useCaseName, error.message(), correlationId);
                        break;
                    case INFRASTRUCTURE_ERROR:
                        log.error("🔥 Infrastructure error in {} | message={} | correlationId={}",
                            useCaseName, error.message(), correlationId,
                            error.cause().orElse(null));
                        break;
                    case VALIDATION_ERROR:
                        log.warn("⚠ Validation error in {} | message={} | correlationId={}",
                            useCaseName, error.message(), correlationId);
                        break;
                    case NOT_FOUND:
                        log.info("ℹ Not found in {} | message={} | correlationId={}",
                            useCaseName, error.message(), correlationId);
                        break;
                }
            });
        }
        
        return result;
    }
}
```

### 10.2 Metrics Strategy

**Use Case Metrics Aspect**:
```java
@Aspect
@Component
public class UseCaseMetricsAspect {
    
    private final MeterRegistry meterRegistry;
    
    @Around("useCaseMethods()")
    public Object recordMetrics(ProceedingJoinPoint pjp) throws Throwable {
        String useCaseName = extractUseCaseName(pjp);
        
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            Object result = pjp.proceed();
            
            sample.stop(Timer.builder("usecase.execution.duration")
                .tag("usecase", useCaseName)
                .tag("status", "success")
                .register(meterRegistry));
            
            meterRegistry.counter("usecase.execution.total",
                "usecase", useCaseName,
                "status", "success"
            ).increment();
            
            return result;
            
        } catch (Exception e) {
            sample.stop(Timer.builder("usecase.execution.duration")
                .tag("usecase", useCaseName)
                .tag("status", "error")
                .register(meterRegistry));
            
            meterRegistry.counter("usecase.errors.total",
                "usecase", useCaseName,
                "error_type", e.getClass().getSimpleName()
            ).increment();
            
            throw e;
        }
    }
}
```

**Domain Event Metrics Listener**:
```java
@Component
public class DomainEventMetricsListener {
    
    private final MeterRegistry meterRegistry;
    
    @EventListener
    public void recordEventMetrics(DomainEvent event) {
        meterRegistry.counter("business.events.total",
            "event_type", event.eventType()
        ).increment();
        
        // Business-specific metrics
        if (event instanceof OrderCreated orderCreated) {
            meterRegistry.counter("business.orders.total",
                "status", "created"
            ).increment();
            
            meterRegistry.summary("business.orders.value",
                "currency", orderCreated.total().currency().getCurrencyCode()
            ).record(orderCreated.total().value().doubleValue());
        }
        
        if (event instanceof OrderCancelled) {
            meterRegistry.counter("business.orders.total",
                "status", "cancelled"
            ).increment();
        }
    }
}
```

**Infrastructure Metrics Aspect**:
```java
@Aspect
@Component
public class InfrastructureMetricsAspect {
    
    private final MeterRegistry meterRegistry;
    
    @Around("within(com.company.project.infrastructure.persistence..*) && " +
            "execution(* save(..))")
    public Object recordDatabaseMetrics(ProceedingJoinPoint pjp) throws Throwable {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            Object result = pjp.proceed();
            
            sample.stop(Timer.builder("infrastructure.database.query.duration")
                .tag("operation", "save")
                .tag("status", "success")
                .register(meterRegistry));
            
            return result;
            
        } catch (Exception e) {
            sample.stop(Timer.builder("infrastructure.database.query.duration")
                .tag("operation", "save")
                .tag("status", "error")
                .register(meterRegistry));
            
            throw e;
        }
    }
}
```

### 10.3 Slow Query Detection

```java
@Aspect
@Component
public class SlowQueryDetectionAspect {
    
    private static final Logger log = LoggerFactory.getLogger(SlowQueryDetectionAspect.class);
    private static final long SLOW_QUERY_THRESHOLD_MS = 1000;
    
    private final MeterRegistry meterRegistry;
    
    @Around("within(com.company.project.infrastructure.persistence..*)")
    public Object detectSlowQueries(ProceedingJoinPoint pjp) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        try {
            return pjp.proceed();
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            if (duration > SLOW_QUERY_THRESHOLD_MS) {
                String methodName = pjp.getSignature().getName();
                String className = pjp.getSignature().getDeclaringType().getSimpleName();
                
                log.warn("🐌 Slow query detected | class={} | method={} | duration={}ms",
                    className, methodName, duration);
                
                meterRegistry.counter("infrastructure.database.slow_queries.total",
                    "class", className,
                    "method", methodName
                ).increment();
            }
        }
    }
}
```

---

## 11. Testing Strategy

### 11.1 Domain Layer Tests

**No mocking needed - pure logic**:
```java
@Test
void shouldCreateOrderSuccessfully() {
    // Arrange
    CustomerId customerId = CustomerId.generate();
    List<OrderLine> lines = List.of(
        OrderLine.of(ProductId.generate(), 2, Money.euros(50))
    );
    
    // Act
    Order order = Order.create(customerId, lines);
    
    // Assert
    assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    assertThat(order.getTotal()).isEqualTo(Money.euros(100));
    assertThat(order.pullDomainEvents())
        .hasSize(1)
        .first()
        .isInstanceOf(OrderCreated.class);
}

@Test
void shouldThrowExceptionWhenCancellingCompletedOrder() {
    // Arrange
    Order order = Order.create(customerId, lines);
    order.markAsPaid();
    order.complete();
    
    // Act & Assert
    assertThatThrownBy(() -> order.cancel())
        .isInstanceOf(OrderCannotBeCancelledException.class)
        .hasMessage("Cannot cancel completed order");
}
```

### 11.2 Application Layer Tests

**With mocked ports**:
```java
@ExtendWith(MockitoExtension.class)
class PlaceOrderUseCaseTest {
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private CustomerRepository customerRepository;
    
    @Mock
    private PaymentGateway paymentGateway;
    
    @Mock
    private EventPublisher eventPublisher;
    
    @InjectMocks
    private PlaceOrderUseCase useCase;
    
    @Test
    void shouldPlaceOrderSuccessfully() {
        // Arrange
        PlaceOrderCommand command = new PlaceOrderCommand(
            CustomerId.of("customer-1"),
            List.of(new OrderLineCommand(
                ProductId.of("product-1"),
                2,
                Money.euros(50)
            ))
        );
        
        Customer customer = Customer.create("John Doe");
        when(customerRepository.findById(any())).thenReturn(Optional.of(customer));
        when(paymentGateway.reserve(any())).thenReturn(new PaymentResult.Success("tx-123"));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        
        // Act
        Result<OrderResponse> result = useCase.execute(command);
        
        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().status()).isEqualTo("PENDING");
        
        verify(orderRepository).save(any(Order.class));
        verify(eventPublisher).publishAll(anyList());
    }
    
    @Test
    void shouldReturnFailureWhenCustomerNotFound() {
        // Arrange
        PlaceOrderCommand command = createCommand();
        when(customerRepository.findById(any())).thenReturn(Optional.empty());
        
        // Act
        Result<OrderResponse> result = useCase.execute(command);
        
        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError())
            .isPresent()
            .get()
            .extracting(Result.Error::type)
            .isEqualTo(ErrorType.NOT_FOUND);
        
        verify(orderRepository, never()).save(any());
    }
}
```

### 11.3 Infrastructure Layer Tests

**Repository tests with testcontainers**:
```java
@DataJpaTest
@Testcontainers
class JpaOrderRepositoryTest {
    
    @Container
    private static PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>("postgres:15");
    
    @Autowired
    private SpringDataOrderRepository springRepo;
    
    private JpaOrderRepository repository;
    private OrderMapper mapper;
    
    @BeforeEach
    void setUp() {
        mapper = new OrderMapper();
        repository = new JpaOrderRepository(springRepo, mapper);
    }
    
    @Test
    void shouldSaveAndRetrieveOrder() {
        // Arrange
        Order order = Order.create(
            CustomerId.generate(),
            List.of(OrderLine.of(ProductId.generate(), 1, Money.euros(100)))
        );
        
        // Act
        Order saved = repository.save(order);
        Optional<Order> retrieved = repository.findById(saved.getId());
        
        // Assert
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getId()).isEqualTo(saved.getId());
        assertThat(retrieved.get().getTotal()).isEqualTo(Money.euros(100));
    }
}
```

### 11.4 Integration Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Test
    void shouldPlaceOrderViaRestApi() throws Exception {
        // Arrange
        String requestBody = """
            {
                "customerId": "customer-123",
                "lines": [
                    {
                        "productId": "product-456",
                        "quantity": 2,
                        "unitPrice": 50.00
                    }
                ]
            }
            """;
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.orderId").exists())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.total").value(100.00));
        
        // Verify persistence
        List<Order> orders = orderRepository.findAll();
        assertThat(orders).hasSize(1);
    }
}
```

---

## 12. Common Pitfalls

### 12.1 ❌ Logging in Domain

```java
// ❌ WRONG
public class Order {
    private static final Logger log = LoggerFactory.getLogger(Order.class);
    
    public void cancel() {
        log.info("Cancelling order {}", id); // Infrastructure leak!
        this.status = OrderStatus.CANCELLED;
    }
}

// ✅ CORRECT
public class Order {
    public void cancel() {
        this.status = OrderStatus.CANCELLED;
        this.registerEvent(new OrderCancelled(id, Instant.now()));
    }
}
```

### 12.2 ❌ Business Logic in Application

```java
// ❌ WRONG
@Service
public class PlaceOrderUseCase {
    public OrderResponse execute(PlaceOrderCommand command) {
        Order order = orderRepository.findById(command.orderId());
        
        // Business logic in application layer!
        if (order.getTotal().isGreaterThan(Money.euros(1000))) {
            order.applyDiscount(0.1);
        }
        
        return OrderResponse.from(order);
    }
}

// ✅ CORRECT
@Service
public class PlaceOrderUseCase {
    private final PricingService pricingService; // Domain service
    
    public OrderResponse execute(PlaceOrderCommand command) {
        Order order = orderRepository.findById(command.orderId());
        Customer customer = customerRepository.findById(command.customerId());
        
        // Delegate to domain service
        Money discount = pricingService.calculateDiscount(order, customer);
        order.applyDiscount(discount);
        
        return OrderResponse.from(order);
    }
}
```

### 12.3 ❌ JPA Annotations in Domain

```java
// ❌ WRONG
@Entity // JPA annotation in domain!
public class Order {
    @Id
    private String id;
    
    @Column
    private String status;
}

// ✅ CORRECT - Domain
public class Order {
    private final OrderId id;
    private OrderStatus status;
}

// ✅ CORRECT - Infrastructure
@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id
    private String id;
    
    @Enumerated(EnumType.STRING)
    private OrderStatusEntity status;
}
```

### 12.4 ❌ Direct Exception Logging in Application

```java
// ❌ WRONG
@Service
public class PlaceOrderUseCase {
    private static final Logger log = LoggerFactory.getLogger(...);
    
    public Result<OrderResponse> execute(PlaceOrderCommand command) {
        try {
            // ...
        } catch (Exception e) {
            log.error("Failed to place order", e); // Logging in application!
            return Result.failure(...);
        }
    }
}

// ✅ CORRECT
@Service
public class PlaceOrderUseCase {
    // No logger!
    
    public Result<OrderResponse> execute(PlaceOrderCommand command) {
        try {
            // ...
        } catch (Exception e) {
            // Just return failure, logging handled by aspect
            return Result.failure("Failed to place order", INFRASTRUCTURE_ERROR, e);
        }
    }
}
```

### 12.5 ❌ Missing Correlation ID

```java
// ❌ WRONG - No correlation tracking
@Component
public class UseCaseLoggingAspect {
    public Object logUseCase(ProceedingJoinPoint pjp) {
        log.info("Executing use case"); // No correlation ID!
        return pjp.proceed();
    }
}

// ✅ CORRECT - With correlation ID
@Component
public class UseCaseLoggingAspect {
    public Object logUseCase(ProceedingJoinPoint pjp) {
        String correlationId = MDC.get("correlationId");
        log.info("Executing use case | correlationId={}", correlationId);
        return pjp.proceed();
    }
}
```

---

## Summary Checklist

### Domain Layer ✅
- [ ] No framework dependencies
- [ ] No logging or metrics
- [ ] Pure business logic
- [ ] Immutable value objects
- [ ] Domain events raised
- [ ] Self-validating entities

### Application Layer ✅
- [ ] Port interfaces defined
- [ ] Use cases orchestrate
- [ ] No logging statements
- [ ] No business rules
- [ ] Result pattern used
- [ ] Transactions managed

### Infrastructure Layer ✅
- [ ] All framework code
- [ ] Repository adapters
- [ ] External gateways
- [ ] Logging via AOP
- [ ] Metrics via AOP
- [ ] Outbox pattern

### Presentation Layer ✅
- [ ] Controllers only
- [ ] Input validation
- [ ] HTTP mapping
- [ ] No business logic
- [ ] Result to HTTP conversion

### Observability ✅
- [ ] Correlation ID filter
- [ ] Use case logging aspect
- [ ] Event logging listener
- [ ] Exception logging aspect
- [ ] Metrics aspects
- [ ] Slow query detection

### Testing ✅
- [ ] Domain tests (no mocks)
- [ ] Application tests (mock ports)
- [ ] Infrastructure tests (testcontainers)
- [ ] Integration tests (full stack)

---

## Quick Reference

```
Domain       = Pure business logic (zero dependencies)
Application  = Orchestration (no implementation)
Infrastructure = Technical details (all frameworks)
Presentation = Input/output (HTTP, CLI)

Logging      → Infrastructure only (via AOP)
Metrics      → Infrastructure only (via AOP)
Events       → Domain creates, Infrastructure publishes
Result       → Application returns, Infrastructure logs failures
```

**Remember**: If removing observability doesn't change system behavior, it belongs in Infrastructure.

---

## End of Guide

This guide provides complete instructions for implementing Clean Architecture with proper separation of concerns. Follow these patterns strictly to maintain clean boundaries and testable code.