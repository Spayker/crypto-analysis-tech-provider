package com.spayker.crypto.analysis.dao.socket.exchange.bybit.handler;

import com.spayker.crypto.analysis.dao.socket.exchange.TickerSocketMessage;
import com.spayker.crypto.analysis.dao.socket.exchange.bybit.dto.TickerDataContainer;
import com.spayker.crypto.analysis.dao.socket.exchange.bybit.dto.TickerMessage;
import com.google.gson.Gson;
import com.spayker.crypto.analysis.dao.socket.exchange.bybit.handler.TickerHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;

import static org.junit.jupiter.api.Assertions.*;

class TickerHandlerTest {

    private TickerHandler tickerHandler;
    private Gson gson;

    @BeforeEach
    void setUp() {
        tickerHandler = new TickerHandler();
        gson = new Gson();
    }

    @Test
    void handleMessage_ShouldReturnCorrectTickerSocketMessage() {
        // given
        TickerMessage tickerMessage = new TickerMessage();
        tickerMessage.setLastPrice("50000.5");
        tickerMessage.setVolume24h("1234.56");
        tickerMessage.setHighPrice24h("51000.0");
        tickerMessage.setSymbol("BTCUSD");

        TickerDataContainer container = new TickerDataContainer();
        container.setRawData(tickerMessage);

        String jsonPayload = gson.toJson(container);
        WebSocketMessage<String> webSocketMessage = new TextMessage(jsonPayload);

        // when
        TickerSocketMessage result = tickerHandler.handleMessage(webSocketMessage);

        // then
        assertNotNull(result);
        assertEquals(50000.5, result.getLastPrice());
        assertEquals(1234.56, result.getVolume());
        assertEquals(51000.0, result.getHighPrice());
        assertEquals("btcusd", result.getSymbol()); // символ приводится к lower case
    }

    @Test
    void handleMessage_ShouldThrowExceptionForInvalidJson() {
        WebSocketMessage<String> invalidMessage = new TextMessage("invalid json");

        assertThrows(com.google.gson.JsonSyntaxException.class, () ->
                tickerHandler.handleMessage(invalidMessage)
        );
    }

    @Test
    void handleMessage_ShouldHandleNumericStringsProperly() {
        // given
        TickerMessage tickerMessage = new TickerMessage();
        tickerMessage.setLastPrice("0");
        tickerMessage.setVolume24h("0");
        tickerMessage.setHighPrice24h("0");
        tickerMessage.setSymbol("ETHUSD");

        TickerDataContainer container = new TickerDataContainer();
        container.setRawData(tickerMessage);

        WebSocketMessage<String> message = new TextMessage(gson.toJson(container));

        // when
        TickerSocketMessage result = tickerHandler.handleMessage(message);

        // then
        assertEquals(0.0, result.getLastPrice());
        assertEquals(0.0, result.getVolume());
        assertEquals(0.0, result.getHighPrice());
        assertEquals("ethusd", result.getSymbol());
    }
}