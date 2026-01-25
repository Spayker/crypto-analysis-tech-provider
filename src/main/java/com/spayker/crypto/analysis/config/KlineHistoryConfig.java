package com.spayker.crypto.analysis.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "kline-history")
public class KlineHistoryConfig {

    private String maxMinuteKlineSize;
    private String maxHourKlineSize;
    private String maxDayKlineSize;
    private String minuteIntervalType;
    private String hourIntervalType;
    private String dayIntervalType;

}