package com.tradeoption.scheduler;

import com.tradeoption.domain.DashboardMetrics;
import com.tradeoption.domain.Strategy;
import com.tradeoption.service.DashboardService;
import com.tradeoption.service.MarketDataService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DashboardBroadcaster {

    private final DashboardService dashboardService;
    private final MarketDataService marketDataService;
    private final SimpMessagingTemplate messagingTemplate;

    // In a real app, this would be injected or fetched
    private final List<Strategy> activeStrategies = new ArrayList<>();

    public DashboardBroadcaster(DashboardService dashboardService, MarketDataService marketDataService,
            SimpMessagingTemplate messagingTemplate) {
        this.dashboardService = dashboardService;
        this.marketDataService = marketDataService;
        this.messagingTemplate = messagingTemplate;
    }

    public void addStrategy(Strategy strategy) {
        this.activeStrategies.add(strategy);
    }

    @Scheduled(fixedRate = 1000)
    public void broadcastDashboardMetrics() {
        if (activeStrategies.isEmpty())
            return;

        double spot = marketDataService.getLtp("NIFTY");
        // Mock environment params
        double vol = 0.20;
        double time = 0.1; // Years
        double rate = 0.05;

        // Broadcast metrics for the *primary* strategy for now, or aggregate?
        // Requirement says "Greeks & Metrics Panel" -> implies single active strategy
        // view or portfolio view.
        // Assuming single strategy focus for the panel.
        Strategy strategy = activeStrategies.get(0);

        DashboardMetrics metrics = dashboardService.calculateMetrics(strategy, spot, vol, time, rate);

        messagingTemplate.convertAndSend("/topic/dashboard", metrics);
    }
}
