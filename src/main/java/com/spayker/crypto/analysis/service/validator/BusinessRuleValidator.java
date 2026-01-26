package com.spayker.crypto.analysis.service.validator;

import com.spayker.crypto.analysis.dto.indicator.IndicatorMetaData;
import com.spayker.crypto.analysis.dto.indicator.IndicatorRequest;
import com.spayker.crypto.analysis.service.validator.exception.IndicatorAlreadyExistsException;
import com.spayker.crypto.analysis.service.validator.exception.MaxIndicatorLimitationReachedException;
import com.spayker.crypto.analysis.service.validator.exception.UnsupportedIndicatorException;
import com.spayker.crypto.analysis.service.validator.exception.UnsupportedTimeFrameWithIndicator;

import java.util.List;
import java.util.Map;

public class BusinessRuleValidator {

    private BusinessRuleValidator() {}

    public static void validate(IndicatorRequest indicatorRequest,
                                Map<String, String> availableIndicators) {
        String indicatorName = indicatorRequest.name();
        String requestedTimeFrame = indicatorRequest.timeFrame().getValue().toLowerCase();
        if (!availableIndicators.containsKey(indicatorName.toLowerCase())) {
            throw new UnsupportedIndicatorException(indicatorName);
        }

        String actualTimeFrame = availableIndicators.get(indicatorName.toLowerCase());
        if (!actualTimeFrame.equalsIgnoreCase(requestedTimeFrame)) {
            throw new UnsupportedTimeFrameWithIndicator(indicatorName, requestedTimeFrame);
        }
    }

    public static void validate(IndicatorRequest indicatorRequest,
                                int maxLimitation,
                                Map<String, List<IndicatorMetaData>> indicatorMetaData) {

        String coin = indicatorRequest.symbol();
        String indName = indicatorRequest.name();
        String timeFrame = indicatorRequest.timeFrame().getValue().toLowerCase();
        List<IndicatorMetaData> existingList = indicatorMetaData.get(coin);

        if (existingList != null) {
            for (IndicatorMetaData existing : existingList) {
                boolean sameName = existing.name().equalsIgnoreCase(indName);
                boolean sameTimeFrame = existing.timeFrame().equalsIgnoreCase(timeFrame);
                if (sameName && sameTimeFrame) {
                    throw new IndicatorAlreadyExistsException(indName, timeFrame);
                }
                if (sameName && !sameTimeFrame) {
                    return;
                }
            }
        }
        long totalIndicators = indicatorMetaData.values()
                .stream()
                .mapToLong(List::size)
                .sum();

        if (totalIndicators >= maxLimitation) {
            throw new MaxIndicatorLimitationReachedException();
        }
    }
}