package de.cofinpro.training.depot.infra;

import de.cofinpro.training.depot.domain.Depot;
import de.cofinpro.training.depot.domain.Position;
import de.cofinpro.training.depot.domain.Wertpapier;
import de.cofinpro.training.depot.infra.persistence.DepotRepository;
import de.cofinpro.training.depot.infra.persistence.PositionRepository;
import de.cofinpro.training.depot.infra.persistence.WertpapierRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DemoDataLoader implements CommandLineRunner {

    private final DepotRepository depotRepository;
    private final WertpapierRepository wertpapierRepository;
    private final PositionRepository positionRepository;

    public DemoDataLoader(DepotRepository depotRepository, WertpapierRepository wertpapierRepository,
                           PositionRepository positionRepository) {
        this.depotRepository = depotRepository;
        this.wertpapierRepository = wertpapierRepository;
        this.positionRepository = positionRepository;
    }

    @Override
    public void run(String... args) {
        Wertpapier apple = wertpapierRepository.save(new Wertpapier(null, "AAPL", "Apple Inc.", new BigDecimal("150.00")));
        wertpapierRepository.save(new Wertpapier(null, "SAP", "SAP SE", new BigDecimal("120.00")));
        wertpapierRepository.save(new Wertpapier(null, "DTE", "Deutsche Telekom AG", new BigDecimal("20.00")));

        Depot depot = depotRepository.save(new Depot(null, "Max Mustermann", new BigDecimal("10000.00")));

        positionRepository.save(new Position(null, depot, apple, new BigDecimal("10")));
    }
}
