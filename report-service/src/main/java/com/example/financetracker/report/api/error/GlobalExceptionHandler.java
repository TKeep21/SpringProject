package com.example.financetracker.report.api.error;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ApiErrorResponse> handleExternalService(ExternalServiceException exception) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiErrorResponse(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(InvalidReportPeriodException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidPeriod(InvalidReportPeriodException exception) {
        return ResponseEntity
                .badRequest()
                .body(new ApiErrorResponse("INVALID_REPORT_PERIOD", exception.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ApiErrorResponse("ACCESS_DENIED", "Access denied"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(new ApiErrorResponse("VALIDATION_FAILED", message));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiErrorResponse> handleBindException(BindException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(new ApiErrorResponse("VALIDATION_FAILED", message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(new ApiErrorResponse("VALIDATION_FAILED", message));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParameter(MissingServletRequestParameterException exception) {
        return ResponseEntity
                .badRequest()
                .body(new ApiErrorResponse("VALIDATION_FAILED", exception.getParameterName() + " is required"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity
                .badRequest()
                .body(new ApiErrorResponse("VALIDATION_FAILED", "Invalid request parameter: " + exception.getName()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception exception) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse("INTERNAL_SERVER_ERROR", "Internal server error"));
    }

    private String formatFieldError(FieldError error) {
        String message = error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage();
        return error.getField() + ": " + message;
    }
}
