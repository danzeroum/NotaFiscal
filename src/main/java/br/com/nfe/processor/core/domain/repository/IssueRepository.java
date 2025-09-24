package br.com.nfe.processor.core.domain.repository;

import br.com.nfe.processor.core.domain.model.Issue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssueRepository extends JpaRepository<Issue, Long> {
}
