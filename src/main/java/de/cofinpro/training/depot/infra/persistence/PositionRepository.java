package de.cofinpro.training.depot.infra.persistence;

import de.cofinpro.training.depot.domain.Position;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PositionRepository extends JpaRepository<Position, Long> {

    List<Position> findByDepotId(Long depotId);

    Optional<Position> findByDepotIdAndWertpapierId(Long depotId, Long wertpapierId);
}
