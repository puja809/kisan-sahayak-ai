package com.farmer.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Standard error response DTO for API errors.
 * Requirements: 11.7
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private String code;
    private String message;
    private String messageEn;
    private Map<String, String> details;
    private LocalDateTime timestamp;
    private String requestId;
    private String supportContact;

    /**
     * Create a simple error response.
     */
    public static ErrorResponse of(String code, String message) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create an error response with details.
     */
    public static ErrorResponse of(String code, String message, Map<String, String> details) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create an error response with support contact.
     */
    public static ErrorResponse withSupport(String code, String message, String supportContact) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .supportContact(supportContact)
                .timestamp(LocalDateTime.now())
                .build();
    }
}