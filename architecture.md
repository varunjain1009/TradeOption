# TradeOption System Architecture

## 1. High-Level Overview
TradeOption is a web-based Options Analytics Platform designed for real-time risk management and strategy analysis. It follows a **Single-Process Spring Boot** architecture, where:
- **Spring Boot (Backend & Host)**: Handles all business logic, complex calculations (Black-Scholes, Greeks), market data processing, persistence, and serves the static frontend assets.
- **Browser (Client)**: Renders the dashboard using HTML5/JS and connects to the backend via WebSockets.

## 2. Core Modules

### 2.1 Pricing & Risk Engine (Domain Core)
The heart of the system, responsible for theoretical pricing and risk metrics.
- **Black-Scholes Service**: Implementation of the Black-Scholes-Merton model to calculate theoretical option prices (`C` and `P`).
- **Greeks Service**: Calculates sensitivities:
    - **Delta ($\Delta$)**: Sensitivity to underlying price.
    - **Gamma ($\Gamma$)**: Rate of change of Delta.
    - **Theta ($\Theta$)**: Time decay.
    - **Vega ($\nu$)**: Sensitivity to volatility.
- **Probability Engine**: Uses statistical distributions (Z-scores) to calculate Probability of Profit (PoP).

### 2.2 Analytics Services
- **PNL Engine**: Tracks "T-0" (Theoretical PNL today) and "Expiry PNL" (at expiration).
- **Payoff Graph Generator**: Produces data points for rendering interactive risk profiles (PNL vs Spot Price).
- **Volatility Cone**: Generates historical volatility cones to assess whether current IV is high/low relative to history.

### 2.3 Real-Time Data Flow
1. **Market Data Source**: `MarketDataService` fetches/simulates NIFTY indices and option chain data.
2. **Processing**: Schedulers (e.g., `DashboardBroadcaster`) trigger calculations every 1000ms.
3. **Broadcasting**:
    - **WebSockets (STOMP)**: Data is pushed to topics like `/topic/dashboard`, `/topic/greeks`, `/topic/spot`.
    - **Frontend**: Subscribes to these topics via `SockJS` + `stomp.js` for zero-refresh updates.

### 2.4 Persistence Layer
- **RocksDB**: Embedded Key-Value store used for high-performance, local data storage.
- **Usage**:
    - Storing historical snapshots (`HistoricalAnalyticsStore`).
    - Persisting strategy configurations and Positions.
- **Why RocksDB?**: Low latency, no external database server required, perfect for a self-contained local app.

### 2.5 Market Status & Scheduling
- **MarketStatusService**: encapsulated logic to determine if the market is open. Checks:
    - Standard Hours (09:00 - 23:45)
    - Weekends (Closed Sun/Mon)
    - Configurable Holidays
    - Configurable Special Sessions
- **Dynamic Scheduler**: `DashboardBroadcaster` uses `TaskScheduler` to adjust its refresh interval dynamically based on config and pauses updates when the market is closed.

### 2.6 Configuration Management (Admin)
- **SystemConfigService**: Loads/Saves `config.json` and notifies listeners of changes.
- **AdminConfigController**: secure REST API for updating configuration.
- **Admin UI**: Dedicated HTML/JS interface for managing system settings.

### 2.7 Security Layer
- **Rate Limiting**: `Bucket4j` implementation (`RateLimitFilter`) to prevent abuse (20 req/min per IP).
- **Security Headers**: Strict CSP, X-Frame-Options, XSS-Protection enabled in `SecurityConfig`.
- **Access Control**: `/api/admin/**` endpoints restricted to ADMIN role.

## 3. Technology Stack
- **Backend**: Java 17, Spring Boot 3.2
- **Frontend**: HTML5, Vanilla JS, **Plotly.js** (Charting)
- **Database**: RocksDB (Java Driver)
- **Messaging**: Spring WebSocket (STOMP)

## 4. Deployment Structure
The application runs as a standard Java process:
1. User runs `scripts/start.sh`.
2. Script builds/finds the JAR.
3. Script spawns `java -jar TradeOption.jar`.
4. Script launches the default system browser to `http://localhost:8082`.
5. Frontend loads and establishes WebSocket connection.
