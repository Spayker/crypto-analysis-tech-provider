package com.spayker.crypto.analysis.dao.socket;

import com.spayker.crypto.analysis.config.SocketProviderConfig;
import com.spayker.crypto.analysis.dao.socket.bybit.PublicSocketSessionHandler;
import org.springframework.stereotype.Service;


@Service
public class PublicWebSocketManager {

    private final PublicSocketSessionHandler publicSocketSessionHandler;
    private static ReconnectWebSocketManager reconnectWebSocketManager;

    public PublicWebSocketManager(SocketProviderConfig socketProviderConfig,
                                  PublicSocketSessionHandler publicSocketSessionHandler) {
        this.publicSocketSessionHandler = publicSocketSessionHandler;
        this.reconnectWebSocketManager = new ReconnectWebSocketManager(
                publicSocketSessionHandler,
                socketProviderConfig
        );
    }

    public void startListening(String symbol) {
        publicSocketSessionHandler.subscribeSymbol(symbol);
        reconnectWebSocketManager.setAutoStartup(true);
        reconnectWebSocketManager.start();
    }

    public void stopListening(String symbol) {
        publicSocketSessionHandler.unsubscribeSymbol(symbol);
    }

    public static void reconnect() {
        reconnectWebSocketManager.setAutoStartup(false);
        reconnectWebSocketManager.stop();
        reconnectWebSocketManager.setAutoStartup(true);
        reconnectWebSocketManager.start();
    }
}
