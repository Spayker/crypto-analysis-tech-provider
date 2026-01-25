package com.spayker.crypto.analysis.dao.socket.bybit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TickerDataContainer {

    private String topic;
    private Long ts;
    private String type;
    private Long cs;

    @JsonProperty("data")
    @SerializedName(value = "data", alternate = "rawData")
    private TickerMessage rawData;

}
