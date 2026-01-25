package com.spayker.crypto.analysis.controller.advice;

import com.spayker.crypto.analysis.service.validator.exception.BusinessRuleViolationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class TechIndicatorControllerAdvice {

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException ex) {

        Map<String, String> errors = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (a, b) -> a
                ));

        return ResponseEntity.badRequest().body(
                new ApiError(
                        "INVALID_PARAMETER",
                        "Request parameter validation failed",
                        errors
                )
        );
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiError> handleBindException(BindException ex) {
        Map<String, String> errors = ex.getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> Objects.requireNonNullElse(fe.getDefaultMessage(), "Invalid value"),
                        (a, b) -> a
                ));

        return ResponseEntity.badRequest().body(
                new ApiError(
                        "INVALID_REQUEST",
                        "Invalid request parameters",
                        errors
                )
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParams(
            MissingServletRequestParameterException ex) {

        return ResponseEntity.badRequest().body(
                new ApiError(
                        "MISSING_PARAMETER",
                        ex.getParameterName() + " is required",
                        Map.of("parameter", ex.getParameterName())
                )
        );
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ApiError> handleBusinessRuleViolation(
            BusinessRuleViolationException ex) {

        return ResponseEntity.unprocessableEntity().body(
                new ApiError(
                        ex.getCode(),
                        ex.getMessage(),
                        ex.getDetails()
                )
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiError(
                        "INTERNAL_ERROR",
                        "Unexpected error occurred",
                        Map.of()
                )
        );
    }
}