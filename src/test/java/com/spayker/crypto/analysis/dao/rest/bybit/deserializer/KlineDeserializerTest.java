package com.spayker.crypto.analysis.dao.rest.bybit.deserializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.spayker.crypto.analysis.dao.rest.bybit.dto.kline.Kline;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KlineDeserializerTest {

    private final ObjectMapper mapper;

    public KlineDeserializerTest() {
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Kline.class, new KlineDeserializer());
        mapper.registerModule(module);
    }

    @Test
    void testDeserializeKline() throws Exception {
        // given
        String json = "[1672531200000, 50000.0, 51000.0, 49500.0, 50500.0, 10.5, 525000.0]";

        // when
        Kline kline = mapper.readValue(json, Kline.class);

        // then
        assertEquals(1672531200000L, kline.getStartTime());
        assertEquals(50000.0, kline.getOpenPrice());
        assertEquals(51000.0, kline.getHighPrice());
        assertEquals(49500.0, kline.getLowPrice());
        assertEquals(50500.0, kline.getClosePrice());
        assertEquals(10.5, kline.getVolume());
        assertEquals(525000.0, kline.getTurnover());
    }

    @Test
    void testDeserializeKlineWithDifferentValues() throws Exception {
        // given
        String json = "[1672531300000, 60000.0, 61000.0, 59500.0, 60500.0, 20.0, 1210000.0]";

        // when
        Kline kline = mapper.readValue(json, Kline.class);

        // then
        assertEquals(1672531300000L, kline.getStartTime());
        assertEquals(60000.0, kline.getOpenPrice());
        assertEquals(61000.0, kline.getHighPrice());
        assertEquals(59500.0, kline.getLowPrice());
        assertEquals(60500.0, kline.getClosePrice());
        assertEquals(20.0, kline.getVolume());
        assertEquals(1210000.0, kline.getTurnover());
    }
}
