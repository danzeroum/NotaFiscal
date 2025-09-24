package br.com.nfe.processor.core.domain.service.dto;

import br.com.nfe.processor.core.domain.model.ValidationResultType;

public class ValidationResult {
    private final String rule;
    private final ValidationResultType result;
    private final String message;

    public ValidationResult(String rule, ValidationResultType result, String message) {
        this.rule = rule;
        this.result = result;
        this.message = message;
    }

    public String getRule() {
        return rule;
    }

    public ValidationResultType getResult() {
        return result;
    }

    public String getMessage() {
        return message;
    }
}
