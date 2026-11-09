package de.cofinpro.training.depot.infra.web;

import java.math.BigDecimal;

public record OrderRequest(Long wertpapierId, BigDecimal quantity) {
}
