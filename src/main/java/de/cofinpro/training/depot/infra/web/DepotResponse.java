package de.cofinpro.training.depot.infra.web;

import java.math.BigDecimal;

public record DepotResponse(Long id, String owner, BigDecimal cashBalance) {
}
