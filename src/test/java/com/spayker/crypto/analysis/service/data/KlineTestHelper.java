package com.spayker.crypto.analysis.service.data;

import com.spayker.crypto.analysis.dao.rest.bybit.dto.kline.Kline;

import java.util.ArrayList;
import java.util.List;

public class KlineTestHelper {

    public static List<Kline> createKlines(double... closes) {
        List<Kline> klines = new ArrayList<>();
        long timestamp = 1_000_000L;

        for (double close : closes) {
            Kline kline = Kline.builder()
                    .startTime(timestamp)
                    .openPrice(close)
                    .highPrice(close)
                    .lowPrice(close)
                    .closePrice(close)
                    .volume(100)
                    .turnover(1000)
                    .build();
            klines.add(kline);
            timestamp += 60_000;
        }

        return klines;
    }

    public static Kline createKline(double close, long startTime) {
        return Kline.builder()
                .startTime(startTime)
                .openPrice(close)
                .highPrice(close)
                .lowPrice(close)
                .closePrice(close)
                .volume(100)
                .turnover(1000)
                .build();
    }
}

