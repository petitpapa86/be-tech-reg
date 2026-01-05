# Report e Distribuzione Configuration - regtech-report-generation Module

## Project Structure (Organized by Capabilities)

```
regtech-report-generation/
├── domain/
│   └── src/main/java/com/bcbs239/regtech/reportgeneration/domain/
│       └── configuration/                        # Capability: Report Configuration
│           ├── ReportConfiguration.java          → Aggregate Root
│           ├── ReportConfigurationRepository.java → Repository interface
│           └── valueobject/
│               ├── ReportTemplate.java           → Enum
│               ├── ReportLanguage.java           → Enum
│               ├── OutputFormat.java             → Enum
│               ├── ReportFrequency.java          → Enum
│               ├── EmailRecipient.java           → Value object with Result
│               ├── ScheduleDay.java              → Enum
│               ├── ScheduleTime.java             → Value object with Result
│               └── SubmissionDeadline.java       → Value object with Result
│
├── application/
│   └── src/main/java/com/bcbs239/regtech/reportgeneration/application/
│       └── configuration/                        # Capability: Report Configuration
│           ├── GetReportConfigurationHandler.java    → Query handler
│           └── UpdateReportConfigurationHandler.java → Command handler
│
├── infrastructure/
│   └── src/main/java/com/bcbs239/regtech/reportgeneration/infrastructure/
│       └── persistence/configuration/
│           ├── ReportConfigurationJpaEntity.java     → JPA entity
│           ├── ReportConfigurationJpaRepository.java → Spring Data
│           └── ReportConfigurationRepositoryAdapter.java → Repository implementation
│
└── presentation/
    └── src/main/java/com/bcbs239/regtech/reportgeneration/presentation/
        └── configuration/                        # Capability: Report Configuration
            ├── ReportConfigurationController.java    → Route functions
            ├── ReportConfigurationRequest.java       → Request DTO (record)
            └── ReportConfigurationResponse.java      → Response DTO (record)
```

---

## ITERATION 1: Domain Layer - Value Objects with Smart Constructors

### Enums for Configuration Options

**File: `domain/src/main/java/com/bcbs239/regtech/reportgeneration/domain/configuration/valueobject/ReportTemplate.java`**

```java
package com.bcbs239.regtech.reportgeneration.domain.configuration.valueobject;

import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Report template type for BCBS 239 compliance
 */
@Getter
@RequiredArgsConstructor
public enum ReportTemplate {
    BANCA_ITALIA_STANDARD("Banca d'Italia - Standard"),
    ECB_EUROPEAN_STANDARD("ECB - European Standard"),
    CUSTOM("Custom Template");
    
    private final String displayName;
    
    public static Result<ReportTemplate> of(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure("Report template cannot be empty");
        }
        
        try {
            return Result.success(ReportTemplate.valueOf(value.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Result.failure("Invalid report template: " + value);
        }
    }
}
```

**File: `domain/src/main/java/com/bcbs239/regtech/reportgeneration/domain/configuration/valueobject/ReportLanguage.java`**

```java
package com.bcbs239.regtech.reportgeneration.domain.configuration.valueobject;

import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportLanguage {
    ITALIAN("Italiano", "it"),
    ENGLISH("English", "en"),
    BILINGUAL("Bilingue (IT/EN)", "it-en");
    
    private final String displayName;
    private final String code;
    
    public static Result<ReportLanguage> of(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure("Report language cannot be empty");
        }
        
        try {
            return Result.success(ReportLanguage.valueOf(value.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Result.failure("Invalid report language: " + value);
        }
    }
}
```

**File: `domain/src/main/java/com/bcbs239/regtech/reportgeneration/domain/configuration/valueobject/OutputFormat.java`**

```java
package com.bcbs239.regtech.reportgeneration.domain.configuration.valueobject;

import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OutputFormat {
    PDF("PDF", "application/pdf", ".pdf"),
    EXCEL("Excel (.xlsx)", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx"),
    BOTH("Entrambi i formati", "mixed", ".pdf,.xlsx");
    
    private final String displayName;
    private final String mimeType;
    private final String extension;
    
    public static Result<OutputFormat> of(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure("Output format cannot be empty");
        }
        
        try {
            return Result.success(OutputFormat.valueOf(value.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Result.failure("Invalid output format: " + value);
        }
    }
    
    public boolean includesPdf() {
        return this == PDF || this == BOTH;
    }
    
    public boolean includesExcel() {
        return this == EXCEL || this == BOTH;
    }
}
```

**File: `domain/src/main/java/com/bcbs239/regtech/reportgeneration/domain/configuration/valueobject/ReportFrequency.java`**

```java
package com.bcbs239.regtech.reportgeneration.domain.configuration.valueobject;

import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportFrequency {
    MONTHLY("Mensile", 1),
    QUARTERLY("Trimestrale", 3),
    SEMI_ANNUAL("Semestrale", 6),
    ANNUAL("Annuale", 12);
    
    private final String displayName;
    private final int monthsInterval;
    
    public static Result<ReportFrequency> of(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure("Report frequency cannot be empty");
        }
        
        try {
            return Result.success(ReportFrequency.valueOf(value.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Result.failure("Invalid report frequency: " + value);
        }
    }
}
```

**File: `domain/src/main/java/com/bcbs239/regtech/reportgeneration/domain/configuration/valueobject/ScheduleDay.java`**

```java
package com.bcbs239.regtech.reportgeneration.domain.configuration.valueobject;

import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScheduleDay {
    MONDAY("Lunedì", 1),
    TUESDAY("Martedì", 2),
    WEDNESDAY("Mercoledì", 3),
    THURSDAY("Giovedì", 4),
    FRIDAY("Venerdì", 5);
    
    private final String displayName;
    private final int dayOfWeek;
    
    public static Result<ScheduleDay> of(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure("Schedule day cannot be empty");
        }
        
        try {
            return Result.success(ScheduleDay.valueOf(value.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Result.failure("Invalid schedule day: " + value);
        }
    }
}
```

### Value Objects with Smart Constructors

**File: `domain/src/main/java/com/bcbs239/regtech/reportgeneration/domain/configuration/valueobject/EmailRecipient.java`**

```java
package com.bcbs239.regtech.reportgeneration.domain.configuration.valueobject;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import lombok.Value;

/**
 * Email recipient for report distribution
 * 
 * Smart constructor returns Result for required fields,
 * Maybe for optional fields
 */
@Value
public class EmailRecipient {
    String email;
    
    private static final String EMAIL_REGEX = 
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    
    private EmailRecipient(String email) {
        this.email = email;
    }
    
    /**
     * Smart constructor for required email (primary recipient)
     */
    public static Result<EmailRecipient> of(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure("Email recipient cannot be empty");
        }
        
        String trimmed = value.trim().toLowerCase();
        
        if (!trimmed.matches(EMAIL_REGEX)) {
            return Result.failure("Invalid email format: " + trimmed);
        }
        
        return Result.success(new EmailRecipient(trimmed));
    }
    
    /**
     * Smart constructor for optional email (CC recipient)
     */
    public static Maybe<EmailRecipient> ofOptional(String value) {
        if (value == null || value.isBlank()) {
            return Maybe.empty();
        }
        
        String trimmed = value.trim().toLowerCase();
        
        if (!trimmed.matches(EMAIL_REGEX)) {
            return Maybe.empty();
        }
        
        return Maybe.of(new EmailRecipient(trimmed));
    }
    
    @Override
    public String toString() {
        return email;
    }
}
```

**File: `domain/src/main/java/com/bcbs239/regtech/reportgeneration/domain/configuration/valueobject/ScheduleTime.java`**

```java
package com.bcbs239.regtech.reportgeneration.domain.configuration.valueobject;

import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.Value;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Time for scheduled report generation
 * Format: HH:mm (24-hour)
 */
@Value
public class ScheduleTime {
    LocalTime time;
    
    private ScheduleTime(LocalTime time) {
        this.time = time;
    }
    
    public static Result<ScheduleTime> of(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure("Schedule time cannot be empty");
        }
        
        try {
            LocalTime parsed = LocalTime.parse(value, DateTimeFormatter.ofPattern("HH:mm"));
            
            // Business rule: Only allow working hours (08:00 - 18:00)
            if (parsed.getHour() < 8 || parsed.getHour() >= 18) {
                return Result.failure(
                    "Schedule time must be within working hours (08:00 - 18:00): " + value
                );
            }
            
            return Result.success(new ScheduleTime(parsed));
        } catch (DateTimeParseException e) {
            return Result.failure("Invalid time format (expected HH:mm): " + value);
        }
    }
    
    public String toFormattedString() {
        return time.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
    
    @Override
    public String toString() {
        return toFormattedString();
    }
}
```

**File: `domain/src/main/java/com/bcbs239/regtech/reportgeneration/domain/configuration/valueobject/SubmissionDeadline.java`**

```java
package com.bcbs239.regtech.reportgeneration.domain.configuration.valueobject;

import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.Value;

/**
 * Number of days available for report submission after period end
 * 
 * Business Rule: Must be between 1 and 30 days
 */
@Value
public class SubmissionDeadline {
    int days;
    
    private SubmissionDeadline(int days) {
        this.days = days;
    }
    
    public static Result<SubmissionDeadline> of(int value) {
        if (value < 1 || value > 30) {
            return Result.failure(
                "Submission deadline must be between 1 and 30 days, got: " + value
            );
        }
        
        return Result.success(new SubmissionDeadline(value));
    }
    
    /**
     * Domain behavior: Is this a tight deadline?
     */
    public boolean isTight() {
        return days <= 10;
    }
    
    @Override
    public String toString() {
        return days + " giorni";
    }
}
```

---

## ITERATION 2: Domain Model - Aggregate Root

**File: `domain/src/main/java/com/bcbs239/regtech/reportgeneration/domain/configuration/ReportConfiguration.java`**

```java
package com.bcbs239.regtech.reportgeneration.domain.configuration;

import com.bcbs239.regtech.reportgeneration.domain.configuration.valueobject.*;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import lombok.Builder;
import lombok.Value;
import java.time.Instant;

/**
 * ReportConfiguration - Aggregate Root
 * 
 * Represents report generation and distribution settings.
 * Immutable - use builder for updates.
 */
@Value
@Builder(toBuilder = true)
public class ReportConfiguration {
    
    // Template & Format
    ReportTemplate template;
    ReportLanguage language;
    OutputFormat outputFormat;
    
    // Scheduling
    ReportFrequency frequency;
    SubmissionDeadline submissionDeadline;
    boolean autoGenerationEnabled;
    ScheduleDay scheduleDay;
    ScheduleTime scheduleTime;
    
    // Distribution
    EmailRecipient primaryRecipient;
    Maybe<EmailRecipient> ccRecipient;
    boolean autoSendEnabled;
    boolean swiftAutoSubmitEnabled;  // Currently in development
    
    // Metadata
    Instant lastModified;
    String lastModifiedBy;
    
    /**
     * Domain behavior: Is automatic generation enabled?
     */
    public boolean isAutomationEnabled() {
        return autoGenerationEnabled && autoSendEnabled;
    }
    
    /**
     * Domain behavior: Should generate PDF?
     */
    public boolean shouldGeneratePdf() {
        return outputFormat.includesPdf();
    }
    
    /**
     * Domain behavior: Should generate Excel?
     */
    public boolean shouldGenerateExcel() {
        return outputFormat.includesExcel();
    }
    
    /**
     * Domain behavior: Is this a high-frequency report?
     */
    public boolean isHighFrequency() {
        return frequency == ReportFrequency.MONTHLY;
    }
    
    /**
     * Domain behavior: Has tight deadline?
     */
    public boolean hasTightDeadline() {
        return submissionDeadline.isTight();
    }
    
    /**
     * Domain method: Update configuration with new values
     * Returns new instance (immutability)
     */
    public ReportConfiguration update(
            ReportTemplate template,
            ReportLanguage language,
            OutputFormat outputFormat,
            ReportFrequency frequency,
            SubmissionDeadline submissionDeadline,
            boolean autoGenerationEnabled,
            ScheduleDay scheduleDay,
            ScheduleTime scheduleTime,
            EmailRecipient primaryRecipient,
            Maybe<EmailRecipient> ccRecipient,
            boolean autoSendEnabled,
            boolean swiftAutoSubmitEnabled,
            String modifiedBy) {
        
        return this.toBuilder()
            .template(template)
            .language(language)
            .outputFormat(outputFormat)
            .frequency(frequency)
            .submissionDeadline(submissionDeadline)
            .autoGenerationEnabled(autoGenerationEnabled)
            .scheduleDay(scheduleDay)
            .scheduleTime(scheduleTime)
            .primaryRecipient(primaryRecipient)
            .ccRecipient(ccRecipient)
            .autoSendEnabled(autoSendEnabled)
            .swiftAutoSubmitEnabled(swiftAutoSubmitEnabled)
            .lastModified(Instant.now())
            .lastModifiedBy(modifiedBy)
            .build();
    }
}
```

**File: `domain/src/main/java/com/bcbs239/regtech/reportgeneration/domain/configuration/ReportConfigurationRepository.java`**

```java
package com.bcbs239.regtech.reportgeneration.domain.configuration;

import com.bcbs239.regtech.core.domain.shared.Maybe;

/**
 * Repository interface (domain layer)
 * Implementation in infrastructure layer
 */
public interface ReportConfigurationRepository {
    
    /**
     * Get current report configuration (singleton)
     */
    Maybe<ReportConfiguration> findCurrent();
    
    /**
     * Save/update report configuration
     */
    ReportConfiguration save(ReportConfiguration configuration);
}
```

---

## ITERATION 3: Application Layer - Handlers

**File: `application/src/main/java/com/bcbs239/regtech/reportgeneration/application/configuration/GetReportConfigurationHandler.java`**

```java
package com.bcbs239.regtech.reportgeneration.application.configuration;

import com.bcbs239.regtech.reportgeneration.domain.configuration.ReportConfiguration;
import com.bcbs239.regtech.reportgeneration.domain.configuration.ReportConfigurationRepository;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Query Handler: Get Report Configuration
 * 
 * Application layer = GLUE only
 */
@Service
@RequiredArgsConstructor
public class GetReportConfigurationHandler {
    
    private final ReportConfigurationRepository repository;
    
    @Transactional(readOnly = true)
    public Maybe<ReportConfiguration> handle() {
        return repository.findCurrent();
    }
}
```

**File: `application/src/main/java/com/bcbs239/regtech/reportgeneration/application/configuration/UpdateReportConfigurationHandler.java`**

```java
package com.bcbs239.regtech.reportgeneration.application.configuration;

import com.bcbs239.regtech.reportgeneration.domain.configuration.ReportConfiguration;
import com.bcbs239.regtech.reportgeneration.domain.configuration.ReportConfigurationRepository;
import com.bcbs239.regtech.reportgeneration.domain.configuration.valueobject.*;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Command Handler: Update Report Configuration
 * 
 * Returns Result<ReportConfiguration> because:
 * - Value object construction can fail
 * - Validation can fail
 */
@Service
@RequiredArgsConstructor
public class UpdateReportConfigurationHandler {
    
    private final ReportConfigurationRepository repository;
    
    @Value
    public static class UpdateCommand {
        String template;
        String language;
        String outputFormat;
        String frequency;
        int submissionDeadline;
        boolean autoGenerationEnabled;
        String scheduleDay;
        String scheduleTime;
        String primaryRecipient;
        String ccRecipient;
        boolean autoSendEnabled;
        boolean swiftAutoSubmitEnabled;
        String modifiedBy;
    }
    
    @Transactional
    public Result<ReportConfiguration> handle(UpdateCommand command) {
        // Create value objects using smart constructors
        // Compose Results for clean error handling
        
        var templateResult = ReportTemplate.of(command.template);
        if (templateResult.isFailure()) {
            return Result.failure(templateResult.getError());
        }
        
        var languageResult = ReportLanguage.of(command.language);
        if (languageResult.isFailure()) {
            return Result.failure(languageResult.getError());
        }
        
        var outputFormatResult = OutputFormat.of(command.outputFormat);
        if (outputFormatResult.isFailure()) {
            return Result.failure(outputFormatResult.getError());
        }
        
        var frequencyResult = ReportFrequency.of(command.frequency);
        if (frequencyResult.isFailure()) {
            return Result.failure(frequencyResult.getError());
        }
        
        var submissionDeadlineResult = SubmissionDeadline.of(command.submissionDeadline);
        if (submissionDeadlineResult.isFailure()) {
            return Result.failure(submissionDeadlineResult.getError());
        }
        
        var scheduleDayResult = ScheduleDay.of(command.scheduleDay);
        if (scheduleDayResult.isFailure()) {
            return Result.failure(scheduleDayResult.getError());
        }
        
        var scheduleTimeResult = ScheduleTime.of(command.scheduleTime);
        if (scheduleTimeResult.isFailure()) {
            return Result.failure(scheduleTimeResult.getError());
        }
        
        var primaryRecipientResult = EmailRecipient.of(command.primaryRecipient);
        if (primaryRecipientResult.isFailure()) {
            return Result.failure(primaryRecipientResult.getError());
        }
        
        // Optional CC recipient - use Maybe pattern
        var ccRecipient = EmailRecipient.ofOptional(command.ccRecipient);
        
        // Build domain model
        var configuration = ReportConfiguration.builder()
            .template(templateResult.getValue())
            .language(languageResult.getValue())
            .outputFormat(outputFormatResult.getValue())
            .frequency(frequencyResult.getValue())
            .submissionDeadline(submissionDeadlineResult.getValue())
            .autoGenerationEnabled(command.autoGenerationEnabled)
            .scheduleDay(scheduleDayResult.getValue())
            .scheduleTime(scheduleTimeResult.getValue())
            .primaryRecipient(primaryRecipientResult.getValue())
            .ccRecipient(ccRecipient)
            .autoSendEnabled(command.autoSendEnabled)
            .swiftAutoSubmitEnabled(command.swiftAutoSubmitEnabled)
            .lastModified(java.time.Instant.now())
            .lastModifiedBy(command.modifiedBy)
            .build();
        
        // Save via repository
        var saved = repository.save(configuration);
        
        return Result.success(saved);
    }
}
```

---

## ITERATION 4: Presentation Layer - Route Functions

**File: `presentation/src/main/java/com/bcbs239/regtech/reportgeneration/presentation/configuration/ReportConfigurationResponse.java`**

```java
package com.bcbs239.regtech.reportgeneration.presentation.configuration;

/**
 * Response DTO for report configuration
 */
public record ReportConfigurationResponse(
    String template,
    String language,
    String outputFormat,
    String frequency,
    int submissionDeadline,
    boolean autoGenerationEnabled,
    String scheduleDay,
    String scheduleTime,
    String primaryRecipient,
    String ccRecipient,
    boolean autoSendEnabled,
    boolean swiftAutoSubmitEnabled,
    String lastModified,
    String lastModifiedBy
) {
    /**
     * Map domain model → DTO
     */
    public static ReportConfigurationResponse from(
            com.bcbs239.regtech.reportgeneration.domain.configuration.ReportConfiguration config) {
        return new ReportConfigurationResponse(
            config.getTemplate().name(),
            config.getLanguage().name(),
            config.getOutputFormat().name(),
            config.getFrequency().name(),
            config.getSubmissionDeadline().getDays(),
            config.isAutoGenerationEnabled(),
            config.getScheduleDay().name(),
            config.getScheduleTime().toFormattedString(),
            config.getPrimaryRecipient().getEmail(),
            config.getCcRecipient().map(r -> r.getEmail()).orElse(null),
            config.isAutoSendEnabled(),
            config.isSwiftAutoSubmitEnabled(),
            config.getLastModified().toString(),
            config.getLastModifiedBy()
        );
    }
}
```

**File: `presentation/src/main/java/com/bcbs239/regtech/reportgeneration/presentation/configuration/ReportConfigurationRequest.java`**

```java
package com.bcbs239.regtech.reportgeneration.presentation.configuration;

import jakarta.validation.constraints.*;

/**
 * Request DTO for updating report configuration
 * Validation at the boundary
 */
public record ReportConfigurationRequest(
    
    @NotBlank(message = "Template is required")
    String template,
    
    @NotBlank(message = "Language is required")
    String language,
    
    @NotBlank(message = "Output format is required")
    String outputFormat,
    
    @NotBlank(message = "Frequency is required")
    String frequency,
    
    @Min(value = 1, message = "Submission deadline must be at least 1 day")
    @Max(value = 30, message = "Submission deadline cannot exceed 30 days")
    int submissionDeadline,
    
    boolean autoGenerationEnabled,
    
    @NotBlank(message = "Schedule day is required")
    String scheduleDay,
    
    @NotBlank(message = "Schedule time is required")
    @Pattern(regexp = "([01][0-9]|2[0-3]):[0-5][0-9]", message = "Invalid time format (expected HH:mm)")
    String scheduleTime,
    
    @NotBlank(message = "Primary recipient email is required")
    @Email(message = "Invalid primary recipient email format")
    String primaryRecipient,
    
    @Email(message = "Invalid CC recipient email format")
    String ccRecipient,
    
    boolean autoSendEnabled,
    
    boolean swiftAutoSubmitEnabled
) {
    /**
     * Map DTO → Command
     */
    public com.bcbs239.regtech.reportgeneration.application.configuration.UpdateReportConfigurationHandler.UpdateCommand 
            toCommand(String modifiedBy) {
        return new com.bcbs239.regtech.reportgeneration.application.configuration.UpdateReportConfigurationHandler.UpdateCommand(
            template,
            language,
            outputFormat,
            frequency,
            submissionDeadline,
            autoGenerationEnabled,
            scheduleDay,
            scheduleTime,
            primaryRecipient,
            ccRecipient,
            autoSendEnabled,
            swiftAutoSubmitEnabled,
            modifiedBy
        );
    }
}
```

**File: `presentation/src/main/java/com/bcbs239/regtech/reportgeneration/presentation/configuration/ReportConfigurationController.java`**

```java
package com.bcbs239.regtech.reportgeneration.presentation.configuration;

import com.bcbs239.regtech.reportgeneration.application.configuration.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RouterFunctions.route;
import static org.springframework.web.servlet.function.RequestPredicates.*;

/**
 * Report Configuration Routes (Route Functions)
 * 
 * Capability: Report Configuration
 */
@Configuration
@RequiredArgsConstructor
public class ReportConfigurationController {
    
    private final GetReportConfigurationHandler getReportConfigurationHandler;
    private final UpdateReportConfigurationHandler updateReportConfigurationHandler;
    
    @Bean
    public RouterFunction<ServerResponse> reportConfigurationRoutes() {
        return route()
            .GET("/api/v1/configuration/report", 
                 accept(MediaType.APPLICATION_JSON), 
                 this::getReportConfiguration)
            .PUT("/api/v1/configuration/report", 
                 accept(MediaType.APPLICATION_JSON), 
                 this::updateReportConfiguration)
            .build();
    }
    
    /**
     * GET /api/v1/configuration/report
     */
    private ServerResponse getReportConfiguration(
            org.springframework.web.servlet.function.ServerRequest request) {
        
        var configMaybe = getReportConfigurationHandler.handle();
        
        if (configMaybe.isEmpty()) {
            return ServerResponse.notFound().build();
        }
        
        var response = ReportConfigurationResponse.from(configMaybe.get());
        return ServerResponse.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(response);
    }
    
    /**
     * PUT /api/v1/configuration/report
     */
    private ServerResponse updateReportConfiguration(
            org.springframework.web.servlet.function.ServerRequest request) {
        
        try {
            // Parse and validate request
            var requestDto = request.body(ReportConfigurationRequest.class);
            
            // TODO: Get actual user from security context
            String currentUser = "Marco Rossi";
            
            // Convert to command and execute
            var command = requestDto.toCommand(currentUser);
            var result = updateReportConfigurationHandler.handle(command);
            
            // Handle Result
            if (result.isFailure()) {
                return ServerResponse.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ErrorResponse(result.getError()));
            }
            
            var response = ReportConfigurationResponse.from(result.getValue());
            return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
                
        } catch (Exception e) {
            return ServerResponse.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse("Invalid request: " + e.getMessage()));
        }
    }
    
    private record ErrorResponse(String error) {}
}
```

---

## ITERATION 5: Infrastructure - Persistence

**File: `infrastructure/src/main/java/com/bcbs239/regtech/reportgeneration/infrastructure/persistence/configuration/ReportConfigurationJpaEntity.java`**

```java
package com.bcbs239.regtech.reportgeneration.infrastructure.persistence.configuration;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalTime;

/**
 * JPA Entity for report_configuration table
 * Separate from domain model
 */
@Entity
@Table(name = "report_configuration")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportConfigurationJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "template", nullable = false)
    private String template;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false)
    private String language;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "output_format", nullable = false)
    private String outputFormat;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false)
    private String frequency;
    
    @Column(name = "submission_deadline", nullable = false)
    private Integer submissionDeadline;
    
    @Column(name = "auto_generation_enabled", nullable = false)
    private Boolean autoGenerationEnabled;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_day", nullable = false)
    private String scheduleDay;
    
    @Column(name = "schedule_time", nullable = false)
    private LocalTime scheduleTime;
    
    @Column(name = "primary_recipient", nullable = false)
    private String primaryRecipient;
    
    @Column(name = "cc_recipient")
    private String ccRecipient;
    
    @Column(name = "auto_send_enabled", nullable = false)
    private Boolean autoSendEnabled;
    
    @Column(name = "swift_auto_submit_enabled", nullable = false)
    private Boolean swiftAutoSubmitEnabled;
    
    @Column(name = "last_modified", nullable = false)
    private Instant lastModified;
    
    @Column(name = "last_modified_by", nullable = false, length = 100)
    private String lastModifiedBy;
}
```

**File: `infrastructure/src/main/java/com/bcbs239/regtech/reportgeneration/infrastructure/persistence/configuration/ReportConfigurationJpaRepository.java`**

```java
package com.bcbs239.regtech.reportgeneration.infrastructure.persistence.configuration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface ReportConfigurationJpaRepository 
        extends JpaRepository<ReportConfigurationJpaEntity, Long> {
    
    /**
     * Get current report configuration (singleton pattern)
     */
    @Query("SELECT r FROM ReportConfigurationJpaEntity r ORDER BY r.lastModified DESC LIMIT 1")
    Optional<ReportConfigurationJpaEntity> findCurrent();
}
```

**File: `infrastructure/src/main/java/com/bcbs239/regtech/reportgeneration/infrastructure/persistence/configuration/ReportConfigurationRepositoryAdapter.java`**

```java
package com.bcbs239.regtech.reportgeneration.infrastructure.persistence.configuration;

import com.bcbs239.regtech.reportgeneration.domain.configuration.*;
import com.bcbs239.regtech.reportgeneration.domain.configuration.valueobject.*;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Repository Adapter - maps JPA ↔ Domain
 */
@Repository
@RequiredArgsConstructor
public class ReportConfigurationRepositoryAdapter implements ReportConfigurationRepository {
    
    private final ReportConfigurationJpaRepository jpaRepository;
    
    @Override
    public Maybe<ReportConfiguration> findCurrent() {
        return jpaRepository.findCurrent()
            .map(this::toDomain)
            .map(Maybe::of)
            .orElse(Maybe.empty());
    }
    
    @Override
    public ReportConfiguration save(ReportConfiguration configuration) {
        var entity = toEntity(configuration);
        var saved = jpaRepository.save(entity);
        return toDomain(saved);
    }
    
    /**
     * JPA → Domain
     */
    private ReportConfiguration toDomain(ReportConfigurationJpaEntity entity) {
        return ReportConfiguration.builder()
            .template(ReportTemplate.of(entity.getTemplate()).getValue())
            .language(ReportLanguage.of(entity.getLanguage()).getValue())
            .outputFormat(OutputFormat.of(entity.getOutputFormat()).getValue())
            .frequency(ReportFrequency.of(entity.getFrequency()).getValue())
            .submissionDeadline(SubmissionDeadline.of(entity.getSubmissionDeadline()).getValue())
            .autoGenerationEnabled(entity.getAutoGenerationEnabled())
            .scheduleDay(ScheduleDay.of(entity.getScheduleDay()).getValue())
            .scheduleTime(ScheduleTime.of(entity.getScheduleTime().toString()).getValue())
            .primaryRecipient(EmailRecipient.of(entity.getPrimaryRecipient()).getValue())
            .ccRecipient(EmailRecipient.ofOptional(entity.getCcRecipient()))
            .autoSendEnabled(entity.getAutoSendEnabled())
            .swiftAutoSubmitEnabled(entity.getSwiftAutoSubmitEnabled())
            .lastModified(entity.getLastModified())
            .lastModifiedBy(entity.getLastModifiedBy())
            .build();
    }
    
    /**
     * Domain → JPA
     */
    private ReportConfigurationJpaEntity toEntity(ReportConfiguration config) {
        return ReportConfigurationJpaEntity.builder()
            .template(config.getTemplate().name())
            .language(config.getLanguage().name())
            .outputFormat(config.getOutputFormat().name())
            .frequency(config.getFrequency().name())
            .submissionDeadline(config.getSubmissionDeadline().getDays())
            .autoGenerationEnabled(config.isAutoGenerationEnabled())
            .scheduleDay(config.getScheduleDay().name())
            .scheduleTime(config.getScheduleTime().getTime())
            .primaryRecipient(config.getPrimaryRecipient().getEmail())
            .ccRecipient(config.getCcRecipient().map(EmailRecipient::getEmail).orElse(null))
            .autoSendEnabled(config.isAutoSendEnabled())
            .swiftAutoSubmitEnabled(config.isSwiftAutoSubmitEnabled())
            .lastModified(config.getLastModified())
            .lastModifiedBy(config.getLastModifiedBy())
            .build();
    }
}
```

---

## ITERATION 6: Database Migration

**File: `regtech-app/src/main/resources/db/migration/reportgeneration/V002__add_report_configuration.sql`**

```sql
-- Add report configuration table to report generation schema

CREATE TABLE IF NOT EXISTS report_configuration (
    id BIGSERIAL PRIMARY KEY,
    
    -- Template & Format
    template VARCHAR(50) NOT NULL,
    language VARCHAR(20) NOT NULL,
    output_format VARCHAR(20) NOT NULL,
    
    -- Scheduling
    frequency VARCHAR(20) NOT NULL,
    submission_deadline INTEGER NOT NULL,
    auto_generation_enabled BOOLEAN NOT NULL DEFAULT false,
    schedule_day VARCHAR(20) NOT NULL,
    schedule_time TIME NOT NULL,
    
    -- Distribution
    primary_recipient VARCHAR(255) NOT NULL,
    cc_recipient VARCHAR(255),
    auto_send_enabled BOOLEAN NOT NULL DEFAULT false,
    swift_auto_submit_enabled BOOLEAN NOT NULL DEFAULT false,
    
    -- Metadata
    last_modified TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_by VARCHAR(100) NOT NULL,
    
    -- Constraints
    CONSTRAINT chk_template CHECK (template IN ('BANCA_ITALIA_STANDARD', 'ECB_EUROPEAN_STANDARD', 'CUSTOM')),
    CONSTRAINT chk_language CHECK (language IN ('ITALIAN', 'ENGLISH', 'BILINGUAL')),
    CONSTRAINT chk_output_format CHECK (output_format IN ('PDF', 'EXCEL', 'BOTH')),
    CONSTRAINT chk_frequency CHECK (frequency IN ('MONTHLY', 'QUARTERLY', 'SEMI_ANNUAL', 'ANNUAL')),
    CONSTRAINT chk_submission_deadline CHECK (submission_deadline BETWEEN 1 AND 30),
    CONSTRAINT chk_schedule_day CHECK (schedule_day IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY')),
    CONSTRAINT chk_primary_recipient_email CHECK (primary_recipient ~* '^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT chk_cc_recipient_email CHECK (cc_recipient IS NULL OR cc_recipient ~* '^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

-- Index for fast retrieval
CREATE INDEX idx_report_configuration_last_modified ON report_configuration(last_modified DESC);

-- Insert initial configuration data
INSERT INTO report_configuration (
    template,
    language,
    output_format,
    frequency,
    submission_deadline,
    auto_generation_enabled,
    schedule_day,
    schedule_time,
    primary_recipient,
    cc_recipient,
    auto_send_enabled,
    swift_auto_submit_enabled,
    last_modified,
    last_modified_by
) VALUES (
    'BANCA_ITALIA_STANDARD',
    'ITALIAN',
    'PDF',
    'MONTHLY',
    20,
    true,
    'MONDAY',
    '09:00:00',
    'compliance@bancaitaliana.it',
    'direzione.rischi@bancaitaliana.it',
    true,
    false,  -- SWIFT auto-submit in development
    NOW(),
    'System'
);

-- Add comments
COMMENT ON TABLE report_configuration IS 'Report generation and distribution configuration for BCBS 239 - Singleton table (one row only)';
COMMENT ON COLUMN report_configuration.swift_auto_submit_enabled IS 'Auto-submission to Banca d''Italia via SWIFT (currently in development)';
```

---

## Summary: Implementation Checklist

### Domain Layer (regtech-report-generation/domain)
- [ ] Create enums with smart constructors returning Result:
  - [ ] ReportTemplate, ReportLanguage, OutputFormat
  - [ ] ReportFrequency, ScheduleDay
- [ ] Create value objects with smart constructors:
  - [ ] EmailRecipient (Result for required, Maybe for optional)
  - [ ] ScheduleTime (Result with working hours validation)
  - [ ] SubmissionDeadline (Result with range validation)
- [ ] Create ReportConfiguration aggregate root with Lombok @Value @Builder
- [ ] Create ReportConfigurationRepository interface

### Application Layer (regtech-report-generation/application)
- [ ] Create GetReportConfigurationHandler (query)
- [ ] Create UpdateReportConfigurationHandler (command) returning Result

### Presentation Layer (regtech-report-generation/presentation)
- [ ] Create ReportConfigurationResponse record with static from() mapper
- [ ] Create ReportConfigurationRequest record with Jakarta validation
- [ ] Create ReportConfigurationController with route functions (@Configuration + @Bean)

### Infrastructure Layer (regtech-report-generation/infrastructure)
- [ ] Create ReportConfigurationJpaEntity with Lombok
- [ ] Create ReportConfigurationJpaRepository (Spring Data)
- [ ] Create ReportConfigurationRepositoryAdapter implementing domain repository

### Database
- [ ] Add Flyway migration V002__add_report_configuration.sql to regtech-app

---

## Key Features

### 1. **Smart Constructors with Business Rules**
```java
// Time validation with working hours rule
ScheduleTime.of("09:00") // ✅ Success
ScheduleTime.of("19:00") // ❌ Failure: "Must be within working hours (08:00-18:00)"

// Deadline validation with range rule
SubmissionDeadline.of(20) // ✅ Success
SubmissionDeadline.of(35) // ❌ Failure: "Must be between 1 and 30 days"
```

### 2. **Domain Behaviors**
```java
config.isAutomationEnabled()    // Auto-generation AND auto-send both enabled
config.shouldGeneratePdf()      // Based on output format
config.hasTightDeadline()       // <= 10 days
config.isHighFrequency()        // Monthly reports
```

### 3. **Maybe Pattern for Optional Fields**
```java
// CC recipient is optional
Maybe<EmailRecipient> cc = EmailRecipient.ofOptional("cc@bank.it");
String ccEmail = cc.map(r -> r.getEmail()).orElse(null);
```

### 4. **Route Functions**
```java
@Bean
public RouterFunction<ServerResponse> reportConfigurationRoutes() {
    return route()
        .GET("/api/v1/configuration/report", this::getReportConfiguration)
        .PUT("/api/v1/configuration/report", this::updateReportConfiguration)
        .build();
}
```

Start implementing from domain layer (value objects and enums) and work your way out! 🚀