package com.spayker.crypto.analysis.service.data.history;

import com.spayker.crypto.analysis.config.KlineHistoryConfig;
import com.spayker.crypto.analysis.dao.rest.bybit.dto.kline.Kline;
import com.spayker.crypto.analysis.dto.indicator.FixedDataList;
import com.spayker.crypto.analysis.dto.indicator.TimeFrame;
import com.spayker.crypto.analysis.service.data.ByBitExchangeAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.spayker.crypto.analysis.service.data.KlineTestHelper.createKline;
import static com.spayker.crypto.analysis.service.data.KlineTestHelper.createKlines;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TradeHistoryManagerTest {

    private KlineHistoryConfig klineHistoryConfig;
    private ByBitExchangeAdapter byBitExchangeAdapter;
    private TradeHistoryManager tradeHistoryManager;

    @BeforeEach
    void setUp() {
        klineHistoryConfig = mock(KlineHistoryConfig.class);
        byBitExchangeAdapter = mock(ByBitExchangeAdapter.class);

        when(klineHistoryConfig.getMinuteIntervalType()).thenReturn("1");
        when(klineHistoryConfig.getHourIntervalType()).thenReturn("60");
        when(klineHistoryConfig.getDayIntervalType()).thenReturn("D");
        when(klineHistoryConfig.getMaxMinuteKlineSize()).thenReturn("100");
        when(klineHistoryConfig.getMaxHourKlineSize()).thenReturn("100");
        when(klineHistoryConfig.getMaxDayKlineSize()).thenReturn("100");

        tradeHistoryManager = new TradeHistoryManager(klineHistoryConfig, byBitExchangeAdapter);

        for (TimeFrame tf : TimeFrame.values()) {
            tradeHistoryManager.getTradeHistoryData().put(tf, new ConcurrentHashMap<>());
        }
    }

    @Test
    void getSymbolHistory_shouldReturnHistoryFromAdapter() {
        // given
        String symbol = "btcusdt";
        List<Kline> mockKlines = createKlines(1, 2, 3, 4, 5);
        when(byBitExchangeAdapter.getHistory(symbol, "1", "100")).thenReturn(mockKlines);

        // when
        List<Kline> history = tradeHistoryManager.getSymbolHistory(symbol, TimeFrame.MINUTE);

        // then
        assertThat(history).hasSize(5);
        assertThat(history.getFirst().getClosePrice()).isEqualTo(1);
        assertThat(history.getLast().getClosePrice()).isEqualTo(5);
        verify(byBitExchangeAdapter).getHistory(symbol, "1", "100");
    }

    @Test
    void processTickerClosePriceMessage_shouldUpdateLatestKline() {
        // given
        String symbol = "btcusdt";
        long baseTime = Instant.parse("2026-01-25T12:00:00Z").toEpochMilli();
        List<Kline> klines = List.of(
                createKline(1, baseTime),
                createKline(2, baseTime + 60_000)
        );
        when(byBitExchangeAdapter.getHistory(symbol, "1", "100")).thenReturn(klines);

        // when
        tradeHistoryManager.getSymbolHistory(symbol, TimeFrame.MINUTE);
        tradeHistoryManager.processTickerClosePriceMessage(symbol, 10);

        // then
        FixedDataList<Kline> minuteKlines = tradeHistoryManager.getTradeHistoryData()
                .get(TimeFrame.MINUTE).computeIfAbsent(symbol, k -> new ConcurrentHashMap<>())
                .get(symbol);

        Kline last = minuteKlines.snapshot().getLast();
        assertThat(last.getClosePrice()).isEqualTo(10);
        assertThat(last.getHighPrice()).isEqualTo(10);
    }

    @Test
    void processKlineMessage_shouldAddNewMinuteKline_onlyOnMinuteChange() {
        // given
        String symbol = "btcusdt";
        long baseTime = Instant.parse("2026-01-25T12:00:00Z").toEpochMilli();

        List<Kline> klines = List.of(
                createKline(1, roundToMinute(baseTime)),
                createKline(2, roundToMinute(baseTime + 60_000))
        );
        when(byBitExchangeAdapter.getHistory(symbol, "1", "100")).thenReturn(klines);

        // when
        tradeHistoryManager.getSymbolHistory(symbol, TimeFrame.MINUTE);
        long nextMinute = roundToMinute(baseTime + 2 * 60_000);
        Kline newKline = createKline(3, nextMinute);
        tradeHistoryManager.processKlineMessage(symbol, newKline);

        // then
        FixedDataList<Kline> minuteKlines = tradeHistoryManager.getTradeHistoryData()
                .get(TimeFrame.MINUTE).get(symbol).get(symbol);

        Kline last = minuteKlines.snapshot().getLast();
        assertThat(last.getClosePrice()).isEqualTo(3);
        assertThat(minuteKlines.snapshot().size()).isLessThanOrEqualTo(100);
    }

    @Test
    void processKlineMessage_shouldUpdateLastMinuteKline_withinSameMinute() {
        // given
        String symbol = "btcusdt";
        long baseTime = Instant.parse("2026-01-25T12:00:00Z").toEpochMilli();

        List<Kline> klines = List.of(
                createKline(1, roundToMinute(baseTime)),
                createKline(2, roundToMinute(baseTime + 60_000))
        );
        when(byBitExchangeAdapter.getHistory(symbol, "1", "100")).thenReturn(klines);

        tradeHistoryManager.getSymbolHistory(symbol, TimeFrame.MINUTE);

        long sameMinute = roundToMinute(baseTime + 60_000);
        Kline updateKline = Kline.builder()
                .startTime(sameMinute)
                .openPrice(2)
                .closePrice(5)
                .highPrice(6)
                .lowPrice(1)
                .volume(50)
                .turnover(500)
                .build();

        // when
        tradeHistoryManager.processKlineMessage(symbol, updateKline);

        // then
        FixedDataList<Kline> minuteKlines = tradeHistoryManager.getTradeHistoryData()
                .get(TimeFrame.MINUTE).get(symbol).get(symbol);

        Kline last = minuteKlines.snapshot().getLast();
        assertThat(last.getClosePrice()).isEqualTo(5);
        assertThat(last.getHighPrice()).isEqualTo(6);
        assertThat(last.getLowPrice()).isEqualTo(1);
        assertThat(last.getVolume()).isEqualTo(50);
    }

    @Test
    void processKlineMessage_shouldAddNewMinuteHourDayKlines_atMidnight() {
        // given
        String symbol = "btcusdt";
        List<Kline> oldMinuteKlines = List.of(
                createKline(1, 1),
                createKline(2, 2),
                createKline(3, 3)
        );
        List<Kline> oldHourKlines = List.of(
                createKline(1, 1),
                createKline(2, 2),
                createKline(3, 3)
        );
        List<Kline> oldDayKlines = List.of(
                createKline(1, 1),
                createKline(2, 2),
                createKline(3, 3)
        );

        for (TimeFrame tf : TimeFrame.values()) {
            tradeHistoryManager.getTradeHistoryData().put(tf, new ConcurrentHashMap<>());
        }

        tradeHistoryManager.getTradeHistoryData().get(TimeFrame.MINUTE)
                .put(symbol, new ConcurrentHashMap<>());
        tradeHistoryManager.getTradeHistoryData().get(TimeFrame.MINUTE)
                .get(symbol).put(symbol, new FixedDataList<>(symbol, oldMinuteKlines.size(), oldMinuteKlines));

        tradeHistoryManager.getTradeHistoryData().get(TimeFrame.HOUR)
                .put(symbol, new ConcurrentHashMap<>());
        tradeHistoryManager.getTradeHistoryData().get(TimeFrame.HOUR)
                .get(symbol).put(symbol, new FixedDataList<>(symbol, oldHourKlines.size(), oldHourKlines));

        tradeHistoryManager.getTradeHistoryData().get(TimeFrame.DAY)
                .put(symbol, new ConcurrentHashMap<>());
        tradeHistoryManager.getTradeHistoryData().get(TimeFrame.DAY)
                .get(symbol).put(symbol, new FixedDataList<>(symbol, oldDayKlines.size(), oldDayKlines));

        long newStartTime = 24 * 3600_000L;
        Kline newMinuteKline = createKline(4, newStartTime);

        // when
        tradeHistoryManager.processKlineMessage(symbol, newMinuteKline);
        FixedDataList<Kline> minuteKlines = tradeHistoryManager.getTradeHistoryData()
                .get(TimeFrame.MINUTE).get(symbol).get(symbol);
        FixedDataList<Kline> hourKlines = tradeHistoryManager.getTradeHistoryData()
                .get(TimeFrame.HOUR).get(symbol).get(symbol);
        FixedDataList<Kline> dayKlines = tradeHistoryManager.getTradeHistoryData()
                .get(TimeFrame.DAY).get(symbol).get(symbol);

        List<Double> minuteClosePrices = minuteKlines.snapshot().stream()
                .map(Kline::getClosePrice)
                .toList();
        List<Double> hourClosePrices = hourKlines.snapshot().stream()
                .map(Kline::getClosePrice)
                .toList();
        List<Double> dayClosePrices = dayKlines.snapshot().stream()
                .map(Kline::getClosePrice)
                .toList();

        // then
        assertThat(minuteClosePrices).containsExactly(2.0, 3.0, 4.0);
        assertThat(hourClosePrices).containsExactly(2.0, 3.0, 4.0);
        assertThat(dayClosePrices).containsExactly(2.0, 3.0, 4.0);
    }

    private long roundToMinute(long timestamp) {
        return (timestamp / 60_000) * 60_000;
    }
}
