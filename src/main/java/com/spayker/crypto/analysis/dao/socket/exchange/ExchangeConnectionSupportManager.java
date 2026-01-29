package com.spayker.crypto.analysis.dao.socket.exchange;

import com.spayker.crypto.analysis.config.SocketProviderConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.Lifecycle;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.ConnectionManagerSupport;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.LoggingWebSocketHandlerDecorator;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ExchangeConnectionSupportManager extends ConnectionManagerSupport {

    private final WebSocketClient webSocketClient = new StandardWebSocketClient();
    private final WebSocketHandler webSocketHandler;

    private WebSocketSession webSocketSession;
    private final WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public ExchangeConnectionSupportManager(WebSocketHandler webSocketHandler,
                                            SocketProviderConfig socketProviderConfig,
                                            Object... uriVariables) {
        super(socketProviderConfig.getUrl(), uriVariables);
        this.webSocketHandler = decorateWebSocketHandler(webSocketHandler);
    }


    private WebSocketHandler decorateWebSocketHandler(WebSocketHandler handler) {
        return new LoggingWebSocketHandlerDecorator(handler);
    }

    @Override
    public void startInternal() {
        if (this.webSocketClient instanceof Lifecycle lifecycle && !lifecycle.isRunning()) {
            lifecycle.start();
        }
        super.startInternal();
    }

    @Override
    public void stopInternal() throws Exception {
        if (this.webSocketClient instanceof Lifecycle lifecycle && lifecycle.isRunning()) {
            lifecycle.stop();
        }
        super.stopInternal();
    }

    @Override
    protected void openConnection() {
        URI uri = getUri();
        webSocketClient.execute(webSocketHandler, headers, uri)
                .whenCompleteAsync((establishedWebSocketSession, ex) -> {
                    if (ex == null) {
                        // Success
                        webSocketSession = establishedWebSocketSession;
                        log.trace("Connected to: {}", uri);
                    } else {
                        // Failure
                        log.trace("Failed to connect: {}", ex.getMessage());
                        long delay = 0;
                        if (ex.getMessage() != null) {
                            if (ex.getMessage().contains("403")) {
                                delay = 10; // minutes
                            }
                        }
                        scheduler.schedule(this::openConnection, delay, TimeUnit.MINUTES);
                    }
                });
    }

    @Override
    protected void closeConnection() throws Exception {
        if (this.webSocketSession != null) {
            this.webSocketSession.close();
        }
    }

    @Override
    public boolean isConnected() {
        return (this.webSocketSession != null && this.webSocketSession.isOpen());
    }
}