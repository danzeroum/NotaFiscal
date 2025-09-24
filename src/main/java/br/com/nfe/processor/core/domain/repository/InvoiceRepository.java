package br.com.nfe.processor.core.domain.repository;

import br.com.nfe.processor.core.domain.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
}
