package com.bcbs239.regtech.reportgeneration.application.util;

import org.w3c.dom.Document;

import com.bcbs239.regtech.core.domain.shared.Result;

/**
 * Port for converting XML DOM Document to String for upload.
 * Implemented in infrastructure.
 */
public interface XmlSerializer {

    Result<String> convertDocumentToString(Document document);
}
