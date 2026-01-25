package com.spayker.crypto.analysis.dao.rest.bybit.dto.kline;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import com.spayker.crypto.analysis.dao.rest.bybit.dto.ExchangeResponseResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class KlineResult implements ExchangeResponseResult {

    private String category;
    private String symbol;

    @JsonProperty("list")
    @SerializedName(value = "list", alternate = "klines")
    private List<Kline> klines;


}
