package com.spayker.crypto.analysis.dao.socket.exchange;


import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class KlineSocketMessage implements SocketMessage {

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