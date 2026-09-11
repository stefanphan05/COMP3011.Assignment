package comp3011.assignment.controller;

import comp3011.assignment.exception.SpeechToTextUnavailableException;
import comp3011.assignment.service.TranscriptionService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks how POST /api/v1/transcribe answers when things go wrong
 */
@WebMvcTest(TranscriptionController.class)
public class TranscriptionControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TranscriptionService transcriptionService;

    private static MockMultipartFile audio() {
        return new MockMultipartFile(
            "file",
            "recording.webm",
            "audio/webm",
            "not really audio".getBytes(StandardCharsets.UTF_8)
        );
    }

    @Test
    @DisplayName("A valid upload returns 200 with the transcript")
    void validUploadReturnsTranscript() throws Exception {
        when(transcriptionService.transcribe(any())).thenReturn("hello world");

        mockMvc.perform(multipart("/api/v1/transcribe").file(audio()))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        { "text": "hello world" }
                        """, JsonCompareMode.STRICT));
    }

    @Test
    @DisplayName("An upload with no file part returns 400, not 500")
    void missingFilePartReturnsBadRequest() throws Exception {
        mockMvc.perform(multipart("/api/v1/transcribe"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/api/v1/transcribe"));
    }

    @Test
    @DisplayName("A non-multipart body returns 415")
    void nonMultipartBodyReturnsUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/api/v1/transcribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }

    @Test
    @DisplayName("An upstream provider failure returns 502, not 500")
    void upstreamFailureReturnsBadGateway() throws Exception {
        when(transcriptionService.transcribe(any()))
                .thenThrow(new SpeechToTextUnavailableException(
                        "The speech-to-text service could not be reached.",
                        new RuntimeException("connection reset")
                ));

        mockMvc.perform(multipart("/api/v1/transcribe").file(audio()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.error").value("Bad Gateway"));
    }

    @Test
    @DisplayName("An unexpected failure returns 500 and leaks nothing")
    void unexpectedFailureLeaksNothing() throws Exception {
        // The message deliberately looks like something that must never reach a
        // client: an API key. The response must carry the generic text instead.
        when(transcriptionService.transcribe(any()))
                .thenThrow(new IllegalStateException("Authorization: Bearer sk-secret-key-12345"));

        mockMvc.perform(multipart("/api/v1/transcribe").file(audio()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected server error occurred."))
                .andExpect(content().string(
                        Matchers.not(Matchers.containsString("sk-secret-key-12345"))));
    }
}
