package com.tradeoption.scheduler;

import com.tradeoption.domain.OptionLeg;
import com.tradeoption.domain.Strategy;
import com.tradeoption.service.MarketDataService;
import com.tradeoption.service.PnlCalculatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@Component
public class LivePnlScheduler {

    private static final Logger logger = LoggerFactory.getLogger(LivePnlScheduler.class);

    private final MarketDataService marketDataService;
    private final PnlCalculatorService pnlCalculatorService;

    // In a real app, this would be a Repository or Session scoped bean holding
    // active strategies
    private final List<Strategy> activeStrategies = new ArrayList<>();

    public LivePnlScheduler(MarketDataService marketDataService, PnlCalculatorService pnlCalculatorService) {
        this.marketDataService = marketDataService;
        this.pnlCalculatorService = pnlCalculatorService;
    }

    // Helper to add strategies for testing
    public void addStrategy(Strategy strategy) {
        this.activeStrategies.add(strategy);
    }

    @Scheduled(fixedRate = 1000)
    public void calculateAndBroadcastPnl() {
        if (activeStrategies.isEmpty()) {
            return;
        }

        logger.info("Calculating Live PNL for {} strategies...", activeStrategies.size());

        for (Strategy strategy : activeStrategies) {
            Map<OptionLeg, Double> ltpMap = new HashMap<>();

            // 1. Fetch Latest Prices for all legs in the strategy
            for (OptionLeg leg : strategy.getLegs()) {
                java.util.Optional<Double> ltpOpt = marketDataService.getLtp(leg.getSymbol());
                if (ltpOpt.isEmpty())
                    continue;
                double ltp = ltpOpt.get();
                ltpMap.put(leg, ltp);
            }

            // 2. Calculate Live PNL
            double livePnl = pnlCalculatorService.calculateLiveStrategyPnl(strategy, ltpMap);

            // 3. Log/Broadcast
            // For now, we just log it as per requirements for T-0 engine step,
            // Broadcasting is in Story 5.1
            logger.info("Strategy PNL: {}", livePnl);
        }
    }
}
