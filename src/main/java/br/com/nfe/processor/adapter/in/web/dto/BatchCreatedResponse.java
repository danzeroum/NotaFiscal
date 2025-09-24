package br.com.nfe.processor.adapter.in.web.dto;

import br.com.nfe.processor.core.domain.model.BatchStatus;
import java.time.Instant;

public class BatchCreatedResponse {
    private final String id;
    private final BatchStatus status;
    private final Instant receivedAt;

    public BatchCreatedResponse(String id, BatchStatus status, Instant receivedAt) {
        this.id = id;
        this.status = status;
        this.receivedAt = receivedAt;
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
}
