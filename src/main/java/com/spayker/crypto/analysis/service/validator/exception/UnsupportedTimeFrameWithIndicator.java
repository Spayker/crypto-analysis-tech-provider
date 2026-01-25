package com.spayker.crypto.analysis.service.validator.exception;

import java.util.Map;

public class UnsupportedTimeFrameWithIndicator extends BusinessRuleViolationException {

    public UnsupportedTimeFrameWithIndicator(String indicator, String timeFrame) {
        super(
                "UNSUPPORTED_TIMEFRAME_WITH_INDICATOR",
                "Indicator is not supported with requested timeframe",
                Map.of("indicator", indicator, "timeFrame", timeFrame)
        );
    }
}