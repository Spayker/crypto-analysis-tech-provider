package com.spayker.crypto.analysis.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "indicator.provider")
public class IndicatorProviderConfig {

    private int maxIndicators;
    private String stableCoin;

}