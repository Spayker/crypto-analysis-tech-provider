package com.spayker.crypto.analysis.service.data.indicator;

import com.spayker.crypto.analysis.dao.rest.bybit.dto.kline.Kline;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component("bolBands")
public class BolBands implements IndicatorCalculator {

    private static final int PERIOD = 20;
    private static final double MULTIPLIER = 2.0;
    private static final int RESULT_SCALE = 2;

    @Getter
    private static class BollingerBand {
        private final long timestamp;
        private final double middleBand;
        private final double upperBand;
        private final double lowerBand;
        private final double bandwidth;

        public BollingerBand(long timestamp, double middleBand, double upperBand, double lowerBand, double bandwidth) {
            this.timestamp = timestamp;
            this.middleBand = middleBand;
            this.upperBand = upperBand;
            this.lowerBand = lowerBand;
            this.bandwidth = bandwidth;
        }
    }

    @Override
    public List<String> calculate(List<Kline> kLines) {
        return calculateBolBands(kLines)
                .stream()
                .map(bollingerEntity ->
                        bollingerEntity.getMiddleBand() + "," +
                        bollingerEntity.getUpperBand() + "," +
                        bollingerEntity.getLowerBand())
                .toList();
    }

    private List<BollingerBand> calculateBolBands(List<Kline> klines) {
        List<BollingerBand> result = new ArrayList<>();

        for (int i = PERIOD - 1; i < klines.size(); i++) {
            List<Kline> window = klines.subList(i - PERIOD + 1, i + 1);
            double sma = calculateSMA(window);
            double stdDev = calculateStandardDeviation(window, sma);
            double upperBand = sma + (MULTIPLIER * stdDev);
            double lowerBand = sma - (MULTIPLIER * stdDev);
            double bandwidth = upperBand - lowerBand;

            Kline currentCandle = klines.get(i);
            result.add(new BollingerBand(
                    currentCandle.getStartTime(),
                    roundResult(sma),
                    roundResult(upperBand),
                    roundResult(lowerBand),
                    roundResult(bandwidth)
            ));
        }
        return result;
    }

    private double calculateSMA(List<Kline> window) {
        double sum = 0;
        for (Kline candle : window) {
            sum = sum + candle.getClosePrice();
        }
        return sum / window.size();
    }

    private double calculateStandardDeviation(List<Kline> window, double mean) {
        double variance = 0;

        for (Kline kline : window) {
            double diff = kline.getClosePrice() - mean;
            double squared = diff * diff;
            variance = variance + squared;
        }

        double divisor = window.size() - 1;
        double varianceAvg = variance / divisor;

        return Math.sqrt(varianceAvg);
    }

    private double roundResult(double value) {
        return BigDecimal.valueOf(value)
                .setScale(RESULT_SCALE, java.math.RoundingMode.HALF_UP)
                .doubleValue();
    }
}