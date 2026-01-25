package com.spayker.crypto.analysis.dao.rest.bybit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExchangeResponse<T extends ExchangeResponseResult> {

    private Integer retCode;
    private String retMsg;
    private T result;
    private String time;

}