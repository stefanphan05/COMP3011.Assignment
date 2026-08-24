package comp3011.assignment.controller;

import comp3011.assignment.model.dto.GlobalStatsResponse;
import comp3011.assignment.service.TokenUsageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/global")
public class GlobalStatsController {
    private final TokenUsageService tokenUsageService;

    public GlobalStatsController(TokenUsageService tokenUsageService) {
        this.tokenUsageService = tokenUsageService;
    }

    @GetMapping("/stats")
    public ResponseEntity<GlobalStatsResponse> getGlobalStats() {
        return ResponseEntity.status(200)
                .body(tokenUsageService.currentStats());
    }
}
