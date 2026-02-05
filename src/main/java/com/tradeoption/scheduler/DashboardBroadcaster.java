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

    private final com.tradeoption.repository.PositionRepository positionRepository;

    public DashboardBroadcaster(DashboardService dashboardService, MarketDataService marketDataService,
            SystemConfigService systemConfigService, SimpMessagingTemplate messagingTemplate,
            com.tradeoption.repository.PositionRepository positionRepository) {
        this.dashboardService = dashboardService;
        this.marketDataService = marketDataService;
        this.systemConfigService = systemConfigService;
        this.messagingTemplate = messagingTemplate;
        this.positionRepository = positionRepository;
    }

    @Scheduled(fixedRateString = "#{@systemConfigServiceImpl.getConfig().getRefreshIntervalMs()}")
    public void broadcastDashboardMetrics() {
        // Source of Truth: RocksDB
        List<com.tradeoption.domain.Position> positions = positionRepository.findAll();

        if (positions.isEmpty()) {
            return;
        }

        // Aggregate Positions into a Strategy view for calculation
        // Assuming single-symbol portfolio focus for dashboard currently
        // If multiple symbols exist, we might need multiple broadcasts or a portfolio
        // view.
        // For simplicity, let's take the symbol of the first position and filter?
        // Or just aggregate all. Since pricing needs 'spot' of symbol, we better
        // grouping by symbol.

        // Let's implement simpler logic: Group by Symbol
        java.util.Map<String, List<com.tradeoption.domain.Position>> positionsBySymbol = new java.util.HashMap<>();
        for (com.tradeoption.domain.Position p : positions) {
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
                    p.getExpiryDate());
            // leg.setExpiryDate(p.getExpiryDate()); // Already set via constructor now
            strategy.addLeg(leg);
        }

        double spot = marketDataService.getLtp(activeSymbol);
        double vol = 0.20; // Mock
        double time = 0.1; // Mock time to expiry (should be calculated from position expiry ideally)
        double rate = systemConfigService.getConfig().getRiskFreeRate();

        DashboardMetrics metrics = dashboardService.calculateMetrics(strategy, spot, vol, time, rate);

        if (metrics != null) {
            messagingTemplate.convertAndSend("/topic/dashboard", metrics);
        }
    }
}
