package com.spayker.crypto.analysis.dao.socket.bybit;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.spayker.crypto.analysis.config.SocketProviderConfig;
import com.spayker.crypto.analysis.dao.socket.KlineSocketMessage;
import com.spayker.crypto.analysis.dao.socket.PublicWebSocketManager;
import com.spayker.crypto.analysis.dao.socket.TickerSocketMessage;
import com.spayker.crypto.analysis.dao.socket.bybit.dto.MessageTypes;
import com.spayker.crypto.analysis.dao.socket.bybit.dto.PingMessage;
import com.spayker.crypto.analysis.dao.socket.bybit.dto.SocketMessageRequest;
import com.spayker.crypto.analysis.dao.socket.bybit.handler.KlineHandler;
import com.spayker.crypto.analysis.dao.socket.bybit.handler.SocketMessageHandler;
import com.spayker.crypto.analysis.dao.socket.bybit.handler.TickerHandler;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@Profile("bybit")
@RequiredArgsConstructor
public class PublicSocketSessionHandler implements WebSocketHandler {

    private final Gson gson = new Gson();
    private final SocketProviderConfig socketConfig;
    private final SocketDataManager socketDataManager;

    private final Gson mGson = new Gson();
    private final Object lockObj = new Object();
    private final Map<String, Long> lastPingTime = new ConcurrentHashMap<>();
    private final Set<WebSocketSession> activeSessions = ConcurrentHashMap.newKeySet();

    private static final String SESSION_ID_MSG = "Session id: {}";

    @Getter
    private final Set<String> symbols = ConcurrentHashMap.newKeySet();

    private final Map<MessageTypes, SocketMessageHandler> messageHandlers = Map.of(
            MessageTypes.TICKERS, new TickerHandler(),
            MessageTypes.KLINE, new KlineHandler()
    );

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        activeSessions.add(session);
        log.info("New WS session {} established", session.getId());
        sendSubscribeForAllSymbols(session);
    }

    @Override
    public void handleMessage(final WebSocketSession session, final WebSocketMessage<?> message) {
        log.debug(SESSION_ID_MSG, session.getId());
        final String payload = message.getPayload().toString();

        if (payload.contains(MessageTypes.TICKERS.getValue())) {
            TickerSocketMessage tickerSocketMessage = (TickerSocketMessage) messageHandlers.get(MessageTypes.TICKERS).handleMessage(message);
            socketDataManager.transferTickerData(tickerSocketMessage);
        }

        if (payload.contains(MessageTypes.KLINE.getValue())) {
            KlineSocketMessage klineSocketMessage = (KlineSocketMessage) messageHandlers.get(MessageTypes.KLINE).handleMessage(message);
            String symbol = getSymbolFromKlinePayload(payload);
            socketDataManager.transferKlineData(symbol, klineSocketMessage);
        }

        lastPingTime.compute(session.getId(), (id, lastTime) -> {
            long now = System.currentTimeMillis();
            if (lastTime == null || (now - lastTime) >= 10_000) {
                sendPingMessage(session);
                return now;
            }
            return lastTime;
        });
    }

    @Override
    public void handleTransportError(final WebSocketSession session, final Throwable exception) {
        log.debug(SESSION_ID_MSG, session.getId());
        log.error("Socket transport error occurred: {}", exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        activeSessions.remove(session);
        log.warn("WS session {} closed: {}", session.getId(), status.getReason());

        PublicWebSocketManager.reconnect();
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    public void subscribeSymbol(String symbol) {
        symbols.add(symbol);
        List<String> topics = preparePairedNameTopics(symbol);

        broadcast(buildMessage("subscribe", topics));
        log.info("Subscribed to symbol: {}", symbol);
    }

    public void unsubscribeSymbol(String symbol) {
        if (symbols.remove(symbol)) {
            List<String> topics = preparePairedNameTopics(symbol);

            broadcast(buildMessage("unsubscribe", topics));
            log.info("Unsubscribed from symbol: {}", symbol);
        }
    }

    private void sendSubscribeForAllSymbols(WebSocketSession session) {
        for (String symbol : symbols) {
            List<String> topics = preparePairedNameTopics(symbol);
            sendToSession(session, buildMessage("subscribe", topics));
        }
        log.info("Resubscribed {} symbols on new session {}", symbols.size(), session.getId());
    }

    private void sendPingMessage(final WebSocketSession session) {
        final PingMessage pingMessage = new PingMessage("100001", "ping");
        WebSocketMessage<?> sendMessage = new TextMessage(mGson.toJson(pingMessage));
        synchronized (lockObj) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(sendMessage);
                } catch (IOException e) {
                    log.warn(e.getMessage());
                    PublicWebSocketManager.reconnect();
                }
            } else {
                PublicWebSocketManager.reconnect();
            }
        }
    }

    private TextMessage buildMessage(String operation, List<String> topics) {
        SocketMessageRequest req = new SocketMessageRequest(
                "",
                operation,
                topics.toArray(String[]::new)
        );
        return new TextMessage(gson.toJson(req));
    }

    private void broadcast(TextMessage message) {
        for (WebSocketSession session : activeSessions) {
            sendToSession(session, message);
        }
    }

    private void sendToSession(WebSocketSession session, TextMessage message) {
        synchronized (lockObj) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            } catch (Exception e) {
                log.error("Failed to send message to session {}: {}", session.getId(), e.getMessage());
                PublicWebSocketManager.reconnect();
            }
        }
    }

    private List<String> preparePairedNameTopics(String pairName) {
        return socketConfig.getSubscriptions()
                .stream()
                .map(s -> s.concat(pairName.toLowerCase().replace("_", "").toUpperCase()))
                .toList();
    }

    private String getSymbolFromKlinePayload(String payload) {
        JsonObject root = JsonParser.parseString(payload).getAsJsonObject();
        String topic = root.get("topic").getAsString();
        return topic.substring(topic.lastIndexOf('.') + 1).toLowerCase();
    }
}