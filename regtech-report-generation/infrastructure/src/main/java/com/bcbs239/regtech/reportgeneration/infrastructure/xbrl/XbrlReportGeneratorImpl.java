package com.bcbs239.regtech.reportgeneration.infrastructure.xbrl;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.reportgeneration.domain.generation.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Implementation of XBRL Report Generator
 * 
 * Generates XBRL-XML documents conforming to EBA COREP taxonomy for Large Exposures
 * regulatory reporting. Implements templates LE1 (counterparty details) and LE2
 * (exposure amounts) with all required namespaces, contexts, and facts.
 * 
 * Requirements: 10.1, 10.2, 10.3, 10.4, 10.5
 */
@Service
@Slf4j
public class XbrlReportGeneratorImpl implements XbrlReportGenerator {
    
    // EBA COREP namespaces
    private static final String XBRL_NAMESPACE = "http://www.xbrl.org/2003/instance";
    private static final String XBRLI_PREFIX = "xbrli";
    
    private static final String EBA_NAMESPACE = "http://www.eba.europa.eu/xbrl/crr";
    private static final String EBA_PREFIX = "eba";
    
    private static final String ISO4217_NAMESPACE = "http://www.xbrl.org/2003/iso4217";
    private static final String ISO4217_PREFIX = "iso4217";
    
    private static final String XLINK_NAMESPACE = "http://www.w3.org/1999/xlink";
    private static final String XLINK_PREFIX = "xlink";
    
    // EBA taxonomy schema reference
    private static final String EBA_SCHEMA_LOCATION = "http://www.eba.europa.eu/eu/fr/xbrl/crr/fws/corep/its-2023-12/2023-12-31/mod/corep.xsd";
    
    private final XbrlSchemaValidator validator;
    
    public XbrlReportGeneratorImpl(XbrlSchemaValidator validator) {
        this.validator = validator;
    }
    
    @Override
    public Result<Document> generate(CalculationResults results, ReportMetadata metadata) {
        try {
            log.info("Starting XBRL generation for batch: {}", results.batchId().value());
            
            // Create XML document
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            
            // Create root element with namespaces
            Element xbrl = createRootElement(document);
            document.appendChild(xbrl);
            
            // Add schema reference
            addSchemaReference(xbrl);
            
            // Get large exposures (>= 10% of capital)
            List<CalculatedExposure> largeExposures = results.getLargeExposures();
            
            if (largeExposures.isEmpty()) {
                log.warn("No large exposures found for batch: {}", results.batchId().value());
            }
            
            // Create contexts for each exposure
            for (int i = 0; i < largeExposures.size(); i++) {
                CalculatedExposure exposure = largeExposures.get(i);
                String contextId = "ctx_" + i;
                
                Element context = createContext(document, contextId, exposure, metadata);
                xbrl.appendChild(context);
            }
            
            // Create unit for monetary values (EUR)
            Element unit = createMonetaryUnit(document);
            xbrl.appendChild(unit);
            
            // Create unit for percentages
            Element percentUnit = createPercentageUnit(document);
            xbrl.appendChild(percentUnit);
            
            // Populate LE1 facts (counterparty details)
            for (int i = 0; i < largeExposures.size(); i++) {
                CalculatedExposure exposure = largeExposures.get(i);
                String contextId = "ctx_" + i;
                
                addLE1Facts(document, xbrl, contextId, exposure);
            }
            
            // Populate LE2 facts (exposure amounts)
            for (int i = 0; i < largeExposures.size(); i++) {
                CalculatedExposure exposure = largeExposures.get(i);
                String contextId = "ctx_" + i;
                
                addLE2Facts(document, xbrl, contextId, exposure);
            }
            
            log.info("XBRL generation completed for batch: {}, exposures: {}", 
                results.batchId().value(), largeExposures.size());
            
            // Validate generated document
            Result<ValidationResult> validationResult = validator.validate(document);
            if (validationResult.isFailure()) {
                return Result.failure(validationResult.getError().orElseThrow());
            }
            
            ValidationResult result = validationResult.getValue().orElseThrow();
            if (result.hasErrors()) {
                return Result.failure(ErrorDetail.of(
                    "XBRL_VALIDATION_FAILED",
                    ErrorType.BUSINESS_RULE_ERROR,
                    result.getFormattedErrorMessage(),
                    "report.generation.xbrl_validation_failed"
                ));
            }
            
            return Result.success(document);
            
        } catch (Exception e) {
            log.error("XBRL generation failed for batch: {}", results.batchId().value(), e);
            return Result.failure(ErrorDetail.of(
                "XBRL_GENERATION_FAILED",
                ErrorType.SYSTEM_ERROR,
                "Failed to generate XBRL report: " + e.getMessage(),
                "report.generation.xbrl_failed"
            ));
        }
    }
    
    /**
     * Create root XBRL element with all required namespaces
     */
    private Element createRootElement(Document document) {
        Element xbrl = document.createElementNS(XBRL_NAMESPACE, XBRLI_PREFIX + ":xbrl");
        
        // Add namespace declarations
        xbrl.setAttribute("xmlns:" + XBRLI_PREFIX, XBRL_NAMESPACE);
        xbrl.setAttribute("xmlns:" + EBA_PREFIX, EBA_NAMESPACE);
        xbrl.setAttribute("xmlns:" + ISO4217_PREFIX, ISO4217_NAMESPACE);
        xbrl.setAttribute("xmlns:" + XLINK_PREFIX, XLINK_NAMESPACE);
        
        return xbrl;
    }
    
    /**
     * Add schema reference to EBA taxonomy
     */
    private void addSchemaReference(Element xbrl) {
        Element schemaRef = xbrl.getOwnerDocument().createElementNS(
            "http://www.xbrl.org/2003/linkbase", 
            "link:schemaRef"
        );
        schemaRef.setAttribute("xmlns:link", "http://www.xbrl.org/2003/linkbase");
        schemaRef.setAttributeNS(XLINK_NAMESPACE, XLINK_PREFIX + ":type", "simple");
        schemaRef.setAttributeNS(XLINK_NAMESPACE, XLINK_PREFIX + ":href", EBA_SCHEMA_LOCATION);
        
        xbrl.appendChild(schemaRef);
    }
    
    /**
     * Create context with dimensions for a specific exposure
     * Dimensions: CP (counterparty), CT (country), SC (sector)
     */
    private Element createContext(Document document, String contextId, 
                                  CalculatedExposure exposure, ReportMetadata metadata) {
        Element context = document.createElementNS(XBRL_NAMESPACE, XBRLI_PREFIX + ":context");
        context.setAttribute("id", contextId);
        
        // Entity
        Element entity = document.createElementNS(XBRL_NAMESPACE, XBRLI_PREFIX + ":entity");
        Element identifier = document.createElementNS(XBRL_NAMESPACE, XBRLI_PREFIX + ":identifier");
        identifier.setAttribute("scheme", "http://www.eba.europa.eu");
        identifier.setTextContent(metadata.bankId().value());
        entity.appendChild(identifier);
        
        // Segment with dimensions
        Element segment = document.createElementNS(XBRL_NAMESPACE, XBRLI_PREFIX + ":segment");
        
        // CP dimension (counterparty)
        Element cpDimension = document.createElementNS(XBRL_NAMESPACE, XBRLI_PREFIX + ":explicitMember");
        cpDimension.setAttribute("dimension", EBA_PREFIX + ":CP");
        cpDimension.setTextContent(EBA_PREFIX + ":" + sanitizeForXml(exposure.counterpartyName().value()));
        segment.appendChild(cpDimension);
        
        // CT dimension (country)
        Element ctDimension = document.createElementNS(XBRL_NAMESPACE, XBRLI_PREFIX + ":explicitMember");
        ctDimension.setAttribute("dimension", EBA_PREFIX + ":CT");
        ctDimension.setTextContent(EBA_PREFIX + ":" + exposure.countryCode().value());
        segment.appendChild(ctDimension);
        
        // SC dimension (sector)
        Element scDimension = document.createElementNS(XBRL_NAMESPACE, XBRLI_PREFIX + ":explicitMember");
        scDimension.setAttribute("dimension", EBA_PREFIX + ":SC");
        scDimension.setTextContent(EBA_PREFIX + ":" + exposure.sectorCode().value());
        segment.appendChild(scDimension);
        
        entity.appendChild(segment);
        context.appendChild(entity);
        
        // Period (instant)
        Element period = document.createElementNS(XBRL_NAMESPACE, XBRLI_PREFIX + ":period");
        Element instant = document.createElementNS(XBRL_NAMESPACE, XBRLI_PREFIX + ":instant");
        instant.setTextContent(metadata.reportingDate().value().format(DateTimeFormatter.ISO_LOCAL_DATE));
        period.appendChild(instant);
        context.appendChild(period);
        
        return context;
    }
    
    /**
     * Create monetary unit (EUR)
     */
    private Element createMonetaryUnit(Document document) {
        Element unit = document.createElementNS(XBRL_NAMESPACE, XBRLI_PREFIX + ":unit");
        unit.setAttribute("id", "EUR");
        
        Element measure = document.createElementNS(XBRL_NAMESPACE, XBRLI_PREFIX + ":measure");
        measure.setTextContent(ISO4217_PREFIX + ":EUR");
        unit.appendChild(measure);
        
        return unit;
    }
    
    /**
     * Create percentage unit
     */
    private Element createPercentageUnit(Document document) {
        Element unit = document.createElementNS(XBRL_NAMESPACE, XBRLI_PREFIX + ":unit");
        unit.setAttribute("id", "pure");
        
        Element measure = document.createElementNS(XBRL_NAMESPACE, XBRLI_PREFIX + ":measure");
        measure.setTextContent(XBRLI_PREFIX + ":pure");
        unit.appendChild(measure);
        
        return unit;
    }
    
    /**
     * Add LE1 facts (counterparty details)
     * Requirements: 10.4
     */
    private void addLE1Facts(Document document, Element xbrl, String contextId, CalculatedExposure exposure) {
        // Counterparty name
        addFact(document, xbrl, "eba:LE1_CounterpartyName", contextId, null, 
            exposure.counterpartyName().value());
        
        // LEI code or CONCAT identifier
        if (exposure.hasLeiCode()) {
            addFact(document, xbrl, "eba:LE1_LEICode", contextId, null, 
                exposure.leiCode().get().value());
            addFact(document, xbrl, "eba:LE1_IdentifierType", contextId, null, "LEI");
        } else {
            // Use CONCAT for missing LEI
            addFact(document, xbrl, "eba:LE1_IdentifierType", contextId, null, "CONCAT");
            addFact(document, xbrl, "eba:LE1_AlternateIdentifier", contextId, null, 
                exposure.identifierType().name());
        }
        
        // Country code
        addFact(document, xbrl, "eba:LE1_CountryCode", contextId, null, 
            exposure.countryCode().value());
        
        // Sector code
        addFact(document, xbrl, "eba:LE1_SectorCode", contextId, null, 
            exposure.sectorCode().value());
    }
    
    /**
     * Add LE2 facts (exposure amounts)
     * Requirements: 10.5
     */
    private void addLE2Facts(Document document, Element xbrl, String contextId, CalculatedExposure exposure) {
        // Original exposure amount
        addFact(document, xbrl, "eba:LE2_OriginalExposure", contextId, "EUR", 
            formatAmount(exposure.amountEur().value()));
        
        // Exposure after CRM
        addFact(document, xbrl, "eba:LE2_ExposureAfterCRM", contextId, "EUR", 
            formatAmount(exposure.amountAfterCrm().value()));
        
        // Trading book portion
        addFact(document, xbrl, "eba:LE2_TradingBookPortion", contextId, "EUR", 
            formatAmount(exposure.tradingBookPortion().value()));
        
        // Non-trading book portion
        addFact(document, xbrl, "eba:LE2_NonTradingBookPortion", contextId, "EUR", 
            formatAmount(exposure.nonTradingBookPortion().value()));
        
        // Percentage of eligible capital
        addFact(document, xbrl, "eba:LE2_PercentageOfCapital", contextId, "pure", 
            formatPercentage(exposure.percentageOfCapital().value()));
    }
    
    /**
     * Add a fact element to the XBRL document
     */
    private void addFact(Document document, Element parent, String elementName, 
                        String contextRef, String unitRef, String value) {
        Element fact = document.createElementNS(EBA_NAMESPACE, elementName);
        fact.setAttribute("contextRef", contextRef);
        
        if (unitRef != null) {
            fact.setAttribute("unitRef", unitRef);
        }
        
        fact.setTextContent(value);
        parent.appendChild(fact);
    }
    
    /**
     * Format amount for XBRL (2 decimal places)
     */
    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString();
    }
    
    /**
     * Format percentage for XBRL (divide by 100 for pure number)
     */
    private String formatPercentage(BigDecimal percentage) {
        return percentage.divide(new BigDecimal("100"), 4, BigDecimal.ROUND_HALF_UP).toPlainString();
    }
    
    /**
     * Sanitize string for XML element names (remove spaces, special chars)
     */
    private String sanitizeForXml(String value) {
        return value.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
    
    /**
     * Convert Document to formatted XML string
     */
    public String documentToString(Document document) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            
            return writer.toString();
        } catch (Exception e) {
            throw new XbrlGenerationException("Failed to convert document to string", e);
        }
    }
}
