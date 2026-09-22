package com.idr.nav;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NavigationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testSystemStatusEndpoint() throws Exception {
        mockMvc.perform(get("/api/system/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ONLINE"))
                .andExpect(jsonPath("$.mlModelStatus").value("MOCK MODEL — TRAINED MODEL NOT INTEGRATED"));
    }

    @Test
    void testSessionLifecycleAndGnssControls() throws Exception {
        // Start session
        mockMvc.perform(post("/api/navigation/session/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionName\": \"Integration Test\", \"mode\": \"SIMULATION\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.navigationMode").value("FUSION"))
                .andExpect(jsonPath("$.gnssAvailable").value(true));

        // Trigger GNSS loss
        mockMvc.perform(post("/api/navigation/gnss/loss"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.navigationMode").value("DEAD_RECKONING"))
                .andExpect(jsonPath("$.gnssAvailable").value(false));

        // Trigger GNSS restore
        mockMvc.perform(post("/api/navigation/gnss/restore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.navigationMode").value("FUSION"))
                .andExpect(jsonPath("$.gnssAvailable").value(true));
    }

    @Test
    void testRouteNavigationDemoLifecycle() throws Exception {
        String routeRequestJson = """
            {
                "from": { "lat": 28.6129, "lon": 77.2295, "name": "India Gate" },
                "to":   { "lat": 28.6315, "lon": 77.2167, "name": "Connaught Place" }
            }
            """;

        // 1. Create Route
        mockMvc.perform(post("/api/navigation/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(routeRequestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeId").isNotEmpty())
                .andExpect(jsonPath("$.fromName").value("India Gate"))
                .andExpect(jsonPath("$.toName").value("Connaught Place"))
                .andExpect(jsonPath("$.points").isArray());

        // 2. Verify active route retrieved
        mockMvc.perform(get("/api/navigation/routes/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromName").value("India Gate"));

        // 3. Start navigation along route
        mockMvc.perform(post("/api/navigation/routes/navigation/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NAVIGATION_ACTIVE"));

        // 4. Trigger GNSS loss during route navigation
        mockMvc.perform(post("/api/navigation/gnss/loss"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.navigationMode").value("DEAD_RECKONING"))
                .andExpect(jsonPath("$.gnssAvailable").value(false));

        // 5. Restore GNSS
        mockMvc.perform(post("/api/navigation/gnss/restore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.navigationMode").value("FUSION"))
                .andExpect(jsonPath("$.gnssAvailable").value(true));

        // 6. Stop route navigation
        mockMvc.perform(post("/api/navigation/routes/navigation/stop"))
                .andExpect(status().isOk());

        // 7. Reset demo
        mockMvc.perform(post("/api/navigation/routes/navigation/reset"))
                .andExpect(status().isOk());

        // 8. Verify active route cleared (HTTP 204 No Content)
        mockMvc.perform(get("/api/navigation/routes/active"))
                .andExpect(status().isNoContent());
    }
}

