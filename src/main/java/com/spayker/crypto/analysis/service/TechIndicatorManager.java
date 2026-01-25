package com.spayker.crypto.analysis.service;

import com.spayker.crypto.analysis.config.IndicatorProviderConfig;
import com.spayker.crypto.analysis.dao.socket.PublicWebSocketManager;
import com.spayker.crypto.analysis.dto.indicator.FixedDataList;
import com.spayker.crypto.analysis.dto.indicator.IndicatorMetaData;
import com.spayker.crypto.analysis.dto.indicator.IndicatorRequest;
import com.spayker.crypto.analysis.dto.indicator.IndicatorResponse;
import com.spayker.crypto.analysis.dto.indicator.TimeFrame;
import com.spayker.crypto.analysis.service.data.indicator.IndicatorDataProvider;
import com.spayker.crypto.analysis.service.validator.BusinessRuleValidator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@AllArgsConstructor
public class TechIndicatorManager {

    private final IndicatorProviderConfig indicatorProviderConfig;
    private final IndicatorDataProvider indicatorDataProvider;
    private final PublicWebSocketManager publicWebSocketManager;

    public List<IndicatorMetaData> getAvailableIndications() {
        Map<TimeFrame, Map<String, Map<String, FixedDataList<String>>>> rawData =
                indicatorDataProvider.getRawIndicatorData();

        return rawData.entrySet().stream()
                .flatMap(tfEntry -> {
                    TimeFrame timeFrame = tfEntry.getKey();
                    return tfEntry.getValue().values().stream()
                            .flatMap(map -> map.entrySet().stream())
                            .map(indicatorEntry -> {
                                FixedDataList<String> list = indicatorEntry.getValue();
                                String lastValue = list.getLast();
                                return new IndicatorMetaData(
                                        indicatorEntry.getKey(),
                                        list.getSize(),
                                        lastValue,
                                        timeFrame.getValue().toLowerCase()
                                );
                            });
                })
                .toList();
    }

    public IndicatorResponse getTechIndicatorData(IndicatorRequest indicatorRequest) {
        BusinessRuleValidator.validate(indicatorRequest, indicatorDataProvider.getAvailableIndicators());
        String symbol = indicatorRequest.symbol() + indicatorProviderConfig.getStableCoin();
        TimeFrame timeFrame = indicatorRequest.timeFrame();
        String indicatorName = indicatorRequest.name();
        FixedDataList<String> indicatorData = indicatorDataProvider.getIndicatorData(timeFrame, symbol, indicatorName);
        return new IndicatorResponse(
                symbol,
                timeFrame.getValue(),
                indicatorData.snapshot()
        );
    }

    public void addIndicatorBySymbol(IndicatorRequest indicatorRequest) {
        BusinessRuleValidator.validate(indicatorRequest, indicatorProviderConfig.getMaxIndicators(),
                getAvailableIndications());
        String symbol = indicatorRequest.symbol() + indicatorProviderConfig.getStableCoin();
        indicatorDataProvider.initIndicator(indicatorRequest.timeFrame(),
                symbol,
                indicatorRequest.name());
        publicWebSocketManager.startListening(List.of(symbol));
    }
}
