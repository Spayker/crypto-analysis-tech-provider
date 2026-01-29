package com.spayker.crypto.analysis.dao.socket.exchange.bybit;

import com.spayker.crypto.analysis.dao.socket.exchange.KlineSocketMessage;
import com.spayker.crypto.analysis.dao.socket.exchange.TickerSocketMessage;
import com.spayker.crypto.analysis.service.data.history.TradeHistoryManager;
import com.spayker.crypto.analysis.service.data.indicator.IndicatorDataProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SocketDataManagerTest {

    @Mock
    private TradeHistoryManager tradeHistoryManager;

    @Mock
    private IndicatorDataProvider indicatorDataProvider;

    private SocketDataTransfer socketDataTransfer;

    @BeforeEach
    void setUp() {
        socketDataTransfer = new SocketDataTransfer(tradeHistoryManager, indicatorDataProvider);
    }

    @Test
    void transferTickerData_shouldCallTradeHistoryAndIndicatorData() {
        // Arrange
        TickerSocketMessage tickerMessage = TickerSocketMessage.builder()
                .symbol("BTCUSDT")
                .lastPrice(50000.0)
                .highPrice(51000.0)
                .volume(10.5)
                .tradePrice(50010.0)
                .tradeSize(0.1)
                .eventTime(System.currentTimeMillis())
                .build();

        // Act
        socketDataTransfer.transferTickerData(tickerMessage);

        // Assert
        verify(tradeHistoryManager).processTickerClosePriceMessage("BTCUSDT", 50000.0);
        verify(indicatorDataProvider).recalculateIndicatorData();
    }

    @Test
    void transferKlineData_shouldCallTradeHistoryAndIndicatorData() {
        // Arrange
        String symbol = "ETHUSDT";
        KlineSocketMessage klineMessage = KlineSocketMessage.builder()
                .open(1000.0)
                .close(1050.0)
                .high(1060.0)
                .low(990.0)
                .volume(120.0)
                .turnover(126000.0)
                .start(123456789L)
                .end(123456799L)
                .interval("1m")
                .confirm(true)
                .timestamp(System.currentTimeMillis())
                .build();

        // Act
        socketDataTransfer.transferKlineData(symbol, klineMessage);

        // Assert
        verify(tradeHistoryManager).processKlineMessage(eq(symbol), argThat(kline ->
                kline.getOpenPrice() == 1000.0 &&
                        kline.getClosePrice() == 1050.0 &&
                        kline.getHighPrice() == 1060.0 &&
                        kline.getLowPrice() == 990.0 &&
                        kline.getVolume() == 120.0 &&
                        kline.getStartTime() == 123456789L
        ));
        verify(indicatorDataProvider).recalculateIndicatorData();
    }
}