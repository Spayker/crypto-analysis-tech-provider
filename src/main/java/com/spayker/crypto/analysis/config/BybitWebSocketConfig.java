package com.spayker.crypto.analysis.config;

import com.spayker.crypto.analysis.dao.socket.ExchangeConnectionSupportManager;
import com.spayker.crypto.analysis.dao.socket.bybit.PublicSocketSessionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("bybit")
public class BybitWebSocketConfig {

    @Bean
    public ExchangeConnectionSupportManager exchangeConnectionSupportManager(
            PublicSocketSessionHandler socketSessionHandler,
            SocketProviderConfig socketProviderConfig
    ) {
        return new ExchangeConnectionSupportManager(
                socketSessionHandler,
                socketProviderConfig
        );
    }
}
