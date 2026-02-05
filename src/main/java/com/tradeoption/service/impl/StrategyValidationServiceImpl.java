package com.tradeoption.service.impl;

import com.tradeoption.service.MarketDataService;
import com.tradeoption.service.StrategyValidationService;
import com.tradeoption.service.SystemConfigService;
import com.tradeoption.domain.SystemConfig;
import org.springframework.stereotype.Service;

@Service
public class StrategyValidationServiceImpl implements StrategyValidationService {

    private final MarketDataService marketDataService;
    private final SystemConfigService systemConfigService;

    public StrategyValidationServiceImpl(MarketDataService marketDataService, SystemConfigService systemConfigService) {
        this.marketDataService = marketDataService;
        this.systemConfigService = systemConfigService;
    }

    @Override
    public boolean isSpreadValid(String symbol, double strike, String type) {
        SystemConfig config = systemConfigService.getConfig();
        double maxDiffPercent = config.getMaxBidAskDiffPercent();

        // In a real system, we'd get the specific Option Symbol (e.g.,
        // NIFTY24MAR22000CE)
        // and fetch its Quote (Bid/Ask).
        // Since MarketDataService currently only gets Spot (LTP), we will SIMULATE
        // validation
        // or check if extended method exists.

        // TODO: Extend MarketDataService to get Option Quotes.
        // For now, allow all OR simulate random rejection for testing if needed.
        // Let's assume valid unless we can prove otherwise.

        // Simulation logic for demonstration if needed:
        // double randomSpread = Math.random() * 10.0;
        // if (randomSpread > maxDiffPercent) return false;

        return true;
    }
}
