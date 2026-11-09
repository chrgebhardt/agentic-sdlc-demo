package de.cofinpro.training.depot.infra.web;

import de.cofinpro.training.depot.app.OrderConfirmation;
import de.cofinpro.training.depot.app.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/depots")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/{depotId}/orders")
    public OrderResponse placeOrder(@PathVariable Long depotId, @Valid @RequestBody OrderRequest request) {
        OrderConfirmation confirmation = orderService.placeOrder(depotId, request.wertpapierId(), request.quantity());
        return new OrderResponse(confirmation.wertpapierSymbol(), confirmation.quantity(),
                confirmation.pricePerUnit(), confirmation.totalCost(), confirmation.newCashBalance());
    }
}
