package com.spayker.crypto.analysis.dao.socket.publisher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IndicatorSocketPublisherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private IndicatorSocketPublisher publisher;

    @Test
    void publish_shouldSendIndicatorUpdateToCorrectTopic() {
        // given
        String symbol = "btcUSDT";
        String indicatorName = "rsi";
        String indicatorValue = "72.5";
        String timeFrame = "hour";

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<IndicatorUpdateDto> dtoCaptor =
                ArgumentCaptor.forClass(IndicatorUpdateDto.class);

        // when
        publisher.publish(symbol, indicatorName, indicatorValue, timeFrame);

        // then
        verify(messagingTemplate, times(1))
                .convertAndSend(topicCaptor.capture(), dtoCaptor.capture());

        String expectedTopic = "/topic/indicator/btcUSDT/rsi/hour";
        assertEquals(expectedTopic, topicCaptor.getValue());

        IndicatorUpdateDto dto = dtoCaptor.getValue();
        assertEquals(symbol, dto.getSymbol());
        assertEquals(indicatorName, dto.getIndicator());
        assertEquals(timeFrame, dto.getTimeframe());
        assertEquals(indicatorValue, dto.getValue());
    }
}