# RegTech Platform

## Overview

The RegTech Platform is a comprehensive regulatory technology solution designed to help financial institutions comply with BCBS 239 (Principles for effective risk data aggregation and risk reporting). This modular, microservices-based system provides end-to-end capabilities for risk data management, from ingestion and quality assurance to risk calculation and regulatory reporting.

## Business Value

### Regulatory Compliance
- **BCBS 239 Compliance**: Ensures banks can aggregate and report risk data effectively across the organization
- **Automated Reporting**: Generates compliant risk reports with audit trails
- **Data Quality Assurance**: Validates and cleanses risk data to meet regulatory standards

### Operational Efficiency
- **Scalable Data Processing**: Handles large volumes of bank exposure data files (up to 500MB)
- **Automated Workflows**: Event-driven processing reduces manual intervention
- **Multi-Tenant Architecture**: Supports multiple banks with isolated data and role-based access

### Risk Management
- **Real-time Risk Calculation**: Continuous assessment of credit, market, and operational risks
- **Comprehensive Monitoring**: Health checks, metrics, and alerting for system reliability
- **Audit Trail**: Complete traceability of data processing and user actions

## Key Features

### 🔐 Identity & Access Management (IAM)
- **Role-Based Access Control**: Hierarchical roles from Viewer to System Admin
- **Multi-Tenant Support**: Bank-level and holding company user management
- **Secure Authentication**: JWT-based authentication with Stripe payment integration

### 💳 Billing & Subscription Management
- **Stripe Integration**: Secure payment processing for user subscriptions
- **Saga-Based Transactions**: Reliable payment verification with automatic rollback
- **Webhook Processing**: Real-time payment status updates

### 📥 Data Ingestion
- **Multi-Format Support**: JSON and Excel file processing
- **Asynchronous Processing**: Non-blocking file uploads with background processing
- **S3 Storage**: Enterprise-grade file storage with encryption and versioning
- **Bank Enrichment**: Automatic bank data validation and enrichment

### 🔍 Data Quality Assurance
- **Automated Validation**: Business rule validation and data cleansing
- **Rules Engine**: Configurable validation rules for different data types
- **Error Reporting**: Detailed validation reports with actionable insights

### 📊 Risk Calculation
- **Multi-Risk Assessment**: Credit, market, and operational risk calculations
- **Real-time Processing**: Continuous risk monitoring and alerting
- **Historical Analysis**: Trend analysis and risk pattern identification

### 📋 Report Generation
- **Regulatory Reporting**: BCBS 239 compliant risk reports
- **Custom Dashboards**: Role-based report access and visualization
- **Export Capabilities**: Multiple format support (PDF, Excel, JSON)

## Architecture

### Clean Architecture with DDD
The platform follows Domain-Driven Design principles with a layered architecture:

- **Domain Layer**: Business logic, aggregates, value objects, and domain events
- **Application Layer**: Command/query handlers, application services, and sagas
- **Infrastructure Layer**: Repositories, external integrations, and technical implementations
- **Presentation Layer**: REST controllers, DTOs, and API responses

### Event-Driven Communication
- **Internal Events**: Within-module communication
- **Integration Events**: Cross-module communication via event bus
- **Outbox Pattern**: Guaranteed event delivery with inbox processing
- **Saga Pattern**: Complex business transaction coordination

### Technology Stack
- **Java 25** with Spring Boot 3.5.6
- **PostgreSQL** for primary data storage
- **AWS S3** for file storage
- **Stripe** for payment processing
- **Docker** for containerization
- **Maven** for multi-module builds

## Module Structure

```
regtech/
├── regtech-core/              # Shared kernel and cross-cutting concerns
├── regtech-iam/               # Identity and access management
├── regtech-billing/           # Payment processing and subscriptions
├── regtech-ingestion/         # Data file ingestion and processing
├── regtech-data-quality/      # Data validation and quality assurance
├── regtech-risk-calculation/  # Risk assessment and calculations
├── regtech-report-generation/ # Regulatory reporting and dashboards
└── regtech-app/               # Main application entry point
```

## Getting Started

### Prerequisites
- Java 25 (with preview features enabled)
- Maven 3.8+
- PostgreSQL 15+
- Docker (for local development)

### Quick Start
1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd regtech
   ```

2. **Configure environment**
   ```bash
   cp .env.example .env
   # Edit .env with your Stripe keys and database settings
   ```

3. **Set up database**
   ```bash
   # Create PostgreSQL database
   createdb regtech
   
   # Run Flyway migrations
   mvn flyway:migrate -pl regtech-app
   ```
   
   See [DATABASE_MIGRATIONS.md](DATABASE_MIGRATIONS.md) for detailed migration instructions.

4. **Run the application**
   ```bash
   mvn clean install
   mvn spring-boot:run -pl regtech-app
   ```

### Development Setup
See [README-DEV.md](README-DEV.md) for detailed development environment setup, including IDE configuration and local testing.

## API Documentation

The platform provides comprehensive REST APIs for all business operations. Key endpoints include:

- **User Management**: `/api/v1/users/*`
- **Billing**: `/api/v1/billing/*`
- **Data Ingestion**: `/api/v1/ingestion/*`
- **Reports**: `/api/v1/reports/*`

See [API_ENDPOINTS.md](API_ENDPOINTS.md) for complete API documentation with request examples.

## Security & Compliance

### Authentication & Authorization
- JWT-based authentication with refresh tokens
- Role-based access control with hierarchical permissions
- Multi-tenant data isolation

### Data Protection
- Encryption at rest and in transit
- PCI DSS compliance for payment data
- GDPR compliance for user data

### Audit & Monitoring
- Comprehensive audit trails for all operations
- Real-time monitoring with Spring Boot Actuator
- Structured logging with correlation IDs

## Contributing

This platform follows established conventions and best practices. See [IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md) for:

- Architecture patterns and principles
- Implementation best practices
- Step-by-step guides for new features
- Code organization standards

## Support

- **Database Migrations**: [DATABASE_MIGRATIONS.md](DATABASE_MIGRATIONS.md)
- **Architecture Documentation**: [ARCHITECTURE_VIOLATIONS.md](ARCHITECTURE_VIOLATIONS.md)
- **Role System**: [ROLES_REFERENCE.md](ROLES_REFERENCE.md)
- **Saga Implementation**: [SAGA_IMPLEMENTATION_GUIDE.md](SAGA_IMPLEMENTATION_GUIDE.md)
- **Authentication**: [AUTHENTICATION_GUIDE.md](AUTHENTICATION_GUIDE.md)

## License

[License information to be added]