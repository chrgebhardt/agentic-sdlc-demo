package de.cofinpro.training.depot.app;

/**
 * Fachliche Ablehnung einer Order (z.B. ungültige Stückzahl, unzureichendes
 * Guthaben). Wird im ApiExceptionHandler auf 400 abgebildet.
 */
public class OrderRejectedException extends RuntimeException {

    public OrderRejectedException(String message) {
        super(message);
    }
}
