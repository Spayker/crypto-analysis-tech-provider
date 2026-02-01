package com.spayker.crypto.analysis.service.data.indicator;

import com.spayker.crypto.analysis.dao.rest.bybit.dto.kline.Kline;
import com.spayker.crypto.analysis.dao.socket.publisher.IndicatorSocketPublisher;
import com.spayker.crypto.analysis.dto.indicator.IndicatorValue;
import com.spayker.crypto.analysis.dto.indicator.TimeFrame;
import com.spayker.crypto.analysis.dto.indicator.FixedDataList;
import com.spayker.crypto.analysis.service.data.history.TradeHistoryManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
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
    private final IndicatorSocketPublisher indicatorSocketPublisher;
    private final Map<String, IndicatorCalculator> indicatorCalculators;

    private final Map<TimeFrame, Map<String, Map<String, FixedDataList<IndicatorValue>>>> indicatorData =
            new EnumMap<>(TimeFrame.class);

    public IndicatorDataProvider(@Autowired TradeHistoryManager tradeHistoryManager,
                                 @Autowired IndicatorSocketPublisher indicatorSocketPublisher,
                                 @Autowired Map<String, IndicatorCalculator> indicatorCalculators) {
        this.tradeHistoryManager = tradeHistoryManager;
        this.indicatorSocketPublisher = indicatorSocketPublisher;
        this.indicatorCalculators = indicatorCalculators;
        Arrays.stream(TimeFrame.values()).forEach(tf -> indicatorData.put(tf, new ConcurrentHashMap<>()));
    }

    public Map<TimeFrame, Map<String, Map<String, FixedDataList<IndicatorValue>>>> getRawIndicatorData() {
        return indicatorData;
    }

    public Map<String, String> getAvailableIndicators() {
        Map<String, String> availableIndicatorsByTimeFrames = new HashMap<>();
        for (Map.Entry<TimeFrame, Map<String, Map<String, FixedDataList<IndicatorValue>>>> tfEntry : indicatorData.entrySet()) {
            TimeFrame timeFrame = tfEntry.getKey();
            Map<String, Map<String, FixedDataList<IndicatorValue>>> symbolsMap = tfEntry.getValue();

            for (Map<String, FixedDataList<IndicatorValue>> indicatorMap : symbolsMap.values()) {
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
        if (isEmpty(kLines)) {
            log.error("Could not get trade history for {}", symbol);
            return;
        }

        initSymbol(timeFrame, symbol);
        List<String> calculatedValues = indicatorCalculators.get(indicatorName).calculate(kLines);
        if (isEmpty(calculatedValues)) {
            log.error("Indicator {} calculation returned empty for {}", indicatorName, symbol);
            return;
        }

        List<IndicatorValue> indicatorValues = new ArrayList<>();
        int startIndex = kLines.size() - calculatedValues.size();
        for (int i = 0; i < calculatedValues.size(); i++) {
            long ts = kLines.get(startIndex + i).getStartTime();
            indicatorValues.add(new IndicatorValue(calculatedValues.get(i), ts));
        }

        indicatorData.get(timeFrame)
                .computeIfAbsent(symbol, s -> new ConcurrentHashMap<>())
                .computeIfAbsent(
                        indicatorName,
                        name -> new FixedDataList<>(indicatorName, calculatedValues.size(), indicatorValues)
                );
    }

    public void recalculateIndicatorData() {
        for (TimeFrame timeFrame : TimeFrame.values()) {
            Map<String, Map<String, FixedDataList<IndicatorValue>>> timeFrameData =
                    indicatorData.get(timeFrame);

            if (isEmpty(timeFrameData)) {
                continue;
            }

            processTimeFrame(timeFrame, timeFrameData);
        }
    }

    private void processTimeFrame(TimeFrame timeFrame, Map<String, Map<String, FixedDataList<IndicatorValue>>> timeFrameData) {
        for (var symbolEntry : timeFrameData.entrySet()) {
            String symbol = symbolEntry.getKey();
            Map<String, FixedDataList<IndicatorValue>> indicators = symbolEntry.getValue();

            List<Kline> klines = tradeHistoryManager.getSymbolHistory(symbol, timeFrame);
            if (isEmpty(klines)) {
                continue;
            }
            updateIndicators(symbol, indicators, klines, timeFrame);
        }
    }

    private void updateIndicators(String symbol,
                                  Map<String, FixedDataList<IndicatorValue>> indicators,
                                  List<Kline> klines,
                                  TimeFrame timeFrame) {
        for (var indicatorEntry : indicators.entrySet()) {
            String indicatorName = indicatorEntry.getKey();
            IndicatorCalculator calculator = indicatorCalculators.get(indicatorName);
            updateSingleIndicator(calculator, indicatorEntry.getValue(), klines, timeFrame);
            indicatorSocketPublisher.publish(
                    symbol,
                    indicatorName,
                    indicatorEntry.getValue().getLast(),
                    timeFrame.getValue().toLowerCase()
            );
        }
    }

    private void updateSingleIndicator(
            IndicatorCalculator calculator,
            FixedDataList<IndicatorValue> indicatorData,
            List<Kline> klines,
            TimeFrame timeFrame
    ) {
        Kline lastCandle = klines.getLast();
        long lastTs = lastCandle.getStartTime();

        List<String> values = calculator.calculate(klines);
        if (values.isEmpty()) return;
        String lastValue = values.getLast();

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime lastIndicatorTime = Instant.ofEpochMilli(lastTs)
                .atZone(ZoneOffset.UTC)
                .toLocalDateTime();

        boolean isNewPeriod = false;
        switch (timeFrame) {
            case MINUTE -> isNewPeriod = now.getMinute() != lastIndicatorTime.getMinute();
            case HOUR   -> isNewPeriod = now.getHour()   != lastIndicatorTime.getHour();
            case DAY   -> isNewPeriod = now.getDayOfMonth() != lastIndicatorTime.getDayOfMonth();
        }

        if (isNewPeriod) {
            indicatorData.add(new IndicatorValue(lastValue, lastTs));
        } else {
            indicatorData.replaceLast(new IndicatorValue(lastValue, lastTs));
        }
    }

    private boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    private boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }

    public FixedDataList<IndicatorValue> getIndicatorData(TimeFrame timeFrame, String symbol, String indicatorName) {
        var timeFrameIndicatorData = indicatorData.get(timeFrame);
        if (timeFrameIndicatorData == null) {
            return null;
        }
        Map<String, FixedDataList<IndicatorValue>> symbolIndicatorData = timeFrameIndicatorData.get(symbol);
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
        Map<String, Map<String, FixedDataList<IndicatorValue>>> timeFrameData = indicatorData.get(timeFrame);
        if (timeFrameData != null) {
            Map<String, FixedDataList<IndicatorValue>> symbolData = timeFrameData.get(symbol);
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