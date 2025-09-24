package br.com.nfe.processor.adapter.out.excel;

import br.com.nfe.processor.core.domain.model.Batch;
import br.com.nfe.processor.core.domain.model.Invoice;
import br.com.nfe.processor.core.domain.model.Issue;
import br.com.nfe.processor.core.domain.model.IssueSeverity;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class ExcelExportService {

    public byte[] export(Batch batch) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("NFe Batch");
            createHeader(sheet);
            int rowIndex = 1;
            for (Invoice invoice : batch.getInvoices()) {
                Row row = sheet.createRow(rowIndex++);
                fillRow(row, invoice);
            }
            for (int i = 0; i < 10; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao gerar planilha Excel", ex);
        }
    }

    private void createHeader(Sheet sheet) {
        Row header = sheet.createRow(0);
        String[] titles = new String[] {
            "chave_acesso",
            "emitente",
            "destinatario",
            "qtde_itens",
            "total",
            "icms",
            "ipi",
            "iss",
            "erros",
            "avisos"
        };
        for (int i = 0; i < titles.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(titles[i]);
        }
    }

    private void fillRow(Row row, Invoice invoice) {
        row.createCell(0).setCellValue(invoice.getAccessKey());
        row.createCell(1).setCellValue(invoice.getEmitterName());
        row.createCell(2).setCellValue(invoice.getRecipientName());
        row.createCell(3).setCellValue(invoice.getItemCount());
        row.createCell(4).setCellValue(invoice.getTotalAmount().doubleValue());
        row.createCell(5).setCellValue(invoice.getIcmsAmount().doubleValue());
        row.createCell(6).setCellValue(invoice.getIpiAmount().doubleValue());
        row.createCell(7).setCellValue(invoice.getIssAmount() != null ? invoice.getIssAmount().doubleValue() : 0d);

        List<Issue> issues = invoice.getIssues().stream()
                .sorted(Comparator.comparing(Issue::getSeverity))
                .collect(Collectors.toList());

        String errors = issues.stream()
                .filter(issue -> issue.getSeverity() == IssueSeverity.HIGH)
                .map(Issue::getDetail)
                .collect(Collectors.joining(" | "));
        String warnings = issues.stream()
                .filter(issue -> issue.getSeverity() != IssueSeverity.HIGH)
                .map(Issue::getDetail)
                .collect(Collectors.joining(" | "));

        row.createCell(8).setCellValue(errors);
        row.createCell(9).setCellValue(warnings);
    }
}
