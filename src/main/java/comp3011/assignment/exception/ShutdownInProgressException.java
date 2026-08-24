package comp3011.assignment.exception;

public class ShutdownInProgressException extends RuntimeException {
    public ShutdownInProgressException() {
        // This message from 409 example in the documentation
        super("Graceful shutdown is already in progress.");
    }
}
