package com.spayker.crypto.analysis.dao.socket.bybit;

import com.spayker.crypto.analysis.config.SocketProviderConfig;
import com.spayker.crypto.analysis.dao.socket.bybit.handler.WebSocketReconnectEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicSocketSessionHandlerTest {

    @Mock
    private SocketProviderConfig socketConfig;

    @Mock
    private SocketDataTransfer socketDataTransfer;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private WebSocketSession session;

    private PublicSocketSessionHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new PublicSocketSessionHandler(socketConfig, socketDataTransfer, eventPublisher);
        handler.getSymbols().add("BTCUSDT");
    }

    @Test
    void afterConnectionEstablished_sendsSubscriptionMessage() throws Exception {
        // given
        when(session.isOpen()).thenReturn(true);
        when(socketConfig.getSubscriptions()).thenReturn(List.of("ticker.", "kline."));

        // when
        handler.afterConnectionEstablished(session);

        // then
        verify(session, atLeastOnce()).sendMessage(any(TextMessage.class));
    }

    @Test
    void afterConnectionClosed_publishesReconnectEvent() {
        // given
        CloseStatus status = CloseStatus.NORMAL;

        // when
        handler.afterConnectionClosed(session, status);

        // then
        verify(eventPublisher).publishEvent(argThat(event ->
                event instanceof WebSocketReconnectEvent &&
                        "connection closed".equals(((WebSocketReconnectEvent) event).reason())
        ));
    }

    @Test
    void subscribeSymbol_addsSymbol() {
        // given
        String symbol = "ETHUSDT";

        // when
        handler.subscribeSymbol(symbol);

        // then
        assertTrue(handler.getSymbols().contains(symbol));
    }

    @Test
    void unsubscribeSymbol_removesSymbol() {
        // given
        // when
        handler.unsubscribeSymbol("BTCUSDT");

        // then
        assertFalse(handler.getSymbols().contains("BTCUSDT"));
    }
}