package com.spayker.crypto.analysis.service.data.indicator;

import com.spayker.crypto.analysis.dao.rest.bybit.dto.kline.Kline;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component("macd")
public class Macd implements IndicatorCalculator {

    private static final int PERIOD_9 = 9;
    private static final int PERIOD_12 = 12;
    private static final int PERIOD_26 = 26;

    @Data
    @AllArgsConstructor
    class MACDResult {
        Double macd;
        Double signal;
        Double histogram;
    }

    public List<String> calculate(List<Kline> kLines) {
        List<Double> closes = kLines.stream()
                .map(Kline::getClosePrice)
                .toList();
        List<MACDResult> macdResults = calculateLast30MACD(closes);
        return macdResults.stream()
                .map(macd -> macd.histogram + "," + macd.macd + "," + macd.signal)
                .toList();
    }

    public List<MACDResult> calculateLast30MACD(List<Double> closes) {
        int n = closes.size();
        // Calculate full EMAs for 12 and 26 periods
        List<Double> ema12 = calculateEMAFull(closes, PERIOD_12);
        List<Double> ema26 = calculateEMAFull(closes, PERIOD_26);

        // Calculate MACD line = EMA12 - EMA26 (null if either null)
        List<Double> macdLine = new ArrayList<>(Collections.nCopies(n, null));
        for (int i = 0; i < n; i++) {
            Double e12 = ema12.get(i);
            Double e26 = ema26.get(i);
            if (e12 != null && e26 != null) {
                macdLine.set(i, e12 - e26);
            }
        }

        // Find first valid MACD index to skip leading nulls before calculating signal line
        int firstValidMacdIndex = 0;
        while (firstValidMacdIndex < macdLine.size() && macdLine.get(firstValidMacdIndex) == null) {
            firstValidMacdIndex++;
        }

        // Defensive check
        if (firstValidMacdIndex >= macdLine.size()) {
            throw new IllegalStateException("MACD line contains only nulls.");
        }

        // Calculate signal line (9-period EMA of MACD line without leading nulls)
        List<Double> macdSubList = macdLine.subList(firstValidMacdIndex, n);
        List<Double> signalSubList = calculateEMAFull(macdSubList, PERIOD_9);

        // Pad signalSubList with nulls at start to align with full length
        List<Double> signalLine = new ArrayList<>(Collections.nCopies(firstValidMacdIndex, null));
        signalLine.addAll(signalSubList);

        // Calculate histogram = MACD - signal (null if either null)
        List<MACDResult> results = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Double macd = macdLine.get(i);
            Double signal = signalLine.get(i);
            Double histogram = (macd != null && signal != null) ? macd - signal : null;
            results.add(new MACDResult(macd, signal, histogram));
        }

        // Extract last 30 valid results (all fields non-null)
        List<MACDResult> last30 = new ArrayList<>();
        for (int i = results.size() - 1; i >= 0 && last30.size() < 30; i--) {
            MACDResult r = results.get(i);
            if (r.macd != null && r.signal != null && r.histogram != null) {
                last30.add(r);
            }
        }
        Collections.reverse(last30);

        return last30;
    }

    public List<Double> calculateEMAFull(List<Double> prices, int period) {
        List<Double> emaList = new ArrayList<>(Collections.nCopies(prices.size(), null));
        if (prices.size() < period) {
            return emaList; // Not enough data for any EMA
        }

        // Calculate initial SMA for first 'period' elements
        double sum = 0.0;
        for (int i = 0; i < period; i++) {
            Double price = prices.get(i);
            if (price == null) {
                return emaList; // If null price in initial period, cannot compute SMA
            }
            sum += price;
        }
        double sma = sum / period;
        emaList.set(period - 1, sma);

        double multiplier = 2.0 / (period + 1);
        double ema = sma;

        // Calculate EMA for rest
        for (int i = period; i < prices.size(); i++) {
            Double price = prices.get(i);
            if (price == null) {
                emaList.set(i, null);
                continue;
            }
            ema = (price - ema) * multiplier + ema;
            emaList.set(i, ema);
        }
        return emaList;
    }
}