package de.cofinpro.training.depot.infra.persistence;

import de.cofinpro.training.depot.domain.Position;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PositionRepository extends JpaRepository<Position, Long> {

    List<Position> findByDepotId(Long depotId);
}
