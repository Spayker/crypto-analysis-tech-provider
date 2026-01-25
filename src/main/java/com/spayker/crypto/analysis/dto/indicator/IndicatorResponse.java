package com.spayker.crypto.analysis.dto.indicator;

import java.util.List;

public record IndicatorResponse(
        String coin,
        String timeFrame,
        List<String> data
) {}
