package com.spayker.crypto.analysis.service.validator.exception;

import java.util.Map;

public abstract class BusinessRuleViolationException extends RuntimeException {

    private final String code;
    private final Map<String, String> details;

    protected BusinessRuleViolationException(
            String code,
            String message,
            Map<String, String> details) {

        super(message);
        this.code = code;
        this.details = details == null ? Map.of() : Map.copyOf(details);
    }

    protected BusinessRuleViolationException(String code, String message) {
        this(code, message, Map.of());
    }

    public String getCode() {
        return code;
    }

    public Map<String, String> getDetails() {
        return details;
    }
}

