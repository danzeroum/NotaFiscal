package br.com.nfe.processor.unit;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.nfe.processor.adapter.out.excel.ExcelExportService;
import br.com.nfe.processor.core.domain.model.Batch;
import br.com.nfe.processor.core.domain.model.BatchStatus;
import br.com.nfe.processor.core.domain.model.Invoice;
import br.com.nfe.processor.core.domain.model.Issue;
import br.com.nfe.processor.core.domain.model.IssueSeverity;
import br.com.nfe.processor.core.domain.model.IssueType;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExcelExportServiceTest {

    private ExcelExportService service;
    private Batch batch;

    @BeforeEach
    void setUp() {
        service = new ExcelExportService();
        batch = new Batch();
        batch.setId("b_excel");
        batch.setStatus(BatchStatus.DONE);
        batch.setReceivedAt(Instant.now());
    }

    @Test
    void shouldExportWorkbookWithInvoiceData() throws Exception {
        batch.getInvoices().add(invoice("Empresa", "Cliente"));
        byte[] bytes = service.export(batch);
        assertThat(bytes).isNotEmpty();

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row row = sheet.getRow(1);
            assertThat(row.getCell(1).getStringCellValue()).isEqualTo("Empresa");
            assertThat(row.getCell(2).getStringCellValue()).isEqualTo("Cliente");
        }
    }

    @Test
    void shouldIncludeIssuesInErrorColumns() throws Exception {
        Invoice invoice = invoice("Emit", "Dest");
        Issue issue = new Issue();
        issue.setBatch(batch);
        issue.setInvoice(invoice);
        issue.setType(IssueType.TOTALS_MISMATCH);
        issue.setSeverity(IssueSeverity.HIGH);
        issue.setDetail("Erro grave");
        invoice.getIssues().add(issue);
        batch.getInvoices().add(invoice);

        byte[] bytes = service.export(batch);
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row row = sheet.getRow(1);
            assertThat(row.getCell(8).getStringCellValue()).contains("Erro grave");
            assertThat(row.getCell(9).getStringCellValue()).isBlank();
        }
    }

    private Invoice invoice(String emitter, String recipient) {
        Invoice invoice = new Invoice();
        invoice.setAccessKey("chave");
        invoice.setEmitterName(emitter);
        invoice.setRecipientName(recipient);
        invoice.setItemCount(1);
        invoice.setTotalAmount(BigDecimal.ONE);
        invoice.setProductsAmount(BigDecimal.ONE);
        invoice.setIcmsAmount(BigDecimal.ZERO);
        invoice.setIpiAmount(BigDecimal.ZERO);
        invoice.setIssAmount(BigDecimal.ZERO);
        return invoice;
    }
}
