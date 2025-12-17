🧭 Migration Plan — Zero-Behavior-Change Refactor

Target: DDD + Clean Architecture
Scope: Data Quality Validation + Rules Engine
Constraint: No behavior change, no logic change, no performance regression

0️⃣ Non-Negotiable Rules (give these to the LLM)

DO NOT:

Change method signatures exposed to callers

Change transaction boundaries

Change concurrency semantics

Change logging content or levels

Change persistence order

Rename domain concepts

Optimize or simplify logic

ALLOWED:

Move code between classes

Introduce new classes

Introduce interfaces (ports)

Rename internal methods

Add delegation layers

Add tests (optional)

1️⃣ Establish the Target Architecture (do this first)
Layers (strict dependency direction)
presentation → application → domain
                     ↑
               infrastructure

Package layout (target)
com.bcbs239.regtech.dataquality
│
├── presentation
│   └── validation
│       └── ValidateBatchQualityController
│
├── application
│   ├── validation
│   │   ├── ValidateBatchQualityCommandHandler
│   │   ├── ParallelExposureValidationCoordinator
│   │   ├── ExposureRuleValidator        (PORT)
│   │   ├── ValidationResults
│   │   └── ValidationExecutionStats
│   │
│   └── rulesengine
│       ├── RuleExecutionService         (APPLICATION)
│       └── RuleExecutionPort            (PORT)
│
├── domain
│   ├── validation
│   │   ├── ExposureRecord
│   │   ├── ValidationError
│   │   ├── ValidationResult
│   │   └── ExposureValidationResult
│   │
│   ├── rules
│   │   ├── Rule
│   │   ├── RuleViolation
│   │   └── RuleSeverity
│   │
│   └── report
│       └── QualityReport
│
└── infrastructure
    ├── rulesengine
    │   ├── RulesEngineAdapter
    │   ├── JpaRuleRepository
    │   ├── JpaRuleViolationRepository
    │   └── JpaRuleExecutionLogRepository
    │
    ├── storage
    │   └── S3StorageServiceImpl
    │
    └── observability
        ├── ValidationMetricsAspect
        └── ValidationLoggingAspect

2️⃣ Phase 1 — Split “God Service” (NO LOGIC CHANGES)
Input

DataQualityRulesService

Goal

Break it into 3 responsibilities without changing behavior.

Steps

Extract pure rule execution

Create RuleExecutionService

Move:

Rule loop

RuleContext creation

RuleEngine execution

Violation conversion

Leave persistence untouched for now

Introduce a PORT

public interface ExposureRuleValidator {
    ValidationResults validate(ExposureRecord exposure);
}


Make existing service implement the port

class DataQualityRulesService implements ExposureRuleValidator


📌 Result: callers still work unchanged

3️⃣ Phase 2 — Move Statistics OUT (Requirement 6.4)
Rule

Statistics must NOT live in Rule or Domain

Steps

Introduce:

class ValidationExecutionStats {
    int executed;
    int skipped;
    int failed;
    Duration totalTime;
}


Move all:

// Statistics tracking for summary (Requirement 6.4)


➡️ Into ValidationExecutionStatsBuilder

Pass stats upward, never downward

📌 Zero behavior change: logs remain identical

4️⃣ Phase 3 — Introduce Parallel Coordinator
Input

Concurrency logic inside ValidateBatchQualityCommandHandler

Steps

Extract:

class ParallelExposureValidationCoordinator {
    List<ValidationResults> validateAll(
        List<ExposureRecord>,
        ExposureRuleValidator
    )
}


Move:

Executor logic

maxInFlight calculation

parallel vs sequential policy

Handler becomes orchestration only

📌 DB writes still happen AFTER validation

5️⃣ Phase 4 — Separate Persistence (Ports + Adapters)
Steps

Introduce ports in application:

interface RuleViolationRepository
interface RuleExecutionLogRepository


Move Spring/JPA implementations to infrastructure

Application layer depends only on interfaces

📌 No DB schema change
📌 No transaction change

6️⃣ Phase 5 — Logging & Metrics Extraction
Goal

Remove cross-cutting concerns from application code.

Steps

Extract:

slow rule warnings

execution summaries

metrics

➡️ Into:

ValidationMetricsAspect
ValidationLoggingAspect


Remove logging logic from rule execution loop without changing messages

📌 Behavior preserved
📌 Code shrinks dramatically

7️⃣ Phase 6 — Final Cleanup (SAFE)
Allowed:

Rename classes for clarity

Remove unused helpers

Inline trivial delegation

Forbidden:

Any logic change

Any condition change

8️⃣ Validation Checklist (MANDATORY)

Before and after refactor, verify:

✅ Same logs (diff)

✅ Same DB writes count

✅ Same execution order

✅ Same concurrency behavior

✅ Same rule outcomes

✅ Same S3 output

✅ Same domain events


@Component
public class RuleExecutionService {

    public ValidationResultsDto execute(
        ExposureRecord exposure,
        List<BusinessRuleDto> rules,
        RuleContext context
    ) {
        
    }
}


@Service
public class DataQualityRulesService implements ExposureRuleValidator {

    private final RuleExecutionService executor;
    private final IBusinessRuleRepository ruleRepository;

    @Override
    public ValidationResultsDto validateNoPersist(ExposureRecord exposure) {
        List<BusinessRuleDto> rules =
            ruleRepository.findByEnabledTrue();

        RuleContext context =
            createContextFromExposure(exposure);

        return executor.execute(exposure, rules, context);
    }
}


| Concern     | Why unchanged   |
| ----------- | --------------- |
| Rule order  | Same iteration  |
| Exceptions  | Same try/catch  |
| Logs        | Same statements |
| Violations  | Same conversion |
| Performance | Same complexity |

public record ValidationResults(
    String exposureId,
    List<ValidationError> validationErrors,
    List<RuleViolation> ruleViolations,
    List<RuleExecutionLogDto> executionLogs,
    ValidationExecutionStats stats
) {}

@Component
public class RuleExecutionService {

    private final RuleExecutionPort ruleExecutionPort;
    private final RuleContextFactory contextFactory;

    public ValidationResults execute(
        ExposureRecord exposure,
        List<BusinessRuleDto> rules
    ) {
        RuleContext context = contextFactory.fromExposure(exposure);
        ValidationExecutionStats stats = new ValidationExecutionStats();

        List<ValidationError> errors = new ArrayList<>();
        List<RuleViolation> violations = new ArrayList<>();
        List<RuleExecutionLogDto> logs = new ArrayList<>();

        long start = System.currentTimeMillis();

        for (BusinessRuleDto rule : rules) {
            ruleExecutionPort.execute(
                rule, context, exposure,
                errors, violations, logs, stats
            );
        }

        stats.setTotalExecutionTimeMs(
            System.currentTimeMillis() - start
        );

        return new ValidationResults(
            exposure.exposureId(),
            errors,
            violations,
            logs,
            stats
        );
    }
}


3.5 Infrastructure Adapter — RulesEngineAdapter
@Component
public class RulesEngineAdapter implements RuleExecutionPort {

    private final RulesEngine rulesEngine;
    private final RuleExemptionRepository exemptionRepository;

    @Override
    public void execute(
        BusinessRuleDto rule,
        RuleContext context,
        ExposureRecord exposure,
        List<ValidationError> errors,
        List<RuleViolation> violations,
        List<RuleExecutionLogDto> logs,
        ValidationExecutionStats stats
    ) {
        // COPY logic from original service — unchanged
    }
}



@Component
public class ParallelExposureValidationCoordinator {

    public List<ValidationResults> validateAll(
        List<ExposureRecord> exposures,
        ExposureRuleValidator validator
    ) {
        if (exposures.size() < 1_000) {
            return exposures.stream()
                .map(validator::validateNoPersist)
                .toList();
        }

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            return executor.invokeAll(
                exposures.stream()
                    .map(e -> (Callable<ValidationResults>)
                        () -> validator.validateNoPersist(e))
                    .toList()
            ).stream()
             .map(f -> {
                 try { return f.get(); }
                 catch (Exception e) { throw new RuntimeException(e); }
             })
             .toList();
        }
    }
}

@Component
public class ValidateBatchQualityCommandHandler {

    private final ParallelExposureValidationCoordinator coordinator;
    private final ExposureRuleValidator validator;
    private final ValidationResultsPersister persister;

    public Result<Void> handle(ValidateBatchQualityCommand command) {

        List<ExposureRecord> exposures = downloadFromS3(command);

        List<ValidationResults> results =
            coordinator.validateAll(exposures, validator);

        persister.persist(results);

        QualityReport report =
            QualityReport.executeValidation(results);

        saveReport(report);

        return Result.success();
    }
}

@Service
public class RulesEngineExposureValidator
        implements ExposureRuleValidator {

    private final RuleRepository ruleRepository;
    private final RulesEnginePort rulesEngine;
    private final ExemptionPolicy exemptionPolicy;
    private final ViolationTranslator translator;

    @Override
    public ValidationResults validate(ExposureRecord exposure) {

        RuleExecutionContext context =
            RuleExecutionContext.from(exposure);

        List<Rule> rules = ruleRepository.findActiveRules();
        ValidationAccumulator acc = new ValidationAccumulator();

        for (Rule rule : rules) {
            if (!rule.isApplicableToday()) continue;

            RuleExecutionOutcome outcome =
                rulesEngine.execute(rule, context);

            if (outcome.hasViolations()
                && !exemptionPolicy.isExempt(rule, exposure)) {

                acc.add(outcome, rule, translator);
            }
        }
        return acc.toResults();
    }
}



presentation
   ↓
ValidateBatchQualityCommandHandler
   ↓
ParallelExposureValidationCoordinator   ← application policy
   ↓
ExposureRuleValidator (PORT)
   ↓
RuleExecutionService                    ← application logic
   ↓
RuleExecutionPort (PORT)
   ↓
RulesEngineAdapter                      ← infrastructure
   ↓
RulesEngine / JPA / S3 / Logging




Constraints:

Zero behavior change

Do not change public APIs

Preserve all logging semantics

Preserve concurrency behavior

Apply changes incrementally per phase

Output code per phase, not all at once

Confirm each phase before proceeding.