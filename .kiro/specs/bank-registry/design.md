# Bank Registry Context - Design Document

## Overview

The Bank Registry context serves as the authoritative source for comprehensive Italian bank information, BCBS 239 parameters, and report configuration. It implements a complete bank onboarding and configuration system that captures all regulatory requirements for Italian banks while maintaining functional programming patterns and Service Composer integration.

The design emphasizes value objects for Italian banking concepts (ABI codes, LEI codes), pure business functions for bank registration and configuration, and closure-based external validation against Italian and European banking registries.

## Architecture

### Core Architecture Principles

1. **Foundational Upstream Context**: Provides authoritative bank identity and configuration data to all downstream contexts
2. **Autonomous Data Ownership**: Owns bank profiles, BCBS parameters, and report configurations in dedicated schema
3. **Thin Event Publishing**: Publishes minimal events with IDs, downstream contexts query for additional data when needed
4. **Italian Banking Compliance**: Full support for Banca d'Italia requirements and BCBS 239 parameters
5. **External Registry Integration**: Real-time validation against GLEIF, Banca d'Italia, and Italian Banking Registry

### Context Dependencies

#### ⬆️ Upstream Dependencies
- **External APIs Only**: GLEIF Registry (LEI validation), Italian Banking Registry (ABI validation), Stripe API (payment processing)
- **No Internal Dependencies**: Foundational service with no upstream bounded contexts

#### ⬇️ Downstream Dependencies (Consumers of Bank Registry Events)
- **Exposure Ingestion**: Bank validation before processing exposures
- **Risk Calculation**: Capital data requirements for percentage calculations
- **Data Quality**: Bank context for compliance scoring and BCBS 239 validation
- **Report Generation**: Bank metadata and configuration for report templates
- **Billing**: Subscription limits and pricing enforcement

### Autonomous Context Architecture Diagram

```mermaid
graph TB
    subgraph "Bank Registry Context (Upstream Foundation)"
        subgraph "Domain Aggregates"
            BA[Bank Aggregate]
            BPA[BankParameters Aggregate]
            RCA[ReportConfig Aggregate]
        end
        
        subgraph "Event Publishing"
            EP[Event Publisher]
            BRE[BankRegisteredEvent]
            BPE[BankParametersConfiguredEvent]
            RCE[ReportConfigUpdatedEvent]
        end
        
        subgraph "API Query Layer"
            BQL[Bank Query API]
            BPQL[Parameters Query API]
            RCQL[Config Query API]
        end
    end
    
    subgraph "External Dependencies (Upstream)"
        GLEIF[GLEIF Registry]
        BDI[Banca d'Italia Registry]
        STRIPE[Stripe API]
    end
    
    subgraph "Downstream Contexts (Event Consumers)"
        EI[Exposure Ingestion]
        RC[Risk Calculation]
        DQ[Data Quality]
        RG[Report Generation]
        BI[Billing]
    end
    
    subgraph "Event Flow Pattern"
        TE[Thin Events: {bank_id, event_type, timestamp}]
        QWN[Query When Needed: API calls for full data]
    end
    
    %% External Dependencies
    GLEIF -.-> BA
    BDI -.-> BA
    STRIPE -.-> BA
    
    %% Event Publishing (Thin Events)
    BA --> EP
    BPA --> EP
    RCA --> EP
    
    EP --> BRE
    EP --> BPE
    EP --> RCE
    
    %% Thin Event Distribution
    BRE -.-> EI
    BRE -.-> RC
    BRE -.-> DQ
    BRE -.-> RG
    BRE -.-> BI
    
    BPE -.-> RC
    BPE -.-> DQ
    
    RCE -.-> RG
    
    %% Query When Needed Pattern
    EI -.-> BQL
    RC -.-> BQL
    RC -.-> BPQL
    DQ -.-> BQL
    DQ -.-> BPQL
    RG -.-> BQL
    RG -.-> RCQL
    BI -.-> BQL
    
    %% Data Ownership
    BA --> BQL
    BPA --> BPQL
    RCA --> RCQL
```

### Event-Driven Architecture Patterns

#### Template-Driven Configuration Events
Bank Registry publishes template configuration events that inform downstream contexts about their data ownership:

```java
public record MultiTemplateConfiguredEvent(
    BankId bankId,
    BankTemplateConfigId configId,
    Set<TemplateId> configuredTemplates,
    Map<String, Set<String>> contextSections,
    Instant configuredAt,
    String eventType
) {
    public static MultiTemplateConfiguredEvent from(
        BankId bankId, 
        BankTemplateConfiguration templateConfig
    ) {
        return new MultiTemplateConfiguredEvent(
            bankId,
            templateConfig.id(),
            templateConfig.getConfiguredTemplates(),
            getContextSectionMapping(templateConfig.getConfiguredTemplates()),
            Instant.now(),
            "MULTI_TEMPLATE_CONFIGURED"
        );
    }
    
    private static Map<String, Set<String>> getContextSectionMapping(Set<TemplateId> templateIds) {
        Map<String, Set<String>> sections = new HashMap<>();
        
        // Base sections that all templates share
        sections.put("bank_registry", new HashSet<>(Set.of("bank_information", "bcbs_239_compliance.governance", "outputs")));
        sections.put("exposure_ingestion", new HashSet<>(Set.of("exposures", "credit_risk_mitigation")));
        sections.put("risk_calculation", new HashSet<>(Set.of("capital_data", "business_rules", "calculation_rules", "exemptions")));
        sections.put("data_quality", new HashSet<>(Set.of("bcbs_239_compliance.data_aggregation", "compliance_tracking")));
        sections.put("report_generation", new HashSet<>(Set.of("outputs.excel", "outputs.xml", "outputs.pdf")));
        
        // Add template-specific sections
        for (TemplateId templateId : templateIds) {
            addTemplateSpecificSections(sections, templateId);
        }
        
        return sections;
    }
    
    private static void addTemplateSpecificSections(Map<String, Set<String>> sections, TemplateId templateId) {
        switch (templateId) {
            case IT_LARGE_EXPOSURES_CIRCULARE_285 -> {
                sections.get("bank_registry").addAll(Set.of("italian_banking_info", "circolare_285_config"));
                sections.get("risk_calculation").addAll(Set.of("italian_large_exposure_rules", "banca_italia_calculations"));
                sections.get("report_generation").addAll(Set.of("banca_italia_xml_format", "circolare_285_pdf"));
            }
            case EU_LARGE_EXPOSURES_EBA_ITS -> {
                sections.get("bank_registry").addAll(Set.of("eba_institution_info", "eba_its_config"));
                sections.get("risk_calculation").addAll(Set.of("eba_large_exposure_rules", "crr_calculations"));
                sections.get("report_generation").addAll(Set.of("eba_xml_format", "eba_excel_format"));
            }
            case IT_RISK_CONCENTRATION_SUPERVISORY -> {
                sections.get("bank_registry").addAll(Set.of("supervisory_info", "concentration_config"));
                sections.get("risk_calculation").addAll(Set.of("concentration_rules", "supervisory_calculations"));
                sections.get("report_generation").addAll(Set.of("supervisory_pdf_format", "concentration_analysis"));
            }
        }
    }
    
    public Set<String> getSectionsForContext(String contextName) {
        return contextSections.getOrDefault(contextName, Set.of());
    }
    
    public boolean hasTemplate(TemplateId templateId) {
        return configuredTemplates.contains(templateId);
    }
}
```

#### Thin Event Publishing
Bank Registry publishes minimal events containing only essential identifiers and event metadata:

```java
public record BankRegisteredEvent(
    BankId bankId,
    AbiCode abiCode,
    LeiCode leiCode,
    Instant registeredAt,
    String eventType
) {
    public static BankRegisteredEvent from(Bank bank) {
        return new BankRegisteredEvent(
            bank.id(),
            bank.abiCode(),
            bank.leiCode(),
            bank.createdAt(),
            "BANK_REGISTERED"
        );
    }
}

public record BankParametersConfiguredEvent(
    BankId bankId,
    BankParametersId parametersId,
    BigDecimal capitaleAmmissibile,
    Instant configuredAt,
    String eventType
) {
    public static BankParametersConfiguredEvent from(BankParameters parameters) {
        return new BankParametersConfiguredEvent(
            parameters.bankId(),
            parameters.id(),
            parameters.capitaleAmmissibile().amount(),
            parameters.configuredAt(),
            "BANK_PARAMETERS_CONFIGURED"
        );
    }
}
```

#### Query When Needed Pattern
Downstream contexts receive thin events and query Bank Registry APIs for additional data:

```java
// Exposure Ingestion Context receives BankRegisteredEvent
// Then queries for full bank details when needed
public class ExposureValidationReactor {
    
    private final Function<BankId, Result<BankSummary, ErrorDetail>> bankQuery;
    
    public void handleBankRegisteredEvent(BankRegisteredEvent event) {
        // Store minimal event data locally
        storeBankReference(event.bankId(), event.abiCode());
        
        // Query for full details only when processing exposures
        // This happens later in the exposure validation flow
    }
    
    public Result<Void, ErrorDetail> validateExposureForBank(ExposureId exposureId, BankId bankId) {
        return bankQuery.apply(bankId)  // Query when needed
            .map(bankSummary -> {
                // Use full bank data for validation
                validateExposureAgainstBankLimits(exposureId, bankSummary);
                return null;
            });
    }
}
```

#### Template-Driven Data Ownership
Each bounded context owns specific sections of the regulatory template:

```yaml
# From IT_LARGE_EXPOSURES_CIRCULARE_285 template
# Data ownership mapping:

bank_registry_owned_sections:
  - bank_information:
      abi_code: "^[0-9]{5}$"
      lei_code: "^[A-Z0-9]{20}$"
      bank_name: "string"
      reporting_date: "date"
  - bcbs_239_compliance.governance:
      data_owner: "CRO Office"
      data_steward: "Head of Regulatory Reporting"
      lineage_required: true
  - outputs:
      excel: {enabled: true, filename_pattern: "..."}
      xml: {enabled: true, namespace: "..."}
      pdf: {enabled: true, language: "italian"}

exposure_ingestion_owned_sections:
  - exposures: [array of exposure records]
  - credit_risk_mitigation: [array of CRM records]

risk_calculation_owned_sections:
  - capital_data:
      total_capital: "decimal"
      eligible_capital_large_exposures: "decimal"
  - business_rules:
      large_exposure_threshold: 10.0
      legal_limit_general: 25.0
  - calculation_rules:
      exposure_percentage: "(net_exposure_amount / eligible_capital_large_exposures) * 100"

data_quality_owned_sections:
  - bcbs_239_compliance.data_aggregation:
      completeness_check: "ALL_MATERIAL_RISKS"
      groupings: ["business_line", "legal_entity", "sector"]
  - compliance_tracking:
      validation_status: "COMPLIANT"
      remediation_actions: []

report_generation_owned_sections:
  - outputs.excel.sheets: [sheet configurations]
  - outputs.xml.namespace: "urn:bancaditalia:xsd:LE:1.0"
  - outputs.pdf.sections: ["cover", "executive_summary"]
```

#### Data Ownership and Schema Isolation

```sql
-- Bank Registry owns its data in dedicated schema
CREATE SCHEMA bank_registry;

-- Bank Registry tables
CREATE TABLE bank_registry.banks (
    bank_id UUID PRIMARY KEY,
    denominazione_sociale VARCHAR(200) NOT NULL,
    abi_code VARCHAR(5) NOT NULL,
    lei_code VARCHAR(20) NOT NULL,
    sede_legale JSONB NOT NULL,
    gruppo_bancario VARCHAR(100),
    tipologia_banca VARCHAR(50),
    categoria_vigilanza VARCHAR(50),
    status VARCHAR(20) DEFAULT 'PENDING_VALIDATION',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    validated_at TIMESTAMP NULL
);

CREATE TABLE bank_registry.bank_parameters (
    parameters_id UUID PRIMARY KEY,
    bank_id UUID NOT NULL,
    limite_grande_esposizione DECIMAL(5,2) NOT NULL,
    soglia_classificazione DECIMAL(5,2) NOT NULL,
    capitale_ammissibile DECIMAL(15,2) NOT NULL,
    metodo_calcolo VARCHAR(50) NOT NULL,
    soglia_qualita_minima DECIMAL(5,2) NOT NULL,
    configured_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- No foreign keys to other schemas - enforced at application level
-- Other contexts store only bank_id references and query via API
```

## Components and Interfaces

### 1. Domain Models (Value Objects for Italian Banking)

#### Bank Aggregate
The central bank entity with comprehensive Italian banking information.

```java
public record Bank(
    BankId id,
    DenominazioneSociale denominazioneSociale,
    AbiCode abiCode,
    LeiCode leiCode,
    SedeLegale sedeLegale,
    GruppoBancario gruppoBancario,
    TipologiaBanca tipologiaBanca,
    CategoriaVigilanza categoriaVigilanza,
    BankStatus status,
    Instant createdAt,
    Maybe<Instant> validatedAt
) {
    public static Result<Bank, ErrorDetail> create(
        DenominazioneSociale denominazioneSociale,
        AbiCode abiCode,
        LeiCode leiCode,
        SedeLegale sedeLegale,
        GruppoBancario gruppoBancario,
        TipologiaBanca tipologiaBanca,
        CategoriaVigilanza categoriaVigilanza
    ) {
        return Result.success(new Bank(
            BankId.generate(),
            denominazioneSociale,
            abiCode,
            leiCode,
            sedeLegale,
            gruppoBancario,
            tipologiaBanca,
            categoriaVigilanza,
            BankStatus.PENDING_VALIDATION,
            Instant.now(),
            Maybe.none()
        ));
    }
    
    public Bank markAsValidated() {
        return new Bank(
            id, denominazioneSociale, abiCode, leiCode, sedeLegale,
            gruppoBancario, tipologiaBanca, categoriaVigilanza,
            BankStatus.ACTIVE, createdAt, Maybe.some(Instant.now())
        );
    }
    
    public boolean isConfigurationComplete() {
        return status == BankStatus.ACTIVE && validatedAt.isPresent();
    }
}
```

#### Italian Banking Value Objects

```java
public record AbiCode(String value) {
    private static final Pattern ABI_PATTERN = Pattern.compile("^[0-9]{5}$");
    
    public static Result<AbiCode, ErrorDetail> create(String value) {
        if (value == null || !ABI_PATTERN.matcher(value).matches()) {
            return Result.failure(ErrorDetail.validation("ABI code must be exactly 5 digits"));
        }
        return Result.success(new AbiCode(value));
    }
}

public record LeiCode(String value) {
    private static final Pattern LEI_PATTERN = Pattern.compile("^[A-Z0-9]{20}$");
    
    public static Result<LeiCode, ErrorDetail> create(String value) {
        if (value == null || !LEI_PATTERN.matcher(value).matches()) {
            return Result.failure(ErrorDetail.validation("LEI code must be exactly 20 alphanumeric characters"));
        }
        return Result.success(new LeiCode(value.toUpperCase()));
    }
}

public record DenominazioneSociale(String value) {
    public static Result<DenominazioneSociale, ErrorDetail> create(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Result.failure(ErrorDetail.validation("Denominazione sociale is required"));
        }
        if (value.length() > 200) {
            return Result.failure(ErrorDetail.validation("Denominazione sociale cannot exceed 200 characters"));
        }
        return Result.success(new DenominazioneSociale(value.trim()));
    }
}

public record SedeLegale(
    String via,
    String cap,
    String citta,
    String provincia,
    String paese
) {
    public static Result<SedeLegale, ErrorDetail> create(
        String via, String cap, String citta, String provincia, String paese
    ) {
        List<String> errors = new ArrayList<>();
        
        if (via == null || via.trim().isEmpty()) {
            errors.add("Via is required");
        }
        if (cap == null || !cap.matches("^[0-9]{5}$")) {
            errors.add("CAP must be 5 digits");
        }
        if (citta == null || citta.trim().isEmpty()) {
            errors.add("Città is required");
        }
        if (provincia == null || provincia.length() != 2) {
            errors.add("Provincia must be 2 characters");
        }
        if (paese == null || paese.trim().isEmpty()) {
            errors.add("Paese is required");
        }
        
        if (!errors.isEmpty()) {
            return Result.failure(ErrorDetail.validation("Sede legale validation failed", 
                Map.of("errors", errors)));
        }
        
        return Result.success(new SedeLegale(
            via.trim(), cap, citta.trim(), provincia.toUpperCase(), paese.trim()
        ));
    }
    
    public String formatAddress() {
        return String.format("%s, %s %s %s, %s", via, cap, citta, provincia, paese);
    }
}
```

#### BCBS 239 Parameters Aggregate

```java
public record BankParameters(
    BankParametersId id,
    BankId bankId,
    LimiteGrandeEsposizione limiteGrandeEsposizione,
    SogliaClassificazione sogliaClassificazione,
    CapitaleAmmissibile capitaleAmmissibile,
    MetodoCalcolo metodoCalcolo,
    SogliaQualitaMinima sogliaQualitaMinima,
    ValidazioneFile validazioneFile,
    FrequenzaStandard frequenzaStandard,
    GiorniSubmission giorniSubmission,
    Instant configuredAt
) {
    public static Result<BankParameters, ErrorDetail> create(
        BankId bankId,
        BigDecimal limitePercentuale,
        BigDecimal sogliaPercentuale,
        BigDecimal capitaleAmount,
        MetodoCalcolo metodo,
        BigDecimal qualitaMinima,
        ValidazioneFile validazione,
        FrequenzaStandard frequenza,
        int giorniSubmission
    ) {
        return LimiteGrandeEsposizione.create(limitePercentuale)
            .flatMap(limite -> SogliaClassificazione.create(sogliaPercentuale)
                .flatMap(soglia -> CapitaleAmmissibile.create(capitaleAmount)
                    .flatMap(capitale -> SogliaQualitaMinima.create(qualitaMinima)
                        .map(qualita -> new BankParameters(
                            BankParametersId.generate(),
                            bankId,
                            limite,
                            soglia,
                            capitale,
                            metodo,
                            qualita,
                            validazione,
                            frequenza,
                            giorniSubmission,
                            Instant.now()
                        ))
                    )
                )
            );
    }
    
    public boolean isCompliant() {
        return limiteGrandeEsposizione.value().compareTo(BigDecimal.valueOf(25.0)) <= 0 &&
               sogliaClassificazione.value().compareTo(BigDecimal.valueOf(10.0)) >= 0 &&
               sogliaQualitaMinima.value().compareTo(BigDecimal.valueOf(95.0)) >= 0;
    }
}

public record LimiteGrandeEsposizione(BigDecimal value) {
    public static Result<LimiteGrandeEsposizione, ErrorDetail> create(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.failure(ErrorDetail.validation("Limite grande esposizione must be positive"));
        }
        if (value.compareTo(BigDecimal.valueOf(25.0)) > 0) {
            return Result.failure(ErrorDetail.businessRule("LIMITE_EXCEEDED", 
                "Limite grande esposizione cannot exceed 25% (EU Article 395 CRR)"));
        }
        return Result.success(new LimiteGrandeEsposizione(value));
    }
}

public record CapitaleAmmissibile(BigDecimal amount, String currency) {
    public static Result<CapitaleAmmissibile, ErrorDetail> create(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.failure(ErrorDetail.validation("Capitale ammissibile must be positive"));
        }
        if (amount.compareTo(BigDecimal.valueOf(1_000_000)) < 0) {
            return Result.failure(ErrorDetail.businessRule("CAPITALE_TOO_LOW", 
                "Capitale ammissibile must be at least €1,000,000"));
        }
        return Result.success(new CapitaleAmmissibile(amount, "EUR"));
    }
    
    public String formatAmount() {
        return String.format("€%,.2f", amount);
    }
}
```

#### Multi-Template Configuration Aggregate

```java
public record BankTemplateConfiguration(
    BankTemplateConfigId id,
    BankId bankId,
    Map<TemplateId, TemplateConfiguration> configuredTemplates,
    Instant configuredAt,
    Instant lastUpdatedAt
) {
    public static Result<BankTemplateConfiguration, ErrorDetail> create(
        BankId bankId,
        List<TemplateConfigurationRequest> templateRequests
    ) {
        Map<TemplateId, TemplateConfiguration> templates = new HashMap<>();
        
        for (TemplateConfigurationRequest request : templateRequests) {
            Result<TemplateConfiguration, ErrorDetail> templateResult = 
                TemplateConfiguration.fromRequest(request);
            
            if (templateResult.isFailure()) {
                return Result.failure(templateResult.getError());
            }
            
            templates.put(request.templateId(), templateResult.getValue());
        }
        
        return Result.success(new BankTemplateConfiguration(
            BankTemplateConfigId.generate(),
            bankId,
            templates,
            Instant.now(),
            Instant.now()
        ));
    }
    
    public Set<TemplateId> getConfiguredTemplates() {
        return configuredTemplates.keySet();
    }
    
    public Maybe<TemplateConfiguration> getTemplateConfig(TemplateId templateId) {
        return Maybe.ofNullable(configuredTemplates.get(templateId));
    }
    
    public boolean hasTemplate(TemplateId templateId) {
        return configuredTemplates.containsKey(templateId);
    }
}

public record TemplateConfiguration(
    TemplateId templateId,
    RegulatoryTemplate regulatoryTemplate,
    OutputConfiguration outputConfiguration,
    DistribuzioneAutomatica distribuzioneAutomatica,
    Maybe<PianificazioneAutomatica> pianificazioneAutomatica,
    SubmissionConfiguration submissionConfiguration,
    boolean isActive
) {
    public static Result<TemplateConfiguration, ErrorDetail> fromRequest(
        TemplateConfigurationRequest request
    ) {
        return RegulatoryTemplate.loadTemplate(request.templateId())
            .flatMap(template -> OutputConfiguration.fromTemplate(template)
                .flatMap(outputConfig -> DistribuzioneAutomatica.create(request.emailAddresses())
                    .map(distribuzione -> new TemplateConfiguration(
                        request.templateId(),
                        template,
                        outputConfig,
                        distribuzione,
                        request.pianificazioneAutomatica(),
                        SubmissionConfiguration.forAuthority(template.authority()),
                        true
                    ))
                )
            );
    }
    
    public boolean isReadyForProcessing() {
        return isActive && 
               outputConfiguration != null && 
               distribuzioneAutomatica.hasValidEmails();
    }
}

public record TemplateConfigurationRequest(
    TemplateId templateId,
    List<String> emailAddresses,
    Maybe<PianificazioneAutomatica> pianificazioneAutomatica,
    Map<String, Object> templateSpecificSettings
) {}

public record SubmissionConfiguration(
    RegulatoryAuthority authority,
    String submissionEndpoint,
    String submissionMethod,
    boolean autoSubmissionEnabled,
    Map<String, String> authenticationConfig
) {
    public static SubmissionConfiguration forAuthority(RegulatoryAuthority authority) {
        return switch (authority) {
            case BANCA_ITALIA -> new SubmissionConfiguration(
                authority,
                "https://api.bancaditalia.it/regulatory-reports",
                "SWIFT",
                false, // Currently in development
                Map.of("protocol", "SWIFT", "format", "XML")
            );
            case EBA -> new SubmissionConfiguration(
                authority,
                "https://eba.europa.eu/reporting-portal",
                "HTTPS",
                false,
                Map.of("protocol", "HTTPS", "format", "XML")
            );
            case ECB -> new SubmissionConfiguration(
                authority,
                "https://ecb.europa.eu/stats/reporting",
                "HTTPS",
                false,
                Map.of("protocol", "HTTPS", "format", "CSV")
            );
        };
    }
}
    
    public static Result<ReportConfig, ErrorDetail> createFromTemplate(
        BankId bankId,
        String templateId,
        Map<String, Object> templateConfig
    ) {
        return RegulatoryTemplate.fromYaml(templateId, templateConfig)
            .flatMap(template -> OutputConfiguration.fromTemplate(template)
                .map(outputConfig -> new ReportConfig(
                    ReportConfigId.generate(),
                    bankId,
                    template,
                    outputConfig,
                    DistribuzioneAutomatica.defaultConfig(),
                    Maybe.none(),
                    SubmissionBancaItalia.create(false),
                    Instant.now()
                ))
            );
    }
}

public record RegulatoryTemplate(
    TemplateId templateId,
    String name,
    String version,
    RegulatoryAuthority authority,
    String regulation,
    boolean bcbs239Compliance,
    TemplateFrequency frequency,
    int deadlineDays,
    String language,
    String currency,
    Map<String, SectionSchema> schema,
    BusinessRules businessRules,
    Bcbs239ComplianceConfig bcbs239Config
) {
    
    // Supported template types
    public enum TemplateId {
        IT_LARGE_EXPOSURES_CIRCULARE_285("IT_LARGE_EXPOSURES_CIRCULARE_285", "Italian Large Exposures (Circolare 285)"),
        EU_LARGE_EXPOSURES_EBA_ITS("EU_LARGE_EXPOSURES_EBA_ITS", "EU Large Exposures EBA ITS"),
        IT_RISK_CONCENTRATION_SUPERVISORY("IT_RISK_CONCENTRATION_SUPERVISORY", "Italian Risk Concentration Supervisory");
        
        private final String id;
        private final String displayName;
        
        TemplateId(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }
        
        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        
        public static TemplateId fromId(String id) {
            return Arrays.stream(values())
                .filter(template -> template.id.equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown template ID: " + id));
        }
    }
    
    public enum RegulatoryAuthority {
        BANCA_ITALIA("Banca d'Italia"),
        EBA("European Banking Authority"),
        ECB("European Central Bank");
        
        private final String displayName;
        
        RegulatoryAuthority(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
    public static Result<RegulatoryTemplate, ErrorDetail> loadTemplate(TemplateId templateId) {
        return switch (templateId) {
            case IT_LARGE_EXPOSURES_CIRCULARE_285 -> loadItalianLargeExposuresTemplate();
            case EU_LARGE_EXPOSURES_EBA_ITS -> loadEuLargeExposuresTemplate();
            case IT_RISK_CONCENTRATION_SUPERVISORY -> loadItalianRiskConcentrationTemplate();
        };
    }
    
    private static Result<RegulatoryTemplate, ErrorDetail> loadItalianLargeExposuresTemplate() {
        try {
            // Load IT_LARGE_EXPOSURES_CIRCULARE_285.yml from classpath
            Map<String, Object> yamlConfig = loadYamlFromClasspath("templates/IT_LARGE_EXPOSURES_CIRCULARE_285.yml");
            return parseYamlTemplate(TemplateId.IT_LARGE_EXPOSURES_CIRCULARE_285, yamlConfig);
        } catch (Exception e) {
            return Result.failure(ErrorDetail.systemError("Failed to load Italian Large Exposures template", e));
        }
    }
    
    private static Result<RegulatoryTemplate, ErrorDetail> loadEuLargeExposuresTemplate() {
        try {
            // Load EU_LARGE_EXPOSURES_EBA_ITS.yml from classpath
            Map<String, Object> yamlConfig = loadYamlFromClasspath("templates/EU_LARGE_EXPOSURES_EBA_ITS.yml");
            return parseYamlTemplate(TemplateId.EU_LARGE_EXPOSURES_EBA_ITS, yamlConfig);
        } catch (Exception e) {
            return Result.failure(ErrorDetail.systemError("Failed to load EU Large Exposures template", e));
        }
    }
    
    private static Result<RegulatoryTemplate, ErrorDetail> loadItalianRiskConcentrationTemplate() {
        try {
            // Load IT_RISK_CONCENTRATION_SUPERVISORY.yml from classpath
            Map<String, Object> yamlConfig = loadYamlFromClasspath("templates/IT_RISK_CONCENTRATION_SUPERVISORY.yml");
            return parseYamlTemplate(TemplateId.IT_RISK_CONCENTRATION_SUPERVISORY, yamlConfig);
        } catch (Exception e) {
            return Result.failure(ErrorDetail.systemError("Failed to load Italian Risk Concentration template", e));
        }
    }
    
    private static Result<RegulatoryTemplate, ErrorDetail> parseYamlTemplate(
        TemplateId templateId, 
        Map<String, Object> yamlConfig
    ) {
        try {
            // Parse YAML template configuration
            Map<String, Object> metadata = (Map<String, Object>) yamlConfig.get("metadata");
            Map<String, Object> schema = (Map<String, Object>) yamlConfig.get("schema");
            Map<String, Object> businessRules = (Map<String, Object>) yamlConfig.get("business_rules");
            Map<String, Object> bcbs239Config = (Map<String, Object>) yamlConfig.get("bcbs_239_compliance");
            
            return Result.success(new RegulatoryTemplate(
                templateId,
                (String) metadata.get("name"),
                (String) metadata.get("version"),
                (String) metadata.get("authority"),
                (String) metadata.get("regulation"),
                (Boolean) metadata.get("bcbs_239_compliance"),
                TemplateFrequency.valueOf(((String) metadata.get("frequency")).toUpperCase()),
                (Integer) metadata.get("deadline_days"),
                (String) metadata.get("language"),
                (String) metadata.get("currency"),
                parseSectionSchemas(schema),
                BusinessRules.fromYaml(businessRules),
                Bcbs239ComplianceConfig.fromYaml(bcbs239Config)
            ));
        } catch (Exception e) {
            return Result.failure(ErrorDetail.validation("Invalid template configuration", 
                Map.of("templateId", templateId, "error", e.getMessage())));
        }
    }
    
    public Set<String> getOwnedSections() {
        // Bank Registry owns specific sections from the template
        return Set.of("bank_information", "bcbs_239_compliance", "outputs");
    }
    
    public boolean requiresBcbs239Compliance() {
        return bcbs239Compliance;
    }
}
    public static Result<ReportConfig, ErrorDetail> create(
        BankId bankId,
        TemplateReport template,
        FormatoOutput formato,
        LinguaReport lingua,
        List<String> emailAddresses,
        Maybe<PianificazioneAutomatica> pianificazione,
        boolean autoSubmission
    ) {
        return DistribuzioneAutomatica.create(emailAddresses)
            .map(distribuzione -> new ReportConfig(
                ReportConfigId.generate(),
                bankId,
                template,
                formato,
                lingua,
                distribuzione,
                pianificazione,
                SubmissionBancaItalia.create(autoSubmission),
                Instant.now()
            ));
    }
    
    public boolean isReadyForAutomaticGeneration() {
        return pianificazioneAutomatica.isPresent() && 
               distribuzioneAutomatica.hasValidEmails();
    }
}

public record OutputConfiguration(
    ExcelOutputConfig excelConfig,
    XmlOutputConfig xmlConfig,
    PdfOutputConfig pdfConfig
) {
    public static Result<OutputConfiguration, ErrorDetail> fromTemplate(RegulatoryTemplate template) {
        Map<String, Object> outputs = template.schema().get("outputs");
        
        return Result.success(new OutputConfiguration(
            ExcelOutputConfig.fromYaml((Map<String, Object>) outputs.get("excel")),
            XmlOutputConfig.fromYaml((Map<String, Object>) outputs.get("xml")),
            PdfOutputConfig.fromYaml((Map<String, Object>) outputs.get("pdf"))
        ));
    }
}

public record ExcelOutputConfig(
    boolean enabled,
    String filenamePattern,
    List<ExcelSheet> sheets
) {
    public static ExcelOutputConfig fromYaml(Map<String, Object> config) {
        List<Map<String, Object>> sheetsConfig = (List<Map<String, Object>>) config.get("sheets");
        List<ExcelSheet> sheets = sheetsConfig.stream()
            .map(ExcelSheet::fromYaml)
            .toList();
            
        return new ExcelOutputConfig(
            (Boolean) config.get("enabled"),
            (String) config.get("filename_pattern"),
            sheets
        );
    }
}

public record ExcelSheet(
    String name,
    String type,
    Maybe<String> filter,
    Maybe<List<String>> metrics
) {
    public static ExcelSheet fromYaml(Map<String, Object> config) {
        return new ExcelSheet(
            (String) config.get("name"),
            (String) config.get("type"),
            Maybe.ofNullable((String) config.get("filter")),
            Maybe.ofNullable((List<String>) config.get("metrics"))
        );
    }
}

public record Bcbs239ComplianceConfig(
    GovernanceConfig governance,
    DataAggregationConfig dataAggregation,
    RiskReportingConfig riskReporting,
    ComplianceTrackingConfig complianceTracking
) {
    public static Result<Bcbs239ComplianceConfig, ErrorDetail> fromYaml(Map<String, Object> config) {
        return Result.success(new Bcbs239ComplianceConfig(
            GovernanceConfig.fromYaml((Map<String, Object>) config.get("governance")),
            DataAggregationConfig.fromYaml((Map<String, Object>) config.get("data_aggregation")),
            RiskReportingConfig.fromYaml((Map<String, Object>) config.get("risk_reporting")),
            ComplianceTrackingConfig.fromYaml((Map<String, Object>) config.get("compliance_tracking"))
        ));
    }
}

public record GovernanceConfig(
    String dataOwner,
    String dataSteward,
    boolean lineageRequired,
    boolean automatedAggregation
) {
    public static GovernanceConfig fromYaml(Map<String, Object> config) {
        return new GovernanceConfig(
            (String) config.get("data_owner"),
            (String) config.get("data_steward"),
            (Boolean) config.get("lineage_required"),
            (Boolean) config.get("automated_aggregation")
        );
    }
}

public record DistribuzioneAutomatica(
    Email emailCompliance,
    List<Email> emailCC,
    boolean invioAutomatico
) {
    public static Result<DistribuzioneAutomatica, ErrorDetail> create(List<String> emailAddresses) {
        if (emailAddresses == null || emailAddresses.isEmpty()) {
            return Result.failure(ErrorDetail.validation("At least one email address is required"));
        }
        
        List<Result<Email, ErrorDetail>> emailResults = emailAddresses.stream()
            .map(Email::create)
            .toList();
            
        List<ErrorDetail> errors = emailResults.stream()
            .filter(Result::isFailure)
            .map(Result::getError)
            .toList();
            
        if (!errors.isEmpty()) {
            return Result.failure(ErrorDetail.validation("Invalid email addresses", 
                Map.of("errors", errors)));
        }
        
        List<Email> validEmails = emailResults.stream()
            .map(Result::getValue)
            .toList();
            
        return Result.success(new DistribuzioneAutomatica(
            validEmails.get(0),
            validEmails.subList(1, validEmails.size()),
            true
        ));
    }
    
    public boolean hasValidEmails() {
        return emailCompliance != null && !emailCC.isEmpty();
    }
}
```

### 2. Pure Business Functions

#### Bank Registration Function

```java
public class BankRegistrationService {
    
    public static Result<BankRegistrationResponse, ErrorDetail> registerBank(
        RegisterBankCommand command,
        Function<AbiCode, Result<Maybe<Bank>, ErrorDetail>> bankLookup,
        Function<LeiCode, Result<Boolean, ErrorDetail>> leiValidator,
        Function<AbiCode, Result<Boolean, ErrorDetail>> abiValidator,
        Function<Bank, Result<BankId, ErrorDetail>> bankSave
    ) {
        return AbiCode.create(command.abiCode())
            .flatMap(abiCode -> LeiCode.create(command.leiCode())
                .flatMap(leiCode -> DenominazioneSociale.create(command.denominazioneSociale())
                    .flatMap(denominazione -> SedeLegale.create(
                        command.via(), command.cap(), command.citta(), 
                        command.provincia(), command.paese())
                        .flatMap(sede -> validateBankNotExists(abiCode, bankLookup)
                            .flatMap(__ -> validateExternalRegistries(leiCode, abiCode, leiValidator, abiValidator)
                                .flatMap(__ -> Bank.create(
                                    denominazione, abiCode, leiCode, sede,
                                    command.gruppoBancario(), command.tipologiaBanca(), 
                                    command.categoriaVigilanza())
                                    .flatMap(bank -> bankSave.apply(bank)
                                        .map(bankId -> new BankRegistrationResponse(
                                            bankId,
                                            abiCode,
                                            leiCode,
                                            "BCBS_PARAMETERS",
                                            "Configure BCBS 239 parameters and thresholds"
                                        ))
                                    )
                                )
                            )
                        )
                    )
                )
            );
    }
    
    private static Result<Void, ErrorDetail> validateExternalRegistries(
        LeiCode leiCode,
        AbiCode abiCode,
        Function<LeiCode, Result<Boolean, ErrorDetail>> leiValidator,
        Function<AbiCode, Result<Boolean, ErrorDetail>> abiValidator
    ) {
        return leiValidator.apply(leiCode)
            .flatMap(leiValid -> {
                if (!leiValid) {
                    return Result.failure(ErrorDetail.businessRule("INVALID_LEI", 
                        "LEI code not found in GLEIF registry"));
                }
                return abiValidator.apply(abiCode)
                    .flatMap(abiValid -> {
                        if (!abiValid) {
                            return Result.failure(ErrorDetail.businessRule("INVALID_ABI", 
                                "ABI code not found in Banca d'Italia registry"));
                        }
                        return Result.success(null);
                    });
            });
    }
}
```

#### BCBS 239 Parameters Configuration Function

```java
public class BcbsParametersService {
    
    public static Result<BcbsParametersResponse, ErrorDetail> configureBcbsParameters(
        ConfigureBcbsParametersCommand command,
        Function<BankId, Result<Maybe<Bank>, ErrorDetail>> bankLookup,
        Function<BankParameters, Result<BankParametersId, ErrorDetail>> parametersSave
    ) {
        return bankLookup.apply(command.bankId())
            .flatMap(maybeBank -> maybeBank.map(bank -> 
                BankParameters.create(
                    bank.id(),
                    command.limiteGrandeEsposizione(),
                    command.sogliaClassificazione(),
                    command.capitaleAmmissibile(),
                    command.metodoCalcolo(),
                    command.sogliaQualitaMinima(),
                    command.validazioneFile(),
                    command.frequenzaStandard(),
                    command.giorniSubmission()
                ).flatMap(parameters -> parametersSave.apply(parameters)
                    .map(parametersId -> new BcbsParametersResponse(
                        parametersId,
                        bank.id(),
                        parameters.isCompliant(),
                        "REPORT_CONFIG",
                        "Configure report templates and distribution settings"
                    ))
                )
            ).orElse(Result.failure(ErrorDetail.notFound("Bank not found")))
            );
    }
}
```

#### Report Configuration Function

```java
public class ReportConfigurationService {
    
    public static Result<ReportConfigResponse, ErrorDetail> configureReports(
        ConfigureReportsCommand command,
        Function<BankId, Result<Maybe<Bank>, ErrorDetail>> bankLookup,
        Function<ReportConfig, Result<ReportConfigId, ErrorDetail>> configSave
    ) {
        return bankLookup.apply(command.bankId())
            .flatMap(maybeBank -> maybeBank.map(bank ->
                ReportConfig.create(
                    bank.id(),
                    command.templateReport(),
                    command.formatoOutput(),
                    command.linguaReport(),
                    command.emailAddresses(),
                    command.pianificazioneAutomatica(),
                    command.autoSubmission()
                ).flatMap(config -> configSave.apply(config)
                    .map(configId -> new ReportConfigResponse(
                        configId,
                        bank.id(),
                        config.isReadyForAutomaticGeneration(),
                        "CONFIGURATION_COMPLETE",
                        "Bank configuration completed successfully"
                    ))
                )
            ).orElse(Result.failure(ErrorDetail.notFound("Bank not found")))
            );
    }
}
```

### 3. Query API for Downstream Contexts

#### Bank Query API
Provides bank information to downstream contexts via closure-based functions:

```java
@Configuration
public class BankQueryConfiguration {
    
    @Bean
    public Function<BankId, Result<BankSummary, ErrorDetail>> bankSummaryQuery(
        BankRepository bankRepository
    ) {
        return bankId -> {
            try {
                return bankRepository.findById(bankId)
                    .map(bank -> Result.success(BankSummary.from(bank)))
                    .orElse(Result.failure(ErrorDetail.notFound("Bank not found")));
            } catch (Exception e) {
                return Result.failure(ErrorDetail.systemError("Bank query failed", e));
            }
        };
    }
    
    @Bean
    public Function<AbiCode, Result<Maybe<Bank>, ErrorDetail>> bankByAbiQuery(
        BankRepository bankRepository
    ) {
        return abiCode -> {
            try {
                Maybe<Bank> bank = bankRepository.findByAbiCode(abiCode);
                return Result.success(bank);
            } catch (Exception e) {
                return Result.failure(ErrorDetail.systemError("Bank ABI query failed", e));
            }
        };
    }
    
    @Bean
    public Function<BankId, Result<BankParameters, ErrorDetail>> bankParametersQuery(
        BankParametersRepository parametersRepository
    ) {
        return bankId -> {
            try {
                return parametersRepository.findByBankId(bankId)
                    .map(params -> Result.success(params))
                    .orElse(Result.failure(ErrorDetail.notFound("Bank parameters not found")));
            } catch (Exception e) {
                return Result.failure(ErrorDetail.systemError("Parameters query failed", e));
            }
        };
    }
}
```

#### Lightweight DTOs for Cross-Context Communication

```java
public record BankSummary(
    BankId bankId,
    AbiCode abiCode,
    LeiCode leiCode,
    String denominazioneSociale,
    String sedeLegaleFormatted,
    TipologiaBanca tipologiaBanca,
    CategoriaVigilanza categoriaVigilanza,
    BankStatus status
) {
    public static BankSummary from(Bank bank) {
        return new BankSummary(
            bank.id(),
            bank.abiCode(),
            bank.leiCode(),
            bank.denominazioneSociale().value(),
            bank.sedeLegale().formatAddress(),
            bank.tipologiaBanca(),
            bank.categoriaVigilanza(),
            bank.status()
        );
    }
}

public record CapitalInfo(
    BankId bankId,
    BigDecimal capitaleAmmissibile,
    String currency,
    BigDecimal limiteGrandeEsposizione,
    BigDecimal sogliaClassificazione,
    Instant lastUpdated
) {
    public static CapitalInfo from(BankParameters parameters) {
        return new CapitalInfo(
            parameters.bankId(),
            parameters.capitaleAmmissibile().amount(),
            parameters.capitaleAmmissibile().currency(),
            parameters.limiteGrandeEsposizione().value(),
            parameters.sogliaClassificazione().value(),
            parameters.configuredAt()
        );
    }
}
```

### 4. Service Composer Integration

#### BankConfigReactor (Upstream Orchestrator)
Coordinates the multi-stage bank configuration process and publishes events for downstream contexts.

```java
@Component
@CompositionHandler(route = "/banks/configure", order = 1)
public class BankConfigReactor implements PostCompositionHandler {
    
    private final Function<RegisterBankCommand, Result<BankRegistrationResponse, ErrorDetail>> registerBank;
    private final Function<ConfigureBcbsParametersCommand, Result<BcbsParametersResponse, ErrorDetail>> configureBcbs;
    private final Function<ConfigureReportsCommand, Result<ReportConfigResponse, ErrorDetail>> configureReports;
    private final EventPublisher eventPublisher;
    
    @Override
    public Result<Void, ErrorDetail> onInitialized(
        HttpServletRequest request, 
        Map<String, Object> body, 
        CompositionContext context
    ) {
        String configurationStage = (String) body.get("configurationStage");
        
        return switch (configurationStage) {
            case "BANK_INSTITUTION" -> handleBankInstitutionConfig(body, context);
            case "BCBS_PARAMETERS" -> handleBcbsParametersConfig(body, context);
            case "REPORT_CONFIG" -> handleReportConfig(body, context);
            default -> Result.failure(ErrorDetail.validation("Invalid configuration stage"));
        };
    }
    
    private Result<Void, ErrorDetail> handleBankInstitutionConfig(
        Map<String, Object> body, 
        CompositionContext context
    ) {
        return extractBankRegistrationCommand(body)
            .flatMap(registerBank)
            .map(response -> {
                // Store response in context
                context.putData("bankRegistrationResponse", response);
                context.putData("processingPhase", "BANK_REGISTERED");
                context.putData("nextStep", response.nextStep());
                
                // Publish thin event for downstream contexts
                BankRegisteredEvent event = new BankRegisteredEvent(
                    response.bankId(),
                    response.abiCode(),
                    response.leiCode(),
                    Instant.now(),
                    "BANK_REGISTERED"
                );
                eventPublisher.publish(event);
                
                return null;
            });
    }
    
    private Result<Void, ErrorDetail> handleBcbsParametersConfig(
        Map<String, Object> body, 
        CompositionContext context
    ) {
        return extractBcbsParametersCommand(body)
            .flatMap(configureBcbs)
            .map(response -> {
                context.putData("bcbsParametersResponse", response);
                context.putData("processingPhase", "BCBS_CONFIGURED");
                context.putData("nextStep", response.nextStep());
                
                // Publish parameters configured event
                BankParametersConfiguredEvent event = new BankParametersConfiguredEvent(
                    response.bankId(),
                    response.parametersId(),
                    extractCapitalAmount(body),
                    Instant.now(),
                    "BANK_PARAMETERS_CONFIGURED"
                );
                eventPublisher.publish(event);
                
                return null;
            });
    }
    
    @Override
    public Result<Void, ErrorDetail> onBackgroundWork(
        HttpServletRequest request, 
        Map<String, Object> body, 
        CompositionContext context
    ) {
        // Background work: Update subscription limits in billing context
        // This happens after main configuration is complete
        return context.getData("configurationComplete", Boolean.class)
            .map(complete -> {
                if (complete) {
                    // Notify billing context about new bank for subscription limit tracking
                    BankId bankId = extractBankId(context);
                    eventPublisher.publish(new BankConfigurationCompletedEvent(
                        bankId, Instant.now(), "BANK_CONFIGURATION_COMPLETED"
                    ));
                }
                return null;
            })
            .orElse(Result.success(null));
    }
}
```

#### BankInfoReactor (Downstream Query Provider)
Provides bank information to other contexts via Service Composer GET operations.

```java
@Component
@CompositionHandler(route = "/dashboard", order = 2)
@CompositionHandler(route = "/exposures", order = 2)
@CompositionHandler(route = "/reports", order = 2)
public class BankInfoReactor implements GetCompositionHandler {
    
    private final Function<BankId, Result<BankSummary, ErrorDetail>> bankSummaryQuery;
    private final Function<BankId, Result<CapitalInfo, ErrorDetail>> capitalInfoQuery;
    
    @Override
    public Result<Void, ErrorDetail> handleGet(
        HttpServletRequest request, 
        CompositionContext context,
        Map<String, Object> model
    ) {
        return context.getData("tenantContext", TenantContext.class)
            .map(tenantContext -> {
                BankId currentBankId = extractCurrentBankId(request, tenantContext);
                
                // Provide bank summary for dashboard/reports
                if (request.getRequestURI().contains("/dashboard") || 
                    request.getRequestURI().contains("/reports")) {
                    return bankSummaryQuery.apply(currentBankId)
                        .map(bankSummary -> {
                            model.put("bankInfo", bankSummary);
                            return null;
                        });
                }
                
                // Provide capital info for exposure calculations
                if (request.getRequestURI().contains("/exposures")) {
                    return capitalInfoQuery.apply(currentBankId)
                        .map(capitalInfo -> {
                            model.put("capitalInfo", capitalInfo);
                            return null;
                        });
                }
                
                return Result.success(null);
            })
            .orElse(Result.failure(ErrorDetail.businessRule("NO_TENANT_CONTEXT", 
                "Tenant context required for bank information")));
    }
}

#### BankValidationReactor (External Registry Integration)
Handles external registry validation for bank information during configuration.

```java
@Component
@CompositionHandler(route = "/banks/validate", order = 1)
public class BankValidationReactor implements PostCompositionHandler {
    
    private final Function<LeiCode, Result<Boolean, ErrorDetail>> gleifValidator;
    private final Function<AbiCode, Result<Boolean, ErrorDetail>> bancaItaliaValidator;
    private final EventPublisher eventPublisher;
    
    @Override
    public Result<Void, ErrorDetail> onInitialized(
        HttpServletRequest request, 
        Map<String, Object> body, 
        CompositionContext context
    ) {
        return extractValidationRequest(body)
            .flatMap(this::validateBankCodes)
            .map(validationResult -> {
                context.putData("validationResult", validationResult);
                context.putData("processingPhase", validationResult.isValid() ? "VALIDATED" : "VALIDATION_FAILED");
                
                // Publish validation result event
                if (validationResult.isValid()) {
                    eventPublisher.publish(new BankValidationCompletedEvent(
                        validationResult.bankId(),
                        Instant.now(),
                        "BANK_VALIDATION_COMPLETED"
                    ));
                }
                
                return null;
            });
    }
    
    private Result<BankValidationResult, ErrorDetail> validateBankCodes(BankValidationRequest request) {
        return gleifValidator.apply(request.leiCode())
            .flatMap(leiValid -> bancaItaliaValidator.apply(request.abiCode())
                .map(abiValid -> new BankValidationResult(
                    request.bankId(),
                    leiValid,
                    abiValid,
                    leiValid && abiValid,
                    leiValid ? Maybe.none() : Maybe.some("LEI code not found in GLEIF registry"),
                    abiValid ? Maybe.none() : Maybe.some("ABI code not found in Banca d'Italia registry")
                ))
            );
    }
}
```

## Data Models

### Command Objects

```java
public record RegisterBankCommand(
    String denominazioneSociale,
    String abiCode,
    String leiCode,
    String via,
    String cap,
    String citta,
    String provincia,
    String paese,
    GruppoBancario gruppoBancario,
    TipologiaBanca tipologiaBanca,
    CategoriaVigilanza categoriaVigilanza
) {}

public record ConfigureBcbsParametersCommand(
    BankId bankId,
    BigDecimal limiteGrandeEsposizione,
    BigDecimal sogliaClassificazione,
    BigDecimal capitaleAmmissibile,
    MetodoCalcolo metodoCalcolo,
    BigDecimal sogliaQualitaMinima,
    ValidazioneFile validazioneFile,
    FrequenzaStandard frequenzaStandard,
    int giorniSubmission
) {}

public record ConfigureReportsCommand(
    BankId bankId,
    TemplateReport templateReport,
    FormatoOutput formatoOutput,
    LinguaReport linguaReport,
    List<String> emailAddresses,
    Maybe<PianificazioneAutomatica> pianificazioneAutomatica,
    boolean autoSubmission
) {}
```

### Response Objects

```java
public record BankRegistrationResponse(
    BankId bankId,
    AbiCode abiCode,
    LeiCode leiCode,
    String nextStep,
    String message
) {}

public record BcbsParametersResponse(
    BankParametersId parametersId,
    BankId bankId,
    boolean isCompliant,
    String nextStep,
    String message
) {}

public record ReportConfigResponse(
    ReportConfigId configId,
    BankId bankId,
    boolean isReadyForAutomaticGeneration,
    String nextStep,
    String message
) {}
```

### Enums and Constants

```java
public enum TipologiaBanca {
    BANCA_COMMERCIALE("Banca Commerciale"),
    BANCA_CREDITO_COOPERATIVO("Banca di Credito Cooperativo"),
    BANCA_POPOLARE("Banca Popolare"),
    CASSA_RISPARMIO("Cassa di Risparmio"),
    ISTITUTO_CREDITO("Istituto di Credito");
    
    private final String displayName;
    
    TipologiaBanca(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() { return displayName; }
}

public enum CategoriaVigilanza {
    SSM("Banche significative (SSM)"),
    LSI("Less Significant Institutions"),
    FILIALI_ESTERE("Filiali di banche estere");
    
    private final String description;
    
    CategoriaVigilanza(String description) {
        this.description = description;
    }
    
    public String getDescription() { return description; }
}

public enum TemplateReport {
    BANCA_ITALIA_STANDARD("Banca d'Italia - Standard"),
    EBA_STANDARD("EBA Standard"),
    CUSTOM("Custom Template");
    
    private final String displayName;
    
    TemplateReport(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() { return displayName; }
}

public enum FormatoOutput {
    PDF("PDF (raccomandato)"),
    EXCEL("Excel (.xlsx)"),
    BOTH("Entrambi i formati");
    
    private final String description;
    
    FormatoOutput(String description) {
        this.description = description;
    }
    
    public String getDescription() { return description; }
}
```

## Error Handling

### Domain-Specific Error Types

```java
public class BankRegistryErrorCodes {
    public static final String INVALID_ABI = "INVALID_ABI";
    public static final String INVALID_LEI = "INVALID_LEI";
    public static final String BANK_EXISTS = "BANK_EXISTS";
    public static final String LIMITE_EXCEEDED = "LIMITE_EXCEEDED";
    public static final String CAPITALE_TOO_LOW = "CAPITALE_TOO_LOW";
    public static final String CONFIGURATION_INCOMPLETE = "CONFIGURATION_INCOMPLETE";
    public static final String EXTERNAL_VALIDATION_FAILED = "EXTERNAL_VALIDATION_FAILED";
}
```

## Testing Strategy

### Pure Function Testing

```java
class BankRegistrationServiceTest {
    
    @Test
    void shouldRegisterBankWithValidItalianData() {
        // Given
        RegisterBankCommand command = new RegisterBankCommand(
            "Banca Italiana SpA",
            "12345",
            "549300ABCDEFGHIJ1234",
            "Via Roma 1",
            "20121",
            "Milano",
            "MI",
            "Italia",
            GruppoBancario.INTESA_SANPAOLO,
            TipologiaBanca.BANCA_COMMERCIALE,
            CategoriaVigilanza.SSM
        );
        
        // Mock functions
        Function<AbiCode, Result<Maybe<Bank>, ErrorDetail>> bankLookup = 
            abi -> Result.success(Maybe.none());
        Function<LeiCode, Result<Boolean, ErrorDetail>> leiValidator = 
            lei -> Result.success(true);
        Function<AbiCode, Result<Boolean, ErrorDetail>> abiValidator = 
            abi -> Result.success(true);
        Function<Bank, Result<BankId, ErrorDetail>> bankSave = 
            bank -> Result.success(bank.id());
        
        // When
        Result<BankRegistrationResponse, ErrorDetail> result = 
            BankRegistrationService.registerBank(command, bankLookup, leiValidator, abiValidator, bankSave);
        
        // Then
        assertTrue(result.isSuccess());
        assertEquals("BCBS_PARAMETERS", result.getValue().nextStep());
    }
}
```

This design provides a comprehensive foundation for Italian bank registration and configuration while maintaining functional programming principles and Service Composer integration patterns.