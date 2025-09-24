package br.com.nfe.processor.adapter.in.web;

import br.com.nfe.processor.adapter.in.web.dto.BatchCreatedResponse;
import br.com.nfe.processor.adapter.in.web.dto.BatchStatsResponse;
import br.com.nfe.processor.adapter.in.web.dto.BatchSummaryResponse;
import br.com.nfe.processor.adapter.in.web.dto.IssueResponse;
import br.com.nfe.processor.adapter.in.web.dto.ValidationEntryResponse;
import br.com.nfe.processor.adapter.out.excel.ExcelExportService;
import br.com.nfe.processor.core.domain.model.Batch;
import br.com.nfe.processor.core.domain.model.BatchStatus;
import br.com.nfe.processor.core.domain.model.ValidationReport;
import br.com.nfe.processor.core.domain.repository.BatchRepository;
import br.com.nfe.processor.core.domain.service.ZipIngestionService;
import br.com.nfe.processor.exception.ConflictException;
import br.com.nfe.processor.exception.NotFoundException;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/batches")
@Validated
public class BatchController {

    private final ZipIngestionService zipIngestionService;
    private final BatchRepository batchRepository;
    private final ExcelExportService excelExportService;

    public BatchController(
            ZipIngestionService zipIngestionService,
            BatchRepository batchRepository,
            ExcelExportService excelExportService) {
        this.zipIngestionService = zipIngestionService;
        this.batchRepository = batchRepository;
        this.excelExportService = excelExportService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BatchCreatedResponse createBatch(
            @RequestParam("file") @NotNull MultipartFile file,
            @RequestParam(value = "ocr", defaultValue = "false") boolean ocr) {
        Batch batch = zipIngestionService.ingest(file, ocr);
        return new BatchCreatedResponse(batch.getId(), batch.getStatus(), batch.getReceivedAt());
    }

    @GetMapping("/{id}")
    public BatchSummaryResponse getBatch(@PathVariable("id") String id) {
        Batch batch = batchRepository.findWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException("Lote não encontrado"));
        BatchStatsResponse stats = new BatchStatsResponse(
                defaultInt(batch.getInvoicesTotal()),
                defaultInt(batch.getInvoicesOk()),
                defaultInt(batch.getInvoicesWithIssues()),
                defaultLong(batch.getProcessingMsP95()));
        List<IssueResponse> issueResponses = batch.getIssues().stream()
                .map(issue -> new IssueResponse(issue.getType(), issue.getSeverity(), issue.getDetail()))
                .collect(Collectors.toList());
        List<ValidationEntryResponse> validations = batch.getValidations().stream()
                .limit(10)
                .map(this::toValidationEntry)
                .collect(Collectors.toList());
        return new BatchSummaryResponse(batch.getId(), batch.getStatus(), stats, issueResponses, validations);
    }

    @GetMapping("/{id}/export.xlsx")
    public ResponseEntity<byte[]> exportBatch(@PathVariable("id") String id) {
        Batch batch = batchRepository.findWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException("Lote não encontrado"));
        if (batch.getStatus() != BatchStatus.DONE) {
            throw new ConflictException("Lote ainda em processamento");
        }
        byte[] file = excelExportService.export(batch);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=nfe-batch.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }

    private ValidationEntryResponse toValidationEntry(ValidationReport report) {
        return new ValidationEntryResponse(report.getRule(), report.getResult(), report.getMessage());
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }
}
