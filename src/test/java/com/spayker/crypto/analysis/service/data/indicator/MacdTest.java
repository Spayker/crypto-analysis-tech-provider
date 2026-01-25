package com.spayker.crypto.analysis.service.data.indicator;

import com.spayker.crypto.analysis.dao.rest.bybit.dto.kline.Kline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class MacdTest {

    private Macd macd;

    @BeforeEach
    void setUp() {
        macd = new Macd();
    }

    @Test
    void calculateEMAFull_ShouldReturnNulls_WhenNotEnoughData() {
        List<Double> prices = List.of(1.0, 2.0, 3.0); // меньше периода 12
        List<Double> ema = macd.calculateEMAFull(prices, 12);
        assertEquals(prices.size(), ema.size());
        assertTrue(ema.stream().allMatch(Objects::isNull));
    }

    @Test
    void calculateEMAFull_ShouldReturnValidEMA_WhenEnoughData() {
        List<Double> prices = IntStream.rangeClosed(1, 20).asDoubleStream().boxed().toList();
        List<Double> ema = macd.calculateEMAFull(prices, 5);
        assertEquals(prices.size(), ema.size());
        for (int i = 0; i < 4; i++) {
            assertNull(ema.get(i));
        }
        assertEquals((1+2+3+4+5)/5.0, ema.get(4));
        for (int i = 5; i < ema.size(); i++) {
            assertNotNull(ema.get(i));
        }
    }

    @Test
    void calculateLast30MACD_ShouldReturnLast30ValidResults() {
        List<Kline> kLines = new ArrayList<>();
        for (double i = 1; i <= 50; i++) {
            Kline kline = new Kline();
            kline.setClosePrice(i);
            kLines.add(kline);
        }
        List<Macd.MACDResult> results = macd.calculateLast30MACD(
                kLines.stream().map(Kline::getClosePrice).toList()
        );
        assertTrue(results.size() <= 30);
        for (Macd.MACDResult r : results) {
            assertNotNull(r.macd);
            assertNotNull(r.signal);
            assertNotNull(r.histogram);
        }
    }

    @Test
    void calculate_ShouldReturnListOfStrings_InCorrectFormat() {
        List<Kline> kLines = new ArrayList<>();
        for (double i = 1; i <= 50; i++) {
            Kline kline = new Kline();
            kline.setClosePrice(i);
            kLines.add(kline);
        }
        List<String> resultStrings = macd.calculate(kLines);
        assertTrue(resultStrings.size() <= 30);
        for (String s : resultStrings) {
            String[] parts = s.split(",");
            assertEquals(3, parts.length);
        }
    }
}