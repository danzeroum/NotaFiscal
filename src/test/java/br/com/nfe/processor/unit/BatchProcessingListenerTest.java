package br.com.nfe.processor.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.nfe.processor.adapter.out.ocr.OcrAdapter;
import br.com.nfe.processor.adapter.out.sefaz.SefazClient;
import br.com.nfe.processor.adapter.out.sefaz.SefazStatus;
import br.com.nfe.processor.core.domain.model.Batch;
import br.com.nfe.processor.core.domain.model.BatchStatus;
import br.com.nfe.processor.core.domain.model.Issue;
import br.com.nfe.processor.core.domain.model.IssueSeverity;
import br.com.nfe.processor.core.domain.model.IssueType;
import br.com.nfe.processor.core.domain.model.Invoice;
import br.com.nfe.processor.core.domain.model.ValidationResultType;
import br.com.nfe.processor.core.domain.repository.BatchRepository;
import br.com.nfe.processor.core.domain.service.AnomalyService;
import br.com.nfe.processor.core.domain.service.BatchProcessingListener;
import br.com.nfe.processor.core.domain.service.FiscalValidationService;
import br.com.nfe.processor.core.domain.service.XmlParserService;
import br.com.nfe.processor.core.domain.service.dto.ParsedInvoice;
import br.com.nfe.processor.core.domain.service.dto.ValidationResult;
import br.com.nfe.processor.core.domain.service.event.BatchProcessingRequestedEvent;
import br.com.nfe.processor.core.domain.valueobject.Cnpj;
import br.com.nfe.processor.infrastructure.metrics.NfeMetricsService;
import io.micrometer.core.instrument.Timer;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BatchProcessingListenerTest {

    @Mock
    private BatchRepository batchRepository;
    @Mock
    private XmlParserService xmlParserService;
    @Mock
    private FiscalValidationService fiscalValidationService;
    @Mock
    private AnomalyService anomalyService;
    @Mock
    private OcrAdapter ocrAdapter;
    @Mock
    private SefazClient sefazClient;
    @Mock
    private NfeMetricsService metricsService;

    private BatchProcessingListener listener;
    private Batch batch;

    @BeforeEach
    void setUp() {
        listener = new BatchProcessingListener(
                batchRepository,
                xmlParserService,
                fiscalValidationService,
                anomalyService,
                ocrAdapter,
                sefazClient,
                metricsService);

        batch = new Batch();
        batch.setId("b_test");
        batch.setStatus(BatchStatus.RECEIVED);
        batch.setReceivedAt(Instant.now());

        when(batchRepository.findById("b_test")).thenReturn(Optional.of(batch));
        when(batchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(xmlParserService.parse(any())).thenReturn(parsedInvoice());
        when(fiscalValidationService.validate(any()))
                .thenReturn(List.of(new ValidationResult("TOTALS", ValidationResultType.OK, "ok")));
        when(anomalyService.detect(any(), any(), any(), any())).thenReturn(List.of());
        when(sefazClient.checkStatus(any())).thenReturn(SefazStatus.AUTORIZADA);
        when(metricsService.startTimer()).thenReturn(Timer.start());
    }

    @Test
    void shouldProcessBatchAsynchronouslyWhenEventIsReceived() throws Exception {
        when(ocrAdapter.extractXml(any())).thenReturn(CompletableFuture.completedFuture(Optional.of(sampleXml())));

        BatchProcessingRequestedEvent event = new BatchProcessingRequestedEvent(
                batch.getId(),
                List.of(
                        BatchProcessingRequestedEvent.InvoiceSource.xml("nota.xml", sampleXml()),
                        BatchProcessingRequestedEvent.InvoiceSource.ocr("imagem.pdf", "pdf".getBytes())));

        listener.handleBatchProcessingRequested(event);

        assertThat(batch.getStatus()).isEqualTo(BatchStatus.DONE);
        assertThat(batch.getInvoices()).hasSize(2);
        long ocrInvoices = batch.getInvoices().stream().filter(Invoice::isOcrProcessed).count();
        assertThat(ocrInvoices).isEqualTo(1);
        assertThat(batch.getIssues())
                .extracting(Issue::getDetail)
                .contains("Dados extraídos via OCR, verificação manual recomendada");
    }

    @Test
    void shouldCreateIssueAndContinueWhenOcrFails() throws Exception {
        when(ocrAdapter.extractXml(any())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        BatchProcessingRequestedEvent event = new BatchProcessingRequestedEvent(
                batch.getId(),
                List.of(
                        BatchProcessingRequestedEvent.InvoiceSource.xml("nota.xml", sampleXml()),
                        BatchProcessingRequestedEvent.InvoiceSource.ocr("imagem.pdf", "pdf".getBytes())));

        listener.handleBatchProcessingRequested(event);

        assertThat(batch.getStatus()).isEqualTo(BatchStatus.DONE);
        assertThat(batch.getInvoices()).hasSize(1);
        assertThat(batch.getIssues())
                .anyMatch(issue -> issue.getType() == IssueType.OCR_EXTRACTION_FAILED
                        && issue.getSeverity() == IssueSeverity.HIGH);
    }

    private ParsedInvoice parsedInvoice() {
        return new ParsedInvoice(
                "12345678901234567890123456789012345678901234",
                "Emitente",
                new Cnpj("12345678000195"),
                "Destinatario",
                "45678912301",
                1,
                BigDecimal.TEN,
                BigDecimal.TEN,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "5102");
    }

    private String sampleXml() throws Exception {
        return Files.readString(Path.of("samples/xml/ok-01.xml"));
    }
}
