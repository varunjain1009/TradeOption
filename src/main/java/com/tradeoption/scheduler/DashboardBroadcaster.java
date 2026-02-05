package com.tradeoption.scheduler;

import com.tradeoption.domain.DashboardMetrics;
import com.tradeoption.domain.Strategy;
import com.tradeoption.service.DashboardService;
import com.tradeoption.service.MarketDataService;
import com.tradeoption.service.SystemConfigService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DashboardBroadcaster {

    private final DashboardService dashboardService;
    private final MarketDataService marketDataService;
    private final SystemConfigService systemConfigService;
    private final SimpMessagingTemplate messagingTemplate;

    // In a real app, this would be injected or fetched
    private final List<Strategy> activeStrategies = new ArrayList<>();

    public DashboardBroadcaster(DashboardService dashboardService, MarketDataService marketDataService,
            SystemConfigService systemConfigService, SimpMessagingTemplate messagingTemplate) {
        this.dashboardService = dashboardService;
        this.marketDataService = marketDataService;
        this.systemConfigService = systemConfigService;
        this.messagingTemplate = messagingTemplate;
    }

    public void addStrategy(Strategy strategy) {
        this.activeStrategies.add(strategy);
    }

    public void setStrategy(Strategy strategy) {
        this.activeStrategies.clear();
        this.activeStrategies.add(strategy);
        // Trigger immediate broadcast
        broadcastDashboardMetrics();
    }

    @Scheduled(fixedRateString = "#{@systemConfigServiceImpl.getConfig().getRefreshIntervalMs()}")
    public void broadcastDashboardMetrics() {
        if (activeStrategies.isEmpty())
            return;

        // Note: Dynamic fixedRate via SpEL might only read once at startup or refresh
        // bean.
        // For true dynamic resizing of schedule, we need ThreadPoolTaskScheduler or
        // simply
        // obey the config inside validity checks, or just accept that refresh rate
        // requires restart
        // while logic params (rate/vol) are live.
        // Requirement 8.2 says "Config changes reflected live".
        // Let's stick to reading params live.
        // Changing schedule dynamically is complex in Spring @Scheduled so we will keep
        // 1000ms fixed
        // or accept SpEL limitation (read at bean creation).
        // Let's keep 1000ms fixed in annotation for simplicity unless requested.
        // actually let's use fixedRate = 1000 and ignore config for rate for now to
        // ensure stability
        // or just read config values for calculation.

        double spot = marketDataService.getLtp("NIFTY");
        // Mock environment params
        double vol = 0.20;
        double time = 0.1; // Years

        // Read live config
        double rate = systemConfigService.getConfig().getRiskFreeRate();

        Strategy strategy = activeStrategies.get(0);

        DashboardMetrics metrics = dashboardService.calculateMetrics(strategy, spot, vol, time, rate);

        if (metrics != null) {
            messagingTemplate.convertAndSend("/topic/dashboard", metrics);
        }
    }
}
