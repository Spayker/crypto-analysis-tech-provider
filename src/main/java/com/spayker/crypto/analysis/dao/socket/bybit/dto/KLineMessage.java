package com.spayker.crypto.analysis.dao.socket.bybit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KLineMessage {

    private long start;
    private long end;
    private String interval;
    private double open;
    private double close;
    private double high;
    private double low;
    private double volume;
    private double turnover;
    private boolean confirm;
    private long timestamp;
}
