package com.tradeoption.scheduler;

import com.tradeoption.domain.Strategy;
import com.tradeoption.service.MarketDataService;
import com.tradeoption.service.PnlCalculatorService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PnlBroadcaster {

    private final PnlCalculatorService pnlService;
    private final MarketDataService marketDataService;
    private final SimpMessagingTemplate messagingTemplate;

    // In a real app, this would be fetched from a repository of active strategies
    private final List<Strategy> activeStrategies = new ArrayList<>();

    public PnlBroadcaster(PnlCalculatorService pnlService, MarketDataService marketDataService,
            SimpMessagingTemplate messagingTemplate) {
        this.pnlService = pnlService;
        this.marketDataService = marketDataService;
        this.messagingTemplate = messagingTemplate;
    }

    public void addStrategy(Strategy strategy) {
        this.activeStrategies.add(strategy);
    }

    @Scheduled(fixedRate = 1000)
    public void broadcastPnl() {
        double spot = marketDataService.getLtp("NIFTY");
        double totalPnl = 0.0;

        for (Strategy strategy : activeStrategies) {
            totalPnl += pnlService.calculateStrategyPnl(strategy, spot);
        }

        // Broadcast total PNL (simple double or object)
        messagingTemplate.convertAndSend("/topic/pnl", totalPnl);
    }
}
