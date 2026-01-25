package com.spayker.crypto.analysis.service.data.indicator;

import com.spayker.crypto.analysis.dao.rest.bybit.dto.kline.Kline;

import java.util.List;

public interface IndicatorCalculator {

    List<String> calculate(List<Kline> kLines);

}
