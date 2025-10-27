# Railway Pattern Guide

This guide explains how to use the `Maybe<T>`, `Result<T>`, and `ErrorDetail` types in the RegTech application, along with the railway-oriented programming pattern for handling operations that may succeed or fail.

## Table of Contents

1. [Maybe Type](#maybe-type)
2. [Result Type](#result-type)
3. [Error Handling](#error-handling)
4. [Railway Pattern](#railway-pattern)
5. [Usage Examples](#usage-examples)
6. [Best Practices](#best-practices)

## Maybe Type

The `Maybe<T>` type represents an optional value that may or may not be present. It's similar to `Optional<T>` but designed for functional programming patterns.

### Creating Maybe Instances

```java
// Create a Maybe with a value
Maybe<String> someValue = Maybe.some("Hello World");

// Create an empty Maybe
Maybe<String> noneValue = Maybe.none();
```

### Checking and Accessing Values

```java
Maybe<String> maybe = getUserInput();

if (maybe.isPresent()) {
    String value = maybe.getValue();
    // Use the value
}

if (maybe.isEmpty()) {
    // Handle the absence of value
}
```

### Converting to Result

```java
Maybe<String> maybe = findUserById(id);
Result<String> result = maybe.toResult("User not found");
// If maybe has a value: Result.success(value)
// If maybe is empty: Result.failure(ErrorDetail.of("NONE", "User not found", "maybe.none"))
```

## Result Type

The `Result<T>` type represents the outcome of an operation that can either succeed with a value or fail with an error.

### Creating Result Instances

```java
// Success result
Result<String> success = Result.success("Operation completed");

// Failure result
ErrorDetail error = ErrorDetail.of("VALIDATION_ERROR", "Invalid input", "error.validation");
Result<String> failure = Result.failure(error);
```

### Checking Result State

```java
Result<String> result = performOperation();

if (result.isSuccess()) {
    String value = result.getValue().orElse("default");
    // Handle success
}

if (result.isFailure()) {
    ErrorDetail error = result.getError().orElseThrow();
    // Handle failure
}
```

### Transforming Results

```java
Result<String> result = validateInput(input);

// Map success value
Result<Integer> length = result.map(String::length);

// FlatMap for operations that return Result
Result<String> upperCase = result.flatMap(s -> Result.success(s.toUpperCase()));

// Convert to Optional (discards error information)
Optional<String> optional = result.toOptional();
```

## Error Handling

### ErrorDetail Structure

```java
public class ErrorDetail {
    private final String code;
    private final String message;
    private final String messageKey;
    private final String details;
    private final List<FieldError> fieldErrors;
}
```

### Creating Error Details

```java
// Simple error
ErrorDetail error = ErrorDetail.of("NOT_FOUND", "User not found");

// Error with message key for internationalization
ErrorDetail error = ErrorDetail.of("VALIDATION_ERROR", "Validation failed", "error.validation");

// Validation error with field errors
List<FieldError> fieldErrors = List.of(
    new FieldError("email", "REQUIRED", "Email is required"),
    new FieldError("password", "TOO_SHORT", "Password must be at least 8 characters")
);
ErrorDetail validationError = ErrorDetail.validationError(fieldErrors);
```

### Error Types

Use the `ErrorType` enum to categorize errors for frontend handling:

```java
public enum ErrorType {
    VALIDATION_ERROR,      // Field validation errors
    BUSINESS_RULE_ERROR,   // Business logic violations
    SYSTEM_ERROR,          // System failures
    AUTHENTICATION_ERROR,  // Auth/authz issues
    NOT_FOUND_ERROR        // Resource not found
}
```

## Railway Pattern

The railway pattern allows chaining operations where each step can succeed or fail, with failures short-circuiting the chain. This provides a clean way to handle complex workflows with multiple potential failure points.

### Basic Railway Operations

```java
Result<User> result = validateEmail(email)
    .onSuccess(() -> logValidation(email))
    .onSuccess(() -> createUser(email))
    .onFailure(() -> logFailure(email));
```

## Railway Pattern Methods

The railway pattern provides several methods for handling success and failure paths:

### Core Methods

#### flatMap(Function<T, Result<U>> mapper)
Chains operations that return Results. If the current result is successful, applies the mapper function. If failed, returns the original failure.

```java
Result<String> result = validateInput(input)
    .flatMap(validInput -> processInput(validInput))
    .flatMap(processed -> saveToDatabase(processed));
```

#### onSuccess(Runnable action)
Executes a side effect if the result is successful. Returns the original result unchanged.

```java
result.onSuccess(() -> logger.info("Operation completed successfully"));
```

#### onSuccess(Supplier<Result<T>> func)
Chains to another operation that returns a Result. Useful for conditional chaining.

```java
result.onSuccess(() -> performAdditionalOperation());
```

#### onFailure(Runnable action)
Executes a side effect if the result is a failure. Returns the original result unchanged.

```java
result.onFailure(() -> logger.error("Operation failed"));
```

#### onBoth(Consumer<Result<T>> action) / onBoth(Function<Result<T>, U> func)
Executes actions regardless of success or failure.

### Chaining Side-Effect Operations

When you have multiple operations that return `Result<Void>` (side effects) that depend on a previous successful result, use `flatMap` with `map` to preserve the original value:

```java
// Create customer, then attach payment method, then set as default
Result<StripeCustomer> result = stripeService.createCustomer(email, name)
    .flatMap(customer -> stripeService.attachPaymentMethod(customer.customerId(), paymentMethodId)
        .map(ignored -> customer))  // Preserve the customer value
    .flatMap(customer -> stripeService.setDefaultPaymentMethod(customer.customerId(), paymentMethodId)
        .map(ignored -> customer)); // Preserve the customer value

result.onSuccess(customer -> {
    // Use the customer for business logic
    updateBillingAccount(customer);
})
.onFailure(error -> logger.error("Stripe operations failed: {}", error.getMessage()));
```

### When flatMap Might Be Overused

While `flatMap` is powerful for chaining, consider these alternatives when the chaining becomes complex:

#### Option 1: Group Related Operations
If you have multiple related side effects, consider creating a method that performs them together:

```java
public Result<StripeCustomer> setupCustomerPaymentMethod(StripeCustomer customer, PaymentMethodId paymentMethodId) {
    return stripeService.attachPaymentMethod(customer.customerId(), paymentMethodId)
        .flatMap(ignored -> stripeService.setDefaultPaymentMethod(customer.customerId(), paymentMethodId))
        .map(ignored -> customer);
}

// Usage
Result<StripeCustomer> result = stripeService.createCustomer(email, name)
    .flatMap(customer -> setupCustomerPaymentMethod(customer, paymentMethodId));
```

#### Option 2: Use onSuccess for Independent Side Effects
If operations don't depend on each other and don't return values you need, use `onSuccess`:

```java
Result<StripeCustomer> customerResult = stripeService.createCustomer(email, name);

customerResult.onSuccess(customer -> {
    // These operations are independent and don't affect the result
    stripeService.attachPaymentMethod(customer.customerId(), paymentMethodId);
    stripeService.setDefaultPaymentMethod(customer.customerId(), paymentMethodId);
    updateBillingAccount(customer);
});
```

**Choose based on your error handling needs:**
- Use `flatMap` when you need to stop on first failure and preserve the success value
- Use `onSuccess` when operations are independent and you want to attempt all of them

## Usage Examples

### User Registration Workflow

```java
public Result<User> registerUser(String email, String password) {
    return validateEmail(email)
        .onSuccess(() -> validatePassword(password))
        .onSuccess(() -> checkEmailAvailability(email))
        .onSuccess(() -> createUser(email, password))
        .onSuccess(user -> sendWelcomeEmail(user))
        .onFailure(() -> logRegistrationFailure(email));
}
```

### Real-World Example: Command Validation

From the RegTech IAM module, here's how `RegisterUserCommand` uses Result for validation:

```java
public static Result<RegisterUserCommand> create(
        String email, String password, String firstName, String lastName,
        String bankId, String paymentMethodId, String phone, AddressInfo address) {

    List<FieldError> fieldErrors = new ArrayList<>();

    // Validate each field and collect errors
    if (email == null || email.trim().isEmpty()) {
        fieldErrors.add(new FieldError("email", "REQUIRED", "Email is required"));
    }
    if (password == null || password.trim().isEmpty()) {
        fieldErrors.add(new FieldError("password", "REQUIRED", "Password is required"));
    }
    // ... more validations

    if (!fieldErrors.isEmpty()) {
        ErrorDetail error = ErrorDetail.validationError(fieldErrors);
        return Result.failure(error);
    }

    return Result.success(new RegisterUserCommand(/* ... */));
}
```

Usage in tests:
```java
Result<RegisterUserCommand> result = RegisterUserCommand.create(email, password, firstName, lastName, bankId, paymentMethodId, null, null);

if (result.isSuccess()) {
    RegisterUserCommand command = result.getValue().get();
    // Process the valid command
} else {
    ErrorDetail error = result.getError().get();
    // Handle validation errors
}
```

### Railway Pattern with Chaining

Now that we've added railway pattern methods, you can chain operations more elegantly:

```java
public Result<User> registerUser(String email, String password) {
    return RegisterUserCommand.create(email, password, firstName, lastName, bankId, paymentMethodId, phone, address)
        .onSuccess(command -> validateBusinessRules(command))
        .onSuccess(validCommand -> createUser(validCommand))
        .onSuccess(user -> sendWelcomeEmail(user))
        .onFailure(() -> logRegistrationFailure(email))
        .onBoth(result -> auditUserRegistration(email, result));
}
```

Or using the functional chaining style:
```java
public Result<ProcessedOrder> processOrder(OrderRequest request) {
    return Maybe.some(request)
        .toResult("Order request cannot be null")
        .onSuccess(() -> validateOrderRequest(request))
        .flatMap(validRequest -> enrichOrderWithDefaults(validRequest))
        .flatMap(enriched -> calculatePricing(enriched))
        .flatMap(priced -> saveOrder(priced))
        .onSuccess(savedOrder -> publishOrderCreatedEvent(savedOrder))
        .onBoth(result -> logOrderProcessing(request.getId(), result));
}
```

### Data Processing Pipeline

```java
public Result<ProcessedData> processData(RawData rawData) {
    return validateRawData(rawData)
        .onSuccess(() -> parseData(rawData))
        .flatMap(parsed -> validateParsedData(parsed))
        .flatMap(validated -> transformData(validated))
        .onSuccess(processed -> saveToDatabase(processed))
        .onBoth(result -> auditLog(result, rawData.getId()));
}
```

### Converting Between Types

```java
public Result<Order> processOrderRequest(OrderRequest request) {
    return Maybe.some(request)
        .toResult("Order request is null")
        .onSuccess(() -> validateOrderRequest(request))
        .flatMap(validRequest -> createOrder(validRequest))
        .onSuccess(order -> publishOrderCreatedEvent(order));
}
```

### Error Handling with Recovery

```java
public Result<String> getUserName(String userId) {
    return findUserById(userId)
        .toResult("User not found")
        .map(User::getName)
        .onFailure(() -> attemptFallbackLookup(userId))
        .onBoth(result -> cacheLookupResult(userId, result));
}
```

## Best Practices

### 1. Use Descriptive Error Messages
```java
// Good
ErrorDetail error = ErrorDetail.of("EMAIL_EXISTS", "Email address already registered", "user.email.exists");

// Avoid
ErrorDetail error = ErrorDetail.of("ERROR", "Something went wrong");
```

### 2. Chain Operations Logically
```java
// Good: Each step depends on the previous success
Result<Order> result = validateOrder(order)
    .onSuccess(() -> checkInventory(order))
    .onSuccess(() -> calculateTotal(order))
    .onSuccess(() -> processPayment(order));

// Avoid: Unnecessary chaining
Result<String> result = Result.success("hello")
    .onSuccess(() -> System.out.println("world")) // Side effect in chain
    .map(s -> s.toUpperCase());
```

### 3. Handle Errors Appropriately
```java
// Good: Log failures, don't throw exceptions
result.onFailure(() -> logger.error("Operation failed: {}", result.getError().map(ErrorDetail::getMessage).orElse("Unknown")));

// Avoid: Throwing exceptions from railway chains
// result.onFailure(() -> { throw new RuntimeException("Failed"); });
```

### 4. Use Maybe for Optional Values
```java
// Good: Use Maybe for truly optional values
Maybe<String> middleName = user.getMiddleName();
Result<String> fullName = Maybe.some(user.getFirstName())
    .toResult("First name required")
    .flatMap(first -> middleName.isPresent()
        ? Result.success(first + " " + middleName.getValue() + " " + user.getLastName())
        : Result.success(first + " " + user.getLastName()));

// Avoid: Using null checks everywhere
String fullName = user.getFirstName() + " " +
    (user.getMiddleName() != null ? user.getMiddleName() + " " : "") +
    user.getLastName();
```

### 5. Keep Railway Chains Readable
```java
// Good: Break long chains into methods
public Result<Order> processOrder(OrderRequest request) {
    return validateAndEnrich(request)
        .flatMap(this::checkBusinessRules)
        .flatMap(this::saveAndNotify);
}

private Result<EnrichedOrder> validateAndEnrich(OrderRequest request) {
    return validateOrder(request)
        .flatMap(valid -> enrichWithDefaults(valid));
}

// Avoid: Very long single chains
public Result<Order> processOrder(OrderRequest request) {
    return validateOrder(request).onSuccess(() -> enrich(request)).onSuccess(() -> checkRules(request))...
}
```

### 6. Use Appropriate Error Types
```java
// Good: Use specific error types for different scenarios
switch (errorType) {
    case VALIDATION_ERROR -> handleValidationError(fieldErrors);
    case BUSINESS_RULE_ERROR -> handleBusinessError(error);
    case SYSTEM_ERROR -> handleSystemError(error);
    case AUTHENTICATION_ERROR -> redirectToLogin();
    case NOT_FOUND_ERROR -> showNotFoundPage();
}
```

This railway pattern implementation provides a robust, functional approach to error handling and operation chaining in the RegTech application, making code more readable and maintainable.