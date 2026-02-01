package com.spayker.crypto.analysis.dto.indicator;

import java.util.List;

public record IndicatorResponse<T>(
        String coin,
        String timeFrame,
        List<T> data
) {}
