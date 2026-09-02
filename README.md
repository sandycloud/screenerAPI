# ScreenerAPI Spring Boot Project

This project provides REST APIs to:
- Fetch and store 5-minute stock price candle data for a stock from an external API into SQLite.
- Calculate and return ADX, +DI, and -DI for a stock based on stored data.

## Features
- Stores each candle as a row in the `stock_price_5min` table.
- Fetches candle data from an external API using ISIN, time frame, and fromTime.
- Calculates ADX, +DI, -DI for a given stock and time range.

## Tech Stack
- Java
- Spring Boot (Web, Data JPA)
- SQLite

## How to Run
1. Build the project with Maven or Gradle.
2. Start the application with the required auth tokens passed as command-line arguments so the values are not hardcoded in source:
   - `./mvnw spring-boot:run -Dspring-boot.run.arguments="--scanx.auth=YOUR_SCANX_TOKEN,--adx.auth=YOUR_ADX_TOKEN"`
   - or `java -jar target/screenerAPI-0.0.1-SNAPSHOT.jar --scanx.auth=YOUR_SCANX_TOKEN --adx.auth=YOUR_ADX_TOKEN`

Note: Use the above command with auth info only if connecting to new scanx url to retrieve momentum stock names. Because "dhan" broker has added security to the new url. you will need to create an account in Dhan. 
If you use the older url to find momentum stocks then no need to supply auth info.

3. Use the provided REST endpoints to interact with the API.

> Do not commit real tokens to the repository. Keep them in your local environment or pass them as startup arguments.

## Endpoints
- `POST /api/stock/fetch-and-store` - Fetches and stores candle data.
- `GET /api/stock/adx` - Returns ADX, +DI, -DI for a stock.

## Background stock processors

The priority processors are disabled by default. Set `stock.processor.enabled=true` to enable them.
They first run one minute after the next five-minute boundary. The high-priority processor fetches
ScanX ADX uptrend/downtrend stocks and configured indices, then stores candles through the existing
subsequent-fetch flow. The low-priority processor fetches unusual-volume stocks while high priority
is idle and skips stocks whose `stock_info.timeAtLastDataFetch` is within the high-priority interval.

Important properties include `stock.processor.interval.minutes`,
`stock.processor.low-priority.interval.minutes`, `scanx.api.url`, `nse_index`, and `bse_index`.
Set `stock.processor.low-priority.mode=one-pass` to run the secondary processor only once after startup;
the default `repeat` mode runs it at the configured interval.
Processor log entries are tagged `HIGH Priority` or `LOW Priority`.

---
