package de.cofinpro.training.depot.infra.web;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OrderRequest(@NotNull(message = "wertpapierId must not be null") Long wertpapierId,
                           @NotNull(message = "quantity must not be null") BigDecimal quantity) {

    /**
     * Position.quantity wird mit Scale 2 persistiert (Hibernate-Default
     * numeric(38,2)) und würde sonst beim Speichern stillschweigend gerundet.
     * Überzählige Nullen (z.B. 0.250) sind erlaubt, daher kein @Digits.
     */
    @AssertTrue(message = "quantity must have at most 2 decimal places")
    boolean isQuantityScaleValid() {
        return quantity == null || quantity.stripTrailingZeros().scale() <= 2;
    }
}
