package comp3011.assignment.controller;

import comp3011.assignment.model.dto.GlobalStatsResponse;
import comp3011.assignment.service.TokenUsageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GlobalStatsController.class)
public class GlobalStatsControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TokenUsageService tokenUsageService;

    @Test
    @DisplayName("GET /api/v1/global/stats returns the correct shape")
    void statsReturnCorrectShape() throws Exception {
        when(tokenUsageService.currentStats()).thenReturn(new GlobalStatsResponse(18432, 4096));

        mockMvc.perform(get("/api/v1/global/stats"))
            .andExpect(status().isOk())
            .andExpect(content().json("""
                { "inputTokens": 18432, "outputTokens": 4096 }
                """, JsonCompareMode.STRICT));
    }

    @Test
    @DisplayName("Counters above 2^31 survive the round trip")
    void statsHandleValuesBeyondIntRange() throws Exception {
        // int64. A counter held as an int would overflow
        // here and come back negative, so this test pins the type down.
        long beyondIntRange = 5_000_000_000L;

        when(tokenUsageService.currentStats())
                .thenReturn(new GlobalStatsResponse(beyondIntRange, beyondIntRange));

        mockMvc.perform(get("/api/v1/global/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inputTokens").value(beyondIntRange))
                .andExpect(jsonPath("$.outputTokens").value(beyondIntRange));
    }
}
