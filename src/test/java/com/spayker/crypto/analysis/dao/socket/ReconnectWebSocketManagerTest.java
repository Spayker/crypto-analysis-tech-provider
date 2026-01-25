package com.spayker.crypto.analysis.dao.socket;

import com.spayker.crypto.analysis.config.SocketProviderConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.Lifecycle;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReconnectWebSocketManagerTest {

    @Mock
    private WebSocketHandler webSocketHandler;

    @Mock
    private SocketProviderConfig socketProviderConfig;

    @Mock
    private WebSocketSession webSocketSession;

    private WebSocketClient webSocketClient;

    private ReconnectWebSocketManager manager;

    @BeforeEach
    void setUp() {
        lenient().when(socketProviderConfig.getUrl()).thenReturn("ws://localhost:8080/ws");
        webSocketClient = mock(WebSocketClient.class, withSettings().extraInterfaces(Lifecycle.class));
        manager = spy(new ReconnectWebSocketManager(webSocketHandler, socketProviderConfig));
        ReflectionTestUtils.setField(manager, "webSocketClient", webSocketClient);
        lenient().doNothing().when(manager).openConnection();
    }

    @Test
    void startInternal_shouldStartWebSocketClient_ifLifecycleAndNotRunning() {
        Lifecycle lifecycle = (Lifecycle) webSocketClient;
        when(lifecycle.isRunning()).thenReturn(false);
        manager.startInternal();
        verify(lifecycle).start();
    }

    @Test
    void stopInternal_shouldStopWebSocketClient_ifLifecycleAndRunning() throws Exception {
        Lifecycle lifecycle = (Lifecycle) webSocketClient;
        when(lifecycle.isRunning()).thenReturn(true);
        manager.stopInternal();
        verify(lifecycle).stop();
    }

    @Test
    void closeConnection_shouldCloseSession_ifExists() throws Exception {
        ReflectionTestUtils.setField(manager, "webSocketSession", webSocketSession);
        manager.closeConnection();
        verify(webSocketSession).close();
    }

    @Test
    void closeConnection_shouldDoNothing_ifSessionIsNull() throws Exception {
        ReflectionTestUtils.setField(manager, "webSocketSession", null);
        manager.closeConnection();
        verifyNoInteractions(webSocketSession);
    }

    @Test
    void isConnected_shouldReturnTrue_whenSessionOpen() {
        when(webSocketSession.isOpen()).thenReturn(true);
        ReflectionTestUtils.setField(manager, "webSocketSession", webSocketSession);
        assertTrue(manager.isConnected());
    }

    @Test
    void isConnected_shouldReturnFalse_whenSessionClosed() {
        when(webSocketSession.isOpen()).thenReturn(false);
        ReflectionTestUtils.setField(manager, "webSocketSession", webSocketSession);
        assertFalse(manager.isConnected());
    }

    @Test
    void isConnected_shouldReturnFalse_whenSessionNull() {
        ReflectionTestUtils.setField(manager, "webSocketSession", null);
        assertFalse(manager.isConnected());
    }
}