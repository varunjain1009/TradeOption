# TradeOption - Desktop Analytics Platform

TradeOption is a sophisticated options analytics tool for traders, providing real-time Greeks, PNL visualization, and Probability of Profit (PoP) analysis for NIFTY/BANKNIFTY strategies.

## ✨ Features
- **Live Greeks & Metrics**: Real-time Delta, Theta, Gamma, Vega, and IV tracking.
- **Interactive Payoff Graphs**: Visualize T-0 and Expiry PNL curves.
- **Probability of Profit (PoP)**: Statistical calculation of trade success probability.
- **Volatility Cones**: Historical volatility analysis.
- **Hot-Reload Config**: Adjust risk-free rates and lot sizes on the fly.
- **Self-Contained**: Runs as a single-folder desktop application (Electron + Spring Boot).

## 🚀 Quick Start

### Prerequisites
- **Java 17+** (JDK must be in PATH)
- **Node.js** (for Electron wrapper)

### Installation & Running
1. **Build the Application**:
   ```bash
   # Generates the distribution folder 'dist/'
   chmod +x scripts/package_app.sh
   ./scripts/package_app.sh
   ```

2. **Run**:
   - **Mac/Linux**:
     ```bash
     cd dist
     ./start.sh
     ```
   - **Windows**:
     ```cmd
     cd dist
     start.bat
     ```

   *Note: The first run will automatically install necessary Node.js dependencies.*

## ⚙️ Configuration
A `config.json` file is generated in the root directory. You can edit this file while the app is running to update parameters live.

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
  "refreshIntervalMs" : 1000
}
```

## 🛠 Development

### Backend (Spring Boot)
- **Run**: `mvn spring-boot:run`
- **Port**: 8080
- **Health Check**: `http://localhost:8080/actuator/health`

### Frontend (Electron)
- **Location**: `electron/`
- **Run**: `npm start` (requires Backend running)

## 🏗 Architecture
See [architecture.md](architecture.md) for detailed system design, data flow, and module descriptions.
