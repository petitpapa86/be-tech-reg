# Regtech Ingestion Module

The Ingestion Module is responsible for receiving, validating, processing, and storing bank exposure data files in a scalable, production-ready manner. This module handles the complete ingestion lifecycle from file upload through S3 storage to event publishing for downstream processing.

## Architecture

The module follows Domain-Driven Design (DDD) principles within a modular monolithic architecture:

- **Domain Layer** (`domain/`): Contains business logic, aggregates, value objects, and domain services
- **Application Layer** (`application/`): Contains command/query handlers, application services, and DTOs
- **Infrastructure Layer** (`infrastructure/`): Contains repositories, external service clients, and technical implementations
- **Web Layer** (`web/`): Contains REST controllers and web-specific configurations

## Key Features

- **File Processing**: Supports JSON and Excel files up to 500MB
- **Asynchronous Processing**: Upload API returns immediately, processing happens asynchronously
- **S3 Storage**: Enterprise-grade storage with encryption, versioning, and lifecycle policies
- **Bank Enrichment**: Integrates with Bank Registry service for bank information
- **Event Publishing**: Uses outbox pattern for guaranteed event delivery
- **Observability**: Real-time metrics via Prometheus/Grafana and distributed tracing via Zipkin.
  - See [Ingestion Dashboard](../../observability/grafana/dashboards/ingestion-dashboard.json)
  - See [Observability Guide](../../observability/README.md)

## Dependencies

This module leverages the following infrastructure from `regtech-core`:

- `Result<T>` pattern for error handling
- `ErrorDetail` and `FieldError` for structured error responses
- `CrossModuleEventBus` for inter-module communication
- `OutboxEventPublisher` and `GenericOutboxEventProcessor` for event publishing
- `Entity` base class for domain aggregates
- `BaseController` and `ApiResponse` for consistent REST API responses

## Configuration

Key configuration properties are defined in `application.yml`:

- S3 bucket and storage settings
- File size limits and supported types
- Bank Registry service configuration
- Processing thresholds and batch ID format

## Testing

The module includes comprehensive test coverage:

- Unit tests for domain logic and business rules
- Integration tests with testcontainers for database and S3
- Performance tests for large file processing
- Security tests for authentication and authorization

## Getting Started

1. Ensure `regtech-core` is built and available
2. Configure AWS credentials for S3 access
3. Set up Bank Registry service endpoint
4. Run tests: `mvn test`
5. Build module: `mvn clean compile`