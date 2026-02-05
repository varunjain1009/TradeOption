package com.tradeoption.scheduler;

import com.tradeoption.domain.Greeks;
import com.tradeoption.domain.Strategy;
import com.tradeoption.service.GreeksService;
import com.tradeoption.service.MarketDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GreeksBroadcaster {

    private static final Logger logger = LoggerFactory.getLogger(GreeksBroadcaster.class);

    private final GreeksService greeksService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MarketDataService marketDataService;

    // Holding active strategies in memory for now, similar to LivePnlScheduler
    private final List<Strategy> activeStrategies = new ArrayList<>();

    public GreeksBroadcaster(GreeksService greeksService, SimpMessagingTemplate messagingTemplate,
            MarketDataService marketDataService) {
        this.greeksService = greeksService;
        this.messagingTemplate = messagingTemplate;
        this.marketDataService = marketDataService;
    }

    public void addStrategy(Strategy strategy) {
        this.activeStrategies.add(strategy);
    }

    @Scheduled(fixedRate = 1000)
    public void broadcastGreeks() {
        if (activeStrategies.isEmpty()) {
            return;
        }

        // logger.info("Broadcasting Greeks for {} strategies...",
        // activeStrategies.size());

        for (Strategy strategy : activeStrategies) {
            // For now, assume spot price is fetched via MarketDataService or passed in.
            // Simplified: Fetch spot for the first leg or handle per strategy.
            // And use dummy values for rate/volatility if not available.
            // In real app, Volatility might come from IV Service (Story 2.1) and Rate from
            // Config.

            if (strategy.getLegs().isEmpty())
                continue;

            // Assume spot price from first leg's symbol (logic to be refined in full
            // system)
            double spot = marketDataService.getLtp(strategy.getLegs().get(0));

            double volatility = 0.20; // Default 20%
            double rate = 0.05; // Default 5%
            double timeToExpiry = 1.0 / 365.0; // 1 day left, simplified

            Greeks greeks = greeksService.calculateStrategyGreeks(strategy, spot, volatility, rate, timeToExpiry);

            if (greeks != null) {
                messagingTemplate.convertAndSend("/topic/greeks", greeks);
            }
        }
    }
}
