package br.com.nfe.processor.core.domain.service;

import br.com.nfe.processor.adapter.out.ocr.OcrAdapter;
import br.com.nfe.processor.adapter.out.sefaz.SefazClient;
import br.com.nfe.processor.adapter.out.sefaz.SefazStatus;
import br.com.nfe.processor.core.domain.model.Batch;
import br.com.nfe.processor.core.domain.model.BatchStatus;
import br.com.nfe.processor.core.domain.model.Issue;
import br.com.nfe.processor.core.domain.model.IssueSeverity;
import br.com.nfe.processor.core.domain.model.IssueType;
import br.com.nfe.processor.core.domain.model.Invoice;
import br.com.nfe.processor.core.domain.model.ValidationReport;
import br.com.nfe.processor.core.domain.repository.BatchRepository;
import br.com.nfe.processor.core.domain.service.dto.ParsedInvoice;
import br.com.nfe.processor.core.domain.service.dto.ValidationResult;
import br.com.nfe.processor.core.domain.service.event.BatchProcessingRequestedEvent;
import br.com.nfe.processor.exception.UnprocessableEntityException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class BatchProcessingListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchProcessingListener.class);

    private final BatchRepository batchRepository;
    private final XmlParserService xmlParserService;
    private final FiscalValidationService fiscalValidationService;
    private final AnomalyService anomalyService;
    private final OcrAdapter ocrAdapter;
    private final SefazClient sefazClient;

    BatchProcessingListener(
            BatchRepository batchRepository,
            XmlParserService xmlParserService,
            FiscalValidationService fiscalValidationService,
            AnomalyService anomalyService,
            OcrAdapter ocrAdapter,
            SefazClient sefazClient) {
        this.batchRepository = batchRepository;
        this.xmlParserService = xmlParserService;
        this.fiscalValidationService = fiscalValidationService;
        this.anomalyService = anomalyService;
        this.ocrAdapter = ocrAdapter;
        this.sefazClient = sefazClient;
    }

    @Async
    @EventListener
    @Transactional
    public void handleBatchProcessingRequested(BatchProcessingRequestedEvent event) {
        Batch batch = batchRepository.findById(event.batchId())
                .orElseThrow(() -> new IllegalStateException("Batch não encontrado: " + event.batchId()));
        long start = System.currentTimeMillis();
        try {
            batch.setStatus(BatchStatus.PROCESSING);
            batchRepository.save(batch);

            List<CompletableFuture<InvoicePayload>> payloadFutures = new ArrayList<>();
            for (BatchProcessingRequestedEvent.InvoiceSource source : event.sources()) {
                if (source.requiresOcr()) {
                    payloadFutures.add(resolveOcrSource(source));
                } else {
                    payloadFutures.add(CompletableFuture.completedFuture(
                            InvoicePayload.success(source.xmlContent(), false, source.name())));
                }
            }

            CompletableFuture<Void> all = CompletableFuture.allOf(payloadFutures.toArray(new CompletableFuture[0]));
            all.join();

            List<InvoicePayload> payloads = payloadFutures.stream().map(CompletableFuture::join).toList();
            payloads.stream()
                    .filter(InvoicePayload::isFailure)
                    .forEach(payload -> {
                        Issue issue = buildOcrFailureIssue(batch, payload.sourceName(), payload.failureReason());
                        batch.getIssues().add(issue);
                    });

            List<InvoicePayload> successfulPayloads = payloads.stream()
                    .filter(payload -> payload.xmlContent().isPresent())
                    .toList();
            if (successfulPayloads.isEmpty()) {
                throw new UnprocessableEntityException("Nenhum XML de NFe encontrado no lote");
            }

            successfulPayloads.forEach(payload -> {
                ParsedInvoice parsed = xmlParserService.parse(payload.xmlContent().orElseThrow());
                buildInvoice(batch, parsed, payload.ocrProcessed());
            });

            finalizeBatch(batch, start);
            LOGGER.info(
                    "Lote {} processado com {} notas ({} via OCR)",
                    batch.getId(),
                    batch.getInvoices().size(),
                    successfulPayloads.stream().filter(InvoicePayload::ocrProcessed).count());
        } catch (RuntimeException ex) {
            batch.setStatus(BatchStatus.FAILED);
            batchRepository.save(batch);
            LOGGER.error("Falha ao processar lote {}", batch.getId(), ex);
        }
    }

    private CompletableFuture<InvoicePayload> resolveOcrSource(BatchProcessingRequestedEvent.InvoiceSource source) {
        return ocrAdapter.extractXml(source.binaryContent()).handle((optional, throwable) -> {
            if (throwable != null) {
                Throwable cause = throwable instanceof CompletionException ? throwable.getCause() : throwable;
                String message = cause != null && cause.getMessage() != null
                        ? cause.getMessage()
                        : cause != null ? cause.getClass().getSimpleName() : "Erro desconhecido";
                LOGGER.error("Falha ao executar OCR para {}", source.name(), cause);
                return InvoicePayload.failure(source.name(), "Erro técnico no OCR: " + message);
            }
            if (optional.isEmpty()) {
                return InvoicePayload.failure(
                        source.name(), "Campos obrigatórios da NFe não foram identificados pelo OCR");
            }
            return InvoicePayload.success(optional.get(), true, source.name());
        });
    }

    private void buildInvoice(Batch batch, ParsedInvoice parsed, boolean ocrProcessed) {
        Invoice invoice = new Invoice();
        invoice.setBatch(batch);
        invoice.setAccessKey(parsed.getAccessKey());
        invoice.setEmitterName(parsed.getEmitterName());
        invoice.setEmitterTaxId(parsed.getEmitterTaxId());
        invoice.setRecipientName(parsed.getRecipientName());
        invoice.setRecipientTaxId(parsed.getRecipientTaxId());
        invoice.setItemCount(parsed.getItemCount());
        invoice.setTotalAmount(parsed.getTotalAmount());
        invoice.setProductsAmount(parsed.getProductsAmount());
        invoice.setIcmsAmount(parsed.getIcmsAmount());
        invoice.setIpiAmount(parsed.getIpiAmount());
        invoice.setIssAmount(parsed.getIssAmount() != null ? parsed.getIssAmount() : BigDecimal.ZERO);
        invoice.setCfop(parsed.getCfop());
        invoice.setOcrProcessed(ocrProcessed);

        List<ValidationResult> validations = fiscalValidationService.validate(invoice);
        validations.forEach(result -> {
            ValidationReport report = toValidationReport(batch, invoice, result);
            batch.getValidations().add(report);
            invoice.getValidations().add(report);
        });

        SefazStatus sefazStatus = sefazClient.checkStatus(invoice.getAccessKey());
        anomalyService.detect(batch, invoice, validations, sefazStatus)
                .forEach(issue -> {
                    batch.getIssues().add(issue);
                    invoice.getIssues().add(issue);
                });

        if (invoice.isOcrProcessed()) {
            Issue ocrIssue = buildOcrIssue(batch, invoice);
            batch.getIssues().add(ocrIssue);
            invoice.getIssues().add(ocrIssue);
        }

        batch.getInvoices().add(invoice);
    }

    private Issue buildOcrIssue(Batch batch, Invoice invoice) {
        Issue issue = new Issue();
        issue.setBatch(batch);
        issue.setInvoice(invoice);
        issue.setType(IssueType.OCR_REQUIRED);
        issue.setSeverity(IssueSeverity.MEDIUM);
        issue.setDetail("Dados extraídos via OCR, verificação manual recomendada");
        return issue;
    }

    private Issue buildOcrFailureIssue(Batch batch, String sourceName, String message) {
        Issue issue = new Issue();
        issue.setBatch(batch);
        issue.setType(IssueType.OCR_EXTRACTION_FAILED);
        issue.setSeverity(IssueSeverity.HIGH);
        issue.setDetail("Falha ao extrair XML do arquivo " + sourceName + ": " + message);
        return issue;
    }

    private ValidationReport toValidationReport(Batch batch, Invoice invoice, ValidationResult validationResult) {
        ValidationReport report = new ValidationReport();
        report.setBatch(batch);
        report.setInvoice(invoice);
        report.setRule(validationResult.getRule());
        report.setResult(validationResult.getResult());
        report.setMessage(validationResult.getMessage());
        return report;
    }

    private void finalizeBatch(Batch batch, long start) {
        long duration = System.currentTimeMillis() - start;
        batch.setCompletedAt(Instant.now());
        batch.setProcessingMsP95(Math.max(120L, duration));
        int totalInvoices = batch.getInvoices().size();
        batch.setInvoicesTotal(totalInvoices);
        long invoicesWithIssues = batch.getInvoices().stream().filter(invoice -> !invoice.getIssues().isEmpty()).count();
        batch.setInvoicesWithIssues((int) invoicesWithIssues);
        batch.setInvoicesOk(totalInvoices - (int) invoicesWithIssues);
        batch.setStatus(BatchStatus.DONE);
        batchRepository.save(batch);
    }

    private record InvoicePayload(Optional<String> xmlContent, boolean ocrProcessed, String sourceName, String failureReason) {
        static InvoicePayload success(String xmlContent, boolean ocrProcessed, String sourceName) {
            return new InvoicePayload(Optional.of(xmlContent), ocrProcessed, sourceName, null);
        }

        static InvoicePayload failure(String sourceName, String failureReason) {
            return new InvoicePayload(Optional.empty(), true, sourceName, failureReason);
        }

        boolean isFailure() {
            return xmlContent.isEmpty();
        }
    }
}
