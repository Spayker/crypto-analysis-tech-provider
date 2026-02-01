package com.spayker.crypto.analysis.service.data.indicator;

import com.spayker.crypto.analysis.dao.rest.bybit.dto.kline.Kline;
import com.spayker.crypto.analysis.dao.socket.publisher.IndicatorSocketPublisher;
import com.spayker.crypto.analysis.dto.indicator.IndicatorValue;
import com.spayker.crypto.analysis.dto.indicator.TimeFrame;
import com.spayker.crypto.analysis.service.data.history.TradeHistoryManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.spayker.crypto.analysis.service.data.KlineTestHelper.createKlines;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


class IndicatorDataProviderTest {

    private TradeHistoryManager tradeHistoryManager;
    private IndicatorCalculator calculator;
    private IndicatorSocketPublisher socketPublisher;
    private IndicatorDataProvider dataProvider;

    @BeforeEach
    void setUp() {
        tradeHistoryManager = mock(TradeHistoryManager.class);
        calculator = mock(IndicatorCalculator.class);
        socketPublisher = mock(IndicatorSocketPublisher.class);

        Map<String, IndicatorCalculator> calculators = Map.of("rsi", calculator);

        dataProvider = new IndicatorDataProvider(
                tradeHistoryManager,
                socketPublisher,
                calculators
        );
    }

    private List<Kline> mockKlines(double... closes) {
        return createKlines(closes);
    }

    @Test
    void initSymbol_shouldCreateEmptySymbolEntry() {
        // given
        // when
        dataProvider.initSymbol(TimeFrame.MINUTE, "btcusdt");

        // then
        assertThat(dataProvider.getRawIndicatorData().get(TimeFrame.MINUTE))
                .containsKey("btcusdt");
    }

    @Test
    void initIndicator_shouldPopulateIndicatorData() {
        // given
        List<Kline> klines = mockKlines(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15);
        when(tradeHistoryManager.getSymbolHistory("btcusdt", TimeFrame.MINUTE))
                .thenReturn(klines);
        when(calculator.calculate(klines))
                .thenReturn(List.of("50.0", "51.0", "52.0"));

        // when
        dataProvider.initIndicator(TimeFrame.MINUTE, "btcusdt", "rsi");

        // then
        assertThat(dataProvider.containsIndicator(TimeFrame.MINUTE, "btcusdt", "rsi")).isTrue();
        assertThat(dataProvider.getIndicatorData(TimeFrame.MINUTE, "btcusdt", "rsi").snapshot())
                .extracting(IndicatorValue::getValue) // берём только значения
                .containsExactly("50.0", "51.0", "52.0");
    }

    @Test
    void getAvailableIndicators_shouldReturnCorrectMap() {
        // given
        List<Kline> klines = mockKlines(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15);
        when(tradeHistoryManager.getSymbolHistory("btcusdt", TimeFrame.MINUTE))
                .thenReturn(klines);
        when(calculator.calculate(klines))
                .thenReturn(List.of("50.0"));
        dataProvider.initIndicator(TimeFrame.MINUTE, "btcusdt", "rsi");

        // when
        Map<String, String> available = dataProvider.getAvailableIndicators();

        // then
        assertThat(available).containsEntry("rsi", "minute");
    }

    @Test
    void removeIndicator_shouldRemoveIndicator() {
        // given
        List<Kline> klines = mockKlines(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15);
        when(tradeHistoryManager.getSymbolHistory("btcusdt", TimeFrame.MINUTE))
                .thenReturn(klines);
        when(calculator.calculate(klines))
                .thenReturn(List.of("50.0"));

        dataProvider.initIndicator(TimeFrame.MINUTE, "btcusdt", "rsi");
        assertThat(dataProvider.containsIndicator(TimeFrame.MINUTE, "btcusdt", "rsi")).isTrue();

        // when
        dataProvider.removeIndicator(TimeFrame.MINUTE, "btcusdt", "rsi");

        // then
        assertThat(dataProvider.containsIndicator(TimeFrame.MINUTE, "btcusdt", "rsi")).isFalse();
    }

    @Test
    void removeSymbol_shouldRemoveAllIndicatorsForSymbol() {
        // given
        List<Kline> klines = mockKlines(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15);
        when(tradeHistoryManager.getSymbolHistory("btcusdt", TimeFrame.MINUTE))
                .thenReturn(klines);
        when(calculator.calculate(klines))
                .thenReturn(List.of("50.0"));

        dataProvider.initIndicator(TimeFrame.MINUTE, "btcusdt", "rsi");

        // when
        dataProvider.removeSymbol(TimeFrame.MINUTE, "btcusdt");

        // then
        assertThat(dataProvider.getRawIndicatorData().get(TimeFrame.MINUTE))
                .doesNotContainKey("btcusdt");
    }

    @Test
    void recalculateIndicatorData_shouldUpdateValuesAndPublish() {
        // given
        List<Kline> klines = mockKlines(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15);
        when(tradeHistoryManager.getSymbolHistory("btcusdt", TimeFrame.MINUTE))
                .thenReturn(klines);
        when(calculator.calculate(klines))
                .thenReturn(List.of("50.0", "51.0"));
        dataProvider.initIndicator(TimeFrame.MINUTE, "btcusdt", "rsi");

        // when
        dataProvider.recalculateIndicatorData();

        var indicatorData = dataProvider.getIndicatorData(TimeFrame.MINUTE, "btcusdt", "rsi");

        // then
        assertThat(indicatorData.snapshot())
                .extracting(IndicatorValue::getValue)
                .contains("51.0");
        verify(socketPublisher, atLeastOnce()).publish(
                eq("btcusdt"),
                eq("rsi"),
                argThat(iv -> "51.0".equals(iv.getValue())), // проверяем value внутри IndicatorValue
                eq("minute")
        );
    }
}