package com.spayker.crypto.analysis.dto.indicator;

import lombok.Builder;

@Builder
public record IndicatorRequest(
        String name,
        String symbol,
        TimeFrame timeFrame
) {}