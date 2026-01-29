package com.spayker.crypto.analysis.dao.socket.exchange.bybit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KLineDataContainer {

    private String topic;
    private Long ts;
    private String type;

    @JsonProperty("data")
    @SerializedName(value = "data", alternate = "rawData")
    private List<KLineMessage> rawData;
}
