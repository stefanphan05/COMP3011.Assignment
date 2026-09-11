package comp3011.assignment.service;

import comp3011.assignment.model.dto.UptimeResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Reports how long the server has been running
 *
 * Using two clocks here
 * - serverStart: using Instant since the response has to report a real calendar timestamp (UTC)
 * - serverStartNanos: measuring elapsed time
 *
 * Instant.now() can be changed to move backwards by operating system, and if using this
 * it could produce a negative number, using nanos won't
 */
@Service
public class UptimeService {

    // Mark as final, not change since it starts
    private final Instant serverStart = Instant.now();
    private final long serverStartNanos = System.nanoTime();

    public UptimeResponse currentUptime() {
        long elapsedNanos = System.nanoTime() - serverStartNanos;

        // The example in the openapi specifically says 9000.5 seconds to if convert
        // directly to seconds, it won't have the .5, and .0 at the end of 1_000_000_000
        // to enable double division
        double uptimeSeconds = elapsedNanos / 1_000_000_000.0;

        return new UptimeResponse(serverStart, serverStart.plusNanos(elapsedNanos), uptimeSeconds);
    }
}
