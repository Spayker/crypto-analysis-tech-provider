package com.spayker.crypto.analysis.dao.socket.exchange.bybit.handler;

import com.google.gson.Gson;
import com.spayker.crypto.analysis.dao.socket.exchange.SocketMessage;
import org.springframework.web.socket.WebSocketMessage;

public interface SocketMessageHandler {

    Gson mGson = new Gson();

    SocketMessage handleMessage(WebSocketMessage<?> message);
}
