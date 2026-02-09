# TradeOption - Desktop Analytics Platform

TradeOption is a sophisticated options analytics tool for traders, providing real-time Greeks, PNL visualization, and Probability of Profit (PoP) analysis for NIFTY/BANKNIFTY strategies.

## ✨ Features
- **Live Greeks & Metrics**: Real-time Delta, Theta, Gamma, Vega, and IV tracking.
- **Interactive Payoff Graphs**: Visualize T-0 and Expiry PNL curves using **Plotly.js**.
- **Probability of Profit (PoP)**: Statistical calculation of trade success probability.
- **Dynamic Admin Panel**: Manage system configuration, holidays, and special sessions via a dedicated UI.
- **Market Status Logic**: Auto-detection of market hours, weekends, and holidays with "Market Closed" banners.
- **Hot-Reload Config**: Adjust parameters instantly without server restarts.
- **Enhanced Security**: 
    - Rate Limiting (Bucket4j)
    - strict CSP & Security Headers
    - Admin-only API access.
- **Lightweight**: Pure Browser + Spring Boot architecture.

## 🚀 Quick Start

### Prerequisites
- **Java 17+** (JDK must be in PATH)

### Installation & Running
1. **Run the Application**:
   - **Mac/Linux**:
     ```bash
     chmod +x scripts/start.sh
     ./scripts/start.sh
     ```
   
   This script will:
   - Build the backend (if needed).
   - Start the Spring Boot server.
   - Automatically open your default web browser to `http://localhost:8082`.

2. **Manual Run (Alternative)**:
   ```bash
   mvn clean spring-boot:run
   # Open http://localhost:8082 in Chrome/Edge/Safari
   ```

## ⚙️ Configuration
The system uses a `config.json` file for dynamic configuration. 

### Admin Panel
You can manage the configuration via the **Admin UI** at `http://localhost:8082/admin.html`.
- **General**: Update refresh interval and risk-free rate.
- **Holidays**: Add/Remove holidays.
- **Special Sessions**: Configure special trading hours (e.g., Mahurat Trading).

**`config.json` Example**:
```json
{
  "riskFreeRate" : 0.05,
  "lotSizes" : {
    "NIFTY" : 50,
    "BANKNIFTY" : 15
  },
  "bidAskConstraints" : {
    "maxSpread" : 5.0
  },
  "symbolExpiries" : {
     "NIFTY" : ["2023-10-26", "2023-11-02"]
  },
  "refreshIntervalMs" : 1000,
  "holidays": ["2023-12-25"],
  "specialSessions": []
}
```

## 🛠 Development

### Backend (Spring Boot)
- **Run**: `mvn spring-boot:run`
- **Port**: 8082
- **Health Check**: `http://localhost:8082/actuator/health`
- **Admin API**: `http://localhost:8082/api/admin/config`

### Frontend (Web)
- **Location**: `src/main/resources/static/`
- **Tech**: HTML5, Vanilla JS, Plotly.js 2.27, CSS Variables
- **Admin UI**: `src/main/resources/static/admin.html`

## 🏗 Architecture
See [architecture.md](architecture.md) for detailed system design, data flow, and module descriptions.
