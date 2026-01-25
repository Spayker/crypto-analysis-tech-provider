package com.spayker.crypto.analysis.service.data.indicator;

import com.spayker.crypto.analysis.dao.rest.bybit.dto.kline.Kline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BolBandsTest {

    private BolBands bolBands;

    @BeforeEach
    void setUp() {
        bolBands = new BolBands();
    }

    @Test
    void calculate_ShouldReturnEmptyList_WhenNotEnoughKlines() {
        List<Kline> klines = new ArrayList<>();
        for (int i = 0; i < 10; i++) { // меньше периода 20
            Kline kline = new Kline();
            kline.setClosePrice(i + 1.0);
            kline.setStartTime(System.currentTimeMillis());
            klines.add(kline);
        }
        List<String> result = bolBands.calculate(klines);
        assertTrue(result.isEmpty(), "Should return empty list when not enough data");
    }

    @Test
    void calculate_ShouldReturnCorrectSize_WhenEnoughKlines() {
        List<Kline> klines = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            Kline kline = new Kline();
            kline.setClosePrice(i + 1.0);
            kline.setStartTime(System.currentTimeMillis());
            klines.add(kline);
        }
        List<String> result = bolBands.calculate(klines);
        assertEquals(6, result.size(), "Result size should be klines.size() - PERIOD + 1");
    }

    @Test
    void calculate_ShouldReturnStrings_InCorrectFormat() {
        List<Kline> klines = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            Kline kline = new Kline();
            kline.setClosePrice(i);
            kline.setStartTime(System.currentTimeMillis());
            klines.add(kline);
        }
        List<String> result = bolBands.calculate(klines);
        for (String s : result) {
            String[] parts = s.split(",");
            assertEquals(3, parts.length, "Each result string should have 3 parts");
            // проверяем, что это числа
            Double.parseDouble(parts[0]);
            Double.parseDouble(parts[1]);
            Double.parseDouble(parts[2]);
        }
    }

    @Test
    void calculate_ShouldProduceIncreasingMiddleBand_WhenPricesIncreasing() {
        List<Kline> klines = new ArrayList<>();
        for (long i = 1; i <= 30; i++) {
            Kline kline = new Kline();
            kline.setClosePrice(i);
            kline.setStartTime(i);
            klines.add(kline);
        }
        List<String> result = bolBands.calculate(klines);
        double prevMiddle = -Double.MAX_VALUE;
        for (String s : result) {
            double middle = Double.parseDouble(s.split(",")[0]);
            assertTrue(middle >= prevMiddle, "Middle band should be increasing with increasing prices");
            prevMiddle = middle;
        }
    }
}
