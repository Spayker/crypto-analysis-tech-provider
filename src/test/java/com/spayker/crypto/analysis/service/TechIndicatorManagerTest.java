package com.spayker.crypto.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.spayker.crypto.analysis.config.IndicatorProviderConfig;
import com.spayker.crypto.analysis.dao.socket.PublicWebSocketManager;
import com.spayker.crypto.analysis.dto.indicator.*;
import com.spayker.crypto.analysis.service.data.indicator.IndicatorDataProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

class TechIndicatorManagerTest {

    private IndicatorDataProvider indicatorDataProvider;
    private PublicWebSocketManager publicWebSocketManager;
    private IndicatorProviderConfig indicatorProviderConfig;
    private TechIndicatorManager techIndicatorManager;

    @BeforeEach
    void setup() {
        indicatorDataProvider = mock(IndicatorDataProvider.class);
        publicWebSocketManager = mock(PublicWebSocketManager.class);
        indicatorProviderConfig = mock(IndicatorProviderConfig.class);
        techIndicatorManager = new TechIndicatorManager(indicatorProviderConfig, indicatorDataProvider, publicWebSocketManager);
    }

    @Test
    void getAvailableIndications_shouldReturnMappedMetaData() {
        // given
        FixedDataList<String> list = mock(FixedDataList.class);
        when(list.getLast()).thenReturn("123.45");
        when(list.getSize()).thenReturn(10);

        when(indicatorDataProvider.getRawIndicatorData()).thenReturn(Map.of(
                TimeFrame.MINUTE, Map.of(
                        "btc", Map.of("rsi", list)
                )
        ));

        // when
        Map<String, List<IndicatorMetaData>> result = techIndicatorManager.getAvailableIndications();

        // then
        assertThat(result).hasSize(1); // только одна монета "btc"
        assertThat(result).containsKey("btc");

        List<IndicatorMetaData> metaList = result.get("btc");
        assertThat(metaList).hasSize(1);

        IndicatorMetaData meta = metaList.get(0);
        assertThat(meta.name()).isEqualTo("rsi");
        assertThat(meta.size()).isEqualTo(10);
        assertThat(meta.last()).isEqualTo("123.45");
        assertThat(meta.timeFrame()).isEqualTo("minute");
    }

    @Test
    void getTechIndicatorData_shouldReturnCorrectSnapshot() {
        // given
        FixedDataList<String> list = mock(FixedDataList.class);
        when(list.snapshot()).thenReturn(List.of("10", "20", "30"));

        IndicatorRequest request = new IndicatorRequest("rsi", "BTC", TimeFrame.MINUTE);

        when(indicatorDataProvider.getAvailableIndicators()).thenReturn(Map.of("rsi", "minute"));
        when(indicatorDataProvider.getIndicatorData(TimeFrame.MINUTE, "BTCUSDT", "rsi")).thenReturn(list);
        when(indicatorProviderConfig.getStableCoin()).thenReturn("USDT");
        // when
        IndicatorResponse response = techIndicatorManager.getTechIndicatorData(request);

        // then
        assertThat(response.coin()).isEqualTo("BTCUSDT");
        assertThat(response.timeFrame()).isEqualTo("minute");
        assertThat(response.data()).containsExactly("10", "20", "30");
    }

    @Test
    void addIndicatorBySymbol_shouldInitializeIndicatorAndStartListening() {
        // given
        IndicatorRequest request = new IndicatorRequest("macd", "ETH", TimeFrame.HOUR);
        when(indicatorProviderConfig.getStableCoin()).thenReturn("USDT");
        when(indicatorProviderConfig.getMaxIndicators()).thenReturn(5);

        // when
        techIndicatorManager.addIndicatorBySymbol(request);

        // then
        verify(indicatorDataProvider).initIndicator(TimeFrame.HOUR, "ETHUSDT", "macd");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(publicWebSocketManager).startListening(captor.capture());
        assertThat(captor.getValue()).isEqualTo("ETHUSDT");
    }
}
