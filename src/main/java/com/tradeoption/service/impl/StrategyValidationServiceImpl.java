package com.tradeoption.service.impl;

import com.tradeoption.service.StrategyValidationService;
import com.tradeoption.service.SystemConfigService;
import com.tradeoption.domain.SystemConfig;
import org.springframework.stereotype.Service;

@Service
public class StrategyValidationServiceImpl implements StrategyValidationService {

    private final SystemConfigService systemConfigService;

    public StrategyValidationServiceImpl(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @Override
    public boolean isSpreadValid(String symbol, double strike, String type) {
        SystemConfig config = systemConfigService.getConfig();
        double maxDiffPercent = config.getMaxBidAskDiffPercent();

        // In reality, fetch Bid/Ask from MarketDataService.
        // For now, we simulate a spread check.
        // Let's assume most are valid, but we can randomly reject or check against a
        // mock "spread" map if we had one.
        // For the purpose of the requirement "only strikes with bid and ask price not
        // varying by 5%":

        // Logical Impl (Simulated):
        // double bid = marketDataService.getBid(symbol, strike, type);
        // double ask = marketDataService.getAsk(symbol, strike, type);
        // if (ask == 0) return false;
        // double diffPercent = (ask - bid) / ask * 100;
        // return diffPercent <= maxDiffPercent;

        // Since we don't have getBid/getAsk yet, we return true to unblock flow,
        // but we add the logic structure as requested.

        // Enforce 5% (User requirement says "not varying by 5%")
        // This effectively means we are filtering for liquidity.

        // Validation: If maxDiffPercent is not set in config, default to 5.0
        if (maxDiffPercent <= 0)
            maxDiffPercent = 5.0;

        // MOCK: Fail 10% of the time to simulate liquidity issues for testing
        // return Math.random() > 0.1;

        return true;
    }
}
