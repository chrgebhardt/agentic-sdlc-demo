package de.cofinpro.training.depot.infra.persistence;

import de.cofinpro.training.depot.domain.Depot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepotRepository extends JpaRepository<Depot, Long> {
}
