package com.spayker.crypto.analysis.dao.socket.bybit.handler;

import com.spayker.crypto.analysis.dao.socket.KlineSocketMessage;
import com.spayker.crypto.analysis.dao.socket.SocketMessage;
import com.spayker.crypto.analysis.dao.socket.bybit.dto.KLineDataContainer;
import com.spayker.crypto.analysis.dao.socket.bybit.dto.KLineMessage;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;

import static org.junit.jupiter.api.Assertions.*;

class KlineHandlerTest {

    private KlineHandler klineHandler;
    private Gson gson;

    @BeforeEach
    void setUp() {
        klineHandler = new KlineHandler();
        gson = new Gson(); // только для подготовки JSON
    }

    @Test
    void handleMessage_ShouldReturnCorrectKlineSocketMessage() {
        // given
        KLineDataContainer container = getKLineDataContainer();

        String jsonPayload = gson.toJson(container);
        WebSocketMessage<String> webSocketMessage = new TextMessage(jsonPayload);

        // when
        SocketMessage result = klineHandler.handleMessage(webSocketMessage);

        // then
        assertInstanceOf(KlineSocketMessage.class, result);
        KlineSocketMessage klineResult = (KlineSocketMessage) result;

        assertEquals(100L, klineResult.getStart());
        assertEquals(200L, klineResult.getEnd());
        assertEquals(10.0, klineResult.getOpen());
        assertEquals(20.0, klineResult.getClose());
        assertEquals(25.0, klineResult.getHigh());
        assertEquals(5.0, klineResult.getLow());
        assertEquals(1000.0, klineResult.getVolume());
        assertEquals(2000.0, klineResult.getTurnover());
        assertEquals(123456789L, klineResult.getTimestamp());
        assertTrue(klineResult.isConfirm());
        assertEquals("1m", klineResult.getInterval());
    }

    @Test
    void handleMessage_ShouldThrowExceptionForInvalidJson() {
        WebSocketMessage<String> invalidMessage = new TextMessage("invalid json");
        assertThrows(com.google.gson.JsonSyntaxException.class, () ->
                klineHandler.handleMessage(invalidMessage)
        );
    }

    @Test
    void handleMessage_ShouldReturnFirstKLine_WhenMultipleKLines() {
        // given
        KLineMessage first = new KLineMessage();
        first.setStart(1L);
        first.setEnd(2L);

        KLineMessage second = new KLineMessage();
        second.setStart(3L);
        second.setEnd(4L);

        KLineDataContainer container = new KLineDataContainer();
        container.setRawData(java.util.Arrays.asList(first, second));

        String jsonPayload = gson.toJson(container);
        WebSocketMessage<String> message = new TextMessage(jsonPayload);

        // when
        KlineSocketMessage result = (KlineSocketMessage) klineHandler.handleMessage(message);

        // then
        assertEquals(1L, result.getStart());
        assertEquals(2L, result.getEnd());
    }

    private static KLineDataContainer getKLineDataContainer() {
        KLineMessage kLineMessage = new KLineMessage();
        kLineMessage.setStart(100L);
        kLineMessage.setEnd(200L);
        kLineMessage.setOpen(10.0);
        kLineMessage.setClose(20.0);
        kLineMessage.setHigh(25.0);
        kLineMessage.setLow(5.0);
        kLineMessage.setVolume(1000.0);
        kLineMessage.setTurnover(2000.0);
        kLineMessage.setTimestamp(123456789L);
        kLineMessage.setConfirm(true);
        kLineMessage.setInterval("1m");

        KLineDataContainer container = new KLineDataContainer();
        container.setRawData(java.util.Collections.singletonList(kLineMessage));
        return container;
    }
}

