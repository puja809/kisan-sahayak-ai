package com.farmer.user.config;

import com.farmer.user.dto.ErrorResponse;
import com.farmer.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for consistent error responses.
 * Requirements: 11.7
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String SUPPORT_CONTACT = "+91-1800-XXX-XXXX";

    /**
     * Handle authentication exceptions.
     * Requirements: 11.7
     */
    @ExceptionHandler(UserService.AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(UserService.AuthenticationException e) {
        log.warn("Authentication error: {} - {}", e.getErrorCode(), e.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .code(e.getErrorCode())
                .message(e.getMessage())
                .supportContact(SUPPORT_CONTACT)
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle user not found exceptions.
     */
    @ExceptionHandler(UserService.UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserService.UserNotFoundException e) {
        log.warn("User not found: {}", e.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .code("USER_NOT_FOUND")
                .message(e.getMessage())
                .supportContact(SUPPORT_CONTACT)
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handle validation exceptions.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("Validation error: {}", e.getMessage());

        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse error = ErrorResponse.builder()
                .code("VALIDATION_ERROR")
                .message("Validation failed")
                .details(errors)
                .supportContact(SUPPORT_CONTACT)
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Handle access denied exceptions.
     * Requirements: 22.5
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .code("ACCESS_DENIED")
                .message("You do not have permission to access this resource")
                .supportContact(SUPPORT_CONTACT)
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handle all other exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);

        ErrorResponse error = ErrorResponse.builder()
                .code("INTERNAL_ERROR")
                .message("An unexpected error occurred. Please try again later.")
                .supportContact(SUPPORT_CONTACT)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}