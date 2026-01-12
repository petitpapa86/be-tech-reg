package com.bcbs239.regtech.dataquality.application.rulesengine;

import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import com.bcbs239.regtech.dataquality.rulesengine.engine.DefaultRuleContext;
import com.bcbs239.regtech.dataquality.rulesengine.engine.RuleContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class RuleContextFactory {

    /**
     * Creates a rule context from an exposure record.
     *
     * @param exposure The exposure record
     * @return RuleContext ready for rule execution
     */
    public RuleContext fromExposure(ExposureRecord exposure) {
        Map<String, Object> data = new HashMap<>();

        // Map exposure fields to context
        putIfNotNull(data, "exposureId", exposure.exposureId());
        putIfNotNull(data, "amount", exposure.exposureAmount());
        putIfNotNull(data, "currency", exposure.currency());
        putIfNotNull(data, "country", exposure.countryCode());
        putIfNotNull(data, "sector", exposure.sector());
        putIfNotNull(data, "counterpartyId", exposure.counterpartyId());
        putIfNotNull(data, "counterpartyType", exposure.counterpartyType());
        putIfNotNull(data, "leiCode", exposure.counterpartyLei());
        putIfNotNull(data, "productType", exposure.productType());
        putIfNotNull(data, "internalRating", exposure.internalRating());
        putIfNotNull(data, "riskCategory", exposure.riskCategory());
        putIfNotNull(data, "riskWeight", exposure.riskWeight());
        putIfNotNull(data, "reportingDate", exposure.reportingDate());
        putIfNotNull(data, "valuationDate", exposure.valuationDate());
        putIfNotNull(data, "maturityDate", exposure.maturityDate());
        putIfNotNull(data, "referenceNumber", exposure.referenceNumber());

        // Add helper flags
        data.put("isCorporateExposure", exposure.isCorporateExposure());
        data.put("isTermExposure", exposure.isTermExposure());

        // Add entity metadata for exemption checking
        data.put("entity_type", "EXPOSURE");
        putIfNotNull(data, "entity_id", exposure.exposureId());

        return new DefaultRuleContext(data);
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}