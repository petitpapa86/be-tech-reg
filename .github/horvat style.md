# Horvat Emergent Design - Simplified Production Guide

## Core Philosophy (30 seconds read)

**Three Principles:**
1. **Domain First** - Name business concepts before writing code
2. **Wrap When Needed** - Only create objects if they add behavior (2+ domain methods)
3. **Infrastructure Last** - Keep databases, HTTP, and external APIs out of domain logic

---

## Quick Decision Trees

### Where Does This Code Go?
```
Is it a business rule or calculation?
├─ YES → domain/ package
│
Is it about HTTP, validation, or formatting responses?
├─ YES → presentation/ package  
│
Is it about database, external APIs, or file I/O?
├─ YES → infrastructure/ package
│
Does it orchestrate multiple steps?
└─ YES → application/ package (use case)
```

### Should I Create a Class or Keep It Simple?
```
How many domain methods does this need? (not counting getters)
│
├─ 0-1 methods → Keep as primitive (int, String) or use Record
├─ 2-3 methods → Consider a class
└─ 4+ methods → Definitely create a class
```

**Examples:**
- `Money` → Create class (has: add, subtract, multiply, isPositive, isZero)
- `OrderId` → Use Record (just holds a value)
- `int quantity` → Keep primitive (just a number)

---

## Layer Rules (One Page)

### 🟢 Domain Layer (`domain/`)

**Allowed:**
- Business logic and rules
- Entity classes with behavior
- Value Objects (use Records when immutable)
- Domain exceptions

**Forbidden:**
- `@Entity`, `@Table`, `@Column`
- `@Service`, `@Autowired`
- `import java.sql.*`
- `import org.springframework.data.*`

**Example:**
```java
// ✅ GOOD - Rich domain object
public class Order {
    private final OrderId id;
    private OrderStatus status;
    private final List<OrderLine> lines;
    
    public Money totalAmount() {
        return lines.stream()
            .map(OrderLine::subtotal)
            .reduce(Money.zero(), Money::add);
    }
    
    public void cancel(CancellationReason reason) {
        if (!status.isCancellable()) {
            throw new OrderNotCancellableException();
        }
        this.status = OrderStatus.CANCELLED;
    }
}

// ❌ BAD - Anemic model (just data)
@Entity
public class Order {
    private Long id;
    private String status;
    private BigDecimal total;
    
    // Only getters/setters
}
```

---

### 🔵 Application Layer (`application/`)

**Purpose:** Coordinate workflow between domain and infrastructure

**Pattern:**
```java
@Service
public class CompleteCheckoutUseCase {
    private final OrderRepository orderRepo;  // Port (interface)
    private final PaymentGateway paymentGateway;  // Port (interface)
    
    public Result<Order> execute(CheckoutCommand command) {
        // 1. Load domain objects (via ports)
        ShoppingCart cart = cartRepo.findById(command.cartId());
        
        // 2. Execute domain logic
        Result<Order> result = cart.checkout(command.payment(), command.shipping());
        
        // 3. Save if successful (via ports)
        if (result.isSuccess()) {
            orderRepo.save(result.getValue());
        }
        
        return result;
    }
}
```

**Forbidden:**
- Direct database queries (use repositories)
- HTTP request/response handling
- Business calculations (delegate to domain)

---

### 🟡 Infrastructure Layer (`infrastructure/`)

**Purpose:** Implement technical details (DB, APIs, files)

**Allowed:**
- `@Repository`, `@Entity`, JPA annotations
- SQL queries, JDBC code
- External API calls
- File I/O

**Pattern:**
```java
@Repository
public class JpaOrderRepository implements OrderRepository {
    
    @PersistenceContext
    private EntityManager em;
    
    @Override
    public Optional<Order> findById(OrderId id) {
        OrderEntity entity = em.find(OrderEntity.class, id.value());
        return Optional.ofNullable(entity)
            .map(this::toDomain);  // Convert to domain object
    }
    
    @Override
    public void save(Order order) {
        OrderEntity entity = fromDomain(order);
        em.persist(entity);
    }
    
    private Order toDomain(OrderEntity entity) {
        // Map JPA entity → Domain object
    }
    
    private OrderEntity fromDomain(Order order) {
        // Map Domain object → JPA entity
    }
}
```

---

### 🔴 Presentation Layer (`presentation/`)

**Purpose:** Handle HTTP, validation, response formatting

**Pattern - Functional Endpoints:**
```java
@Component
public class OrderEndpoints implements IEndpoint {
    
    private final CompleteCheckoutUseCase checkoutUseCase;
    
    @Bean
    public RouterFunction<ServerResponse> orderRoutes() {
        return route()
            .POST("/api/orders/checkout", this::checkout)
            .GET("/api/orders/{id}", this::getOrder)
            .build();
    }
    
    private Mono<ServerResponse> checkout(ServerRequest request) {
        return request.bodyToMono(CheckoutRequest.class)
            .map(this::toCommand)  // DTO → Command
            .map(checkoutUseCase::execute)  // Execute use case
            .flatMap(result -> {
                if (result.isSuccess()) {
                    return ok().bodyValue(toResponse(result.getValue()));
                }
                return badRequest().bodyValue(toErrorResponse(result));
            });
    }
}
```

**Forbidden:**
- Business logic
- Database access
- Domain calculations

---

## Result Pattern Rules

### When to Use Result vs Exception

**Use `Result<T>` when:**
- Method has 3+ possible business outcomes
- Caller needs different behavior per outcome
- Failures are expected (out of stock, payment declined, validation failed)

**Use Exception when:**
- Technical failure (database down, timeout)
- Programming error (null pointer)
- Simple validation (fail fast)

**Use `Optional<T>` when:**
- Binary outcome (found/not found)
- No special failure reasons needed

**Examples:**
```java
// ✅ Use Result - multiple business outcomes
Result<Order> checkout(Payment payment) {
    if (cart.isEmpty()) {
        return Result.failure("EMPTY_CART", "Cart is empty");
    }
    if (inventory.insufficient()) {
        return Result.failure("OUT_OF_STOCK", "Not enough stock");
    }
    if (payment.declined()) {
        return Result.failure("PAYMENT_DECLINED", "Payment was declined");
    }
    return Result.success(order);
}

// ✅ Use Optional - simple query
Optional<Order> findById(OrderId id) {
    return orderRepo.findById(id);
}

// ✅ Use Exception - fail fast validation
void validateEmail(String email) {
    if (email == null || !email.contains("@")) {
        throw new ValidationException("Invalid email");
    }
}
```

---

## Common Patterns

### Pattern 1: Value Object (Immutable)
```java
// Use Record for simple value objects
public record OrderId(String value) {
    public OrderId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("OrderId cannot be blank");
        }
    }
}

// Use Class when 2+ domain methods needed
public final class Money {
    private final BigDecimal amount;
    private final Currency currency;
    
    public Money add(Money other) { /*...*/ }
    public Money multiply(int factor) { /*...*/ }
    public boolean isPositive() { /*...*/ }
    public boolean isZero() { /*...*/ }
}
```

### Pattern 2: Entity (Mutable State)
```java
public class Order {
    private final OrderId id;  // Immutable identity
    private OrderStatus status;  // Mutable state
    private final List<OrderLine> lines;
    
    // Domain behavior
    public void cancel() {
        if (!status.isCancellable()) {
            throw new OrderNotCancellableException();
        }
        this.status = OrderStatus.CANCELLED;
    }
}
```

### Pattern 3: Repository Port (Interface)
```java
// Domain defines what it needs (interface)
public interface OrderRepository {
    Optional<Order> findById(OrderId id);
    void save(Order order);
    List<Order> findByCustomer(CustomerId customerId);
}

// Infrastructure implements it (class)
@Repository
public class JpaOrderRepository implements OrderRepository {
    // JPA implementation
}
```

---

## Anti-Patterns to Avoid

### ❌ Anemic Domain Model
```java
// BAD - Just data, no behavior
public class Order {
    private String status;
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

// Service does all the work
@Service
public class OrderService {
    public void cancelOrder(Order order) {
        order.setStatus("CANCELLED");  // Logic in service!
    }
}
```

### ❌ Domain Depending on Infrastructure
```java
// BAD - Domain importing Spring/JPA
import org.springframework.data.jpa.repository.JpaRepository;

public class Order {
    @Autowired
    private OrderRepository repo;  // ❌ Domain should not autowire!
}
```

### ❌ Business Logic in Controller
```java
// BAD - Calculation in presentation layer
@PostMapping("/orders")
public ResponseEntity<?> createOrder(@RequestBody OrderRequest req) {
    double total = 0;
    for (Item item : req.getItems()) {
        total += item.getPrice() * item.getQuantity();  // ❌ Business logic!
    }
}
```

---

## Naming Conventions

### Classes
- Domain: `Order`, `PaymentAuthorization`, `ShippingArrangement` (nouns)
- Use Cases: `CompleteCheckoutUseCase`, `CancelOrderUseCase` (verb + UseCase)
- Repositories: `OrderRepository`, `CustomerRepository` (Entity + Repository)

### Methods
- Domain: `cancel()`, `authorize()`, `reserve()` (verbs for commands)
- Domain: `isValid()`, `canBeCancelled()`, `totalAmount()` (queries)
- Use Cases: `execute()`, `handle()`

### Avoid
- `*Service`, `*Manager`, `*Helper`, `*Util` (too generic)
- `process()`, `do()`, `handle()` in domain (use specific verbs)

---

## Quick Validation Checklist

Before committing:
- [ ] Domain has NO Spring/JPA annotations
- [ ] Domain classes have behavior (not just getters/setters)
- [ ] Use Cases delegate to domain objects
- [ ] Controllers only convert DTO ↔ Domain
- [ ] Repositories return domain objects (not entities)
- [ ] Result used for multiple business outcomes
- [ ] Exceptions used for technical failures

---

## One-Sentence Summary

**Put business logic in domain objects with meaningful names, keep infrastructure (database/HTTP) separate, and wrap primitives only when they need 2+ domain operations.**