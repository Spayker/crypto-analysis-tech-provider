package com.spayker.crypto.analysis.service.data.indicator;

import com.spayker.crypto.analysis.dao.rest.bybit.dto.kline.Kline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RsiTest {

    private Rsi rsiCalculator;

    @BeforeEach
    void setUp() {
        rsiCalculator = new Rsi();
    }

    private List<Kline> createKlines(double... closes) {
        List<Kline> klines = new ArrayList<>();
        long timestamp = 1_000_000L;
        for (double close : closes) {
            klines.add(Kline.builder()
                    .closePrice(close)
                    .openPrice(close)
                    .highPrice(close)
                    .lowPrice(close)
                    .volume(1.0)
                    .startTime(timestamp++)
                    .build());
        }
        return klines;
    }

    @Test
    void calculate_shouldReturnEmpty_whenNotEnoughData() {
        List<Kline> klines = createKlines(1, 2, 3, 4, 5);
        List<String> result = rsiCalculator.calculate(klines);
        assertThat(result).isEmpty();
    }

    @Test
    void calculate_shouldComputeRsi_onIncreasingPrices() {
        List<Kline> klines = createKlines(
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                11, 12, 13, 14, 15, 16, 17
        );

        List<String> result = rsiCalculator.calculate(klines);

        assertThat(result).isNotEmpty();
        result.forEach(rsi -> assertThat(Double.parseDouble(rsi)).isBetween(50.0, 100.0));
    }

    @Test
    void calculate_shouldComputeRsi_onDecreasingPrices() {
        List<Kline> klines = createKlines(
                17, 16, 15, 14, 13, 12, 11, 10, 9, 8,
                7, 6, 5, 4, 3, 2, 1
        );

        List<String> result = rsiCalculator.calculate(klines);

        assertThat(result).isNotEmpty();
        result.forEach(rsi -> assertThat(Double.parseDouble(rsi)).isBetween(0.0, 50.0));
    }

    @Test
    void calculate_shouldComputeRsi_onMixedPrices() {
        List<Kline> klines = createKlines(
                10, 12, 11, 13, 12, 14, 13, 15, 14, 16,
                15, 17, 16, 18, 17, 19, 18
        );

        List<String> result = rsiCalculator.calculate(klines);

        assertThat(result).isNotEmpty();
        result.forEach(rsi -> assertThat(Double.parseDouble(rsi)).isBetween(0.0, 100.0));
    }
}