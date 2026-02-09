package com.tradeoption.scheduler;

import com.tradeoption.domain.DashboardMetrics;
import com.tradeoption.domain.Strategy;
import com.tradeoption.service.DashboardService;
import com.tradeoption.service.MarketDataService;
import com.tradeoption.service.MarketStatusService;
import com.tradeoption.service.SystemConfigService;
import jakarta.annotation.PostConstruct;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

@Component
public class DashboardBroadcaster {

    private final DashboardService dashboardService;
    private final MarketDataService marketDataService;
    private final SystemConfigService systemConfigService;
    private final MarketStatusService marketStatusService;
    private final SimpMessagingTemplate messagingTemplate;
    private final com.tradeoption.repository.PositionRepository positionRepository;
    private final com.tradeoption.repository.PositionHistoryRepository positionHistoryRepository;

    // Dynamic Scheduling
    private final TaskScheduler taskScheduler;
    private ScheduledFuture<?> scheduledTask;

    public DashboardBroadcaster(DashboardService dashboardService, MarketDataService marketDataService,
            SystemConfigService systemConfigService, MarketStatusService marketStatusService,
            SimpMessagingTemplate messagingTemplate,
            com.tradeoption.repository.PositionRepository positionRepository,
            com.tradeoption.repository.PositionHistoryRepository positionHistoryRepository) {
        this.dashboardService = dashboardService;
        this.marketDataService = marketDataService;
        this.systemConfigService = systemConfigService;
        this.marketStatusService = marketStatusService;
        this.messagingTemplate = messagingTemplate;
        this.positionRepository = positionRepository;
        this.positionHistoryRepository = positionHistoryRepository;

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("DashboardScheduler-");
        scheduler.initialize();
        this.taskScheduler = scheduler;
    }

    @PostConstruct
    public void init() {
        // Start the loop
        scheduleNextRun(1000);

        // Listen for config changes to adjust interval immediately
        systemConfigService.addListener(config -> {
            // If the interval changed drastically, we might want to cancel and restart
            // But for simplicity, the next run will pick up the new interval.
            // If the user wants "immediate" effect and the current interval is long (e.g. 1
            // hour),
            // we should cancel and restart.
            if (scheduledTask != null && !scheduledTask.isDone()) {
                scheduledTask.cancel(false);
            }
            scheduleNextRun(100); // Restart almost immediately
        });
    }

    private void scheduleNextRun(long delayMs) {
        scheduledTask = taskScheduler.schedule(this::broadcastDashboardMetrics,
                new java.util.Date(System.currentTimeMillis() + delayMs));
    }

    public void broadcastDashboardMetrics() {
        long nextInterval = systemConfigService.getConfig().getRefreshIntervalMs();

        try {
            // 1. Check Market Status
            if (!marketStatusService.isMarketOpen()) {
                // Market closed, maybe check less frequently? Or just respect interval.
                // User said "refresh should not happen if ... market is closed"
                // But we should still keep the loop alive.
                // We can broadcast a "Market Closed" status if we want, or just verify no
                // updates.
                // For now, skip logic but keep loop.
                return;
            }

            // 2. Source of Truth: RocksDB
            List<com.tradeoption.domain.Position> positions = positionRepository.findAll();

            // 3. Check Active Positions
            if (positions.isEmpty()) {
                // "refresh should not happen if there is no account which has active position"
                return;
            }

            // ... Logic ...
            // Reuse existing logic, just extract it
            processAndBroadcast(positions);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Schedule next run
            if (nextInterval < 1000)
                nextInterval = 1000; // Safety floor
            scheduleNextRun(nextInterval);
        }
    }

    private void processAndBroadcast(List<com.tradeoption.domain.Position> positions) {
        // Filter for active positions only
        List<com.tradeoption.domain.Position> activePositions = new ArrayList<>();
        for (com.tradeoption.domain.Position p : positions) {
            if (p.getNetQuantity() != 0) {
                activePositions.add(p);
            }
        }

        if (activePositions.isEmpty()) {
            return;
        }

        // Group by Symbol
        java.util.Map<String, List<com.tradeoption.domain.Position>> positionsBySymbol = new java.util.HashMap<>();
        for (com.tradeoption.domain.Position p : activePositions) {
            positionsBySymbol.computeIfAbsent(p.getSymbol(), k -> new ArrayList<>()).add(p);
        }

        // Broadcast for each symbol (or just the first one active for now as UI only
        // shows one dashboard)
        // Let's pick the first available symbol for the main dashboard view
        String activeSymbol = positionsBySymbol.keySet().iterator().next();
        List<com.tradeoption.domain.Position> symbolPositions = positionsBySymbol.get(activeSymbol);

        Strategy strategy = new Strategy();
        strategy.setId("Live-Portfolio-" + activeSymbol);
        strategy.setSymbol(activeSymbol);

        for (com.tradeoption.domain.Position p : symbolPositions) {
            if (p.getNetQuantity() == 0)
                continue; // Skip closed positions

            // Convert Position to OptionLeg for calculation
            // Note: OptionLeg needs positive quantity and Action determines sign.
            // Position.netQuantity could be negative (Short).

            int netQty = p.getNetQuantity();
            com.tradeoption.domain.TradeAction action = netQty > 0
                    ? com.tradeoption.domain.TradeAction.BUY
                    : com.tradeoption.domain.TradeAction.SELL;

            com.tradeoption.domain.OptionLeg leg = new com.tradeoption.domain.OptionLeg(
                    p.getStrikePrice(),
                    p.getOptionType(),
                    action,
                    p.getAveragePrice(),
                    Math.abs(netQty),
                    p.getExpiryDate(),
                    p.getSymbol());
            // leg.setExpiryDate(p.getExpiryDate()); // Already set via constructor now
            strategy.addLeg(leg);
        }

        java.util.Optional<Double> spotOpt = marketDataService.getLtp(activeSymbol);
        if (spotOpt.isEmpty()) {
            return; // Skip update if data fetch failed
        }
        double spot = spotOpt.get(); // Safe get
        double vol = 0.20; // Mock
        double time = 0.1; // Mock time to expiry (should be calculated from position expiry ideally)
        double rate = systemConfigService.getConfig().getRiskFreeRate();

        DashboardMetrics metrics = dashboardService.calculateMetrics(strategy, spot, vol, time, rate);

        if (metrics != null) {
            messagingTemplate.convertAndSend("/topic/dashboard", metrics);

            // Persist History for each position in this strategy
            // Using logic: metrics apply to the strategy (portfolio of positions)
            // But we want history per position.
            // Simplified approach: Attribute the strategy PNL/Greeks proportionally or just
            // store strategy level?
            // User asked "for closed trades, keep this data... see how PNL graph has
            // changed since positions taken"
            // Accessing "per position" history might mean we need granular metrics.
            // DashboardService.calculateMetrics returns aggregate.
            // For now, let's store the AGGREGATE metrics against the Symbol/Strategy ID to
            // allow "Portfolio" history.
            // Or better, if we want per-position, we need to calculate per position.
            // For Story 9.2, let's store the metric for the "Strategy" (Symbol).

            com.tradeoption.domain.PositionMetric metric = new com.tradeoption.domain.PositionMetric(
                    System.currentTimeMillis(),
                    0.0, // DashboardMetrics essentially tracks MTM (Unrealized) for the active view.
                         // Realized is separate.
                    metrics.getCurrentPnl(),
                    spot,
                    metrics.getGreeks());
            // We use activeSymbol as the key for now since we grouped by it.
            // Ideally we iterate positions and save for each, but we only have aggregate.
            // Let's save for the symbol key.
            positionHistoryRepository.addMetric("SYMBOL:" + activeSymbol, metric);
        }
    }
}
