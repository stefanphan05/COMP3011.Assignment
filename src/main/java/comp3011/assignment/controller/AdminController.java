package comp3011.assignment.controller;

import comp3011.assignment.model.dto.UptimeResponse;
import comp3011.assignment.service.UptimeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final UptimeService uptimeService;

    public AdminController(UptimeService uptimeService) {
        this.uptimeService = uptimeService;
    }

    @GetMapping("/uptime")
    public ResponseEntity<UptimeResponse> getServerUptime() {
        return ResponseEntity.status(200)
                .body(uptimeService.currentUptime());
    }
}
