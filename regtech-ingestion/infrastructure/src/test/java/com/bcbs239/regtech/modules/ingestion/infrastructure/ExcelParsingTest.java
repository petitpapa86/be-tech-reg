package com.bcbs239.regtech.modules.ingestion.infrastructure;

import com.bcbs239.regtech.ingestion.domain.model.DomainMapper;
import com.bcbs239.regtech.ingestion.domain.model.LoanExposure;
import com.bcbs239.regtech.ingestion.domain.model.ExposureDto;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Excel parsing unit tests")
class ExcelParsingTest {

    @Test
    @DisplayName("Should create Excel in memory, parse it and map rows to ExposureDto")
    void shouldParseExcelToExposureDtos() throws Exception {
        // Create workbook in memory
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("loans");

            // Header
            Row header = sheet.createRow(0);
            String[] cols = new String[]{
                "loan_id","exposure_id","borrower_name","borrower_id","counterparty_lei",
                "loan_amount","gross_exposure_amount","net_exposure_amount","currency",
                "loan_type","sector","exposure_type","borrower_country","country_code"
            };
            for (int i = 0; i < cols.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(cols[i]);
            }

            // Row 1 (LOAN_A)
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("LOAN_A");
            r1.createCell(1).setCellValue("EXP_A_2025");
            r1.createCell(2).setCellValue("Alpha Corp");
            r1.createCell(3).setCellValue("C123");
            r1.createCell(4).setCellValue("LEI123");
            r1.createCell(5).setCellValue(100000);
            r1.createCell(6).setCellValue(100000.00);
            r1.createCell(7).setCellValue(95000.00);
            r1.createCell(8).setCellValue("EUR");
            r1.createCell(9).setCellValue("Business Loan");
            r1.createCell(10).setCellValue("CORPORATE");
            r1.createCell(11).setCellValue("ON_BALANCE");
            r1.createCell(12).setCellValue("IT");
            r1.createCell(13).setCellValue("IT");

            // Row 2 (LOAN_B)
            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue("LOAN_B");
            r2.createCell(1).setCellValue("EXP_B_2025");
            r2.createCell(2).setCellValue("Beta LLC");
            r2.createCell(3).setCellValue("P456");
            r2.createCell(4).setCellValue("");
            r2.createCell(5).setCellValue(250000);
            r2.createCell(6).setCellValue(250000.00);
            r2.createCell(7).setCellValue(250000.00);
            r2.createCell(8).setCellValue("EUR");
            r2.createCell(9).setCellValue("Mortgage");
            r2.createCell(10).setCellValue("RETAIL");
            r2.createCell(11).setCellValue("ON_BALANCE");
            r2.createCell(12).setCellValue("DE");
            r2.createCell(13).setCellValue("DE");

            // Write workbook to bytes
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            byte[] bytes = out.toByteArray();

            // Parse workbook from bytes
            try (XSSFWorkbook readWb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
                XSSFSheet readSheet = readWb.getSheetAt(0);
                DataFormatter formatter = new DataFormatter();
                List<LoanExposureDto> exposures = new ArrayList<>();

                for (int r = 1; r <= readSheet.getLastRowNum(); r++) {
                    Row row = readSheet.getRow(r);
                    if (row == null) continue;

                    String loanId = formatter.formatCellValue(row.getCell(0));
                    String exposureId = formatter.formatCellValue(row.getCell(1));
                    String borrowerName = formatter.formatCellValue(row.getCell(2));
                    String borrowerId = formatter.formatCellValue(row.getCell(3));
                    String counterpartyLei = formatter.formatCellValue(row.getCell(4));
                    double loanAmount = Double.parseDouble(formatter.formatCellValue(row.getCell(5)));
                    double gross = Double.parseDouble(formatter.formatCellValue(row.getCell(6)));
                    double net = Double.parseDouble(formatter.formatCellValue(row.getCell(7)));
                    String currency = formatter.formatCellValue(row.getCell(8));
                    String loanType = formatter.formatCellValue(row.getCell(9));
                    String sector = formatter.formatCellValue(row.getCell(10));
                    String exposureType = formatter.formatCellValue(row.getCell(11));
                    String borrowerCountry = formatter.formatCellValue(row.getCell(12));
                    String countryCode = formatter.formatCellValue(row.getCell(13));

                    LoanExposureDto dto = new LoanExposureDto(
                        loanId, exposureId, borrowerName, borrowerId, counterpartyLei,
                        loanAmount, gross, net, currency, loanType, sector, exposureType,
                        borrowerCountry, countryCode
                    );
                    exposures.add(dto);
                }

                // Map to domain objects
                java.util.List<LoanExposure> models =
                    DomainMapper.toLoanExposureList(
                        exposures.toArray(new LoanExposureDto[0])
                    );

                // Assertions on domain objects
                assertThat(models).hasSize(2);
                LoanExposure a = models.get(0);
                LoanExposure b = models.get(1);

                assertThat(a.loanId()).isEqualTo("LOAN_A");
                assertThat(a.exposureId()).isEqualTo("EXP_A_2025");
                assertThat(a.netExposureAmount()).isEqualTo(95000.0);
                assertThat(a.counterpartyLei()).isEqualTo("LEI123");

                assertThat(b.loanId()).isEqualTo("LOAN_B");
                assertThat(b.counterpartyLei()).isEmpty();
                assertThat(b.borrowerCountry()).isEqualTo("DE");
            }
        }
    }
}

