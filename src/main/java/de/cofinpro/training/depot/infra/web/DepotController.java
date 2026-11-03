package de.cofinpro.training.depot.infra.web;

import de.cofinpro.training.depot.app.DepotQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/depots")
public class DepotController {

    private final DepotQueryService depotQueryService;

    public DepotController(DepotQueryService depotQueryService) {
        this.depotQueryService = depotQueryService;
    }

    @GetMapping("/{depotId}")
    public DepotResponse getDepot(@PathVariable Long depotId) {
        return depotQueryService.getDepot(depotId);
    }

    @GetMapping("/{depotId}/positions")
    public List<PositionResponse> getPositions(@PathVariable Long depotId) {
        return depotQueryService.getPositions(depotId);
    }
}
