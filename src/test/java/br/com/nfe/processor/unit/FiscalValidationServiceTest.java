package br.com.nfe.processor.unit;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.nfe.processor.core.domain.model.Invoice;
import br.com.nfe.processor.core.domain.model.ValidationResultType;
import br.com.nfe.processor.core.domain.service.FiscalValidationService;
import br.com.nfe.processor.core.domain.service.dto.ValidationResult;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FiscalValidationServiceTest {

    private FiscalValidationService service;

    @BeforeEach
    void setUp() {
        service = new FiscalValidationService();
    }

    @Test
    void shouldValidateTotalsWhenConsistent() {
        Invoice invoice = invoice(BigDecimal.valueOf(500), BigDecimal.valueOf(500));
        List<ValidationResult> results = service.validate(invoice);
        assertThat(results)
                .anySatisfy(result -> assertThat(result.getResult()).isEqualTo(ValidationResultType.OK));
    }

    @Test
    void shouldDetectTotalsMismatch() {
        Invoice invoice = invoice(BigDecimal.valueOf(300), BigDecimal.valueOf(350));
        List<ValidationResult> results = service.validate(invoice);
        assertThat(results)
                .anySatisfy(result -> {
                    if ("TOTALS_RECONCILIATION".equals(result.getRule())) {
                        assertThat(result.getResult()).isEqualTo(ValidationResultType.ERROR);
                    }
                });
    }

    private Invoice invoice(BigDecimal total, BigDecimal products) {
        Invoice invoice = new Invoice();
        invoice.setTotalAmount(total);
        invoice.setProductsAmount(products);
        invoice.setIcmsAmount(BigDecimal.ZERO);
        invoice.setIpiAmount(BigDecimal.ZERO);
        invoice.setIssAmount(BigDecimal.ZERO);
        invoice.setItemCount(1);
        return invoice;
    }
}
