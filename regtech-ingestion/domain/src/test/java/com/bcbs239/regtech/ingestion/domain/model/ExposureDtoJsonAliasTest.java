package com.bcbs239.regtech.ingestion.domain.model;

import com.bcbs239.regtech.core.domain.shared.dto.ExposureDTO;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExposureDtoJsonAliasTest {

    @Test
    void shouldDeserializeExposureDtoFromCamelCaseAndSnakeCaseVariants() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);

        String json = "{"
            + "\"exposureId\":\"EXP-001\","
            + "\"product_Type\":\"Loan\","
            + "\"instrument_id\":\"INS-001\","
            + "\"instrument_type\":\"BOND\","
            + "\"counterparty_name\":\"Acme\","
            + "\"counterparty_id\":\"CP-1\","
            + "\"counterparty_lei\":\"LEI-1\","
            + "\"exposure_amount\":123.45,"
            + "\"currency\":\"EUR\","
            + "\"balance_sheet_type\":\"ASSET\","
            + "\"country_code\":\"DE\""
            + "}";

        ExposureDto dto = objectMapper.readValue(json, ExposureDto.class);
        assertEquals("EXP-001", dto.exposureId());
        assertEquals("Loan", dto.productType());
    }

    @Test
    void shouldDeserializeCoreExposureDTOFromCamelCaseAndSnakeCaseVariants() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);

        String json = "{"
            + "\"exposure_Id\":\"EXP-002\","
            + "\"productType\":\"Derivative\","
            + "\"instrumentId\":\"INS-002\","
            + "\"instrumentType\":\"DERIVATIVE\","
            + "\"counterpartyName\":\"Globex\","
            + "\"counterpartyId\":\"CP-2\","
            + "\"counterpartyLei\":\"LEI-2\","
            + "\"exposureAmount\":999.99,"
            + "\"currency\":\"USD\","
            + "\"balanceSheetType\":\"LIABILITY\","
            + "\"countryCode\":\"US\""
            + "}";

        ExposureDTO dto = objectMapper.readValue(json, ExposureDTO.class);
        assertEquals("EXP-002", dto.exposureId());
        assertEquals("Derivative", dto.productType());
        assertEquals(new BigDecimal("999.99"), dto.exposureAmount());
    }
}
