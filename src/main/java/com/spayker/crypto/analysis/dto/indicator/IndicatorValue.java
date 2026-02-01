package com.spayker.crypto.analysis.dto.indicator;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class IndicatorValue {
    private String value;
    private long timestamp; // начало периода в UTC millis
}
