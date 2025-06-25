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
2. Run the application: `./mvnw spring-boot:run` or `./gradlew bootRun`
3. Use the provided REST endpoints to interact with the API.

## Endpoints
- `POST /api/stock/fetch-and-store` - Fetches and stores candle data.
- `GET /api/stock/adx` - Returns ADX, +DI, -DI for a stock.

---
