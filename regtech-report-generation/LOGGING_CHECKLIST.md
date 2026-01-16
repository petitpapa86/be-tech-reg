# Report Generation - Logging Checklist

This checklist summarizes the logging guidelines extracted from the repository Copilot instructions and records the minimal changes applied to `regtech-report-generation`.

- **Layer responsibilities**: Application = business flow logs; Infrastructure = technical/IO logs; Domain = no logging.
- **Application handlers**: Log start and end of use-cases, include key identifiers (e.g., `bankId`, `modifiedBy`). Keep messages concise.
- **Validation failures**: Aggregate validation errors in application layer; log them at WARN before returning `Result.failure`.
- **Persistence/IO errors**: Log errors in repository/infrastructure layers only (no changes made here now).
- **Context**: Use MDC for `correlationId` / `use-case` when available (do not log sensitive data).

Changes applied:
- Added `logger` and start/end logging to `GetReportConfigurationHandler`.
- Added `logger`, start/end logs, validation error logging, and save logging to `UpdateReportConfigurationHandler`.

Next recommended steps:
- Add MDC enrichment at web boundary if not already present (`X-Correlation-ID` filter).
- Add targeted error logs in repository adapters for DB exceptions.
