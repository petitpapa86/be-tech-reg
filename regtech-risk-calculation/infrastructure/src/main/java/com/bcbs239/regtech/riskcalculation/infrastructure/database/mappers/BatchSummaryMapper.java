package com.bcbs239.regtech.riskcalculation.infrastructure.database.mappers;

import com.bcbs239.regtech.riskcalculation.domain.analysis.Breakdown;
import com.bcbs239.regtech.riskcalculation.domain.analysis.HHI;
import com.bcbs239.regtech.riskcalculation.domain.analysis.Share;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.*;
import com.bcbs239.regtech.riskcalculation.infrastructure.database.entities.BatchSummaryEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps between BatchSummary domain objects and BatchSummaryEntity persistence objects.
 */
@Component
public class BatchSummaryMapper {

    /**
     * Convert domain BatchSummary to persistence entity
     */
    public BatchSummaryEntity toEntity(BatchSummary batchSummary) {
        BatchSummaryEntity entity = new BatchSummaryEntity();
        
        // Basic fields
        entity.setBatchId(batchSummary.getBatchId());
        entity.setBankId(batchSummary.getBankId());
        entity.setTotalExposures(batchSummary.getTotalExposures());
        entity.setTotalAmountEur(batchSummary.getTotalAmountEur().getValue());
        
        // Geographic breakdown
        Map<String, Share> geoShares = batchSummary.getGeographicBreakdown().getShares();
        entity.setItalyAmount(getShareAmount(geoShares, "ITALY"));
        entity.setItalyPct(getSharePercentage(geoShares, "ITALY"));
        entity.setEuAmount(getShareAmount(geoShares, "EU"));
        entity.setEuPct(getSharePercentage(geoShares, "EU"));
        entity.setNonEuAmount(getShareAmount(geoShares, "NON_EU"));
        entity.setNonEuPct(getSharePercentage(geoShares, "NON_EU"));
        
        // Sector breakdown
        Map<String, Share> sectorShares = batchSummary.getSectorBreakdown().getShares();
        entity.setRetailAmount(getShareAmount(sectorShares, "RETAIL"));
        entity.setRetailPct(getSharePercentage(sectorShares, "RETAIL"));
        entity.setSovereignAmount(getShareAmount(sectorShares, "SOVEREIGN"));
        entity.setSovereignPct(getSharePercentage(sectorShares, "SOVEREIGN"));
        entity.setCorporateAmount(getShareAmount(sectorShares, "CORPORATE"));
        entity.setCorporatePct(getSharePercentage(sectorShares, "CORPORATE"));
        entity.setBankingAmount(getShareAmount(sectorShares, "BANKING"));
        entity.setBankingPct(getSharePercentage(sectorShares, "BANKING"));
        
        // Concentration indices
        entity.setHerfindahlGeographic(batchSummary.getHerfindahlGeographic().getValue());
        entity.setHerfindahlSector(batchSummary.getHerfindahlSector().getValue());
        
        // File references
        entity.setInputFileUri(batchSummary.getInputFileUri().uri());
        entity.setOutputFileUri(batchSummary.getOutputFileUri().uri());
        
        // Status and timestamps
        entity.setStatus(batchSummary.getStatus().name());
        entity.setProcessedAt(batchSummary.getTimestamps().getProcessedAt());
        entity.setCreatedAt(batchSummary.getTimestamps().getCreatedAt());
        
        return entity;
    }

    /**
     * Convert persistence entity to domain BatchSummary
     */
    public BatchSummary toDomain(BatchSummaryEntity entity) {
        // Reconstruct geographic breakdown
        Map<String, Share> geoShares = new HashMap<>();
        geoShares.put("ITALY", new Share(
            AmountEur.of(entity.getItalyAmount()),
            PercentageOfTotal.of(entity.getItalyPct())
        ));
        geoShares.put("EU", new Share(
            AmountEur.of(entity.getEuAmount()),
            PercentageOfTotal.of(entity.getEuPct())
        ));
        geoShares.put("NON_EU", new Share(
            AmountEur.of(entity.getNonEuAmount()),
            PercentageOfTotal.of(entity.getNonEuPct())
        ));
        Breakdown geographicBreakdown = new Breakdown(geoShares);
        
        // Reconstruct sector breakdown
        Map<String, Share> sectorShares = new HashMap<>();
        sectorShares.put("RETAIL", new Share(
            AmountEur.of(entity.getRetailAmount()),
            PercentageOfTotal.of(entity.getRetailPct())
        ));
        sectorShares.put("SOVEREIGN", new Share(
            AmountEur.of(entity.getSovereignAmount()),
            PercentageOfTotal.of(entity.getSovereignPct())
        ));
        sectorShares.put("CORPORATE", new Share(
            AmountEur.of(entity.getCorporateAmount()),
            PercentageOfTotal.of(entity.getCorporatePct())
        ));
        sectorShares.put("BANKING", new Share(
            AmountEur.of(entity.getBankingAmount()),
            PercentageOfTotal.of(entity.getBankingPct())
        ));
        Breakdown sectorBreakdown = new Breakdown(sectorShares);
        
        // Reconstruct timestamps
        ProcessingTimestamps timestamps = new ProcessingTimestamps(
            entity.getCreatedAt(),
            entity.getProcessedAt(),
            entity.getProcessedAt()
        );
        
        return new BatchSummary(
            entity.getBatchId(),
            entity.getBankId(),
            entity.getTotalExposures(),
            AmountEur.of(entity.getTotalAmountEur()),
            geographicBreakdown,
            sectorBreakdown,
            HHI.of(entity.getHerfindahlGeographic()),
            HHI.of(entity.getHerfindahlSector()),
            FileStorageUri.of(entity.getInputFileUri()),
            FileStorageUri.of(entity.getOutputFileUri()),
            BatchSummaryStatus.valueOf(entity.getStatus()),
            timestamps
        );
    }
    
    private double getShareAmount(Map<String, Share> shares, String key) {
        Share share = shares.get(key);
        return share != null ? share.getAmount().getValue() : 0.0;
    }
    
    private double getSharePercentage(Map<String, Share> shares, String key) {
        Share share = shares.get(key);
        return share != null ? share.getPercentage().getValue() : 0.0;
    }
}
