package de.cofinpro.training.depot.infra.web;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OrderRequest(@NotNull(message = "wertpapierId must not be null") Long wertpapierId,
                           @NotNull(message = "quantity must not be null") BigDecimal quantity) {
}
