# crypto-analysis-tech-provider

[![Build Status](https://github.com/Spayker/crypto-analysis-tech-provider/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/Spayker/crypto-analysis-tech-provider/actions/workflows/ci.yml) &nbsp;
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://github.com/Spayker/crypto-analysis-tech-provider/blob/main/LICENSE)

## Overview
Originally, [TradingView](https://www.tradingview.com/) API does not provide any possibilities to get favourite indications (f.e. rsi, macd or bollinger bands). It gives only well shaped UI snippet that can be integrated with frontend and that's it.

This MVP tries to solve the problem, by giving a free REST API, which can provide latest indicator data by requested:
- time frame
- symbol
- indicator name 

Works only with ByBit crypto exchange (for now). Currently supported indications:
- rsi
- macd
- bollinger bands

Supported time frames:
- minute
- hour
- day

Service limitations:
- max indicator limitation: 10 (configurable)
- max indicator range: 240 for minutes and hours, 120 for days (configurable)

From the beginning the user with ADMIN role must use POST end point to add indicators. Then users can get list of supported indications per symbol, time frame and request data. More details are described below. 

## REST API end points
Below is a table of REST endpoints defined in `TechIndicatorController`.

| Method | Endpoint | Parameters | Description                                                             | Authorization |
|--------|----------|-------------|-------------------------------------------------------------------------|----------------|
| GET    | `/v1/tech/indicators` | — | Fetch all available technical indicators                                | Not required |
| GET    | `/v1/{symbol}/tech/indicators/{indicatorName}` | `symbol` — [a-zA-Z]{1,10}<br>`indicatorName` — string<br>`timeFrame` — query param | Retrieve indicator data for a specific symbol                           | Not required |
| POST   | `/v1/{symbol}/tech/indicators/{indicatorName}` | `symbol` — [a-zA-Z]{1,10}<br>`indicatorName` — string<br>`timeFrame` — query param | Add a new technical indicator for a symbol| Requires `ADMIN` role |
| DELETE | `/v1/{symbol}/tech/indicators/{indicatorName}` | `symbol` — [a-zA-Z]{1,10}<br>`indicatorName` — string<br>`timeFrame` — query param | Removes technical indicator for a symbol  | Requires `ADMIN` role |--------|----------|-------------|--------------|----------------|

### Example Requests
```
GET /v1/tech/indicators
{
    "btcUSDT": [
        {
            "name": "rsi",
            "size": 225,
            "last": "50.19",
            "timeFrame": "hour"
        },
        {
            "name": "bolBands",
            "size": 221,
            "last": "87421.64,88552.39,86290.88",
            "timeFrame": "hour"
        }
    ],
    "ethUSDT": [
        {
            "name": "bolBands",
            "size": 221,
            "last": "2863.28,2933.56,2793.01",
            "timeFrame": "hour"
        }
    ]
}

GET /v1/eth/tech/indicators/rsi?timeFrame=hour
Response example:
{
    "coin": "ethUSDT",
    "timeFrame": "hour",
    "data": [
        "48.71",
        "48.49",
        ...
        "53.97"
    ]
}

```
---

## How It Works
1. The client sends a GET request.
2. The controller constructs an `IndicatorRequest`.
3. `TechIndicatorManager` processes the request:
    - validates parameters;
    - fetches kline data from exchange;
    - calculates the requested indicator;
    - runs socket subscription to requested channel in order to keep indicator data updated
    - returns the result to the controller.
4. The controller responds back to the client.

---

## Architecture
![alt text](resources/svg/Technical_data_provider.svg)

Main components:
- Tech Indicator Controller: handles user requests, performs request validation, forwards requests to service layer
- Indicator Data Provider: stores indicator data and updates it, according to web socket ticker, kline subscriptions
- Tech Indicator Manager: processes users requests after additional business related validations
- Trade History Manager: stores and updates trade history by time frame and symbols
- Public Web Socket Manager: establishes socket channels with Bybit exchange to listen latest kline and ticker events

---

## Technical Stack
1. Java 21
2. Spring Boot 3
3. Feign Client
4. WebSockets
5. Jackson
6. Gson
7. JUnit 5
8. GitHub Actions

## How To Run
For local IDE only: use local run config with added VM option:
```
-Dspring.profiles.active=bybit
```

Docker run:
```
docker build -t crypto-analysis-tech-provider .
docker run -d --name crypto-analysis-tech-provider \
  -p 8084:8084 \
  crypto-analysis-tech-provider  
```

## Socket subscription
The service supports subscription to get latest indicator values by symbol, indicator name and time frame.
Connect address: ws://localhost:8084/ws
Subscribe topic examples:
```
   /topic/indicator/ethUSDT/rsi/minute
   /topic/indicator/ethUSDT/rsi/hour
   /topic/indicator/ethUSDT/rsi/day
   
   /topic/indicator/btcUSDT/macd/minute
   /topic/indicator/btcUSDT/macd/hour
   /topic/indicator/btcUSDT/macd/day
```

Published message example:
```
   {
     "symbol": "ethUSDT",
     "indicator": "rsi",
     "timeframe": "hour",
     "value": 47.83
   }
```

## Further vectors of improvements
Most exciting vectors of improvements are presented below:
- different exchanges support (Binance, CoinBase, Kraken etc...)
- different indicators support (ADX, EMA, ATR etc...)
- security part (enabling https, adding jwt)
- indicator data persistence for accumulating of bigger data volumes per exchange, symbol, indicator, time frame
- further improvements of data accuracy generation
- timestamps for provided data items (rest and socket)

## License
GNU 3