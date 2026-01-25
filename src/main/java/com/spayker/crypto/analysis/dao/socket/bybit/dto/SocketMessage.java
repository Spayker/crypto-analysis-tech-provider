package com.spayker.crypto.analysis.dao.socket.bybit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SocketMessage {

    private String req_id = "";
    private String op;
    private String[] args;

    public SocketMessage(String op, String[] args) {
        this.op = op;
        this.args = args;
    }
}
