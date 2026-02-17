package com.tallyo.tallyo_backend.exception;

import com.tallyo.tallyo_backend.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.DateTimeException;
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiError> handleInvalidRequest(
            InvalidRequestException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.badRequest().body(
                buildError("BAD_REQUEST", ex.getMessage(), null, request)
        );
    }

    @ExceptionHandler(DateTimeException.class)
    public ResponseEntity<ApiError> handleDateTimeException(
            DateTimeException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.badRequest().body(
                buildError("INVALID_TIMEZONE", "Invalid timezone", ex.getMessage(), request)
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpectedException(
            Exception ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                buildError(
                        "INTERNAL_ERROR",
                        "An unexpected error occurred",
                        extractDetails(ex),
                        request
                )
        );
    }

    private String extractDetails(Throwable throwable) {
        if (throwable == null) {
            return null;
        }

        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }

        String top = throwable.getMessage();
        String bottom = root.getMessage();

        if (bottom == null || bottom.isBlank() || root == throwable) {
            return top;
        }

        String rootSummary = root.getClass().getSimpleName() + ": " + bottom;
        return (top == null || top.isBlank()) ? rootSummary : top + " | root cause: " + rootSummary;
    }

    private ApiError buildError(String code, String message, String details, HttpServletRequest request) {
        return ApiError.builder()
                .code(code)
                .message(message)
                .details(details)
                .path(request.getRequestURI())
                .timestamp(Instant.now().toString())
                .build();
    }
}
