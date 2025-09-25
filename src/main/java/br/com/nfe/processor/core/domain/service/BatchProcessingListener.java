package br.com.nfe.processor.core.domain.service;

import br.com.nfe.processor.adapter.out.ocr.OcrAdapter;
import br.com.nfe.processor.adapter.out.sefaz.SefazVerificationClient;
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
    private final SefazVerificationClient sefazVerificationClient;

    BatchProcessingListener(
            BatchRepository batchRepository,
            XmlParserService xmlParserService,
            FiscalValidationService fiscalValidationService,
            AnomalyService anomalyService,
            OcrAdapter ocrAdapter,
            SefazVerificationClient sefazVerificationClient) {
        this.batchRepository = batchRepository;
        this.xmlParserService = xmlParserService;
        this.fiscalValidationService = fiscalValidationService;
        this.anomalyService = anomalyService;
        this.ocrAdapter = ocrAdapter;
        this.sefazVerificationClient = sefazVerificationClient;
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
                            new InvoicePayload(source.xmlContent(), false)));
                }
            }

            CompletableFuture<Void> all = CompletableFuture.allOf(payloadFutures.toArray(new CompletableFuture[0]));
            all.join();

            List<InvoicePayload> payloads = payloadFutures.stream().map(this::joinPayload).toList();
            if (payloads.isEmpty()) {
                throw new UnprocessableEntityException("Nenhum XML de NFe encontrado no lote");
            }

            payloads.forEach(payload -> {
                ParsedInvoice parsed = xmlParserService.parse(payload.xmlContent());
                buildInvoice(batch, parsed, payload.ocrProcessed());
            });

            finalizeBatch(batch, start);
            LOGGER.info(
                    "Lote {} processado com {} notas ({} via OCR)",
                    batch.getId(),
                    batch.getInvoices().size(),
                    payloads.stream().filter(InvoicePayload::ocrProcessed).count());
        } catch (RuntimeException ex) {
            batch.setStatus(BatchStatus.FAILED);
            batchRepository.save(batch);
            LOGGER.error("Falha ao processar lote {}", batch.getId(), ex);
        }
    }

    private CompletableFuture<InvoicePayload> resolveOcrSource(BatchProcessingRequestedEvent.InvoiceSource source) {
        return ocrAdapter.extractXml(source.binaryContent()).thenApply(optional -> optional
                .map(xml -> new InvoicePayload(xml, true))
                .orElseThrow(() -> new UnprocessableEntityException(
                        "OCR não conseguiu extrair XML do arquivo " + source.name())));
    }

    private InvoicePayload joinPayload(CompletableFuture<InvoicePayload> future) {
        try {
            return future.join();
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new UnprocessableEntityException("Falha ao processar OCR", cause);
        }
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

        boolean sefazValid = sefazVerificationClient.isValidAccessKey(invoice.getAccessKey());
        anomalyService.detect(batch, invoice, validations, sefazValid)
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

    private record InvoicePayload(String xmlContent, boolean ocrProcessed) {}
}
