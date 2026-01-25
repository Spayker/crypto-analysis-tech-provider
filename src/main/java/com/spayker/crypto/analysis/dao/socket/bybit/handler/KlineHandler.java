package com.spayker.crypto.analysis.dao.socket.bybit.handler;

import com.spayker.crypto.analysis.dao.socket.KlineSocketMessage;
import com.spayker.crypto.analysis.dao.socket.SocketMessage;
import com.spayker.crypto.analysis.dao.socket.bybit.dto.KLineDataContainer;
import com.spayker.crypto.analysis.dao.socket.bybit.dto.KLineMessage;
import org.springframework.web.socket.WebSocketMessage;

public class KlineHandler implements SocketMessageHandler {

    @Override
    public SocketMessage handleMessage(WebSocketMessage<?> message) {
        KLineDataContainer kLineDataContainer = mGson.fromJson(message.getPayload().toString(), KLineDataContainer.class);
        final KLineMessage kLineMessage = kLineDataContainer.getRawData().getFirst();
        return KlineSocketMessage.builder()
                .start(kLineMessage.getStart())
                .end(kLineMessage.getEnd())
                .low(kLineMessage.getLow())
                .high(kLineMessage.getHigh())
                .open(kLineMessage.getOpen())
                .close(kLineMessage.getClose())
                .volume(kLineMessage.getVolume())
                .turnover(kLineMessage.getTurnover())
                .timestamp(kLineMessage.getTimestamp())
                .confirm(kLineMessage.isConfirm())
                .interval(kLineMessage.getInterval())
                .build();
    }
}