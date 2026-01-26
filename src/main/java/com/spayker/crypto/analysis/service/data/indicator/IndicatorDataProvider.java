package com.spayker.crypto.analysis.service.data.indicator;

import com.spayker.crypto.analysis.dao.rest.bybit.dto.kline.Kline;
import com.spayker.crypto.analysis.dto.indicator.TimeFrame;
import com.spayker.crypto.analysis.dto.indicator.FixedDataList;
import com.spayker.crypto.analysis.service.data.history.TradeHistoryManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class IndicatorDataProvider {

    private final TradeHistoryManager tradeHistoryManager;
    private final Map<String, IndicatorCalculator> indicatorCalculators;

    private final Map<TimeFrame, Map<String, Map<String, FixedDataList<String>>>> indicatorData =
            new EnumMap<>(TimeFrame.class);

    public IndicatorDataProvider(@Autowired TradeHistoryManager tradeHistoryManager,
                                 @Autowired Map<String, IndicatorCalculator> indicatorCalculators) {
        this.tradeHistoryManager = tradeHistoryManager;
        this.indicatorCalculators = indicatorCalculators;
        Arrays.stream(TimeFrame.values()).forEach(tf -> indicatorData.put(tf, new ConcurrentHashMap<>()));
    }

    public Map<TimeFrame, Map<String, Map<String, FixedDataList<String>>>> getRawIndicatorData() {
        return indicatorData;
    }

    public Map<String, String> getAvailableIndicators() {
        Map<String, String> availableIndicatorsByTimeFrames = new HashMap<>();
        for (Map.Entry<TimeFrame, Map<String, Map<String, FixedDataList<String>>>> tfEntry : indicatorData.entrySet()) {
            TimeFrame timeFrame = tfEntry.getKey();
            Map<String, Map<String, FixedDataList<String>>> symbolsMap = tfEntry.getValue();

            for (Map<String, FixedDataList<String>> indicatorMap : symbolsMap.values()) {
                for (String indicatorName : indicatorMap.keySet()) {
                    availableIndicatorsByTimeFrames.put(indicatorName.toLowerCase(), timeFrame.getValue().toLowerCase());
                }
            }
        }
        return availableIndicatorsByTimeFrames;
    }

    public void initSymbol(TimeFrame timeFrame, String symbol) {
        indicatorData.get(timeFrame)
                .computeIfAbsent(symbol, s -> new ConcurrentHashMap<>());
    }

    public void initIndicator(TimeFrame timeFrame, String symbol, String indicatorName) {
        List<Kline> kLines = tradeHistoryManager.getSymbolHistory(symbol, timeFrame);
        if (kLines.isEmpty()) {
            log.error("Could not get trade history for {}", symbol);
        } else {
            initSymbol(timeFrame, symbol);
            indicatorData.get(timeFrame)
                    .computeIfAbsent(symbol, s -> new ConcurrentHashMap<>())
                    .computeIfAbsent(
                            indicatorName,
                            name -> new FixedDataList<>(
                                    indicatorName,
                                    kLines.size(),
                                    indicatorCalculators.get(indicatorName).calculate(kLines))
                    );

        }
    }

    public void recalculateIndicatorData() {
        for (TimeFrame timeFrame : TimeFrame.values()) {
            Map<String, Map<String, FixedDataList<String>>> timeFrameData =
                    indicatorData.get(timeFrame);

            if (isEmpty(timeFrameData)) {
                continue;
            }

            processTimeFrame(timeFrame, timeFrameData);
        }
    }

    private void processTimeFrame(TimeFrame timeFrame, Map<String, Map<String, FixedDataList<String>>> timeFrameData) {
        for (var symbolEntry : timeFrameData.entrySet()) {
            String symbol = symbolEntry.getKey();
            Map<String, FixedDataList<String>> indicators = symbolEntry.getValue();

            List<Kline> klines = tradeHistoryManager.getSymbolHistory(symbol, timeFrame);
            if (isEmpty(klines)) {
                continue;
            }
            updateIndicators(indicators, klines);
        }
    }

    private void updateIndicators(Map<String, FixedDataList<String>> indicators, List<Kline> klines) {
        int candleCount = klines.size();
        for (var indicatorEntry : indicators.entrySet()) {
            String indicatorName = indicatorEntry.getKey();
            IndicatorCalculator calculator = indicatorCalculators.get(indicatorName);
            updateSingleIndicator(calculator, indicatorEntry.getValue(), klines, candleCount);
        }
    }

    private void updateSingleIndicator(
            IndicatorCalculator calculator,
            FixedDataList<String> indicatorData,
            List<Kline> klines,
            int candleCount
    ) {
        List<String> values = calculator.calculate(klines);
        String lastValue = values.getLast();

        int indicatorSize = indicatorData.getSize();

        if (indicatorSize == candleCount) {
            indicatorData.replaceLast(lastValue);
            return;
        }

        if (indicatorSize == candleCount - 1) {
            indicatorData.add(lastValue);
        }
    }

    private boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    private boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }

    public FixedDataList<String> getIndicatorData(TimeFrame timeFrame, String symbol, String indicatorName) {
        var timeFrameIndicatorData = indicatorData.get(timeFrame);
        if (timeFrameIndicatorData == null) {
            return null;
        }
        Map<String, FixedDataList<String>> symbolIndicatorData = timeFrameIndicatorData.get(symbol);
        if (symbolIndicatorData == null) {
            return null;
        }
        return symbolIndicatorData.get(indicatorName);
    }

    public boolean containsIndicator(TimeFrame timeFrame,
                                     String symbol,
                                     String indicatorName) {
        return indicatorData.getOrDefault(timeFrame, Map.of())
                .getOrDefault(symbol, Map.of())
                .containsKey(indicatorName);
    }

    public void removeIndicator(TimeFrame timeFrame, String symbol, String indicatorName) {
        Map<String, Map<String, FixedDataList<String>>> timeFrameData = indicatorData.get(timeFrame);
        if (timeFrameData != null) {
            Map<String, FixedDataList<String>> symbolData = timeFrameData.get(symbol);
            if (symbolData != null) {
                symbolData.remove(indicatorName);
                if (symbolData.isEmpty()) {
                    timeFrameData.remove(symbol);
                }
                if (timeFrameData.isEmpty()) {
                    indicatorData.remove(timeFrame);
                }
            }
        }
    }

    public void removeSymbol(TimeFrame timeFrame, String symbol) {
        indicatorData.get(timeFrame).remove(symbol);
    }
}