package comp3011.assignment.service;

import comp3011.assignment.model.dto.OpenAiTranscriptionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TranscriptionService {
    private final RestClient openAi;
    private final TokenUsageService tokenUsageService;
    private final String model;

    public TranscriptionService(
        RestClient openAiRestClient,
        TokenUsageService tokenUsageService,
        @Value("${openai.model}") String model
    ) {
        this.openAi = openAiRestClient;
        this.tokenUsageService = tokenUsageService;
        this.model = model;
    }

    public String transcribe(MultipartFile audio) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();

        form.add("file", audio.getResource());
        form.add("model", model);
        form.add("response_format", "json");

        OpenAiTranscriptionResponse response = openAi.post()
                .uri("/v1/audio/transcriptions")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(form)
                .retrieve()
                .body(OpenAiTranscriptionResponse.class);

        if (response == null || response.text() == null) {
            throw new IllegalStateException("Transcription service returned no text.");
        }

        if (response.usage() != null) {
            tokenUsageService.addUsage(
                response.usage().inputTokens(),
                response.usage().outputTokens()
            );
        }

        return response.text();
    }
}
