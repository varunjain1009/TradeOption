package com.tradeoption.service.impl;

import com.tradeoption.domain.LegType;
import com.tradeoption.domain.OptionLeg;
import com.tradeoption.domain.Strategy;
import com.tradeoption.domain.SystemConfig;
import com.tradeoption.domain.TradeAction;
import com.tradeoption.service.MarketDataService;
import com.tradeoption.service.StrategySuggestionService;
import com.tradeoption.service.SystemConfigService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StrategySuggestionServiceImpl implements StrategySuggestionService {

    private final MarketDataService marketDataService;
    private final SystemConfigService systemConfigService;

    public StrategySuggestionServiceImpl(MarketDataService marketDataService, SystemConfigService systemConfigService) {
        this.marketDataService = marketDataService;
        this.systemConfigService = systemConfigService;
    }

    @Override
    public Strategy suggestStraddle(String symbol) {
        // 1. Get Spot Price
        double spot = marketDataService.getLtp(symbol);
        if (spot <= 0) {
            // Fallback default if market data is missing/mock
            spot = 22000.0;
        }

        // 2. Find ATM Strike (Round to nearest 50 for NIFTY, 100 for BN)
        // Simple logic for now: Nearest 50
        double strikeStep = 50.0;
        // Adjust step based on symbol if needed
        if ("BANKNIFTY".equalsIgnoreCase(symbol)) {
            strikeStep = 100.0;
        }

        double atmStrike = Math.round(spot / strikeStep) * strikeStep;

        // 3. Find Expiry
        SystemConfig config = systemConfigService.getConfig();
        String expiry = null;
        if (config.getSymbolExpiries() != null && config.getSymbolExpiries().containsKey(symbol)) {
            List<String> dates = config.getSymbolExpiries().get(symbol);
            if (dates != null && !dates.isEmpty()) {
                expiry = dates.get(0); // Pick nearest
            }
        }
        if (expiry == null)
            expiry = "2024-03-28"; // Fallback

        // 4. Construct Iron Fly (ATM Straddle + Wings)
        // Width: 500 points for NIFTY, 1000 for BN?
        double width = 500.0;
        if ("BANKNIFTY".equalsIgnoreCase(symbol)) {
            width = 1000.0;
        }

        Strategy strategy = new Strategy();
        strategy.setSymbol(symbol);
        strategy.setId("Suggested-Straddle-" + System.currentTimeMillis());

        // Leg 1: Sell ATM CE
        strategy.addLeg(createLeg(atmStrike, LegType.CE, TradeAction.SELL, expiry));
        // Leg 2: Sell ATM PE
        strategy.addLeg(createLeg(atmStrike, LegType.PE, TradeAction.SELL, expiry));
        // Leg 3: Buy (ATM + Width) CE (Protection)
        strategy.addLeg(createLeg(atmStrike + width, LegType.CE, TradeAction.BUY, expiry));
        // Leg 4: Buy (ATM - Width) PE (Protection)
        strategy.addLeg(createLeg(atmStrike - width, LegType.PE, TradeAction.BUY, expiry));

        return strategy;
    }

    private OptionLeg createLeg(double strike, LegType type, TradeAction action, String expiry) {
        // Price is 0.0 initially, UI or Analysis will fetch it
        // Quantity default 50
        OptionLeg leg = new OptionLeg(strike, type, action, 0.0, 50, expiry);
        return leg;
    }
}
