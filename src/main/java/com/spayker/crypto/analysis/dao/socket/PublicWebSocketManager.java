package com.spayker.crypto.analysis.dao.socket;

import com.spayker.crypto.analysis.dao.socket.bybit.PublicSocketSessionHandler;
import com.spayker.crypto.analysis.dao.socket.bybit.handler.WebSocketReconnectEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;


@Slf4j
@Service
@RequiredArgsConstructor
public class PublicWebSocketManager {

    private final ExchangeConnectionSupportManager exchangeConnectionSupportManager;
    private final PublicSocketSessionHandler socketHandler;

    private final AtomicBoolean reconnecting = new AtomicBoolean(false);

    public void startListening(String symbol) {
        socketHandler.subscribeSymbol(symbol);
        exchangeConnectionSupportManager.setAutoStartup(true);
        exchangeConnectionSupportManager.start();
    }

    public void stopListening(String symbol) {
        socketHandler.unsubscribeSymbol(symbol);
    }

    @EventListener
    public void onReconnectEvent(WebSocketReconnectEvent event) {
        if (!reconnecting.compareAndSet(false, true)) {
            log.debug("Reconnect already in progress, skipping");
            return;
        }

        try {
            log.warn("WS reconnect requested: {}", event.reason());
            exchangeConnectionSupportManager.setAutoStartup(false);
            exchangeConnectionSupportManager.stop();
            exchangeConnectionSupportManager.setAutoStartup(true);
            exchangeConnectionSupportManager.start();
        } catch (Exception e) {
            log.error("Reconnect failed", e);
        } finally {
            reconnecting.set(false);
        }
    }
}