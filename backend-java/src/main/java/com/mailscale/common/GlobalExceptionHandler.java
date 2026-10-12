package com.mailscale.common;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> unauthorized(UnauthorizedException ex,
                                                   jakarta.servlet.http.HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(401, "UNAUTHORIZED", ex.getMessage(),
                        request.getRequestURI(), CorrelationHolder.get()));
    }

    @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
    public ResponseEntity<ApiError> forbidden(RuntimeException ex,
                                                jakarta.servlet.http.HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of(403, "FORBIDDEN", ex.getMessage(),
                        request.getRequestURI(), CorrelationHolder.get()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> notFound(NotFoundException ex,
                                               jakarta.servlet.http.HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(404, "NOT_FOUND", ex.getMessage(),
                        request.getRequestURI(), CorrelationHolder.get()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> conflict(ConflictException ex,
                                               jakarta.servlet.http.HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(409, "CONFLICT", ex.getMessage(),
                        request.getRequestURI(), CorrelationHolder.get()));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiError> rateLimited(RateLimitExceededException ex,
                                                  jakarta.servlet.http.HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiError.of(429, "RATE_LIMITED", ex.getMessage(),
                        request.getRequestURI(), CorrelationHolder.get()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> badRequest(IllegalArgumentException ex,
                                                 jakarta.servlet.http.HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(400, "BAD_REQUEST", ex.getMessage(),
                        request.getRequestURI(), CorrelationHolder.get()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> invalidState(IllegalStateException ex,
                                                   jakarta.servlet.http.HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(409, "INVALID_STATE", ex.getMessage(),
                        request.getRequestURI(), CorrelationHolder.get()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex,
                                                 jakarta.servlet.http.HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(400, "VALIDATION_FAILED", message,
                        request.getRequestURI(), CorrelationHolder.get()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> constraintViolation(ConstraintViolationException ex,
                                                          jakarta.servlet.http.HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(400, "VALIDATION_FAILED", ex.getMessage(),
                        request.getRequestURI(), CorrelationHolder.get()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException ex,
                                                 jakarta.servlet.http.HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(400, "BAD_REQUEST", "Request body is not readable",
                        request.getRequestURI(), CorrelationHolder.get()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception ex,
                                                 jakarta.servlet.http.HttpServletRequest request) {
        log.error("Unexpected error [{}]", CorrelationHolder.get(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(500, "INTERNAL_ERROR",
                        "An unexpected error occurred",
                        request.getRequestURI(), CorrelationHolder.get()));
    }
}
