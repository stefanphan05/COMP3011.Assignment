package comp3011.assignment.logging;

import comp3011.assignment.service.TranscriptionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Checks what the server WRITES to ITS LOG
 *
 * @DirtiesContext throws the server away afterwards
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("stub")
@ExtendWith(OutputCaptureExtension.class)
@DirtiesContext
public class LoggingRegressionTests {

    @Autowired
    private TestRestTemplate rest;

    @MockitoBean
    private TranscriptionService transcriptionService;

    @Test
    @DisplayName("An unexpected failure is logged with the request path but hidden from the client")
    void unexpectedFailureIsLoggedButNotReturned(CapturedOutput output) {
        when(transcriptionService.transcribe(any()))
                .thenThrow(new IllegalStateException("database on fire"));

        var response = rest.postForEntity("/api/v1/transcribe", audioUpload(), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(500);

        // The operator can find it: level, path and cause are all in the log.
        assertThat(output).contains("ERROR");
        assertThat(output).contains("Unhandled exception for POST /api/v1/transcribe");
        assertThat(output).contains("database on fire");

        // The client cannot: the body carries only the generic message.
        assertThat(response.getBody()).contains("An unexpected server error occurred.");
        assertThat(response.getBody()).doesNotContain("database on fire");
    }

    @Test
    @DisplayName("Accepting and rejecting a shutdown are both recorded")
    void shutdownDecisionsAreLogged(CapturedOutput output) {
        assertThat(rest.postForEntity("/api/v1/admin/shutdown", null, String.class)
                .getStatusCode().value()).isEqualTo(202);
        assertThat(rest.postForEntity("/api/v1/admin/shutdown", null, String.class)
                .getStatusCode().value()).isEqualTo(409);

        // One accepted, one rejected. ShutdownRaceTest proves the split holds
        // under 200 simultaneous requests; this proves the decision is auditable.
        assertThat(output).contains("Graceful shutdown accepted.");
        assertThat(output).contains("Shutdown rejected: a graceful shutdown is already in progress");
    }

    private static HttpEntity<MultiValueMap<String, Object>> audioUpload() {
        ByteArrayResource file = new ByteArrayResource(
                "not really audio".getBytes(StandardCharsets.UTF_8)) {
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
