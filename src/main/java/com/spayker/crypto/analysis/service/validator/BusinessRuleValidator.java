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
                                List<IndicatorMetaData> indicatorMetaData) {

        boolean alreadyExists = indicatorMetaData.stream()
                .anyMatch(meta ->
                        meta.name().equalsIgnoreCase(indicatorRequest.name())
                                && meta.timeFrame().equalsIgnoreCase(
                                indicatorRequest.timeFrame().getValue()
                        )
                );

        if (alreadyExists) {
            throw new IndicatorAlreadyExistsException(
                    indicatorRequest.name(),
                    indicatorRequest.timeFrame().getValue()
            );
        }

        if (maxLimitation < indicatorMetaData.size()) {
            throw new MaxIndicatorLimitationReachedException();
        }
    }
}