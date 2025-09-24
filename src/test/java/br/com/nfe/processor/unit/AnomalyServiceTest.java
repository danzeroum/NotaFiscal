package br.com.nfe.processor.unit;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.nfe.processor.core.domain.model.Batch;
import br.com.nfe.processor.core.domain.model.Invoice;
import br.com.nfe.processor.core.domain.model.IssueType;
import br.com.nfe.processor.core.domain.model.ValidationResultType;
import br.com.nfe.processor.core.domain.service.AnomalyService;
import br.com.nfe.processor.core.domain.service.dto.ValidationResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnomalyServiceTest {

    private AnomalyService service;
    private Batch batch;

    @BeforeEach
    void setUp() {
        service = new AnomalyService();
        batch = new Batch();
        batch.setId("b_test");
        batch.setReceivedAt(Instant.now());
    }

    @Test
    void shouldCreateTotalsMismatchIssue() {
        Invoice invoice = invoice();
        invoice.setCfop("5102");
        List<ValidationResult> validations = List.of(
                new ValidationResult("TOTALS_RECONCILIATION", ValidationResultType.ERROR, "Mismatch"));
        assertThat(service.detect(batch, invoice, validations, true))
                .anySatisfy(issue -> assertThat(issue.getType()).isEqualTo(IssueType.TOTALS_MISMATCH));
    }

    @Test
    void shouldReturnEmptyWhenValid() {
        Invoice invoice = invoice();
        invoice.setCfop("5102");
        List<ValidationResult> validations = List.of(
                new ValidationResult("TOTALS_RECONCILIATION", ValidationResultType.OK, "Ok"));
        assertThat(service.detect(batch, invoice, validations, true)).isEmpty();
    }

    private Invoice invoice() {
        Invoice invoice = new Invoice();
        invoice.setTotalAmount(BigDecimal.valueOf(300));
        invoice.setProductsAmount(BigDecimal.valueOf(300));
        invoice.setIcmsAmount(BigDecimal.ZERO);
        invoice.setIpiAmount(BigDecimal.ZERO);
        invoice.setIssAmount(BigDecimal.ZERO);
        invoice.setItemCount(1);
        invoice.setCfop(null);
        return invoice;
    }
}
