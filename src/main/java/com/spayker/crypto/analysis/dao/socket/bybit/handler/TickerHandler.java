package com.spayker.crypto.analysis.dao.socket.bybit.handler;

import com.spayker.crypto.analysis.dao.socket.TickerSocketMessage;
import com.spayker.crypto.analysis.dao.socket.bybit.dto.TickerDataContainer;
import com.spayker.crypto.analysis.dao.socket.bybit.dto.TickerMessage;
import org.springframework.web.socket.WebSocketMessage;

public class TickerHandler implements SocketMessageHandler {

    @Override
    public TickerSocketMessage handleMessage(WebSocketMessage<?> message) {
        final TickerDataContainer tickerDataContainer = mGson.fromJson(message.getPayload().toString(), TickerDataContainer.class);
        final TickerMessage tickerMessage = tickerDataContainer.getRawData();
        return TickerSocketMessage.builder()
                .lastPrice(Double.parseDouble(tickerMessage.getLastPrice()))
                .volume(Double.parseDouble(tickerMessage.getVolume24h()))
                .highPrice(Double.parseDouble(tickerMessage.getHighPrice24h()))
                .symbol(tickerMessage.getSymbol().toLowerCase())
                .build();
    }
}