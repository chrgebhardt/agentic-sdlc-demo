package de.cofinpro.training.depot.infra.web;

import de.cofinpro.training.depot.domain.Depot;
import de.cofinpro.training.depot.domain.Position;
import de.cofinpro.training.depot.domain.Wertpapier;
import de.cofinpro.training.depot.infra.persistence.DepotRepository;
import de.cofinpro.training.depot.infra.persistence.PositionRepository;
import de.cofinpro.training.depot.infra.persistence.WertpapierRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TICKET-142: POST /api/depots/{depotId}/orders
 *
 * Jeder Test legt seine Ausgangsdaten (Depot, Wertpapiere, Positionen) im
 * given-Block selbst an und hängt nicht von den Demo-Daten des DemoDataLoader ab.
 * Jeder Test läuft in einer Transaktion, die danach zurückgerollt wird.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("POST /api/depots/{depotId}/orders – Kauforder aufgeben")
class OrderControllerIntegrationTest {

    private static final long UNKNOWN_ID = 999_999L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DepotRepository depotRepository;

    @Autowired
    private WertpapierRepository wertpapierRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Kauf auf bestehende Position liefert Bestätigung mit Stückzahl, Preis, "
            + "auf 2 Nachkommastellen kaufmännisch gerundeten Gesamtkosten (HALF_UP) und neuem Guthaben")
    void placeOrder_onExistingPosition_returnsConfirmation() throws Exception {
        // given
        Depot depot = givenDepot("10000.00");
        Wertpapier apple = givenWertpapier("AAPL", "150.10");
        givenPosition(depot, apple, "10");

        // when: 0.25 x 150.10 = 37.525 (roh)
        ResultActions result = placeOrder(depot.getId(), apple.getId(), "0.25");

        // then: HALF_UP → 37.53 (HALF_EVEN ergäbe 37.52), Spec Open Question 1
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.wertpapierSymbol").value("AAPL"))
                .andExpect(jsonPath("$.quantity").value(0.25))
                .andExpect(jsonPath("$.pricePerUnit").value(150.10))
                .andExpect(jsonPath("$.totalCost").value(37.53))
                .andExpect(jsonPath("$.newCashBalance").value(9962.47));
    }

    @Test
    @DisplayName("Kauf reduziert das Guthaben des Depots um die Gesamtkosten")
    void placeOrder_onExistingPosition_reducesCashBalance() throws Exception {
        // given
        Depot depot = givenDepot("10000.00");
        Wertpapier apple = givenWertpapier("AAPL", "150.00");
        givenPosition(depot, apple, "10");

        // when
        placeOrder(depot.getId(), apple.getId(), "5.0")
                .andExpect(status().isOk());

        // then
        mockMvc.perform(get("/api/depots/{id}", depot.getId()))
                .andExpect(jsonPath("$.cashBalance").value(9250.00));
    }

    @Test
    @DisplayName("Kauf auf bestehende Position erhöht deren Stückzahl, statt eine zweite Position anzulegen")
    void placeOrder_onExistingPosition_increasesQuantityWithoutDuplicatePosition() throws Exception {
        // given
        Depot depot = givenDepot("10000.00");
        Wertpapier apple = givenWertpapier("AAPL", "150.00");
        givenPosition(depot, apple, "10");

        // when
        placeOrder(depot.getId(), apple.getId(), "5.0")
                .andExpect(status().isOk());

        // then
        mockMvc.perform(get("/api/depots/{id}/positions", depot.getId()))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].wertpapierSymbol").value("AAPL"))
                .andExpect(jsonPath("$[0].quantity").value(15.0));
    }

    @Test
    @DisplayName("Erstkauf eines Wertpapiers legt eine neue Position an und lässt bestehende Positionen unverändert")
    void placeOrder_withoutExistingPosition_createsNewPosition() throws Exception {
        // given
        Depot depot = givenDepot("10000.00");
        Wertpapier apple = givenWertpapier("AAPL", "150.00");
        Wertpapier sap = givenWertpapier("SAP", "120.00");
        givenPosition(depot, apple, "10");

        // when
        ResultActions result = placeOrder(depot.getId(), sap.getId(), "2");

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.wertpapierSymbol").value("SAP"))
                .andExpect(jsonPath("$.totalCost").value(240.00))
                .andExpect(jsonPath("$.newCashBalance").value(9760.00));

        // Persistenzkontext leeren, damit die Positionen so gelesen werden, wie sie
        // persistiert sind (Scale 2), statt als ungespeicherte Instanzen aus dem Test.
        entityManager.flush();
        entityManager.clear();
        mockMvc.perform(get("/api/depots/{id}/positions", depot.getId()))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.wertpapierSymbol == 'SAP')].quantity").value(contains(2.0)))
                .andExpect(jsonPath("$[?(@.wertpapierSymbol == 'AAPL')].quantity").value(contains(10.0)));
    }

    @Test
    @DisplayName("Unbekanntes Depot wird mit 404 abgelehnt")
    void placeOrder_unknownDepot_returns404() throws Exception {
        // given
        Wertpapier apple = givenWertpapier("AAPL", "150.00");
        assertThat(depotRepository.existsById(UNKNOWN_ID)).isFalse();

        // when
        ResultActions result = placeOrder(UNKNOWN_ID, apple.getId(), "5.0");

        // then
        result.andExpect(status().isNotFound())
                .andExpect(content().string("Depot not found: " + UNKNOWN_ID));
    }

    @Test
    @DisplayName("Unbekanntes Wertpapier wird mit 404 abgelehnt, ohne das Guthaben zu verändern")
    void placeOrder_unknownWertpapier_returns404WithoutSideEffects() throws Exception {
        // given
        Depot depot = givenDepot("10000.00");
        assertThat(wertpapierRepository.existsById(UNKNOWN_ID)).isFalse();

        // when
        ResultActions result = placeOrder(depot.getId(), UNKNOWN_ID, "5.0");

        // then
        result.andExpect(status().isNotFound())
                .andExpect(content().string("Wertpapier not found: " + UNKNOWN_ID));

        mockMvc.perform(get("/api/depots/{id}", depot.getId()))
                .andExpect(jsonPath("$.cashBalance").value(10000.00));
    }

    @ParameterizedTest(name = "quantity = {0}")
    @ValueSource(strings = {"0", "-1", "-0.5"})
    @DisplayName("Nicht-positive Stückzahl wird mit 400 abgelehnt, ohne Guthaben oder Positionen zu verändern")
    void placeOrder_nonPositiveQuantity_returns400WithoutSideEffects(String quantity) throws Exception {
        // given
        Depot depot = givenDepot("10000.00");
        Wertpapier apple = givenWertpapier("AAPL", "150.00");
        givenPosition(depot, apple, "10");

        // when
        ResultActions result = placeOrder(depot.getId(), apple.getId(), quantity);

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(content().string("Quantity must be positive"));

        mockMvc.perform(get("/api/depots/{id}", depot.getId()))
                .andExpect(jsonPath("$.cashBalance").value(10000.00));
        mockMvc.perform(get("/api/depots/{id}/positions", depot.getId()))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].quantity").value(10));
    }

    @Test
    @DisplayName("Bestellwert über dem Guthaben wird mit 400 abgelehnt, ohne Guthaben oder Positionen zu verändern")
    void placeOrder_exceedingCashBalance_returns400WithoutSideEffects() throws Exception {
        // given
        Depot depot = givenDepot("1000.00");
        Wertpapier apple = givenWertpapier("AAPL", "150.00");
        givenPosition(depot, apple, "10");

        // when: 10 x 150.00 = 1500.00 > 1000.00
        ResultActions result = placeOrder(depot.getId(), apple.getId(), "10");

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(content().string("Insufficient balance: required 1500.00, available 1000.00"));

        mockMvc.perform(get("/api/depots/{id}", depot.getId()))
                .andExpect(jsonPath("$.cashBalance").value(1000.00));
        mockMvc.perform(get("/api/depots/{id}/positions", depot.getId()))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].quantity").value(10));
    }

    private Depot givenDepot(String cashBalance) {
        return depotRepository.save(new Depot(null, "Erika Musterfrau", new BigDecimal(cashBalance)));
    }

    private Wertpapier givenWertpapier(String symbol, String price) {
        return wertpapierRepository.save(new Wertpapier(null, symbol, symbol + " Testwertpapier", new BigDecimal(price)));
    }

    private void givenPosition(Depot depot, Wertpapier wertpapier, String quantity) {
        positionRepository.save(new Position(null, depot, wertpapier, new BigDecimal(quantity)));
    }

    private ResultActions placeOrder(long depotId, long wertpapierId, String quantity) throws Exception {
        return mockMvc.perform(post("/api/depots/{id}/orders", depotId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "wertpapierId": %d, "quantity": %s }
                        """.formatted(wertpapierId, quantity)));
    }
}
