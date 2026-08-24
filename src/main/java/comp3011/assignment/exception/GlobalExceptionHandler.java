package comp3011.assignment.exception;

import comp3011.assignment.model.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e, HttpServletRequest request) {
        return ResponseEntity.status(500).body(new ErrorResponse(
                Instant.now(),
                500,
                "Internal Server Error",
                "An unexpected server error occurred.",
                request.getRequestURI()));
    }

    // Thrown when no route matches the requested path
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException e, HttpServletRequest request) {
        return ResponseEntity.status(404).body(new ErrorResponse(
                Instant.now(),
                404,
                "Not Found",
                "The requested resource was not found.",
                request.getRequestURI()));
    }

    // Thrown when the path matches but the HTTP method does not
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        return ResponseEntity.status(405).body(new ErrorResponse(
                Instant.now(),
                405,
                "Method Not Allowed",
                "The requested method is not supported for this resource.",
                request.getRequestURI()));
    }

    @ExceptionHandler(ShutdownInProgressException.class)
    public ResponseEntity<ErrorResponse> handleShutDownInProgress(ShutdownInProgressException e, HttpServletRequest request) {
        return ResponseEntity.status(409).body(new ErrorResponse(
                Instant.now(),
                409,
                "Conflict",
                e.getMessage(),
                request.getRequestURI()
        ));
    }
}
