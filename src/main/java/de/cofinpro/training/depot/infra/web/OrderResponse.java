package de.cofinpro.training.depot.infra.web;

import java.math.BigDecimal;

public record OrderResponse(String wertpapierSymbol, BigDecimal quantity, BigDecimal pricePerUnit,
                            BigDecimal totalCost, BigDecimal newCashBalance) {
}
