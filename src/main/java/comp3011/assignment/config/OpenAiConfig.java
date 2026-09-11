package comp3011.assignment.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class OpenAiConfig {
    private static final Logger log = LoggerFactory.getLogger(OpenAiConfig.class);

    @Bean
    RestClient openAiRestClient(
        @Value("${openai.api.key}") String apiKey,
        @Value("${openai.api.base-url}") String baseUrl
    ) {
        if (apiKey.isBlank()) {
            log.warn("OPENAI_API_KEY is not set; transcription requests will fail.");
        }

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }
}
