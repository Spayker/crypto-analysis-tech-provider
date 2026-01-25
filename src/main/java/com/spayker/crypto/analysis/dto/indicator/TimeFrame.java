package com.spayker.crypto.analysis.dto.indicator;

import lombok.Getter;

public enum TimeFrame {
    DAY("day"), HOUR("hour"), MINUTE("minute");

    @Getter
    private final String value;

    TimeFrame(String value) {
        this.value = value;
    }
}
