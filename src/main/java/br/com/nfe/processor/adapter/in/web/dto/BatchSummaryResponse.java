package br.com.nfe.processor.adapter.in.web.dto;

import br.com.nfe.processor.core.domain.model.BatchStatus;
import java.time.Instant;
import java.util.List;

public class BatchSummaryResponse {
    private final String id;
    private final BatchStatus status;
    private final Instant receivedAt;
    private final Instant completedAt;
    private final BatchStatsResponse stats;
    private final List<IssueResponse> issuesSummary;
    private final List<ValidationEntryResponse> validationsSample;

    public BatchSummaryResponse(
            String id,
            BatchStatus status,
            Instant receivedAt,
            Instant completedAt,
            BatchStatsResponse stats,
            List<IssueResponse> issuesSummary,
            List<ValidationEntryResponse> validationsSample) {
        this.id = id;
        this.status = status;
        this.receivedAt = receivedAt;
        this.completedAt = completedAt;
        this.stats = stats;
        this.issuesSummary = issuesSummary;
        this.validationsSample = validationsSample;
    }

    public String getId() {
        return id;
    }

    public BatchStatus getStatus() {
        return status;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public BatchStatsResponse getStats() {
        return stats;
    }

    public List<IssueResponse> getIssuesSummary() {
        return issuesSummary;
    }

    public List<ValidationEntryResponse> getValidationsSample() {
        return validationsSample;
    }
}
