package com.spayker.crypto.analysis.dao.rest.bybit.dto.kline;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.spayker.crypto.analysis.dao.rest.bybit.deserializer.KlineDeserializer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = KlineDeserializer.class)
public class Kline {

    private Long startTime;
    private double openPrice;
    private double highPrice;
    private double lowPrice;
    private double closePrice;
    private double volume;
    private double turnover;

}