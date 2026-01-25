package com.spayker.crypto.analysis.dao.socket;

import com.spayker.crypto.analysis.config.SocketProviderConfig;
import com.spayker.crypto.analysis.dao.socket.bybit.PublicSocketSessionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
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

    public void stopListening() {
        reconnectWebSocketManager.setAutoStartup(false);
        reconnectWebSocketManager.stop();
        log.info("Public socket listening is stopped.");
    }

    public void startListening(List<String> symbols) {
        publicSocketSessionHandler.setSymbols(symbols);
        reconnectWebSocketManager.setAutoStartup(true);
        reconnectWebSocketManager.start();
        log.info("Public socket listening is started.");
    }

    public static void reconnect() {
        log.warn("Reconnecting to public socket API");
        reconnectWebSocketManager.setAutoStartup(false);
        reconnectWebSocketManager.stop();
        reconnectWebSocketManager.setAutoStartup(true);
        reconnectWebSocketManager.start();
    }
}
