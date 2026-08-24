package comp3011.assignment.model.dto;

import java.time.Instant;

public record UptimeResponse(
        Instant utcServerStart,
        Instant utcNow,
        double serverUptimeSeconds
) {
}
