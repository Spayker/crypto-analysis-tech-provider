package com.spayker.crypto.analysis.dao.rest.bybit;


import com.spayker.crypto.analysis.dao.rest.bybit.dto.ExchangeResponse;
import com.spayker.crypto.analysis.dao.rest.bybit.dto.kline.KlineResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Profile("bybit")
@FeignClient(name = "${exchange.rest-provider.public-api-name}", url = "${exchange.rest-provider.url}")
public interface PublicApiClient {

    String LIMIT = "limit";
    String INTERVAL = "interval";
    String SYMBOL = "symbol";
    String CATEGORY = "category";

    @GetMapping(value = "/v5/market/kline")
    ExchangeResponse<KlineResult> getCandlesHistory(@RequestParam(CATEGORY) final String category,
                                                    @RequestParam(SYMBOL) final String symbol,
                                                    @RequestParam(INTERVAL) final String interval,
                                                    @RequestParam(LIMIT) final String limit);


}