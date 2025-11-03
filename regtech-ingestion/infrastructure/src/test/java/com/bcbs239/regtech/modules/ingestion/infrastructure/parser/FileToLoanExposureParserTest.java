package com.bcbs239.regtech.modules.ingestion.infrastructure.parser;

import com.bcbs239.regtech.ingestion.infrastructure.parser.FileToLoanExposureParser;
import com.bcbs239.regtech.ingestion.domain.model.LoanExposure;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unified FileToLoanExposureParser tests")
class FileToLoanExposureParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FileToLoanExposureParser parser = new FileToLoanExposureParser(objectMapper);

    @Test
    @DisplayName("Parse loan exposures from JSON with capacity limit")
    void parseFromJsonWithLimit() throws Exception {
        ClassPathResource resource = new ClassPathResource("test-data/enhanced_daily_loans_2024_09_12.json");
        try (InputStream is = resource.getInputStream()) {
            List<LoanExposure> exposures = parser.parseLoanExposuresFromJson(is, 3);
            assertThat(exposures).hasSize(3);
            assertThat(exposures.get(0).loanId()).isEqualTo("LOAN001");
        }
    }

    @Test
    @DisplayName("Parse loan exposures from generated Excel with capacity limit")
    void parseFromExcelWithLimit() throws Exception {
        // create workbook
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("loans");
            Row header = sheet.createRow(0);
            String[] cols = new String[]{
                "loan_id","exposure_id","borrower_name","borrower_id","counterparty_lei",
                "loan_amount","gross_exposure_amount","net_exposure_amount","currency",
                "loan_type","sector","exposure_type","borrower_country","country_code"
            };
            for (int i = 0; i < cols.length; i++) header.createCell(i).setCellValue(cols[i]);

            for (int r = 1; r <= 5; r++) {
                Row row = sheet.createRow(r);
                row.createCell(0).setCellValue("L" + r);
                row.createCell(1).setCellValue("EXP_L" + r);
                row.createCell(2).setCellValue("Borrower " + r);
                row.createCell(3).setCellValue("B" + r);
                row.createCell(4).setCellValue("");
                row.createCell(5).setCellValue(1000 * r);
                row.createCell(6).setCellValue(1000.0 * r);
                row.createCell(7).setCellValue(900.0 * r);
                row.createCell(8).setCellValue("EUR");
                row.createCell(9).setCellValue("Type");
                row.createCell(10).setCellValue("Sector");
                row.createCell(11).setCellValue("ON_BALANCE");
                row.createCell(12).setCellValue("IT");
                row.createCell(13).setCellValue("IT");
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            byte[] bytes = out.toByteArray();

            try (InputStream is = new ByteArrayInputStream(bytes)) {
                List<LoanExposure> exposures = parser.parseLoanExposuresFromExcel(is, 4);
                assertThat(exposures).hasSize(4);
                assertThat(exposures.get(3).loanId()).isEqualTo("L4");
            }
        }
    }
}

