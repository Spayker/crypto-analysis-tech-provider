package com.spayker.crypto.analysis.dao.socket;

import com.spayker.crypto.analysis.config.SocketProviderConfig;
import com.spayker.crypto.analysis.dao.socket.bybit.PublicSocketSessionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicWebSocketManagerTest {

    @Mock
    private PublicSocketSessionHandler publicSocketSessionHandler;

    @Mock
    private SocketProviderConfig socketProviderConfig;

    @Mock
    private ReconnectWebSocketManager reconnectWebSocketManager;

    private PublicWebSocketManager publicWebSocketManager;

    @BeforeEach
    void setUp() {
        when(socketProviderConfig.getUrl())
                .thenReturn("ws://localhost:8080/ws");

        publicWebSocketManager = new PublicWebSocketManager(
                socketProviderConfig,
                publicSocketSessionHandler
        );

        ReflectionTestUtils.setField(
                PublicWebSocketManager.class,
                "reconnectWebSocketManager",
                reconnectWebSocketManager
        );
    }

    @Test
    void startListening_shouldSetSymbolsAndStartWebSocket() {
        // given
        var symbol = "BTCUSDT";

        // when
        publicWebSocketManager.startListening(symbol);

        // then
        verify(publicSocketSessionHandler).subscribeSymbol(symbol);
        verify(reconnectWebSocketManager).setAutoStartup(true);
        verify(reconnectWebSocketManager).start();
    }

    @Test
    void stopListening_shouldStopWebSocket() {
        // given
        var symbol = "BTCUSDT";

        // when
        publicWebSocketManager.stopListening(symbol);

        // then
        verify(publicSocketSessionHandler).unsubscribeSymbol(symbol);
    }

    @Test
    void reconnect_shouldRestartWebSocket() {
        // given
        // when
        PublicWebSocketManager.reconnect();

        // then
        verify(reconnectWebSocketManager).setAutoStartup(false);
        verify(reconnectWebSocketManager).stop();
        verify(reconnectWebSocketManager).setAutoStartup(true);
        verify(reconnectWebSocketManager).start();
    }
}

