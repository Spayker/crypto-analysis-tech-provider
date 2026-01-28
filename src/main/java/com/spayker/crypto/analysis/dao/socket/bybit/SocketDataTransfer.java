package com.spayker.crypto.analysis.dao.socket.bybit;

import com.spayker.crypto.analysis.dao.rest.bybit.dto.kline.Kline;
import com.spayker.crypto.analysis.dao.socket.KlineSocketMessage;
import com.spayker.crypto.analysis.dao.socket.TickerSocketMessage;
import com.spayker.crypto.analysis.service.data.history.TradeHistoryManager;
import com.spayker.crypto.analysis.service.data.indicator.IndicatorDataProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class SocketDataTransfer {

    private final TradeHistoryManager tradeHistoryManager;
    private final IndicatorDataProvider indicatorDataProvider;

    public void transferTickerData(TickerSocketMessage tickerSocketMessage) {
        tradeHistoryManager.processTickerClosePriceMessage(
                tickerSocketMessage.getSymbol(),
                tickerSocketMessage.getLastPrice()
        );
        indicatorDataProvider.recalculateIndicatorData();
    }

    public void transferKlineData(String symbol, KlineSocketMessage klineSocketMessage) {
        tradeHistoryManager.processKlineMessage(symbol, Kline.builder()
                .volume(klineSocketMessage.getVolume())
                .lowPrice(klineSocketMessage.getLow())
                .highPrice(klineSocketMessage.getHigh())
                .openPrice(klineSocketMessage.getOpen())
                .closePrice(klineSocketMessage.getClose())
                .startTime(klineSocketMessage.getStart())
                .build());
        indicatorDataProvider.recalculateIndicatorData();
    }
}