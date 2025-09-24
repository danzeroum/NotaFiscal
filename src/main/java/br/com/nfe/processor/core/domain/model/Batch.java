package br.com.nfe.processor.core.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "batches")
public class Batch {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BatchStatus status;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "processing_ms_p95")
    private Long processingMsP95;

    @Column(name = "invoices_total")
    private Integer invoicesTotal;

    @Column(name = "invoices_ok")
    private Integer invoicesOk;

    @Column(name = "invoices_with_issues")
    private Integer invoicesWithIssues;

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Invoice> invoices = new ArrayList<>();

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Issue> issues = new ArrayList<>();

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ValidationReport> validations = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public BatchStatus getStatus() {
        return status;
    }

    public void setStatus(BatchStatus status) {
        this.status = status;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Long getProcessingMsP95() {
        return processingMsP95;
    }

    public void setProcessingMsP95(Long processingMsP95) {
        this.processingMsP95 = processingMsP95;
    }

    public Integer getInvoicesTotal() {
        return invoicesTotal;
    }

    public void setInvoicesTotal(Integer invoicesTotal) {
        this.invoicesTotal = invoicesTotal;
    }

    public Integer getInvoicesOk() {
        return invoicesOk;
    }

    public void setInvoicesOk(Integer invoicesOk) {
        this.invoicesOk = invoicesOk;
    }

    public Integer getInvoicesWithIssues() {
        return invoicesWithIssues;
    }

    public void setInvoicesWithIssues(Integer invoicesWithIssues) {
        this.invoicesWithIssues = invoicesWithIssues;
    }

    public List<Invoice> getInvoices() {
        return invoices;
    }

    public List<Issue> getIssues() {
        return issues;
    }

    public List<ValidationReport> getValidations() {
        return validations;
    }
}
