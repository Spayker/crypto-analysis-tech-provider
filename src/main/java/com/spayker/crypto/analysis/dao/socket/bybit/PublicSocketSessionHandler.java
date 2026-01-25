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
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@Profile("bybit")
@RequiredArgsConstructor
public class PublicSocketSessionHandler implements WebSocketHandler {

    private static final String PAIR_DELIMITER = "_";
    private static final String PAIR_DELIMITER_REPLACEMENT = "";
    private static final String SESSION_ID_MSG = "Session id: {}";

    private final Gson mGson = new Gson();
    private final SocketProviderConfig socketConfig;
    private final SocketDataManager socketDataManager;


    private final Object lockObj = new Object();

    @Setter
    private List<String> symbols;

    private final Map<String, Long> lastPingTime = new ConcurrentHashMap<>();

    private final Map<MessageTypes, SocketMessageHandler> messageHandlers = Map.of(
            MessageTypes.TICKERS, new TickerHandler(),
            MessageTypes.KLINE, new KlineHandler()
    );

    @Override
    public void afterConnectionEstablished(final WebSocketSession session) throws Exception {
        log.info("Market's session [{}] has been established", session.getId());
        SocketMessageRequest socketMessage = formSubscriptionMessage();
        WebSocketMessage<?> sendMessage = new TextMessage(mGson.toJson(socketMessage));
        synchronized (lockObj) {
            if (session.isOpen()) {
                session.sendMessage(sendMessage);
            }
        }
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

    @Override
    public void handleTransportError(final WebSocketSession session, final Throwable exception) {
        log.debug(SESSION_ID_MSG, session.getId());
        log.error("Socket transport error occurred: {}", exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(final WebSocketSession session, final CloseStatus closeStatus) {
        log.debug(SESSION_ID_MSG, session.getId());
        log.info("Connection closed: {}", closeStatus.getReason());
        PublicWebSocketManager.reconnect();
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    SocketMessageRequest formSubscriptionMessage() {
        final String subscriptionMethodName = socketConfig.getSubscribeMethodName();
        List<String> topics = new ArrayList<>();
        symbols.forEach(pN -> topics.addAll(preparePairedNameTopics(pN)));
        return new SocketMessageRequest("", subscriptionMethodName, topics.toArray(String[]::new));
    }

    private List<String> preparePairedNameTopics(String pairName) {
        return socketConfig.getSubscriptions()
                .stream()
                .map(sB -> sB.concat(pairName.toLowerCase().replace(PAIR_DELIMITER, PAIR_DELIMITER_REPLACEMENT).toUpperCase()))
                .toList();
    }

    private String getSymbolFromKlinePayload(String payload) {
        JsonObject root = JsonParser.parseString(payload).getAsJsonObject();
        String topic = root.get("topic").getAsString();
        return topic.substring(topic.lastIndexOf('.') + 1).toLowerCase();
    }
}