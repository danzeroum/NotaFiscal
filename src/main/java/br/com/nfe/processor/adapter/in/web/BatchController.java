package br.com.nfe.processor.adapter.in.web;

import br.com.nfe.processor.adapter.in.web.dto.BatchCreatedResponse;
import br.com.nfe.processor.adapter.in.web.dto.BatchStatsResponse;
import br.com.nfe.processor.adapter.in.web.dto.BatchSummaryResponse;
import br.com.nfe.processor.adapter.in.web.dto.IssueResponse;
import br.com.nfe.processor.adapter.in.web.dto.ValidationEntryResponse;
import br.com.nfe.processor.adapter.out.excel.ExcelExportService;
import br.com.nfe.processor.core.domain.model.Batch;
import br.com.nfe.processor.core.domain.model.BatchStatus;
import br.com.nfe.processor.core.domain.model.Issue;
import br.com.nfe.processor.core.domain.model.ValidationReport;
import br.com.nfe.processor.core.domain.repository.BatchRepository;
import br.com.nfe.processor.core.domain.service.ZipIngestionService;
import br.com.nfe.processor.exception.ConflictException;
import br.com.nfe.processor.exception.NotFoundException;
import jakarta.validation.constraints.NotNull;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

    @GetMapping
    public Page<BatchSummaryResponse> listBatches(
            @PageableDefault(sort = "receivedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return batchRepository.findAllBy(pageable).map(batch -> toBatchSummary(batch, 3, 0));
    }

    @GetMapping("/{id}")
    public BatchSummaryResponse getBatch(@PathVariable("id") String id) {
        Batch batch = batchRepository.findWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException("Lote não encontrado"));
        return toBatchSummary(batch, -1, 10);
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

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private BatchSummaryResponse toBatchSummary(Batch batch, int issueLimit, int validationLimit) {
        BatchStatsResponse stats = new BatchStatsResponse(
                defaultInt(batch.getInvoicesTotal()),
                defaultInt(batch.getInvoicesOk()),
                defaultInt(batch.getInvoicesWithIssues()),
                defaultLong(batch.getProcessingMsP95()));

        Stream<Issue> issueStream = batch.getIssues().stream()
                .sorted(Comparator.comparing(Issue::getSeverity).reversed());
        if (issueLimit >= 0) {
            issueStream = issueStream.limit(issueLimit);
        }
        List<IssueResponse> issueResponses = issueStream
                .map(issue -> new IssueResponse(issue.getType(), issue.getSeverity(), issue.getDetail()))
                .collect(Collectors.toList());

        Stream<ValidationReport> validationStream = batch.getValidations().stream();
        if (validationLimit >= 0) {
            validationStream = validationStream.limit(validationLimit);
        }
        List<ValidationEntryResponse> validations = validationStream
                .map(this::toValidationEntry)
                .collect(Collectors.toList());

        return new BatchSummaryResponse(
                batch.getId(),
                batch.getStatus(),
                batch.getReceivedAt(),
                batch.getCompletedAt(),
                stats,
                issueResponses,
                validations);
    }

    private ValidationEntryResponse toValidationEntry(ValidationReport report) {
        return new ValidationEntryResponse(report.getRule(), report.getResult(), report.getMessage());
    }
}
