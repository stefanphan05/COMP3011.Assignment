package comp3011.assignment.service;

import comp3011.assignment.client.SpeechToTextClient;
import comp3011.assignment.client.Transcription;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Transcribes audio and records what it cost against the global token counters
 */
@Service
public class TranscriptionService {
    private final SpeechToTextClient speechToText;
    private final TokenUsageService tokenUsageService;

    public TranscriptionService(
        SpeechToTextClient speechToText,
        TokenUsageService tokenUsageService
    ) {
        this.speechToText = speechToText;
        this.tokenUsageService = tokenUsageService;
    }

    public String transcribe(MultipartFile audio) {
        Transcription result = speechToText.transcribe(audio);

        tokenUsageService.addUsage(result.inputTokens(), result.outputTokens());

        return result.text();
    }
}
