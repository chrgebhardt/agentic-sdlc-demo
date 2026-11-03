package de.cofinpro.training.depot.infra.persistence;

import de.cofinpro.training.depot.domain.Wertpapier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WertpapierRepository extends JpaRepository<Wertpapier, Long> {

    Optional<Wertpapier> findBySymbol(String symbol);
}
