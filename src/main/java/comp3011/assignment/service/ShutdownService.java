package comp3011.assignment.service;

import comp3011.assignment.exception.ShutdownInProgressException;
import comp3011.assignment.lifecycle.ApplicationTerminator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Decides who is allowed to shut the server down
 *
 * If two requests arrive at the exact same moment on different threads
 * a plain boolean would let BOTH read false and BOTH get 202.
 *
 * Using compareAndSet closes that, it reads and writes in one step,
 * so exactly one thread can never see false and flip it to true.
 * Everyone else gets 409
 */
@Service
public class ShutdownService {
    private static final Logger log = LoggerFactory.getLogger(ShutdownService.class);

    private final ApplicationTerminator terminator;

    // two simultaneous requests can never receive 202
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    public ShutdownService(ApplicationTerminator terminator) {
        this.terminator = terminator;
    }

    public void requestShutdown() {
        if (!shuttingDown.compareAndSet(false, true)) {
            log.warn("Shutdown rejected: a graceful shutdown is already in progress");
            throw new ShutdownInProgressException();
        }

        log.info("Graceful shutdown accepted.");
        terminator.terminate();
    }
}
