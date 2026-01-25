package com.spayker.crypto.analysis.service.data.indicator;

import com.spayker.crypto.analysis.dao.rest.bybit.dto.kline.Kline;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component("rsi")
public class Rsi implements IndicatorCalculator {

    private static final int PERIOD = 14;
    private static final int RESULT_SCALE = 2;

    public List<String> calculate(List<Kline> kLines) {
        List<String> rsiValues = new ArrayList<>();
        List<Double> gains = new ArrayList<>();
        List<Double> losses = new ArrayList<>();

        for (int i = 1; i < kLines.size(); i++) {
            double change = kLines.get(i).getClosePrice() - kLines.get(i - 1).getClosePrice();
            if (change > 0) {
                gains.add(change);
                losses.add(0.0);
            } else {
                gains.add(0.0);
                losses.add(Math.abs(change));
            }
        }

        double avgGain = gains.stream().limit(PERIOD).mapToDouble(Double::doubleValue).sum() / PERIOD;
        double avgLoss = losses.stream().limit(PERIOD).mapToDouble(Double::doubleValue).sum() / PERIOD;
        for (int i = PERIOD; i < gains.size(); i++) {
            avgGain = (avgGain * (PERIOD - 1) + gains.get(i)) / PERIOD;
            avgLoss = (avgLoss * (PERIOD - 1) + losses.get(i)) / PERIOD;
            double rs = (avgLoss == 0) ? 100 : avgGain / avgLoss;
            double rsi = 100 - (100 / (1 + rs));
            rsiValues.add(BigDecimal.valueOf(rsi).setScale(RESULT_SCALE, RoundingMode.HALF_DOWN).toPlainString());
        }
        return rsiValues;
    }
}
