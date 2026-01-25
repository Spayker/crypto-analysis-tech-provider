package com.spayker.crypto.analysis.dao.socket.bybit.handler;

import com.google.gson.Gson;
import com.spayker.crypto.analysis.dao.socket.SocketMessage;
import org.springframework.web.socket.WebSocketMessage;

public interface SocketMessageHandler {

    Gson mGson = new Gson();

    SocketMessage handleMessage(WebSocketMessage<?> message);
}
