# TradeOption System Architecture

## 1. High-Level Overview
TradeOption is a desktop-based Options Analytics Platform designed for real-time risk management and strategy analysis. It follows a hybrid **Electron + Spring Boot** architecture, where:
- **Spring Boot (Backend)**: Handles all business logic, complex calculations (Black-Scholes, Greeks), market data processing, and persistence.
- **Electron (Frontend Wrapper)**: Provides a native desktop experience, manages the Java backend lifecycle, and renders the dashboard.

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
    - Persisting strategy configurations.
- **Why RocksDB?**: Low latency, no external database server required, perfect for a self-contained desktop app.

### 2.5 Configuration Management
- **SystemConfigService**: Loads `config.json` from the application root.
- **Hot-Reload**: Polls for changes every 5 seconds, allowing users to tweak parameters (Risk-Free Rate, Lot Sizes) without restarting.

## 3. Technology Stack
- **Backend**: Java 17, Spring Boot 3.2
- **Frontend**: HTML5, Vanilla JS, Chart.js
- **Desktop Wrapper**: Electron, Node.js
- **Database**: RocksDB (Java Driver)
- **Messaging**: Spring WebSocket (STOMP)

## 4. Deployment Structure
The application processes are managed as follows:
1. User runs `start.sh` / `start.bat`.
2. Electron starts (`main.js`).
3. Electron spawns `java -jar TradeOption.jar` as a child process.
4. Electron window waits for Spring Boot health check (port 8080).
5. On close, Electron ensures the Java process is terminated.
