package com.bcbs239.regtech.reportgeneration.infrastructure.util;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.reportgeneration.application.util.XmlSerializer;

/**
 * Infrastructure implementation delegating to the XmlUtils helper.
 */
@Component
public class XmlSerializerImpl implements XmlSerializer {

    @Override
    public Result<String> convertDocumentToString(Document document) {
        return XmlUtils.convertDocumentToString(document);
    }
}
