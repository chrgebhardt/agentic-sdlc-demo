package de.cofinpro.training.depot.infra.web;

import java.math.BigDecimal;

public record PositionResponse(String wertpapierSymbol, String wertpapierName, BigDecimal quantity,
                                BigDecimal currentPrice) {
}
