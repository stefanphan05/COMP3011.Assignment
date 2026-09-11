package comp3011.assignment.concurrency;

import comp3011.assignment.lifecycle.ApplicationTerminator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * the first POST /api/v1/admin/shutdown gets 202 and any later one get 409.
 *
 * if firing 200 requests arrive at once, one 200 different threads, the server
 * must still pick exactly ONE
 *
 * @DirtiesContext throws the server away afterwards
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("stub")
@DirtiesContext
public class ShutdownRaceTest {
    private static final int REQUESTS = 200;

    @Autowired
    private TestRestTemplate rest;

    @MockitoBean
    private ApplicationTerminator terminator;

    @Test
    @Timeout(60)
    @DisplayName("Exactly one of 200 simultaneous shutdown request is accepted")
    void exactlyOneShutdownRequestIsAccepted() throws Exception {
        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();
        AtomicInteger unexpected = new AtomicInteger();

        CountDownLatch ready = new CountDownLatch(REQUESTS);
        CountDownLatch start = new CountDownLatch(1);

        long durationMillis;

        try (ExecutorService executor = Executors.newFixedThreadPool(REQUESTS)) {
            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < REQUESTS; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();          // worker is ready
                    start.await();              // wait until everyone is ready

                    try {
                        ResponseEntity<String> response = rest.postForEntity(
                            "/api/v1/admin/shutdown",
                            null,
                            String.class
                        );

                        switch (response.getStatusCode().value()) {
                            case 202 -> accepted.incrementAndGet();
                            case 409 -> conflicts.incrementAndGet();
                            default -> unexpected.incrementAndGet();
                        }
                    } catch (Exception e) {
                        unexpected.incrementAndGet();
                        throw e;
                    }

                    return null;
                }));
            }

            // Wait until every worker is ready
            ready.await();

            long startTime = System.nanoTime();

            // Fire all requests simultaneously
            start.countDown();

            for (Future<?> future: futures) {
                future.get();
            }

            durationMillis = (System.nanoTime() - startTime) / 1_000_000;
        }

        System.out.printf(
            "[shutdown] %d accepted, %d conflicts, %d unexpected (%d ms)%n",
            accepted.get(),
            conflicts.get(),
            unexpected.get(),
            durationMillis
        );

        assertThat(accepted.get())
                .as("202 responses - the server must accept only ONE shutdown")
                .isEqualTo(1);

        assertThat(conflicts.get())
                .as("409 responses")
                .isEqualTo(REQUESTS - 1);

        assertThat(unexpected.get())
                .as("responses that were neither 202 nor 409")
                .isZero();

        // The statuses could in theory be right while the server still tried to
        // shut itself down more than once
        verify(terminator, times(1)).terminate();
    }
}
