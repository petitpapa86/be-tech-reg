package com.bcbs239.regtech.reportgeneration.infrastructure.util;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import org.w3c.dom.Document;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

public final class XmlUtils {

    private XmlUtils() {}

    /**
     * Converts a DOM Document to its String representation wrapped in a Result.
     * Returns Result.failure(...) on conversion errors.
     */
    public static Result<String> convertDocumentToString(Document document) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            return Result.success(writer.toString());
        } catch (Exception e) {
            ErrorDetail err = ErrorDetail.of(
                    "XML_CONVERSION_FAILED",
                    ErrorType.SYSTEM_ERROR,
                    "Failed to convert XML document to string: " + e.getMessage(),
                    "reportgeneration.xml.conversion_failed"
            );
            return Result.failure(err);
        }
    }
}
