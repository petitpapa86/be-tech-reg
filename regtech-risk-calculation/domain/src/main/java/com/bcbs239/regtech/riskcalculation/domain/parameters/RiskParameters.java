package com.bcbs239.regtech.riskcalculation.domain.parameters;

import com.bcbs239.regtech.riskcalculation.domain.shared.Money;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Risk Parameters Aggregate Root
 * 
 * Manages regulatory risk parameters for large exposures, capital base, and concentration risk.
 * These parameters define thresholds and limits used in risk calculations.
 * 
 * Requirements: CRR Art. 395, BCBS 239
 */
public class RiskParameters {
    
    @NonNull
    private final RiskParametersId id;
    
    @NonNull
    private final String bankId;
    
    @NonNull
    private LargeExposuresParameters largeExposures;
    
    @NonNull
    private CapitalBaseParameters capitalBase;
    
    @NonNull
    private ConcentrationRiskParameters concentrationRisk;
    
    @NonNull
    private ValidationStatus validationStatus;
    
    @NonNull
    private Instant createdAt;
    
    @Nullable
    private Instant lastModifiedAt;
    
    @Nullable
    private String lastModifiedBy;
    
    private long version;
    
    private final List<RiskParametersEvent> domainEvents = new ArrayList<>();
    
    // Private constructor - use factory methods
    private RiskParameters(
        @NonNull RiskParametersId id,
        @NonNull String bankId,
        @NonNull LargeExposuresParameters largeExposures,
        @NonNull CapitalBaseParameters capitalBase,
        @NonNull ConcentrationRiskParameters concentrationRisk,
        @NonNull ValidationStatus validationStatus,
        @NonNull Instant createdAt,
        @Nullable Instant lastModifiedAt,
        @Nullable String lastModifiedBy,
        long version
    ) {
        this.id = id;
        this.bankId = bankId;
        this.largeExposures = largeExposures;
        this.capitalBase = capitalBase;
        this.concentrationRisk = concentrationRisk;
        this.validationStatus = validationStatus;
        this.createdAt = createdAt;
        this.lastModifiedAt = lastModifiedAt;
        this.lastModifiedBy = lastModifiedBy;
        this.version = version;
    }
    
    /**
     * Factory method: Create new risk parameters with default values
     */
    @NonNull
    public static RiskParameters createDefault(@NonNull String bankId, @NonNull String createdBy) {
        RiskParametersId id = RiskParametersId.generate();
        Instant now = Instant.now();
        
        // Default parameters per CRR Art. 395
        LargeExposuresParameters largeExposures = LargeExposuresParameters.createDefault();
        CapitalBaseParameters capitalBase = CapitalBaseParameters.createDefault();
        ConcentrationRiskParameters concentrationRisk = ConcentrationRiskParameters.createDefault();
        ValidationStatus validationStatus = ValidationStatus.createValid();
        
        RiskParameters parameters = new RiskParameters(
            id, bankId, largeExposures, capitalBase, concentrationRisk,
            validationStatus, now, now, createdBy, 0L
        );
        
        parameters.registerEvent(new RiskParametersCreatedEvent(id, bankId, createdBy, now));
        
        return parameters;
    }
    
    /**
     * Factory method: Reconstitute from persistence
     */
    @NonNull
    public static RiskParameters reconstitute(
        @NonNull RiskParametersId id,
        @NonNull String bankId,
        @NonNull LargeExposuresParameters largeExposures,
        @NonNull CapitalBaseParameters capitalBase,
        @NonNull ConcentrationRiskParameters concentrationRisk,
        @NonNull ValidationStatus validationStatus,
        @NonNull Instant createdAt,
        @Nullable Instant lastModifiedAt,
        @Nullable String lastModifiedBy,
        long version
    ) {
        return new RiskParameters(
            id, bankId, largeExposures, capitalBase, concentrationRisk,
            validationStatus, createdAt, lastModifiedAt, lastModifiedBy, version
        );
    }
    
    // Business methods
    
    public void updateLargeExposuresParameters(
        @NonNull LargeExposuresParameters newParameters,
        @NonNull String modifiedBy
    ) {
        this.largeExposures = newParameters;
        this.lastModifiedAt = Instant.now();
        this.lastModifiedBy = modifiedBy;
        
        registerEvent(new RiskParametersUpdatedEvent(
            id, bankId, "LARGE_EXPOSURES", modifiedBy, Instant.now()
        ));
    }
    
    public void updateCapitalBase(
        @NonNull CapitalBaseParameters newParameters,
        @NonNull String modifiedBy
    ) {
        this.capitalBase = newParameters;
        this.lastModifiedAt = Instant.now();
        this.lastModifiedBy = modifiedBy;
        
        registerEvent(new RiskParametersUpdatedEvent(
            id, bankId, "CAPITAL_BASE", modifiedBy, Instant.now()
        ));
    }
    
    public void updateConcentrationRisk(
        @NonNull ConcentrationRiskParameters newParameters,
        @NonNull String modifiedBy
    ) {
        this.concentrationRisk = newParameters;
        this.lastModifiedAt = Instant.now();
        this.lastModifiedBy = modifiedBy;
        
        registerEvent(new RiskParametersUpdatedEvent(
            id, bankId, "CONCENTRATION_RISK", modifiedBy, Instant.now()
        ));
    }
    
    public void resetToDefault(@NonNull String modifiedBy) {
        this.largeExposures = LargeExposuresParameters.createDefault();
        this.capitalBase = CapitalBaseParameters.createDefault();
        this.concentrationRisk = ConcentrationRiskParameters.createDefault();
        this.validationStatus = ValidationStatus.createValid();
        this.lastModifiedAt = Instant.now();
        this.lastModifiedBy = modifiedBy;
        
        registerEvent(new RiskParametersResetEvent(id, bankId, modifiedBy, Instant.now()));
    }
    
    public void validate() {
        boolean bcbs239Compliant = validateBcbs239Compliance();
        boolean capitalUpToDate = capitalBase.isUpToDate();
        
        this.validationStatus = new ValidationStatus(bcbs239Compliant, capitalUpToDate);
    }
    
    private boolean validateBcbs239Compliance() {
        // Validate that parameters meet BCBS 239 requirements
        return largeExposures.isValid() && 
               capitalBase.isValid() && 
               concentrationRisk.isValid();
    }
    
    // Event management
    
    private void registerEvent(@NonNull RiskParametersEvent event) {
        this.domainEvents.add(event);
    }
    
    @NonNull
    public List<RiskParametersEvent> pullDomainEvents() {
        List<RiskParametersEvent> events = new ArrayList<>(this.domainEvents);
        this.domainEvents.clear();
        return events;
    }
    
    // Getters
    
    @NonNull
    public RiskParametersId getId() {
        return id;
    }
    
    @NonNull
    public String getBankId() {
        return bankId;
    }
    
    @NonNull
    public LargeExposuresParameters getLargeExposures() {
        return largeExposures;
    }
    
    @NonNull
    public CapitalBaseParameters getCapitalBase() {
        return capitalBase;
    }
    
    @NonNull
    public ConcentrationRiskParameters getConcentrationRisk() {
        return concentrationRisk;
    }
    
    @NonNull
    public ValidationStatus getValidationStatus() {
        return validationStatus;
    }
    
    @NonNull
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    @Nullable
    public Instant getLastModifiedAt() {
        return lastModifiedAt;
    }
    
    @Nullable
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }
    
    public long getVersion() {
        return version;
    }
}
