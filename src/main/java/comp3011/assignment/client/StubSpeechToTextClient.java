package comp3011.assignment.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

/**
 * A fake speech-to-text provider used by the tests
 *
 * Put sleep on Thread
 * A real transcription blocks the request thread for a second or so while it waits
 * on the network. If the stub returned instantly, the concurrency test would prove
 * nothing: 250 requests that each take 0ms will always lok fast.
 * Sleeping makes the stub block exactly the way the real client doesn't
 */
@Component
@Profile("stub")
public class StubSpeechToTextClient implements SpeechToTextClient {
    private final Duration delay;
    private final long inputTokens;
    private final long outputTokens;
    private final String text;

    public StubSpeechToTextClient(
            @Value("${stub.stt.delay:500ms}") Duration delay,
            @Value("${stub.stt.input-tokens:10}") long inputTokens,
            @Value("${stub.stt.output-tokens:1}") long outputTokens,
            @Value("${stub.stt.text:This is a stubbed transcription.}") String text
    ) {
        this.delay = delay;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.text = text;
    }

    @Override
    public Transcription transcribe(MultipartFile audio) {
        try {
            Thread.sleep(delay);        // pretend to wait on the network
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Stub transcription was interrupted", e);
        }
        return new Transcription(text, inputTokens, outputTokens);
    }

}
