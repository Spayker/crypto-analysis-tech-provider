package com.spayker.crypto.analysis.dao.socket.exchange;

import com.spayker.crypto.analysis.dao.socket.exchange.bybit.PublicSocketSessionHandler;
import com.spayker.crypto.analysis.dao.socket.exchange.bybit.handler.WebSocketReconnectEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PublicWebSocketManagerTest {

    @Mock
    private PublicSocketSessionHandler socketHandler; // ← Мокируем

    @Mock
    private ExchangeConnectionSupportManager exchangeConnectionSupportManager;

    private PublicWebSocketManager webSocketManager;

    @BeforeEach
    void setUp() {
        webSocketManager = new PublicWebSocketManager(exchangeConnectionSupportManager, socketHandler);
    }

    @Test
    void startListening_shouldSubscribeSymbolAndStartManager() {
        // given
        String symbol = "BTCUSDT";

        // when
        webSocketManager.startListening(symbol);

        // then
        verify(socketHandler).subscribeSymbol(symbol);
        verify(exchangeConnectionSupportManager).setAutoStartup(true);
        verify(exchangeConnectionSupportManager).start();
    }

    @Test
    void stopListening_shouldUnsubscribeSymbol() {
        // given
        String symbol = "BTCUSDT";

        // when
        webSocketManager.stopListening(symbol);

        // then
        verify(socketHandler).unsubscribeSymbol(symbol);
    }

    @Test
    void onReconnectEvent_shouldRestartExchangeConnection() {
        // given
        WebSocketReconnectEvent event = new WebSocketReconnectEvent(this, "Test reconnect");

        // when
        webSocketManager.onReconnectEvent(event);

        // then
        verify(exchangeConnectionSupportManager).setAutoStartup(false);
        verify(exchangeConnectionSupportManager).stop();
        verify(exchangeConnectionSupportManager).setAutoStartup(true);
        verify(exchangeConnectionSupportManager).start();

        AtomicBoolean reconnectingField = (AtomicBoolean)
                org.springframework.test.util.ReflectionTestUtils.getField(webSocketManager, "reconnecting");
        assertNotNull(reconnectingField);
        assertFalse(reconnectingField.get());
    }

    @Test
    void onReconnectEvent_shouldSkipIfAlreadyReconnecting() {
        // given
        WebSocketReconnectEvent event = new WebSocketReconnectEvent(this,"Test reconnect");
        org.springframework.test.util.ReflectionTestUtils.setField(webSocketManager, "reconnecting", new AtomicBoolean(true));

        // when
        webSocketManager.onReconnectEvent(event);

        // then
        verify(exchangeConnectionSupportManager, never()).stop();
        verify(exchangeConnectionSupportManager, never()).start();
    }
}