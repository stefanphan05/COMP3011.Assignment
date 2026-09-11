package comp3011.assignment.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Shut the server down. Used everywhere except in tests
 */
@Component
public class SpringApplicationTerminator implements ApplicationTerminator {
    private static final Logger log = LoggerFactory.getLogger(SpringApplicationTerminator.class);

    private final ConfigurableApplicationContext context;
    private final Duration delay;

    public SpringApplicationTerminator(
        ConfigurableApplicationContext context,
        @Value("${app.shutdown.delay:500ms}") Duration delay
    ) {
        this.context = context;
        this.delay = delay;
    }

    @Override
    public void terminate() {
        // Close the context on its OWN thread, if we shut the server down from inside
        // the request still needs an answer, and the client would never receive its 202
        Thread.ofPlatform().name("graceful-shutdown").start(() -> {
            try {
                Thread.sleep(delay);            // give the 202 time to reach the client
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            log.info("Closing the application context for graceful shutdown");
            context.close();
        });
    }
}
