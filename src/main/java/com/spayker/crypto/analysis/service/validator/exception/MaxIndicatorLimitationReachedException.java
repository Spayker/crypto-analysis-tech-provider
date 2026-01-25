package com.spayker.crypto.analysis.service.validator.exception;

import java.util.Map;

public class MaxIndicatorLimitationReachedException extends BusinessRuleViolationException {

    public MaxIndicatorLimitationReachedException() {
        super(
                "MAX_LIMIT_INDICATOR",
                "Max indicator limitation reached",
                Map.of()
        );
    }

}
