package comp3011.assignment.client;

import comp3011.assignment.exception.SpeechToTextUnavailableException;
import comp3011.assignment.model.dto.OpenAiTranscriptionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

/**
 * Uploads the audio to OpenAI and reads back the transcript
 *
 * Using Profile !stub to create this bean unless the stub profile is on
 * Normal runs get this one and anything started with the stub profiles
 * get the StubSpeechToTextClient instead of testing purposes
 */
@Component
@Profile("!stub")
public class OpenAiSpeechToTextClient implements SpeechToTextClient {
    private final RestClient openAi;
    private final String model;

    public OpenAiSpeechToTextClient(
        RestClient openAiRestClient,
        @Value("${openai.model}") String model
    ) {
        this.openAi = openAiRestClient;
        this.model = model;
    }

    @Override
    public Transcription transcribe(MultipartFile audio) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();

        form.add("file", audio.getResource());
        form.add("model", model);
        form.add("response_format", "json");

        OpenAiTranscriptionResponse response;

        try {
            response = openAi.post()
                .uri("/v1/audio/transcriptions")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(form)
                .retrieve()
                .body(OpenAiTranscriptionResponse.class);
        } catch (RestClientException e) {
            throw new SpeechToTextUnavailableException(
                "The speech-to-text service could not be reached.",
                e
            );
        }

        if (response == null || response.text() == null) {
            throw new IllegalStateException("Transcription service returned no text.");
        }

        // If OpenAI does not send the usage, return 0
        long inputTokens = response.usage() == null ? 0 : response.usage().inputTokens();
        long outputTokens = response.usage() == null ? 0 : response.usage().outputTokens();

        return new Transcription(response.text(), inputTokens, outputTokens);
    }
}
