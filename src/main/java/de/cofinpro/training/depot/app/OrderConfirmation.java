package de.cofinpro.training.depot.app;

import java.math.BigDecimal;

public record OrderConfirmation(String wertpapierSymbol, BigDecimal quantity, BigDecimal pricePerUnit,
                                BigDecimal totalCost, BigDecimal newCashBalance) {
}
