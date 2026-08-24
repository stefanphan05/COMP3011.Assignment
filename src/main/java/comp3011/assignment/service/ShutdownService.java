package comp3011.assignment.service;

import comp3011.assignment.exception.ShutdownInProgressException;
import org.springframework.stereotype.Service;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ShutdownService {
    private final ConfigurableApplicationContext context;

    // two simultaneous requests can never receive 202
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    public ShutdownService(ConfigurableApplicationContext context) {
        this.context = context;
    }

    public void requestShutdown() {
        if (!shuttingDown.compareAndSet(false, true)) {
            throw new ShutdownInProgressException();
        }

        // Do the shutdown on a separate thread, so the request can send its 202 first
        new Thread(() -> {
            try {
                // wait 500ms for the response to reach the client
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // stop the application
            context.close();
        }).start();
    }
}
