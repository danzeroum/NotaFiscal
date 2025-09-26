package br.com.nfe.processor.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.nfe.processor.adapter.in.web.BatchController;
import br.com.nfe.processor.adapter.out.excel.ExcelExportService;
import br.com.nfe.processor.core.domain.model.Batch;
import br.com.nfe.processor.core.domain.model.BatchStatus;
import br.com.nfe.processor.core.domain.model.Invoice;
import br.com.nfe.processor.core.domain.model.Issue;
import br.com.nfe.processor.core.domain.model.IssueType;
import br.com.nfe.processor.core.domain.model.ValidationResultType;
import br.com.nfe.processor.core.domain.repository.BatchRepository;
import br.com.nfe.processor.core.domain.service.AnomalyService;
import br.com.nfe.processor.core.domain.service.FiscalValidationService;
import br.com.nfe.processor.core.domain.service.XmlParserService;
import br.com.nfe.processor.core.domain.service.ZipIngestionService;
import br.com.nfe.processor.core.domain.service.dto.ValidationResult;
import br.com.nfe.processor.exception.BadRequestException;
import br.com.nfe.processor.exception.ConflictException;
import br.com.nfe.processor.exception.NotFoundException;
import br.com.nfe.processor.exception.UnprocessableEntityException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class MustHaveNegativeTests {

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ZipIngestionService zipIngestionServiceMock;

    @Mock
    private ExcelExportService excelExportService;

    private ZipIngestionService zipIngestionService;
    private XmlParserService xmlParserService;
    private FiscalValidationService fiscalValidationService;
    private AnomalyService anomalyService;
    private BatchController batchController;

    @BeforeEach
    void setUp() {
        when(batchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        zipIngestionService = new ZipIngestionService(batchRepository, eventPublisher, false);
        xmlParserService = new XmlParserService();
        fiscalValidationService = new FiscalValidationService();
        anomalyService = new AnomalyService();
        batchController = new BatchController(zipIngestionServiceMock, batchRepository, excelExportService);
    }

    @Test
    void shouldRejectNonZipFileUpload() {
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file", "notas.txt", "text/plain", "conteudo invalido".getBytes());

        assertThatThrownBy(() -> zipIngestionService.ingest(invalidFile, false))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Apenas arquivos ZIP");
    }

    @Test
    void shouldFailOnMalformedXmlParsing() {
        String malformedXml = "<nfe><broken>";

        assertThatThrownBy(() -> xmlParserService.parse(malformedXml))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("XML");
    }

    @Test
    void shouldReportTotalsMismatchDuringFiscalValidation() {
        Invoice invoice = new Invoice();
        invoice.setTotalAmount(new BigDecimal("120.00"));
        invoice.setProductsAmount(new BigDecimal("100.00"));
        invoice.setIcmsAmount(BigDecimal.ZERO);
        invoice.setIpiAmount(BigDecimal.ZERO);
        invoice.setIssAmount(BigDecimal.ZERO);
        invoice.setItemCount(1);

        List<ValidationResult> results = fiscalValidationService.validate(invoice);

        assertThat(results)
                .anySatisfy(result -> {
                    assertThat(result.getRule()).isEqualTo("TOTALS_RECONCILIATION");
                    assertThat(result.getResult()).isEqualTo(ValidationResultType.ERROR);
                });
    }

    @Test
    void shouldCreateIssuesForTotalsMismatchAndInvalidCfop() {
        Batch batch = new Batch();
        batch.setId("batch-1");
        batch.setStatus(BatchStatus.DONE);
        batch.setReceivedAt(Instant.now());

        Invoice invoice = new Invoice();
        invoice.setCfop(null);

        List<ValidationResult> validations = List.of(
                new ValidationResult("TOTALS_RECONCILIATION", ValidationResultType.ERROR, "Totais divergentes"));

        List<Issue> issues = anomalyService.detect(batch, invoice, validations, null);

        assertThat(issues)
                .anyMatch(issue -> issue.getType() == IssueType.TOTALS_MISMATCH);
        assertThat(issues)
                .anyMatch(issue -> issue.getType() == IssueType.CFOP_INVALID);
    }

    @Test
    void shouldFailExportWhenBatchNotFound() {
        when(batchRepository.findWithDetailsById("missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> batchController.exportBatch("missing"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Lote não encontrado");
    }

    @Test
    void shouldPreventExportWhenBatchStillProcessing() {
        Batch batch = new Batch();
        batch.setId("processing");
        batch.setStatus(BatchStatus.PROCESSING);
        batch.setReceivedAt(Instant.now());

        when(batchRepository.findWithDetailsById("processing"))
                .thenReturn(Optional.of(batch));

        assertThatThrownBy(() -> batchController.exportBatch("processing"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("processamento");
    }
}
