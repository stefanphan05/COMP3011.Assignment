package comp3011.assignment.controller;

import comp3011.assignment.exception.ShutdownInProgressException;
import comp3011.assignment.model.dto.UptimeResponse;
import comp3011.assignment.service.ShutdownService;
import comp3011.assignment.service.UptimeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks the two admin endpoints
 */
@WebMvcTest(AdminController.class)
public class AdminControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UptimeService uptimeService;

    @MockitoBean
    private ShutdownService shutdownService;

    @Test
    @DisplayName("GET /api/v1/admin/uptime returns the spec's example shape")
    void uptimeReturnsSpecShape() throws Exception {
        when(uptimeService.currentUptime()).thenReturn(new UptimeResponse(
            Instant.parse("2026-07-14T01:15:30Z"),
            Instant.parse("2026-07-14T03:45:30.500Z"),
            9000.5
        ));

        mockMvc.perform(get("/api/v1/admin/uptime"))
            .andExpect(status().isOk())
            .andExpect(content().json("""
                {
                  "utcServerStart": "2026-07-14T01:15:30Z",
                  "utcNow": "2026-07-14T03:45:30.500Z",
                  "serverUptimeSeconds": 9000.5
                }
                """, JsonCompareMode.STRICT));
    }

    @Test
    @DisplayName("POST /api/v1/admin/shutdown returns 202 Accepted")
    void shutdownReturnsAccepted() throws Exception {
        mockMvc.perform(post("/api/v1/admin/shutdown"))
            .andExpect(status().isAccepted())
            .andExpect(content().json("""
                { "message": "Graceful shutdown requested." }
                """, JsonCompareMode.STRICT));
    }

    @Test
    @DisplayName("POST /api/v1/admin/shutdown returns 409 when one is already running")
    void shutdownReturnsConflictWhenAlreadyShuttingDown() throws Exception {
        doThrow(new ShutdownInProgressException()).when(shutdownService).requestShutdown();;

        mockMvc.perform(post("/api/v1/admin/shutdown"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.message").value("Graceful shutdown is already in progress."))
            .andExpect(jsonPath("$.path").value("/api/v1/admin/shutdown"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("GET on the shutdown path is rejected with 405")
    void shutdownRejectsWrongMethod() throws Exception {
        mockMvc.perform(get("/api/v1/admin/shutdown"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }
}
