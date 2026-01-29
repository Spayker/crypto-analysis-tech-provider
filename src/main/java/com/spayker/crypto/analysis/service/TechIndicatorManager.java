package com.spayker.crypto.analysis.service;

import com.spayker.crypto.analysis.config.IndicatorProviderConfig;
import com.spayker.crypto.analysis.dao.socket.exchange.PublicWebSocketManager;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class TechIndicatorManager {

    private final IndicatorProviderConfig indicatorProviderConfig;
    private final IndicatorDataProvider indicatorDataProvider;
    private final PublicWebSocketManager publicWebSocketManager;

    public Map<String, List<IndicatorMetaData>> getAvailableIndications() {
        Map<TimeFrame, Map<String, Map<String, FixedDataList<String>>>> rawData =
                indicatorDataProvider.getRawIndicatorData();

        return rawData.entrySet().stream()
                .flatMap(tfEntry -> {
                    TimeFrame timeFrame = tfEntry.getKey();
                    return tfEntry.getValue().entrySet().stream()
                            .flatMap(coinEntry -> {
                                String coin = coinEntry.getKey();

                                return coinEntry.getValue().entrySet().stream()
                                        .map(indEntry -> {
                                            FixedDataList<String> list = indEntry.getValue();
                                            String lastValue = list.getLast();

                                            IndicatorMetaData meta = new IndicatorMetaData(
                                                    indEntry.getKey(),
                                                    list.getSize(),
                                                    lastValue,
                                                    timeFrame.getValue().toLowerCase()
                                            );

                                            return Map.entry(coin, meta);
                                        });
                            });
                })
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new ArrayList<>(List.of(e.getValue())),
                        (list1, list2) -> {
                            list1.addAll(list2);
                            return list1;
                        }
                ));
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
        publicWebSocketManager.startListening(symbol);
    }

    public void removeIndicator(IndicatorRequest indicatorRequest) {
        String symbol = indicatorRequest.symbol() + indicatorProviderConfig.getStableCoin();
        TimeFrame timeFrame = indicatorRequest.timeFrame();
        String indicatorName = indicatorRequest.name();
        indicatorDataProvider.removeIndicator(timeFrame, symbol, indicatorName);
        Map<TimeFrame, Map<String, Map<String, FixedDataList<String>>>> rawData =
                indicatorDataProvider.getRawIndicatorData();

        boolean hasIndicatorsLeft = rawData.getOrDefault(timeFrame, Map.of())
                .getOrDefault(symbol, Map.of())
                .values()
                .stream()
                .anyMatch(list -> list != null && list.getSize() > 0);
        if (!hasIndicatorsLeft) {
            publicWebSocketManager.stopListening(symbol);
            log.info("Stopped public socket subscription for symbol {} as no indicators remain", symbol);
        }
    }
}
