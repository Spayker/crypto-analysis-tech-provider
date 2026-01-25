package com.spayker.crypto.analysis.service.validator.exception;

import java.util.Map;

public class IndicatorAlreadyExistsException extends BusinessRuleViolationException {

    public IndicatorAlreadyExistsException(String indicator, String timeFrame) {
        super(
                "INDICATOR_ALREADY_EXISTS",
                "Max indicator limitation reached",
                Map.of("indicator", indicator,
                        "timeFrame", timeFrame)
        );
    }
}