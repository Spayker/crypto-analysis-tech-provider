package com.spayker.crypto.analysis.service.data.history;

import com.spayker.crypto.analysis.config.KlineHistoryConfig;
import com.spayker.crypto.analysis.dao.rest.bybit.dto.kline.Kline;
import com.spayker.crypto.analysis.dto.indicator.FixedDataList;
import com.spayker.crypto.analysis.dto.indicator.TimeFrame;
import com.spayker.crypto.analysis.service.data.ByBitExchangeAdapter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class TradeHistoryManager {

    private final KlineHistoryConfig kLineHistoryConfig;
    private final ByBitExchangeAdapter byBitExchangeAdapter;

    @Getter(AccessLevel.PACKAGE)
    private final Map<TimeFrame, Map<String, Map<String, FixedDataList<Kline>>>> tradeHistoryData =
            new EnumMap<>(TimeFrame.class);

    private static final ZoneId UTC_ZONE_ID = ZoneId.of("UTC");

    public TradeHistoryManager(KlineHistoryConfig kLineHistoryConfig, ByBitExchangeAdapter byBitExchangeAdapter) {
        this.kLineHistoryConfig = kLineHistoryConfig;
        this.byBitExchangeAdapter = byBitExchangeAdapter;
        Arrays.stream(TimeFrame.values()).forEach(tf -> tradeHistoryData.put(tf, new ConcurrentHashMap<>()));
    }

    public List<Kline> getSymbolHistory(String symbol, TimeFrame timeFrame) {
        String interval = getIntervalByTimeFrame(timeFrame);
        String limit = getLimitByTimeFrame(timeFrame);
        List<Kline> history = byBitExchangeAdapter.getHistory(symbol, interval, limit);

        Map<String, Map<String, FixedDataList<Kline>>> timeFrameData = tradeHistoryData.get(timeFrame);
        Map<String, FixedDataList<Kline>> symbolData =
                timeFrameData.computeIfAbsent(symbol, s -> new ConcurrentHashMap<>());

        symbolData.computeIfAbsent(
                symbol,
                name -> new FixedDataList<>(name, history.size(), history) // инициализация свечами сразу
        );

        return history;
    }

    public void processTickerClosePriceMessage(String symbol, double lastPrice) {
        for (TimeFrame timeFrame : TimeFrame.values()) {
            Map<String, Map<String, FixedDataList<Kline>>> timeFrameHistoryTradeData = tradeHistoryData.get(timeFrame);
            Map<String, FixedDataList<Kline>> symbolHistoryTradeData = timeFrameHistoryTradeData.get(symbol);
            if (symbolHistoryTradeData == null) {
                continue;
            }

            for (FixedDataList<Kline> fixedDataList : symbolHistoryTradeData.values()) {
                Kline latestKline = fixedDataList.getLast();
                if (latestKline != null) {
                    updateLatestCandle(latestKline, lastPrice);
                }
            }
        }
    }

    private void updateLatestCandle(Kline kline, double lastPrice) {
        kline.setClosePrice(lastPrice);
        if (kline.getLowPrice() > lastPrice) {
            kline.setLowPrice(lastPrice);
        }
        if (kline.getHighPrice() < lastPrice) {
            kline.setHighPrice(lastPrice);
        }
    }

    public void processKlineMessage(String symbol, Kline minuteKline) {
        long openTime = minuteKline.getStartTime();
        ZonedDateTime openDateTime = Instant.ofEpochMilli(openTime).atZone(UTC_ZONE_ID);

        boolean isNewHour = openDateTime.getMinute() == 0;
        boolean isNewDay  = isNewHour && openDateTime.getHour() == 0;

        for (TimeFrame timeFrame : TimeFrame.values()) {
            Map<String, Map<String, FixedDataList<Kline>>> tfData = tradeHistoryData.get(timeFrame);
            Map<String, FixedDataList<Kline>> symbolData = tfData != null ? tfData.get(symbol) : null;

            if (symbolData != null) {
                boolean allowAdd = switch (timeFrame) {
                    case MINUTE -> true;
                    case HOUR   -> isNewHour;
                    case DAY    -> isNewDay;
                };

                for (FixedDataList<Kline> fixedDataList : symbolData.values()) {
                    Kline lastKline = fixedDataList.getLast();

                    if (allowAdd) {
                        Kline toAdd = copyKline(minuteKline, timeFrame);
                        if (timeFrame != TimeFrame.MINUTE && lastKline != null) {
                            toAdd.setClosePrice(minuteKline.getClosePrice());
                            toAdd.setOpenPrice(minuteKline.getOpenPrice());
                        }

                        fixedDataList.add(toAdd);
                    } else if (lastKline != null) {
                        lastKline.setClosePrice(minuteKline.getClosePrice());
                        lastKline.setHighPrice(Math.max(lastKline.getHighPrice(), minuteKline.getHighPrice()));
                        lastKline.setLowPrice(Math.min(lastKline.getLowPrice(), minuteKline.getLowPrice()));
                        lastKline.setVolume(lastKline.getVolume() + minuteKline.getVolume());
                    }
                }
            }
        }
    }




    private Kline copyKline(Kline src, TimeFrame timeFrame) {
        Kline kLine = new Kline();
        ZonedDateTime dt = Instant.ofEpochMilli(src.getStartTime()).atZone(UTC_ZONE_ID);

        ZonedDateTime aligned = switch (timeFrame) {
            case MINUTE -> dt.withSecond(0).withNano(0);
            case HOUR   -> dt.withMinute(0).withSecond(0).withNano(0);
            case DAY    -> dt.withHour(0).withMinute(0).withSecond(0).withNano(0);
        };
        kLine.setStartTime(aligned.toInstant().toEpochMilli());
        kLine.setOpenPrice(src.getOpenPrice());
        kLine.setClosePrice(src.getClosePrice());
        kLine.setHighPrice(src.getHighPrice());
        kLine.setLowPrice(src.getLowPrice());
        kLine.setVolume(src.getVolume());
        return kLine;
    }


    private String getIntervalByTimeFrame(TimeFrame timeFrame) {
        return switch (timeFrame) {
            case MINUTE -> kLineHistoryConfig.getMinuteIntervalType();
            case HOUR   -> kLineHistoryConfig.getHourIntervalType();
            case DAY    -> kLineHistoryConfig.getDayIntervalType();
        };
    }

    private String getLimitByTimeFrame(TimeFrame timeFrame) {
        return switch (timeFrame) {
            case MINUTE -> kLineHistoryConfig.getMaxMinuteKlineSize();
            case HOUR   -> kLineHistoryConfig.getMaxHourKlineSize();
            case DAY    -> kLineHistoryConfig.getMaxDayKlineSize();
        };
    }
}
