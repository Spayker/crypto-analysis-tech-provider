package com.spayker.crypto.analysis.dao.rest.bybit;

import com.spayker.crypto.analysis.dao.rest.bybit.dto.ExchangeResponse;
import com.spayker.crypto.analysis.dao.rest.bybit.dto.kline.KlineResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class PublicApiClientTest {

    private PublicApiClient publicApiClient;

    @BeforeEach
    void setUp() {
        publicApiClient = Mockito.mock(PublicApiClient.class);
    }

    @Test
    void testGetCandlesHistory() {
        // given
        ExchangeResponse<KlineResult> mockResponse = new ExchangeResponse<>();
        when(publicApiClient.getCandlesHistory("spot", "BTCUSD", "1m", "100"))
                .thenReturn(mockResponse);

        // when
        ExchangeResponse<KlineResult> response = publicApiClient.getCandlesHistory("spot", "BTCUSD", "1m", "100");

        // then
        verify(publicApiClient, times(1)).getCandlesHistory("spot", "BTCUSD", "1m", "100");
        assertSame(mockResponse, response);
    }
}
