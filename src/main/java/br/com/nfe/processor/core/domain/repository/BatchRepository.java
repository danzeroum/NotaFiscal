package br.com.nfe.processor.core.domain.repository;

import br.com.nfe.processor.core.domain.model.Batch;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchRepository extends JpaRepository<Batch, String> {

    @EntityGraph(attributePaths = {"invoices", "issues", "validations", "invoices.issues"})
    Optional<Batch> findWithDetailsById(String id);
}
