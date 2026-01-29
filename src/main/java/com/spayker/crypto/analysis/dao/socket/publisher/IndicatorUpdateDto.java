package com.spayker.crypto.analysis.dao.socket.publisher;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class IndicatorUpdateDto {

    private String symbol;
    private String indicator;
    private String timeframe;
    private String value;

}