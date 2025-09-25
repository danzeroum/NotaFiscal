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
import br.com.nfe.processor.exception.BadRequestException;
import br.com.nfe.processor.exception.UnprocessableEntityException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ZipIngestionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZipIngestionService.class);

    private final BatchRepository batchRepository;
    private final XmlParserService xmlParserService;
    private final FiscalValidationService fiscalValidationService;
    private final AnomalyService anomalyService;
    private final OcrAdapter ocrAdapter;
    private final SefazVerificationClient sefazVerificationClient;
    private final boolean ocrEnabled;

    public ZipIngestionService(
            BatchRepository batchRepository,
            XmlParserService xmlParserService,
            FiscalValidationService fiscalValidationService,
            AnomalyService anomalyService,
            OcrAdapter ocrAdapter,
            SefazVerificationClient sefazVerificationClient,
            @Value("${ocr.enabled:false}") boolean ocrEnabled) {
        this.batchRepository = batchRepository;
        this.xmlParserService = xmlParserService;
        this.fiscalValidationService = fiscalValidationService;
        this.anomalyService = anomalyService;
        this.ocrAdapter = ocrAdapter;
        this.sefazVerificationClient = sefazVerificationClient;
        this.ocrEnabled = ocrEnabled;
    }

    @Transactional
    public Batch ingest(MultipartFile file, boolean ocrRequested) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Arquivo ZIP é obrigatório");
        }
        if (file.getOriginalFilename() == null || !file.getOriginalFilename().endsWith(".zip")) {
            throw new BadRequestException("Apenas arquivos ZIP são suportados");
        }

        Batch batch = createBatch();
        long start = System.currentTimeMillis();
        try {
            batch.setStatus(BatchStatus.PROCESSING);
            batchRepository.save(batch);

            List<InvoicePayload> invoicePayloads = new ArrayList<>();
            List<CompletableFuture<InvoicePayload>> asyncPayloads = new ArrayList<>();
            try (ZipInputStream zipInputStream = new ZipInputStream(file.getInputStream())) {
                ZipEntry entry;
                while ((entry = zipInputStream.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    String name = entry.getName();
                    if (name.endsWith(".xml")) {
                        invoicePayloads.add(new InvoicePayload(readEntry(zipInputStream), false));
                        zipInputStream.closeEntry();
                    } else if (name.endsWith(".pdf") || name.endsWith(".png") || name.endsWith(".jpg")) {
                        asyncPayloads.add(handleImageEntry(zipInputStream, name, ocrRequested));
                        zipInputStream.closeEntry();
                    } else {
                        throw new UnprocessableEntityException("Extensão não suportada: " + name);
                    }
                }
            } catch (IOException ex) {
                throw new UnprocessableEntityException("Falha ao ler arquivo ZIP");
            }

            invoicePayloads.addAll(resolveAsyncOcrResults(asyncPayloads));

            if (invoicePayloads.isEmpty()) {
                throw new UnprocessableEntityException("Nenhum XML de NFe encontrado no lote");
            }

            processXmlInvoices(batch, invoicePayloads);
            finalizeBatch(batch, start);
            LOGGER.info("Lote {} processado com {} notas ({} via OCR)", batch.getId(), batch.getInvoices().size(), invoicePayloads.stream().filter(InvoicePayload::ocrProcessed).count());
            return batch;
        } catch (RuntimeException ex) {
            batch.setStatus(BatchStatus.FAILED);
            batchRepository.save(batch);
            throw ex;
        }
    }

    private Batch createBatch() {
        Batch batch = new Batch();
        batch.setId("b_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        batch.setStatus(BatchStatus.RECEIVED);
        batch.setReceivedAt(Instant.now());
        return batchRepository.save(batch);
    }

    private String readEntry(ZipInputStream inputStream) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    private CompletableFuture<InvoicePayload> handleImageEntry(
            ZipInputStream inputStream, String name, boolean ocrRequested) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int read;
        while ((read = inputStream.read(data)) != -1) {
            buffer.write(data, 0, read);
        }
        byte[] content = buffer.toByteArray();
        if (!ocrEnabled || !ocrRequested) {
            throw new UnprocessableEntityException("OCR desabilitado: PDF " + name + " não pode ser processado");
        }
        return ocrAdapter.extractXml(content).thenApply(optional -> optional
                .map(xml -> new InvoicePayload(xml, true))
                .orElseThrow(() -> new UnprocessableEntityException(
                        "OCR não conseguiu extrair XML do arquivo " + name)));
    }

    private List<InvoicePayload> resolveAsyncOcrResults(List<CompletableFuture<InvoicePayload>> asyncPayloads) {
        List<InvoicePayload> resolved = new ArrayList<>();
        for (CompletableFuture<InvoicePayload> future : asyncPayloads) {
            try {
                resolved.add(future.get());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new UnprocessableEntityException("Processamento OCR interrompido");
            } catch (ExecutionException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof UnprocessableEntityException exception) {
                    throw exception;
                }
                throw new UnprocessableEntityException("Falha ao processar OCR: " + cause.getMessage());
            }
        }
        return resolved;
    }

    private void processXmlInvoices(Batch batch, List<InvoicePayload> payloads) {
        payloads.forEach(payload -> {
            ParsedInvoice parsed = xmlParserService.parse(payload.xmlContent());
            buildInvoice(batch, parsed, payload.ocrProcessed());
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
