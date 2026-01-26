package com.spayker.crypto.analysis.service.validator;

import com.spayker.crypto.analysis.dto.indicator.IndicatorMetaData;
import com.spayker.crypto.analysis.dto.indicator.IndicatorRequest;
import com.spayker.crypto.analysis.dto.indicator.TimeFrame;
import com.spayker.crypto.analysis.service.validator.exception.IndicatorAlreadyExistsException;
import com.spayker.crypto.analysis.service.validator.exception.MaxIndicatorLimitationReachedException;
import com.spayker.crypto.analysis.service.validator.exception.UnsupportedIndicatorException;
import com.spayker.crypto.analysis.service.validator.exception.UnsupportedTimeFrameWithIndicator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class BusinessRuleValidatorTest {

    private Map<String, List<IndicatorMetaData>> indicatorMetaData;

    @BeforeEach
    void setUp() {
        indicatorMetaData = new HashMap<>();
        indicatorMetaData.put("btcusdt", List.of(
                new IndicatorMetaData("rsi", 14, "50", "minute"),
                new IndicatorMetaData("macd", 26, "1.2", "hour"),
                new IndicatorMetaData("adx", 14, "25", "day")
        ));
    }

    @Test
    void validate_shouldPass_whenIndicatorNotExistsAndLimitNotReached() {
        IndicatorRequest request = IndicatorRequest.builder()
                .name("ema")
                .symbol("btcusdt")
                .timeFrame(TimeFrame.MINUTE)
                .build();

        assertThatCode(() ->
                BusinessRuleValidator.validate(request, 5, indicatorMetaData)
        ).doesNotThrowAnyException();
    }

    @Test
    void validate_shouldBeCaseInsensitiveForIndicatorNameAndTimeFrame() {
        IndicatorRequest request = IndicatorRequest.builder()
                .name("RSI")
                .symbol("btcusdt")
                .timeFrame(TimeFrame.MINUTE)
                .build();

        assertThatThrownBy(() ->
                BusinessRuleValidator.validate(request, 5, indicatorMetaData)
        ).isInstanceOf(IndicatorAlreadyExistsException.class);
    }

    @Test
    void validate_shouldThrowIndicatorAlreadyExistsException_whenSameIndicatorAndTimeFrame() {
        IndicatorRequest request = IndicatorRequest.builder()
                .name("rsi")
                .symbol("btcusdt")
                .timeFrame(TimeFrame.MINUTE)
                .build();

        assertThatThrownBy(() ->
                BusinessRuleValidator.validate(request, 5, indicatorMetaData)
        )
                .isInstanceOf(IndicatorAlreadyExistsException.class)
                .satisfies(ex -> {
                    IndicatorAlreadyExistsException e = (IndicatorAlreadyExistsException) ex;
                    assertThat(e.getDetails())
                            .containsEntry("indicator", "rsi")
                            .containsEntry("timeFrame", "minute");
                });
    }

    @Test
    void validate_shouldPass_whenSameIndicatorButDifferentTimeFrame() {
        IndicatorRequest request = IndicatorRequest.builder()
                .name("rsi")
                .symbol("btcusdt")
                .timeFrame(TimeFrame.HOUR)
                .build();

        assertThatCode(() ->
                BusinessRuleValidator.validate(request, 5, indicatorMetaData)
        ).doesNotThrowAnyException();
    }

    @Test
    void validate_shouldPass_whenMaxLimitationNotReached() {
        IndicatorRequest request = IndicatorRequest.builder()
                .name("ema")
                .symbol("btcusdt")
                .timeFrame(TimeFrame.MINUTE)
                .build();
        assertThatCode(() ->
                BusinessRuleValidator.validate(request, 4, indicatorMetaData)
        ).doesNotThrowAnyException();
    }

    @Test
    void validate_shouldThrowMaxIndicatorLimitationReachedException_whenLimitExceeded() {
        IndicatorRequest request = IndicatorRequest.builder()
                .name("ema")
                .symbol("btcusdt")
                .timeFrame(TimeFrame.MINUTE)
                .build();

        assertThatThrownBy(() ->
                BusinessRuleValidator.validate(request, 2, indicatorMetaData)
        ).isInstanceOf(MaxIndicatorLimitationReachedException.class);
    }

    @Test
    void validate_shouldThrowMaxIndicatorLimitationReachedException_whenLimitIsZero() {
        IndicatorRequest request = IndicatorRequest.builder()
                .name("rsi")
                .symbol("btcusdt")
                .timeFrame(TimeFrame.MINUTE)
                .build();

        assertThatThrownBy(() ->
                BusinessRuleValidator.validate(request, 0, Map.of())
        ).isInstanceOf(MaxIndicatorLimitationReachedException.class);
    }
}
