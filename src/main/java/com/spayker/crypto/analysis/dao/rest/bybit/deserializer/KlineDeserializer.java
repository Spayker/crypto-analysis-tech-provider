package com.spayker.crypto.analysis.dao.rest.bybit.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.spayker.crypto.analysis.dao.rest.bybit.dto.kline.Kline;

import java.io.IOException;

public class KlineDeserializer extends JsonDeserializer<Kline> {

    @Override
    public Kline deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        ObjectCodec oc = jsonParser.getCodec();
        JsonNode node = oc.readTree(jsonParser);
        return Kline.builder()
                .startTime(node.get(0).asLong())
                .openPrice(node.get(1).asDouble())
                .highPrice(node.get(2).asDouble())
                .lowPrice(node.get(3).asDouble())
                .closePrice(node.get(4).asDouble())
                .volume(node.get(5).asDouble())
                .turnover(node.get(6).asDouble())
                .build();
    }
}