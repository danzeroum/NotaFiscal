package br.com.nfe.processor.core.domain.repository;

import br.com.nfe.processor.core.domain.model.ValidationReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ValidationReportRepository extends JpaRepository<ValidationReport, Long> {
}
