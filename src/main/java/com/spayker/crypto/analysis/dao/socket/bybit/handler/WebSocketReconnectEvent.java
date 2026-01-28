package com.spayker.crypto.analysis.dao.socket.bybit.handler;

import org.springframework.context.ApplicationEvent;

public class WebSocketReconnectEvent extends ApplicationEvent {
    private final String reason;

    public WebSocketReconnectEvent(Object source, String reason) {
        super(source);
        this.reason = reason;
    }

    public String reason() {
        return reason;
    }
}