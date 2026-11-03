package de.cofinpro.training.depot.app;

import de.cofinpro.training.depot.domain.Depot;
import de.cofinpro.training.depot.domain.Position;
import de.cofinpro.training.depot.infra.persistence.DepotRepository;
import de.cofinpro.training.depot.infra.persistence.PositionRepository;
import de.cofinpro.training.depot.infra.web.DepotResponse;
import de.cofinpro.training.depot.infra.web.PositionResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class DepotQueryService {

    private final DepotRepository depotRepository;
    private final PositionRepository positionRepository;

    public DepotQueryService(DepotRepository depotRepository, PositionRepository positionRepository) {
        this.depotRepository = depotRepository;
        this.positionRepository = positionRepository;
    }

    public DepotResponse getDepot(Long depotId) {
        Depot depot = depotRepository.findById(depotId)
                .orElseThrow(() -> new NoSuchElementException("Depot not found: " + depotId));
        return new DepotResponse(depot.getId(), depot.getOwner(), depot.getCashBalance());
    }

    public List<PositionResponse> getPositions(Long depotId) {
        return positionRepository.findByDepotId(depotId).stream()
                .map(this::toResponse)
                .toList();
    }

    private PositionResponse toResponse(Position position) {
        return new PositionResponse(
                position.getWertpapier().getSymbol(),
                position.getWertpapier().getName(),
                position.getQuantity(),
                position.getWertpapier().getPrice());
    }
}
