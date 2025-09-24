package br.com.nfe.processor.adapter.in.web.dto;

public class BatchStatsResponse {
    private final int invoicesTotal;
    private final int invoicesOk;
    private final int invoicesWithIssues;
    private final long processingMsP95;

    public BatchStatsResponse(int invoicesTotal, int invoicesOk, int invoicesWithIssues, long processingMsP95) {
        this.invoicesTotal = invoicesTotal;
        this.invoicesOk = invoicesOk;
        this.invoicesWithIssues = invoicesWithIssues;
        this.processingMsP95 = processingMsP95;
    }

    public int getInvoicesTotal() {
        return invoicesTotal;
    }

    public int getInvoicesOk() {
        return invoicesOk;
    }

    public int getInvoicesWithIssues() {
        return invoicesWithIssues;
    }

    public long getProcessingMsP95() {
        return processingMsP95;
    }
}
