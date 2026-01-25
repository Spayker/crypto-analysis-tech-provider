package com.spayker.crypto.analysis.service.data.indicator;

import com.spayker.crypto.analysis.dao.rest.bybit.dto.kline.Kline;
import com.spayker.crypto.analysis.dto.indicator.TimeFrame;
import com.spayker.crypto.analysis.service.data.history.TradeHistoryManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.spayker.crypto.analysis.service.data.KlineTestHelper.createKlines;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class IndicatorDataProviderTest {

    private TradeHistoryManager tradeHistoryManager;
    private IndicatorCalculator calculator;
    private IndicatorDataProvider dataProvider;

    @BeforeEach
    void setUp() {
        tradeHistoryManager = mock(TradeHistoryManager.class);
        calculator = mock(IndicatorCalculator.class);
        Map<String, IndicatorCalculator> calculators = Map.of("rsi", calculator);
        dataProvider = new IndicatorDataProvider(tradeHistoryManager, calculators);
    }

    private List<Kline> mockKlines(double... closes) {
        return createKlines(closes);
    }

    @Test
    void initSymbol_shouldCreateEmptySymbolEntry() {
        dataProvider.initSymbol(TimeFrame.MINUTE, "btcusdt");
        assertThat(dataProvider.getRawIndicatorData().get(TimeFrame.MINUTE))
                .containsKey("btcusdt");
    }

    @Test
    void initIndicator_shouldPopulateIndicatorData() {
        List<Kline> klines = mockKlines(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15);
        when(tradeHistoryManager.getSymbolHistory("btcusdt", TimeFrame.MINUTE)).thenReturn(klines);
        when(calculator.calculate(klines)).thenReturn(List.of("50.0", "51.0", "52.0"));

        dataProvider.initIndicator(TimeFrame.MINUTE, "btcusdt", "rsi");

        assertThat(dataProvider.containsIndicator(TimeFrame.MINUTE, "btcusdt", "rsi")).isTrue();
        assertThat(dataProvider.getIndicatorData(TimeFrame.MINUTE, "btcusdt", "rsi").snapshot())
                .containsExactly("50.0", "51.0", "52.0");
    }

    @Test
    void getAvailableIndicators_shouldReturnCorrectMap() {
        List<Kline> klines = mockKlines(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15);
        when(tradeHistoryManager.getSymbolHistory("btcusdt", TimeFrame.MINUTE)).thenReturn(klines);
        when(calculator.calculate(klines)).thenReturn(List.of("50.0"));

        dataProvider.initIndicator(TimeFrame.MINUTE, "btcusdt", "rsi");

        Map<String, String> available = dataProvider.getAvailableIndicators();
        assertThat(available).containsEntry("rsi", "minute");
    }

    @Test
    void removeIndicator_shouldRemoveIndicator() {
        List<Kline> klines = mockKlines(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15);
        when(tradeHistoryManager.getSymbolHistory("btcusdt", TimeFrame.MINUTE)).thenReturn(klines);
        when(calculator.calculate(klines)).thenReturn(List.of("50.0"));

        dataProvider.initIndicator(TimeFrame.MINUTE, "btcusdt", "rsi");
        assertThat(dataProvider.containsIndicator(TimeFrame.MINUTE, "btcusdt", "rsi")).isTrue();

        dataProvider.removeIndicator(TimeFrame.MINUTE, "btcusdt", "rsi");
        assertThat(dataProvider.containsIndicator(TimeFrame.MINUTE, "btcusdt", "rsi")).isFalse();
    }

    @Test
    void removeSymbol_shouldRemoveAllIndicatorsForSymbol() {
        List<Kline> klines = mockKlines(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15);
        when(tradeHistoryManager.getSymbolHistory("btcusdt", TimeFrame.MINUTE)).thenReturn(klines);
        when(calculator.calculate(klines)).thenReturn(List.of("50.0"));

        dataProvider.initIndicator(TimeFrame.MINUTE, "btcusdt", "rsi");
        dataProvider.removeSymbol(TimeFrame.MINUTE, "btcusdt");

        assertThat(dataProvider.getRawIndicatorData().get(TimeFrame.MINUTE)).doesNotContainKey("btcusdt");
    }

    @Test
    void recalculateIndicatorData_shouldUpdateValues() {
        List<Kline> klines = mockKlines(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15);
        when(tradeHistoryManager.getSymbolHistory("btcusdt", TimeFrame.MINUTE)).thenReturn(klines);
        when(calculator.calculate(klines)).thenReturn(List.of("50.0", "51.0"));

        dataProvider.initIndicator(TimeFrame.MINUTE, "btcusdt", "rsi");

        dataProvider.recalculateIndicatorData();

        List<String> snapshot = dataProvider.getIndicatorData(TimeFrame.MINUTE, "btcusdt", "rsi").snapshot();
        assertThat(snapshot).contains("51.0");
    }
}
