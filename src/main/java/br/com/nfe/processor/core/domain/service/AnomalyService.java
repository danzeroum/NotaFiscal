package br.com.nfe.processor.core.domain.service;

import br.com.nfe.processor.adapter.out.sefaz.SefazStatus;
import br.com.nfe.processor.core.domain.model.Batch;
import br.com.nfe.processor.core.domain.model.Invoice;
import br.com.nfe.processor.core.domain.model.Issue;
import br.com.nfe.processor.core.domain.model.IssueSeverity;
import br.com.nfe.processor.core.domain.model.IssueType;
import br.com.nfe.processor.core.domain.model.ValidationResultType;
import br.com.nfe.processor.core.domain.service.dto.ValidationResult;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AnomalyService {

    public List<Issue> detect(
            Batch batch, Invoice invoice, List<ValidationResult> validations, SefazStatus sefazStatus) {
        List<Issue> issues = new ArrayList<>();
        validations.stream()
                .filter(result -> result.getResult() == ValidationResultType.ERROR)
                .forEach(result -> issues.add(buildValidationIssue(batch, invoice, result)));

        issues.addAll(buildSefazIssues(batch, invoice, sefazStatus));

        if (invoice.getCfop() == null || invoice.getCfop().length() != 4) {
            issues.add(createIssue(
                    batch, invoice, IssueType.CFOP_INVALID, IssueSeverity.MEDIUM, "CFOP ausente ou inválido"));
        }
        return issues;
    }

    private List<Issue> buildSefazIssues(Batch batch, Invoice invoice, SefazStatus status) {
        List<Issue> issues = new ArrayList<>();
        if (status == null) {
            return issues;
        }
        switch (status) {
            case AUTORIZADA -> {
            }
            case CANCELADA -> issues.add(createIssue(batch, invoice, IssueType.SEFAZ_KEY_INVALID, IssueSeverity.HIGH,
                    "Nota fiscal cancelada na SEFAZ"));
            case DENEGADA -> issues.add(createIssue(batch, invoice, IssueType.SEFAZ_KEY_INVALID, IssueSeverity.HIGH,
                    "Uso da chave negado pela SEFAZ"));
            case INEXISTENTE -> issues.add(createIssue(batch, invoice, IssueType.SEFAZ_KEY_INVALID, IssueSeverity.HIGH,
                    "Chave de acesso não encontrada na SEFAZ"));
            case INDISPONIVEL -> issues.add(createIssue(batch, invoice, IssueType.SEFAZ_KEY_INVALID, IssueSeverity.MEDIUM,
                    "SEFAZ indisponível no momento da consulta"));
        }
        return issues;
    }

    private Issue buildValidationIssue(Batch batch, Invoice invoice, ValidationResult result) {
        IssueType type = "TOTALS_RECONCILIATION".equals(result.getRule())
                ? IssueType.TOTALS_MISMATCH
                : IssueType.VALIDATION_RULE_FAILED;
        IssueSeverity severity = "TOTALS_RECONCILIATION".equals(result.getRule())
                ? IssueSeverity.HIGH
                : IssueSeverity.MEDIUM;
        return createIssue(batch, invoice, type, severity, result.getMessage());
    }

    private Issue createIssue(Batch batch, Invoice invoice, IssueType type, IssueSeverity severity, String detail) {
        Issue issue = new Issue();
        issue.setBatch(batch);
        issue.setInvoice(invoice);
        issue.setType(type);
        issue.setSeverity(severity);
        issue.setDetail(detail);
        return issue;
    }
}
