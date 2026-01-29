package com.spayker.crypto.analysis.dao.socket.exchange.bybit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TickerMessage {

    private String symbol;
    private String lastPrice;
    private String highPrice24h;
    private String lowPrice24h;
    private String prevPrice24h;
    private String volume24h;
    private String turnover24h;
    private String price24hPcnt;
    private String usdIndexPrice;

}
