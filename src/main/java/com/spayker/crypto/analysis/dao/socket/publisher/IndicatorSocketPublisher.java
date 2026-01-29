package com.spayker.crypto.analysis.dao.socket.publisher;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IndicatorSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publish(String symbol,
                        String indicatorName,
                        String indicatorValue,
                        String timeFrame) {
        IndicatorUpdateDto indicatorUpdateDto = new IndicatorUpdateDto(
                symbol,
                indicatorName,
                timeFrame,
                indicatorValue
        );
        String topic = String.format(
                "/topic/indicator/%s/%s/%s",
                indicatorUpdateDto.getSymbol(),
                indicatorUpdateDto.getIndicator(),
                indicatorUpdateDto.getTimeframe()
        );
        messagingTemplate.convertAndSend(topic, indicatorUpdateDto);
    }

}