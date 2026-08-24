package comp3011.assignment.service;

import comp3011.assignment.model.dto.GlobalStatsResponse;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class TokenUsageService {

    // same idea with shutdown service, two threads might interleave
    private final AtomicLong inputTokens = new AtomicLong();
    private final AtomicLong outputTokens = new AtomicLong();

    // called after each openAI transcription (cumulative input and output usage)
    public void addUsage(long input, long output) {
        inputTokens.addAndGet(input);
        outputTokens.addAndGet(output);
    }

    public GlobalStatsResponse currentStats() {
        return new GlobalStatsResponse(
                inputTokens.get(),
                outputTokens.get()
        );
    }
}
