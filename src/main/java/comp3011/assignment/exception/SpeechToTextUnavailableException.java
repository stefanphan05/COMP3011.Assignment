package comp3011.assignment.exception;

public class SpeechToTextUnavailableException extends RuntimeException {
    public SpeechToTextUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
