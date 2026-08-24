package comp3011.assignment.exception;

import comp3011.assignment.model.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
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
