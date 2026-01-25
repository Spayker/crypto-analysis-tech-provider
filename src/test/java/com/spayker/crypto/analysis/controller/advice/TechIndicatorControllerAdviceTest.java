package com.spayker.crypto.analysis.controller.advice;

import com.spayker.crypto.analysis.service.validator.exception.BusinessRuleViolationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TechIndicatorControllerAdviceTest {

    private TechIndicatorControllerAdvice advice;

    @BeforeEach
    void setUp() {
        advice = new TechIndicatorControllerAdvice();
    }

    @Test
    void handleConstraintViolation_ShouldReturnBadRequest() {
        // given
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("field1");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be null");
        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        // when
        ResponseEntity<ApiError> response = advice.handleConstraintViolation(ex);

        // then
        assertEquals(400, response.getStatusCodeValue());
        assertEquals("INVALID_PARAMETER", response.getBody().code());
        assertEquals("field1", response.getBody().details().keySet().iterator().next());
        assertEquals("must not be null", response.getBody().details().get("field1"));
    }

    @Test
    void handleBindException_ShouldReturnBadRequest() {
        // given
        BindException ex = mock(BindException.class);
        FieldError fieldError = new FieldError("object", "field2", "invalid value");
        when(ex.getFieldErrors()).thenReturn(List.of(fieldError));

        // when
        ResponseEntity<ApiError> response = advice.handleBindException(ex);

        // then
        assertEquals(400, response.getStatusCodeValue());
        assertEquals("INVALID_REQUEST", response.getBody().code());
        assertEquals("field2", response.getBody().details().keySet().iterator().next());
        assertEquals("invalid value", response.getBody().details().get("field2"));
    }

    @Test
    void handleMissingParams_ShouldReturnBadRequest() {
        // given
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("timeFrame", "String");

        // when
        ResponseEntity<ApiError> response = advice.handleMissingParams(ex);

        // then
        assertEquals(400, response.getStatusCodeValue());
        assertEquals("MISSING_PARAMETER", response.getBody().code());
        assertEquals("timeFrame", response.getBody().details().get("parameter"));
        assertTrue(response.getBody().message().contains("timeFrame"));
    }

    @Test
    void handleBusinessRuleViolation_ShouldReturnUnprocessableEntity() {
        // given
        BusinessRuleViolationException ex = mock(BusinessRuleViolationException.class);
        when(ex.getCode()).thenReturn("RULE_VIOLATION");
        when(ex.getMessage()).thenReturn("Business rule broken");
        when(ex.getDetails()).thenReturn(Map.of("field", "value"));

        // when
        ResponseEntity<ApiError> response = advice.handleBusinessRuleViolation(ex);

        // then
        assertEquals(422, response.getStatusCodeValue());
        assertEquals("RULE_VIOLATION", response.getBody().code());
        assertEquals("Business rule broken", response.getBody().message());
        assertEquals("value", response.getBody().details().get("field"));
    }

    @Test
    void handleUnexpected_ShouldReturnInternalServerError() {
        // given
        Exception ex = new RuntimeException("something went wrong");

        // when
        ResponseEntity<ApiError> response = advice.handleUnexpected(ex);

        // then
        assertEquals(500, response.getStatusCodeValue());
        assertEquals("INTERNAL_ERROR", response.getBody().code());
        assertEquals("Unexpected error occurred", response.getBody().message());
        assertTrue(response.getBody().details().isEmpty());
    }
}