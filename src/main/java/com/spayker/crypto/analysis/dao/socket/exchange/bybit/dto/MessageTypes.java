package com.spayker.crypto.analysis.dao.socket.exchange.bybit.dto;

public enum MessageTypes {
    TICKERS("tickers."), AUTH("auth"), WALLET("wallet"), ORDER("order"), KLINE("kline");

    private final String value;

    MessageTypes(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
