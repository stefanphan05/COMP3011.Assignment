package comp3011.assignment.service;

import comp3011.assignment.model.dto.UptimeResponse;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class UptimeService {

    // Mark as final, not change since it starts
    private final Instant serverStart = Instant.now();

    public UptimeResponse currentUptime() {
        Instant now = Instant.now();

        // The example in the openapi specifically says 9000.5 seconds to if convert directly to seconds, it won't have the .5, and .0 at the end of 1_000_000_000 to enable double division
        double uptimeSeconds = Duration.between(serverStart, now).toNanos() / 1_000_000_000.0;
        return new UptimeResponse(serverStart, now, uptimeSeconds);
    }
}
