package comp3011.assignment.logging;

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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "openai.api.key=" + SecretLeakageTests.FAKE_KEY,
        "openai.api.base-url=http://localhost:1",
    })
@AutoConfigureTestRestTemplate
@ExtendWith(OutputCaptureExtension.class)
public class SecretLeakageTests {
    static final String FAKE_KEY = "sk-test-DO-NOT-LOG-8f3a91c2b7e4";

    @Autowired
    private TestRestTemplate rest;

    @Test
    @DisplayName("A failed upstream call leaks the API key to neither the log nor the client")
    void apiKeyNeverAppearsInLogsOrResponse(CapturedOutput output) {
        var response = rest.postForEntity("/api/v1/transcribe", audioUpload(), String.class);

        // It must be reported as 502 rather than 500.
        assertThat(response.getStatusCode().value()).isEqualTo(502);

        // The failure IS logged - we are testing what it contains, not that it is silent.
        assertThat(output).contains("Speech-to-text provider failed for POST /api/v1/transcribe");

        assertThat(output.getAll())
                .as("the API key must never be written to the log")
                .doesNotContain(FAKE_KEY)
                .doesNotContain("Bearer ");

        assertThat(response.getBody())
                .as("the API key must never be returned to a client")
                .doesNotContain(FAKE_KEY)
                .doesNotContain("Bearer ");
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
