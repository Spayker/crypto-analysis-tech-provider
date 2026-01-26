package com.spayker.crypto.analysis.dao.socket.bybit;

import com.spayker.crypto.analysis.config.SocketProviderConfig;
import com.spayker.crypto.analysis.dao.socket.PublicWebSocketManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

import static org.mockito.Mockito.*;

class PublicSocketSessionHandlerTest {

    @Mock
    private SocketProviderConfig socketConfig;

    @Mock
    private SocketDataManager socketDataManager;

    @Mock
    private WebSocketSession session;

    private PublicSocketSessionHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new PublicSocketSessionHandler(socketConfig, socketDataManager);
        handler.getSymbols().add("BTCUSDT");
    }

    @Test
    void afterConnectionEstablished_sendsSubscriptionMessage() throws Exception {
        when(session.isOpen()).thenReturn(true);
        when(socketConfig.getSubscribeMethodName()).thenReturn("subscribe");
        when(socketConfig.getSubscriptions()).thenReturn(List.of("ticker.", "kline."));

        handler.afterConnectionEstablished(session);

        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    void afterConnectionClosed_callsReconnect() {
        try (var mockedStatic = mockStatic(PublicWebSocketManager.class)) {
            handler.afterConnectionClosed(session, CloseStatus.NORMAL);
            mockedStatic.verify(PublicWebSocketManager::reconnect);
        }
    }
}

