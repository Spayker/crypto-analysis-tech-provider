package com.spayker.crypto.analysis.dao.socket.exchange.bybit.dto;


import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PingMessage {

    private String req_id;
    private String op;

}
