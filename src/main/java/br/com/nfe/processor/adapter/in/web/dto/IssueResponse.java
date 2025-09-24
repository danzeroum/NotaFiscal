package br.com.nfe.processor.adapter.in.web.dto;

import br.com.nfe.processor.core.domain.model.IssueSeverity;
import br.com.nfe.processor.core.domain.model.IssueType;

public class IssueResponse {
    private final IssueType type;
    private final IssueSeverity severity;
    private final String detail;

    public IssueResponse(IssueType type, IssueSeverity severity, String detail) {
        this.type = type;
        this.severity = severity;
        this.detail = detail;
    }

    public IssueType getType() {
        return type;
    }

    public IssueSeverity getSeverity() {
        return severity;
    }

    public String getDetail() {
        return detail;
    }
}
