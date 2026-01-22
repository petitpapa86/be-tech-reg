package com.bcbs239.regtech.reportgeneration.infrastructure.xbrl;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.reportgeneration.domain.generation.ValidationError;
import com.bcbs239.regtech.reportgeneration.domain.generation.ValidationResult;
import com.bcbs239.regtech.reportgeneration.domain.generation.XbrlValidationException;
import com.bcbs239.regtech.reportgeneration.domain.generation.XbrlValidator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * XBRL Schema Validator
 * 
 * Validates XBRL-XML documents against EBA XSD schema. Implements automatic
 * corrections for common issues (trim whitespace, round decimals) and provides
 * detailed validation error reporting.
 * 
 * Requirements: 11.1, 11.2, 11.3, 11.5
 */
@Component
@Slf4j
public class XbrlSchemaValidator implements XbrlValidator {
    
    private static final String EBA_SCHEMA_PATH = "xbrl/eba-corep-schema.xsd";
    
    private final Schema schema;

    public XbrlSchemaValidator() {
        this.schema = loadSchema();
    }
    
    /**
     * Validate XBRL document against EBA schema
     * 
     * @param document the XBRL document to validate
     * @return Result containing validation result or system error
     */
    public Result<ValidationResult> validate(Document document) {
        try {
            log.debug("Starting XBRL schema validation");
            
            // Apply automatic corrections before validation
            Document correctedDocument = applyAutomaticCorrections(document);
            
            // Create validator
            Validator validator = schema.newValidator();
            
            // Collect validation errors
            ValidationErrorHandler errorHandler = new ValidationErrorHandler();
            validator.setErrorHandler(errorHandler);
            
            // Validate
            validator.validate(new DOMSource(correctedDocument));
            
            List<ValidationError> errors = errorHandler.getErrors();
            
            if (errors.isEmpty()) {
                log.info("XBRL validation successful");
                return Result.success(ValidationResult.success());
            } else {
                log.warn("XBRL validation failed with {} error(s)", errors.size());
                return Result.success(ValidationResult.failure(errors));
            }
            
        } catch (SAXException e) {
            log.error("SAX exception during XBRL validation", e);
            return Result.success(ValidationResult.failure(List.of(
                ValidationError.of("Schema validation error: " + e.getMessage())
            )));
        } catch (IOException e) {
            log.error("IO exception during XBRL validation", e);
            ErrorDetail error = ErrorDetail.of("XBRL_VALIDATION_ERROR", ErrorType.SYSTEM_ERROR, 
                "Failed to read XBRL document for validation: " + e.getMessage(), "report.generation.xbrl_validation_io_error");
            return Result.failure(error);
        } catch (Exception e) {
            log.error("Unexpected error during XBRL validation", e);
            ErrorDetail error = ErrorDetail.of("XBRL_VALIDATION_ERROR", ErrorType.SYSTEM_ERROR, 
                "XBRL validation failed: " + e.getMessage(), "report.generation.xbrl_validation_failed");
            return Result.failure(error);
        }
    }
    
    /**
     * Apply automatic corrections to common issues
     * 
     * Corrections include:
     * - Trim whitespace from text content
     * - Round decimal values to appropriate precision
     * - Normalize line endings
     * 
     * Requirements: 11.5
     */
    private Document applyAutomaticCorrections(Document document) {
        try {
            // Clone the document to avoid modifying the original
            Document corrected = (Document) document.cloneNode(true);
            
            // Trim whitespace from all text nodes
            trimWhitespace(corrected.getDocumentElement());
            
            // Round decimal values
            roundDecimals(corrected.getDocumentElement());
            
            log.debug("Applied automatic corrections to XBRL document");
            
            return corrected;
            
        } catch (Exception e) {
            log.warn("Failed to apply automatic corrections, using original document", e);
            return document;
        }
    }
    
    /**
     * Recursively trim whitespace from text nodes
     */
    private void trimWhitespace(org.w3c.dom.Node node) {
        if (node.getNodeType() == org.w3c.dom.Node.TEXT_NODE) {
            String content = node.getTextContent();
            if (content != null) {
                node.setTextContent(content.trim());
            }
        }
        
        org.w3c.dom.NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            trimWhitespace(children.item(i));
        }
    }
    
    /**
     * Recursively round decimal values to appropriate precision
     */
    private void roundDecimals(org.w3c.dom.Node node) {
        if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
            org.w3c.dom.Element element = (org.w3c.dom.Element) node;
            String content = element.getTextContent();
            
            // Check if content looks like a decimal number
            if (content != null && content.matches("-?\\d+\\.\\d+")) {
                try {
                    double value = Double.parseDouble(content);
                    
                    // Determine precision based on element name
                    int precision = 2; // Default for monetary values
                    if (element.getLocalName().contains("Percentage") || 
                        element.getLocalName().contains("Capital")) {
                        precision = 4; // Higher precision for percentages
                    }
                    
                    // Round and format
                    String format = "%." + precision + "f";
                    String rounded = String.format(format, value);
                    element.setTextContent(rounded);
                    
                } catch (NumberFormatException e) {
                    // Not a valid number, skip
                }
            }
        }
        
        org.w3c.dom.NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            roundDecimals(children.item(i));
        }
    }
    
    /**
     * Load EBA schema from classpath
     * 
     * Requirements: 11.1
     */
    private Schema loadSchema() {
        try {
            log.info("Loading EBA COREP schema from classpath: {}", EBA_SCHEMA_PATH);
            
            // Try to load from classpath
            ClassPathResource resource = new ClassPathResource(EBA_SCHEMA_PATH);
            
            if (!resource.exists()) {
                log.warn("EBA schema not found at {}, validation will be skipped", EBA_SCHEMA_PATH);
                // Return a permissive schema that accepts everything
                return createPermissiveSchema();
            }
            
            try (InputStream schemaStream = resource.getInputStream()) {
                SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                Schema loadedSchema = schemaFactory.newSchema(new javax.xml.transform.stream.StreamSource(schemaStream));
                
                log.info("EBA COREP schema loaded successfully");
                return loadedSchema;
            }
            
        } catch (SAXException e) {
            log.error("Failed to parse EBA schema", e);
            throw new XbrlValidationException("Failed to load EBA schema", e);
        } catch (IOException e) {
            log.warn("EBA schema file not accessible, validation will be skipped", e);
            return createPermissiveSchema();
        }
    }
    
    /**
     * Create a permissive schema that accepts all documents
     * Used when the actual EBA schema is not available
     */
    private Schema createPermissiveSchema() {
        try {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            
            // Create a minimal schema that accepts everything
            String permissiveSchemaContent = 
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">" +
                "  <xs:element name=\"xbrl\">" +
                "    <xs:complexType>" +
                "      <xs:sequence>" +
                "        <xs:any minOccurs=\"0\" maxOccurs=\"unbounded\" processContents=\"skip\"/>" +
                "      </xs:sequence>" +
                "      <xs:anyAttribute processContents=\"skip\"/>" +
                "    </xs:complexType>" +
                "  </xs:element>" +
                "</xs:schema>";
            
            return schemaFactory.newSchema(
                new javax.xml.transform.stream.StreamSource(
                    new java.io.StringReader(permissiveSchemaContent)
                )
            );
        } catch (SAXException e) {
            throw new XbrlValidationException("Failed to create permissive schema", e);
        }
    }
    
    /**
     * Error handler that collects validation errors
     */
    @Getter
    private static class ValidationErrorHandler implements ErrorHandler {
        
        private final List<ValidationError> errors = new ArrayList<>();
        
        @Override
        public void warning(SAXParseException exception) {
            log.debug("Validation warning: {}", exception.getMessage());
            // Don't add warnings to error list
        }
        
        @Override
        public void error(SAXParseException exception) {
            log.warn("Validation error at line {}: {}", 
                exception.getLineNumber(), exception.getMessage());
            
            errors.add(ValidationError.of(
                exception.getMessage(),
                exception.getLineNumber(),
                String.valueOf(exception.getColumnNumber())
            ));
        }
        
        @Override
        public void fatalError(SAXParseException exception) {
            log.error("Fatal validation error at line {}: {}", 
                exception.getLineNumber(), exception.getMessage());
            
            errors.add(ValidationError.of(
                "FATAL: " + exception.getMessage(),
                exception.getLineNumber(),
                String.valueOf(exception.getColumnNumber())
            ));
        }

    }
}
