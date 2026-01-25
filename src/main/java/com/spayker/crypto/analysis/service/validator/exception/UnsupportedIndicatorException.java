package com.spayker.crypto.analysis.service.validator.exception;

import java.util.Map;

public class UnsupportedIndicatorException extends BusinessRuleViolationException {

    public UnsupportedIndicatorException(String indicator) {
        super(
                "UNSUPPORTED_INDICATOR",
                "Indicator is not supported",
                Map.of("indicator", indicator)
        );
    }
}

