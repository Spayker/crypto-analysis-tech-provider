package com.spayker.crypto.analysis.dao.socket.exchange.bybit;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.spayker.crypto.analysis.config.SocketProviderConfig;
import com.spayker.crypto.analysis.dao.socket.exchange.KlineSocketMessage;
import com.spayker.crypto.analysis.dao.socket.exchange.TickerSocketMessage;
import com.spayker.crypto.analysis.dao.socket.exchange.bybit.dto.MessageTypes;
import com.spayker.crypto.analysis.dao.socket.exchange.bybit.dto.PingMessage;
import com.spayker.crypto.analysis.dao.socket.exchange.bybit.dto.SocketMessageRequest;
import com.spayker.crypto.analysis.dao.socket.exchange.bybit.handler.KlineHandler;
import com.spayker.crypto.analysis.dao.socket.exchange.bybit.handler.SocketMessageHandler;
import com.spayker.crypto.analysis.dao.socket.exchange.bybit.handler.TickerHandler;
import com.spayker.crypto.analysis.dao.socket.exchange.bybit.handler.WebSocketReconnectEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final Gson mGson = new Gson();

    private final SocketProviderConfig socketConfig;
    private final SocketDataTransfer socketDataTransfer;
    private final ApplicationEventPublisher eventPublisher;

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
    public void afterConnectionEstablished(WebSocketSession session) {
        activeSessions.add(session);
        log.info("New WS session {} established", session.getId());
        sendSubscribeForAllSymbols(session);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        log.debug(SESSION_ID_MSG, session.getId());
        String payload = message.getPayload().toString();

        try {
            if (payload.contains(MessageTypes.TICKERS.getValue())) {
                TickerSocketMessage tickerMsg =
                        (TickerSocketMessage) messageHandlers
                                .get(MessageTypes.TICKERS)
                                .handleMessage(message);

                socketDataTransfer.transferTickerData(tickerMsg);
            }

            if (payload.contains(MessageTypes.KLINE.getValue())) {
                KlineSocketMessage klineMsg =
                        (KlineSocketMessage) messageHandlers
                                .get(MessageTypes.KLINE)
                                .handleMessage(message);

                String symbol = getSymbolFromKlinePayload(payload);
                socketDataTransfer.transferKlineData(symbol, klineMsg);
            }

            schedulePingIfNeeded(session);

        } catch (Exception e) {
            log.error("WS message handling failed", e);
            publishReconnectEvent("message handling error");
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WS transport error [{}]: {}", session.getId(), exception.getMessage());
        publishReconnectEvent("transport error");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        activeSessions.remove(session);
        log.warn("WS session {} closed: {}", session.getId(), status.getReason());
        publishReconnectEvent("connection closed");
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    public void subscribeSymbol(String symbol) {
        symbols.add(symbol);
        broadcast(buildMessage("subscribe", preparePairedNameTopics(symbol)));
        log.info("Subscribed to symbol: {}", symbol);
    }

    public void unsubscribeSymbol(String symbol) {
        if (symbols.remove(symbol)) {
            broadcast(buildMessage("unsubscribe", preparePairedNameTopics(symbol)));
            log.info("Unsubscribed from symbol: {}", symbol);
        }
    }

    private void sendSubscribeForAllSymbols(WebSocketSession session) {
        for (String symbol : symbols) {
            sendToSession(session,
                    buildMessage("subscribe", preparePairedNameTopics(symbol)));
        }
        log.info("Resubscribed {} symbols on new session {}", symbols.size(), session.getId());
    }

    private void schedulePingIfNeeded(WebSocketSession session) {
        lastPingTime.compute(session.getId(), (id, last) -> {
            long now = System.currentTimeMillis();
            if (last == null || now - last >= 10_000) {
                sendPingMessage(session);
                return now;
            }
            return last;
        });
    }

    private void sendPingMessage(WebSocketSession session) {
        PingMessage ping = new PingMessage("100001", "ping");
        TextMessage msg = new TextMessage(mGson.toJson(ping));

        synchronized (lockObj) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(msg);
                } else {
                    publishReconnectEvent("ping session closed");
                }
            } catch (IOException e) {
                log.warn("Ping failed: {}", e.getMessage());
                publishReconnectEvent("ping failed");
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
                log.error("Send failed [{}]: {}", session.getId(), e.getMessage());
                publishReconnectEvent("send failed");
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

    private void publishReconnectEvent(String reason) {
        eventPublisher.publishEvent(new WebSocketReconnectEvent(this, reason));
    }
}