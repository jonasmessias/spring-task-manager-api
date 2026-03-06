package com.example.taskmanagerapi.infra.exception;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * GlobalExceptionHandler - Centralizes error handling for the entire API.
 * Converts Spring validation errors into clean, consistent JSON responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles @Valid validation failures.
     * Returns a list of field errors with clear messages.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        List<FieldError> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> new FieldError(err.getField(), err.getDefaultMessage()))
                .toList();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ValidationErrorResponse("VALIDATION_ERROR", "Invalid request data.", HttpStatus.BAD_REQUEST.value(), errors));
    }

    /**
     * Handles email authentication failures (wrong SMTP credentials).
     */
    @ExceptionHandler(MailAuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleMailAuthenticationException(MailAuthenticationException ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("EMAIL_AUTH_ERROR", "Email service authentication failed. Please contact support.", HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }

    /**
     * Handles general email sending failures (SMTP unreachable, etc).
     */
    @ExceptionHandler(MailException.class)
    public ResponseEntity<ErrorResponse> handleMailException(MailException ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("EMAIL_SEND_ERROR", "Failed to send email. Please try again later.", HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }

    // DTO records used in the response body
    public record ValidationErrorResponse(String code, String message, int statusCode, List<FieldError> errors) {}
    public record FieldError(String field, String message) {}
    public record ErrorResponse(String code, String message, int statusCode) {}
}
