package comp3011.assignment.concurrency;

import comp3011.assignment.client.StubSpeechToTextClient;
import comp3011.assignment.model.dto.GlobalStatsResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fires 250 transcription requests at the server at the same instant and checks
 * all of them are answered quickly and the token counts adding up exactly.
 *
 * The stub sleeps 500ms on every call, exactly like the real network call to
 * OpenAI would. That is what makes this a concurrency test: a server that dealt
 * with requests one at a time would need 250 x 500ms = 125 seconds to finish
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("stub")
public class ConcurrentTranscriptionLoadTest {
    private static final int REQUESTS = 250;

    // The stub's defaults: 500ms per call, 10 input tokens, 1 output token.
    private static final long INPUT_TOKENS_PER_CALL = 10;
    private static final long OUTPUT_TOKENS_PER_CALL = 1;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private StubSpeechToTextClient stub;

    @Test
    @Timeout(180)
    @DisplayName("250 simultaneous blocking transcriptions all succeed, quickly, and count exactly")
    void handles250SimultaneousBlockingRequests() throws Exception {
        HttpEntity<MultiValueMap<String, Object>> upload = audioUpload();

        AtomicInteger okResponses = new AtomicInteger();
        AtomicInteger unexpected = new AtomicInteger();

        CountDownLatch ready = new CountDownLatch(REQUESTS);
        CountDownLatch start = new CountDownLatch(1);

        long durationMillis;

        try (ExecutorService executor = Executors.newFixedThreadPool(REQUESTS)) {
            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < REQUESTS; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();

                    ResponseEntity<String> response = rest.postForEntity("/api/v1/transcribe", upload, String.class);

                    if (response.getStatusCode().value() == 200) {
                        okResponses.incrementAndGet();
                    } else {
                        unexpected.incrementAndGet();
                    }

                    return null;
                }));
            }

            // Wait until every worker is ready
            ready.await();

            long startTime = System.nanoTime();

            // fire all requests simultaneously
            start.countDown();

            for (Future<?> future: futures) {
                future.get();
            }

            durationMillis = (System.nanoTime() - startTime) / 1_000_000;
        }

        GlobalStatsResponse statsResponse = rest.getForObject("/api/v1/global/stats", GlobalStatsResponse.class);

        System.out.printf("[load] %d requests in %d ms, peak concurrency %d%n",
                REQUESTS, durationMillis, stub.peakConcurrentCalls());

        // Make sure nothing crashed, timed out or was refused under load
        assertThat(okResponses.get())
                .as("requests answered 200 OK")
                .isEqualTo(REQUESTS);

        // no unexpected counted
        assertThat(unexpected.get())
                .as("responses that were not 200 OK")
                .isZero();

        // the requests really were inside the server at the same moment
        assertThat(stub.peakConcurrentCalls())
                .as("transcriptions running inside the server at the same moment")
                .isGreaterThan(200);

        // no significant delays (125s if handled one at a time)
        assertThat(durationMillis)
                .as("milliseconds for %d requests (one at a time would be 125000)", REQUESTS)
                .isLessThan(10_000);

        // no one token update was lost by 250 concurrent threads
        assertThat(statsResponse)
                .as("token counters after %d concurrent transcriptions", REQUESTS)
                .isEqualTo(
                        new GlobalStatsResponse(
                REQUESTS * INPUT_TOKENS_PER_CALL,
                REQUESTS * OUTPUT_TOKENS_PER_CALL
                ));
    }

    /** One small multipart upload, reused by every thread */
    private static HttpEntity<MultiValueMap<String, Object>> audioUpload() {
        byte[] bytes = "not really audio".getBytes(StandardCharsets.UTF_8);
        ByteArrayResource file = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return "recording.webm";
            }
        };

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", file);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return new HttpEntity<>(form, headers);
    }
}
