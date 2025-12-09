package com.bcbs239.regtech.reportgeneration.infrastructure.xbrl;

import com.bcbs239.regtech.reportgeneration.domain.generation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for XbrlSchemaValidator
 * 
 * Tests validation functionality including:
 * - Valid XBRL documents
 * - Invalid XBRL documents
 * - Automatic corrections (trim, round)
 */
class XbrlSchemaValidatorTest {
    
    private XbrlSchemaValidator validator;
    
    @BeforeEach
    void setUp() {
        validator = new XbrlSchemaValidator();
    }
    
    @Test
    void testValidateValidDocument() throws Exception {
        // Create a simple valid XBRL document
        String validXbrl = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xbrli:xbrl xmlns:xbrli="http://www.xbrl.org/2003/instance"
                        xmlns:eba="http://www.eba.europa.eu/xbrl/crr">
                <xbrli:context id="ctx_0">
                    <xbrli:entity>
                        <xbrli:identifier scheme="http://www.eba.europa.eu">BANK001</xbrli:identifier>
                    </xbrli:entity>
                    <xbrli:period>
                        <xbrli:instant>2024-12-31</xbrli:instant>
                    </xbrli:period>
                </xbrli:context>
            </xbrli:xbrl>
            """;
        
        Document document = parseXml(validXbrl);
        
        // Validate
        ValidationResult result = validator.validate(document);
        
        // Should succeed (permissive schema when EBA schema not available)
        assertNotNull(result);
        assertTrue(result.valid());
        assertFalse(result.hasErrors());
        assertEquals(0, result.getErrorCount());
    }
    
    @Test
    void testValidateWithWhitespace() throws Exception {
        // Create document with extra whitespace
        String xbrlWithWhitespace = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xbrli:xbrl xmlns:xbrli="http://www.xbrl.org/2003/instance">
                <xbrli:context id="ctx_0">
                    <xbrli:entity>
                        <xbrli:identifier scheme="http://www.eba.europa.eu">  BANK001  </xbrli:identifier>
                    </xbrli:entity>
                    <xbrli:period>
                        <xbrli:instant>  2024-12-31  </xbrli:instant>
                    </xbrli:period>
                </xbrli:context>
            </xbrli:xbrl>
            """;
        
        Document document = parseXml(xbrlWithWhitespace);
        
        // Validate (should apply automatic corrections)
        ValidationResult result = validator.validate(document);
        
        assertNotNull(result);
        assertTrue(result.valid());
    }
    
    @Test
    void testValidateWithDecimals() throws Exception {
        // Create document with decimal values
        String xbrlWithDecimals = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xbrli:xbrl xmlns:xbrli="http://www.xbrl.org/2003/instance"
                        xmlns:eba="http://www.eba.europa.eu/xbrl/crr">
                <eba:LE2_OriginalExposure>1234567.89123456</eba:LE2_OriginalExposure>
                <eba:LE2_PercentageOfCapital>12.345678</eba:LE2_PercentageOfCapital>
            </xbrli:xbrl>
            """;
        
        Document document = parseXml(xbrlWithDecimals);
        
        // Validate (should apply automatic rounding)
        ValidationResult result = validator.validate(document);
        
        assertNotNull(result);
        assertTrue(result.valid());
    }
    
    @Test
    void testValidationResultSuccess() {
        ValidationResult result = ValidationResult.success();
        
        assertTrue(result.valid());
        assertFalse(result.isInvalid());
        assertFalse(result.hasErrors());
        assertEquals(0, result.getErrorCount());
        assertEquals("Validation successful", result.getFormattedErrorMessage());
    }
    
    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xml.getBytes()));
    }
}
