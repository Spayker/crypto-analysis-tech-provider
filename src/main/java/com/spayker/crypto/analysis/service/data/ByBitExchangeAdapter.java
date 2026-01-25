package com.spayker.crypto.analysis.service.data;

import com.spayker.crypto.analysis.dao.rest.bybit.PublicApiClient;
import com.spayker.crypto.analysis.dao.rest.bybit.dto.ExchangeResponse;
import com.spayker.crypto.analysis.dao.rest.bybit.dto.kline.Kline;
import com.spayker.crypto.analysis.dao.rest.bybit.dto.kline.KlineResult;
import feign.RetryableException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class ByBitExchangeAdapter {

    private final PublicApiClient publicApiClient;
    private static final String SPOT_TRADE_CATEGORY = "spot";

    public List<Kline> getHistory(String symbol, String interval, String limit) {
        List<Kline> kLines = Collections.emptyList();
        try {
            final ExchangeResponse<KlineResult> kLineResponse = publicApiClient.getCandlesHistory(SPOT_TRADE_CATEGORY,
                    symbol.toUpperCase(), interval, String.valueOf(limit));
            final KlineResult kLineResult = kLineResponse.getResult();
            if (kLineResult != null) {
                kLines = kLineResult.getKlines();
            }
        } catch (RetryableException rE) {
            log.error(rE.getMessage());
        }
        Collections.reverse(kLines);
        return kLines;
    }
}