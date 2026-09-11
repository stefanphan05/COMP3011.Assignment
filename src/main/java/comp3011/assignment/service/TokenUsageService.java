package comp3011.assignment.service;

import comp3011.assignment.model.dto.GlobalStatsResponse;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Running total of speech-to-text token usage since the server started.
 * Shared by every request thread, reported by GET /api/v1/global/stats
 *
 * Using Two AtomicLong Fields but fails?
 * Two AtomicLongs stop additions from being lost, but they do not keep the two
 * numbers in step with each other. A /stats request could see input tokens from
 * N transcriptions but the output tokens from N-1, these two never really went together
 *
 * See: TokenUsageServiceRaceTest#statsNeverShowAHalfFinishedUpdate
 *
 * The fix: keep BOTH numbers in ONE small immutable object
 */
@Service
public class TokenUsageService {

    /**Both counters together, always replaced as a single value*/
    private record Usage(long inputTokens, long outputTokens) {}

    /**
     * AtomicReference provides a thread-safe way to store and replace the Usage object.
     * Updates happen atomically, preventing lost updates when multiple requests finish at the same time.
     */
    private final AtomicReference<Usage> usage = new AtomicReference<>(new Usage(0, 0));

    /** Records the usage of one finished transcription. Called from request threads */
    public void addUsage(long input, long output) {
        usage.updateAndGet(current -> new Usage(
            current.inputTokens() + input,
            current.outputTokens() + output
        ));
    }

    public GlobalStatsResponse currentStats() {
        Usage snapshot = usage.get();

        return new GlobalStatsResponse(
            snapshot.inputTokens(),
            snapshot.outputTokens()
        );
    }
}
