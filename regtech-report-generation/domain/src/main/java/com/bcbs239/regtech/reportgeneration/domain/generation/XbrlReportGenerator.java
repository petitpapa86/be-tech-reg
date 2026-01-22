package com.bcbs239.regtech.reportgeneration.domain.generation;

import com.bcbs239.regtech.core.domain.shared.Result;
import org.w3c.dom.Document;

/**
 * Domain service interface for generating XBRL-XML reports
 * 
 * Generates XBRL-XML reports conforming to EBA COREP taxonomy for Large Exposures
 * regulatory reporting. The generated XBRL includes LE1 and LE2 templates with
 * all required namespaces, contexts, and facts.
 * 
 * This is a domain service interface that will be implemented in the
 * infrastructure layer using Java DOM API.
 * 
 * Requirements: 7.1
 */
public interface XbrlReportGenerator {
    
    /**
     * Generate an XBRL-XML document from calculation results and metadata
     * 
     * The generated XBRL includes:
     * - All required EBA COREP namespaces and schema reference
     * - One context per large exposure with dimensions (CP, CT, SC)
     * - LE1 facts: counterparty name, LEI code, identifier type, country, sector
     * - LE2 facts: original amount, amount after CRM, trading/non-trading portions,
     *   percentage of capital
     * - CONCAT identifier type for missing LEI codes
     * - Pretty-print formatting with UTF-8 encoding
     * 
     * @param results the calculation results containing exposure data
     * @param metadata the report metadata (bank ID, reporting date, etc.)
     * @return a Result containing the generated XBRL document as a DOM Document, or an error
     */
    Result<Document> generate(CalculationResults results, ReportMetadata metadata);
}
