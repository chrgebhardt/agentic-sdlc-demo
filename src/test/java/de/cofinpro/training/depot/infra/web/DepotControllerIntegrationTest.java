package de.cofinpro.training.depot.infra.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DepotControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getDepot_returnsSeededDemoDepot() throws Exception {
        mockMvc.perform(get("/api/depots/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value("Max Mustermann"))
                .andExpect(jsonPath("$.cashBalance").value(10000.00));
    }

    @Test
    void getDepot_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/depots/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPositions_returnsSeededApplePosition() throws Exception {
        mockMvc.perform(get("/api/depots/1/positions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].wertpapierSymbol").value("AAPL"))
                .andExpect(jsonPath("$[0].quantity").value(10))
                .andExpect(jsonPath("$[0].currentPrice").value(150.00));
    }
}
