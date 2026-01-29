package com.spayker.crypto.analysis.config;

import com.spayker.crypto.analysis.dao.socket.exchange.ExchangeConnectionSupportManager;
import com.spayker.crypto.analysis.dao.socket.exchange.bybit.PublicSocketSessionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("bybit")
public class ExchangeWebSocketConfig {

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
