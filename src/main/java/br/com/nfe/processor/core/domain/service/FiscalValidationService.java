package br.com.nfe.processor.core.domain.service;

import br.com.nfe.processor.core.domain.model.Invoice;
import br.com.nfe.processor.core.domain.model.ValidationResultType;
import br.com.nfe.processor.core.domain.service.dto.ValidationResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FiscalValidationService {

    public List<ValidationResult> validate(Invoice invoice) {
        List<ValidationResult> results = new ArrayList<>();
        results.add(validateTotals(invoice));
        results.add(validateNonNegative("ICMS_BASE", invoice.getIcmsAmount()));
        results.add(validateNonNegative("IPI_BASE", invoice.getIpiAmount()));
        results.add(validateNonNegative("ISS_BASE", invoice.getIssAmount()));
        results.add(validateItemCount(invoice));
        return results;
    }

    private ValidationResult validateTotals(Invoice invoice) {
        BigDecimal total = invoice.getTotalAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal products = invoice.getProductsAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal delta = total.subtract(products).abs();
        if (delta.compareTo(new BigDecimal("0.01")) <= 0) {
            return new ValidationResult("TOTALS_RECONCILIATION", ValidationResultType.OK, "Totais consistentes");
        }
        return new ValidationResult(
                "TOTALS_RECONCILIATION",
                ValidationResultType.ERROR,
                String.format("Soma produtos %s difere do total %s", products, total));
    }

    private ValidationResult validateNonNegative(String rule, BigDecimal value) {
        if (value == null) {
            return new ValidationResult(rule, ValidationResultType.WARN, "Valor não informado");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return new ValidationResult(rule, ValidationResultType.ERROR, "Valor negativo");
        }
        return new ValidationResult(rule, ValidationResultType.OK, "Valor válido");
    }

    private ValidationResult validateItemCount(Invoice invoice) {
        if (invoice.getItemCount() == null || invoice.getItemCount() <= 0) {
            return new ValidationResult(
                    "ITEM_COUNT", ValidationResultType.ERROR, "Nota fiscal sem itens");
        }
        return new ValidationResult("ITEM_COUNT", ValidationResultType.OK, "Quantidade de itens válida");
    }
}
