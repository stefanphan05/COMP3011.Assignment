package comp3011.assignment.service;

import comp3011.assignment.model.dto.GlobalStatsResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression tests for the token counters behind GET/api/v1/global/stats
 *
 * Why these tests exists
 * The counters are the only mutable state that shared by every request thread.
 * Each transcription adds to them, and every /global/stats request reads them,
 * all at the same time on different threads.
 *
 * Two separate things can go wrong, and each test below targets exactly one of them:
 * 1. two threads add at the same time and one addition vanishes, so the report total
 *      too low
 * 2. a reader catches an update half-finished and sees input tokens from N transcriptions
 *      but output tokens from N-1. Nothing is lost but these two numbers never existed together
 */
public class TokenUsageServiceRaceTest {
    // Every fake transcription adds 10 input tokens and 1 output tokens
    // so that at any moment, InputTokens should be exactly 10x OutputTokens
    private static final long INPUT_PER_CALL = 10;
    private static final long OUTPUT_PER_CALL = 1;

    private static final int WRITER_THREADS = 4;
    private static final int CALLS_PER_WRITER = 50_000;
    private static final int READER_THREADS = 2;

    private static final long TOTAL_CALLS = (long) WRITER_THREADS * CALLS_PER_WRITER;

    // volatile: when on thread changes this, the other threads see the change straight away
    // without this, a threader thread could keep looking at an old copy
    private volatile boolean writersFinished = false;
    private volatile GlobalStatsResponse tornSnapshot = null;

    @Test
    @Timeout(60)
    @DisplayName("No token updates are lost when many threads record usage at once")
    void noTokenUpdatesAreLostUnderConcurrentWriters() throws Exception {
        TokenUsageService service = new TokenUsageService();

        CountDownLatch ready = new CountDownLatch(WRITER_THREADS);
        CountDownLatch start = new CountDownLatch(1);

        // One thread per task: eveyr task parks on the start gate and only
        // releases once all of them has arrived
        try (ExecutorService executor = Executors.newFixedThreadPool(WRITER_THREADS)) {
            List<Future<?>> writers = new ArrayList<>();

            for (int i = 0; i < WRITER_THREADS; i++) {
                writers.add(executor.submit(() -> {
                    ready.countDown();          // worker is ready
                    start.await();              // wait until everyone is ready

                    for (int call = 0; call < CALLS_PER_WRITER; call++) {
                        service.addUsage(INPUT_PER_CALL, OUTPUT_PER_CALL);
                    }

                    return null;
                }));
            }

            // Wait until every worker is ready
            ready.await();

            // fire all writers simultaneously
            start.countDown();

            for (Future<?> writer: writers) {
                writer.get();
            }
        }

        GlobalStatsResponse stats = service.currentStats();

        // If everyone addition had been lost, these totals would be short
        assertThat(stats.inputTokens())
                .as("input token after %d transcriptions", TOTAL_CALLS)
                .isEqualTo(TOTAL_CALLS * INPUT_PER_CALL);

        assertThat(stats.outputTokens())
                .as("output token after %d transcriptions", TOTAL_CALLS)
                .isEqualTo(TOTAL_CALLS * OUTPUT_PER_CALL);
    }

    @Test
    @Timeout(60)
    @DisplayName("A stats snapshot never shows a half-finished update")
    void statsNeverShowAHalfFinishedUpdate() throws Exception {
        TokenUsageService service = new TokenUsageService();

        CountDownLatch ready = new CountDownLatch(WRITER_THREADS + READER_THREADS);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(WRITER_THREADS + READER_THREADS)) {
            List<Future<?>> writers = new ArrayList<>();
            for (int i = 0; i < WRITER_THREADS; i++) {
                writers.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();

                    for (int call = 0; call < CALLS_PER_WRITER; call++) {
                        service.addUsage(INPUT_PER_CALL, OUTPUT_PER_CALL);
                    }

                    return null;
                }));
            }

            for (int i = 0; i < READER_THREADS; i++) {
                executor.submit(() -> {
                    ready.countDown();
                    start.await();

                    while (!writersFinished) {
                        GlobalStatsResponse snapshot = service.currentStats();

                        // THE CHECK: every call adds 10 input and 1 output, so a
                        // complete snapshot must always have input == output * 10.
                        // Anything else means we read between the two updates.
                        if (snapshot.inputTokens() != snapshot.outputTokens() * INPUT_PER_CALL) {
                            tornSnapshot = snapshot;  // remember it for the message
                        }
                    }

                    return null;
                });
            }

            ready.await();
            start.countDown();

            for (Future<?> writer: writers) {
                writer.get();
            }

            writersFinished = true;         // tell the readers they can stop
        }

        assertThat(tornSnapshot)
            .as("A /stats snapshot did not match any whole number of transcriptions. "
                            + "Every transcription adds %d input and %d output tokens, so "
                            + "inputTokens should always be %dx outputTokens. A reader saw "
                            + "the counters mid-update.",
                            INPUT_PER_CALL, OUTPUT_PER_CALL, INPUT_PER_CALL)
            .isNull();
    }
}
