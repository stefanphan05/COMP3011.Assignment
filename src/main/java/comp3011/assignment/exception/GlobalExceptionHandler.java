package comp3011.assignment.exception;

import comp3011.assignment.model.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Anything not matched below. Makes sure no exception ever escapes as HTML. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e, HttpServletRequest request) {
        // Log server-side so failures are diagnosable; the client still gets a generic
        // message so that no internal detail leaks out in the response body.
        log.error("Unhandled exception for {} {}", request.getMethod(), request.getRequestURI(), e);

        return errorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected server error occurred.",
            request
        );
    }

    /** No route matches the requested path. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException e, HttpServletRequest request) {
        return errorResponse(
            HttpStatus.NOT_FOUND,
            "The requested resource was not found.",
            request
        );
    }

    /** The path matches but the HTTP method does not. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        return errorResponse(
            HttpStatus.METHOD_NOT_ALLOWED,
            "The requested method is not supported for this resource.",
            request
        );
    }


    /**
     * The upload arrived without its "file" part, or without the part at all.
     * Answer 400 rather than falling through and reporting a server fault for
     * what is really a bad request.
     *
     * Catch Exception because this handles two exception types
     */
    @ExceptionHandler({
        MissingServletRequestPartException.class,
        MissingServletRequestParameterException.class
    })
    public ResponseEntity<ErrorResponse> handleMissingFile(Exception e, HttpServletRequest request) {
        return errorResponse(
            HttpStatus.BAD_REQUEST,
            "A multipart request with a 'file' part is required.",
            request
        );
    }

    /** The request body was not multipart/form-data. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        return errorResponse(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "The request body must be multipart/form-data.",
            request
        );
    }

    /**
     * The upload exceeded the configured limit.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleUploadTooLarge(MaxUploadSizeExceededException e, HttpServletRequest request) {
        return errorResponse(
            HttpStatus.CONTENT_TOO_LARGE,
            "The uploaded audio file is too large.",
            request
        );
    }

    /**
     * The speech-to-text provider failed. 502 rather than 500, because the fault is
     * upstream rather than in this server
     */
    @ExceptionHandler(SpeechToTextUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleSpeechToTextUnavailable(SpeechToTextUnavailableException e, HttpServletRequest request) {
        log.error("Speech-to-text provider failed for {} {}", request.getMethod(), request.getRequestURI(), e);

        return errorResponse(
            HttpStatus.BAD_GATEWAY,
            e.getMessage(),
            request
        );
    }

    /** A shutdown was requested while one was already running. */
    @ExceptionHandler(ShutdownInProgressException.class)
    public ResponseEntity<ErrorResponse> handleShutDownInProgress(ShutdownInProgressException e, HttpServletRequest request) {
        return errorResponse(
            HttpStatus.CONFLICT,
            e.getMessage(),
            request
        );
    }

    private ResponseEntity<ErrorResponse> errorResponse(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ErrorResponse(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            request.getRequestURI()));
    }
}
