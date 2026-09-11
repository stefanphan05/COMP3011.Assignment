package comp3011.assignment.client;

import org.springframework.web.multipart.MultipartFile;

/**
 * Talks to a speech-to-text provider
 *
 * Reason using INTERFACE:
 * The real implementation calls OpenAI over the network:
 *      - slow
 *      - needs an API key
 *      - cost money
 *      - never returns the same thing twice
 * None of that is usable in a test. Putting the provider behind an interface
 * lets the tests run against StubSpeechToTextClient instead, which is
 * instant, free and predictable
 */
public interface SpeechToTextClient {

    /**
     * Transcribes one audio file.
     *
     * Returns the text plus the token counts, because the caller has to
     * record those against the global stats counters
     */
    Transcription transcribe(MultipartFile audio);
}
