package de.cofinpro.training.depot.app;

import de.cofinpro.training.depot.domain.Depot;
import de.cofinpro.training.depot.domain.Position;
import de.cofinpro.training.depot.domain.Wertpapier;
import de.cofinpro.training.depot.infra.persistence.DepotRepository;
import de.cofinpro.training.depot.infra.persistence.PositionRepository;
import de.cofinpro.training.depot.infra.persistence.WertpapierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.NoSuchElementException;

@Service
public class OrderService {

    private final DepotRepository depotRepository;
    private final WertpapierRepository wertpapierRepository;
    private final PositionRepository positionRepository;

    public OrderService(DepotRepository depotRepository, WertpapierRepository wertpapierRepository,
                        PositionRepository positionRepository) {
        this.depotRepository = depotRepository;
        this.wertpapierRepository = wertpapierRepository;
        this.positionRepository = positionRepository;
    }

    @Transactional
    public OrderConfirmation placeOrder(Long depotId, Long wertpapierId, BigDecimal quantity) {
        if (quantity.signum() <= 0) {
            throw new OrderRejectedException("Quantity must be positive");
        }
        Depot depot = depotRepository.findById(depotId)
                .orElseThrow(() -> new NoSuchElementException("Depot not found: " + depotId));
        Wertpapier wertpapier = wertpapierRepository.findById(wertpapierId)
                .orElseThrow(() -> new NoSuchElementException("Wertpapier not found: " + wertpapierId));

        BigDecimal totalCost = quantity.multiply(wertpapier.getPrice()).setScale(2, RoundingMode.HALF_UP);
        if (totalCost.compareTo(depot.getCashBalance()) > 0) {
            throw new OrderRejectedException(
                    "Insufficient balance: required " + totalCost + ", available " + depot.getCashBalance());
        }

        depot.setCashBalance(depot.getCashBalance().subtract(totalCost));
        positionRepository.findByDepotIdAndWertpapierId(depotId, wertpapierId).ifPresentOrElse(
                position -> position.setQuantity(position.getQuantity().add(quantity)),
                () -> positionRepository.save(new Position(null, depot, wertpapier, quantity)));

        return new OrderConfirmation(wertpapier.getSymbol(), quantity, wertpapier.getPrice(), totalCost,
                depot.getCashBalance());
    }
}
