package comp3011.assignment.controller;

import comp3011.assignment.model.dto.ShutdownResponse;
import comp3011.assignment.model.dto.UptimeResponse;
import comp3011.assignment.service.ShutdownService;
import comp3011.assignment.service.UptimeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final UptimeService uptimeService;
    private final ShutdownService shutdownService;

    public AdminController(UptimeService uptimeService, ShutdownService shutdownService) {
        this.uptimeService = uptimeService;
        this.shutdownService = shutdownService;
    }

    @GetMapping("/uptime")
    public ResponseEntity<UptimeResponse> getServerUptime() {
        return ResponseEntity.status(200)
                .body(uptimeService.currentUptime());
    }

    @PostMapping("/shutdown")
    public ResponseEntity<ShutdownResponse> shutdownServer() {
        shutdownService.requestShutdown();
        return ResponseEntity.status(202)
                .body(new ShutdownResponse("Graceful shutdown requested."));
    }
}
