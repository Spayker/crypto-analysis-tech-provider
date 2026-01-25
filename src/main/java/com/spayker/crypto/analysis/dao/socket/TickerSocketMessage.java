package com.spayker.crypto.analysis.dao.socket;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TickerSocketMessage implements SocketMessage {

    private double lastPrice;
    private double highPrice;
    private Long eventTime;
    private double volume;
    private double tradeSize;
    private double tradePrice;
    private String symbol;

}