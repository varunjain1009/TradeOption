package com.tradeoption.controller;

import com.tradeoption.domain.Strategy;
import com.tradeoption.scheduler.DashboardBroadcaster;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/strategy")
public class StrategyController {

    private final com.tradeoption.scheduler.DashboardBroadcaster dashboardBroadcaster;
    private final com.tradeoption.repository.PositionRepository positionRepository;
    private final com.tradeoption.service.DashboardService dashboardService;
    private final com.tradeoption.service.MarketDataService marketDataService;
    private final com.tradeoption.service.SystemConfigService systemConfigService;

    public StrategyController(com.tradeoption.scheduler.DashboardBroadcaster dashboardBroadcaster,
            com.tradeoption.repository.PositionRepository positionRepository,
            com.tradeoption.service.DashboardService dashboardService,
            com.tradeoption.service.MarketDataService marketDataService,
            com.tradeoption.service.SystemConfigService systemConfigService) {
        this.dashboardBroadcaster = dashboardBroadcaster;
        this.positionRepository = positionRepository;
        this.dashboardService = dashboardService;
        this.marketDataService = marketDataService;
        this.systemConfigService = systemConfigService;
    }

    @PostMapping
    public ResponseEntity<String> updateStrategy(@RequestBody Strategy strategy) {
        // Legacy Support: Convert incoming Strategy legs to TradeOrders/Positions
        for (com.tradeoption.domain.OptionLeg leg : strategy.getLegs()) {
            com.tradeoption.domain.TradeOrder order = new com.tradeoption.domain.TradeOrder();
            order.setSymbol(strategy.getSymbol() != null ? strategy.getSymbol() : "NIFTY");
            // Logic pending OptionLeg update
        }
        return ResponseEntity.ok("Strategy updated (Legacy Mode - logic pending OptionLeg update)");
    }

    @PostMapping("/trade")
    public ResponseEntity<String> placeTrade(@RequestBody com.tradeoption.domain.TradeOrder order) {
        // 1. Find or Create Position
        String id = com.tradeoption.domain.Position.generateId(
                order.getSymbol(),
                order.getExpiryDate(),
                order.getStrikePrice(),
                order.getOptionType());

        com.tradeoption.domain.Position position = positionRepository.findById(id);
        if (position == null) {
            position = new com.tradeoption.domain.Position(
                    order.getSymbol(),
                    order.getExpiryDate(),
                    order.getStrikePrice(),
                    order.getOptionType());
        }

        // 2. Add Entry
        com.tradeoption.domain.PositionEntry entry = new com.tradeoption.domain.PositionEntry(
                order.getPrice(),
                order.getQuantity(),
                order.getTradeAction());
        position.addEntry(entry);

        // 3. Update Status & Timestamp
        position.setUpdatedTimestamp(System.currentTimeMillis());
        if (position.getNetQuantity() == 0) {
            position.setStatus(com.tradeoption.domain.PositionStatus.CLOSED);
        } else {
            position.setStatus(com.tradeoption.domain.PositionStatus.OPEN);
        }

        // Calculate PNL
        com.tradeoption.domain.PositionPnl pnl = com.tradeoption.service.PositionPnlCalculator.calculatePnl(position,
                0.0);
        position.setRealizedPnl(pnl.getRealizedPnl());
        position.setUnrealizedPnl(pnl.getUnrealizedPnl());

        // 4. Save
        positionRepository.save(position);

        // 5. Trigger Broadcast
        dashboardBroadcaster.broadcastDashboardMetrics();

        return ResponseEntity.ok("Trade processed successfully");
    }

    @org.springframework.web.bind.annotation.PostMapping("/position/{positionId}/exit")
    public ResponseEntity<String> exitPosition(
            @org.springframework.web.bind.annotation.PathVariable String positionId,
            @RequestBody com.tradeoption.domain.ExitRequest request) {

        com.tradeoption.domain.Position position = positionRepository.findById(positionId);
        if (position == null) {
            return ResponseEntity.notFound().build();
        }

        com.tradeoption.domain.TradeAction action = position.getNetQuantity() > 0
                ? com.tradeoption.domain.TradeAction.SELL
                : com.tradeoption.domain.TradeAction.BUY;

        com.tradeoption.domain.PositionEntry entry = new com.tradeoption.domain.PositionEntry(
                request.getPrice(),
                request.getQuantity(),
                action);
        entry.setLinkedEntryId(request.getLinkedEntryId());

        position.addEntry(entry);

        position.setUpdatedTimestamp(System.currentTimeMillis());
        int netQty = position.getNetQuantity();
        if (netQty == 0) {
            position.setStatus(com.tradeoption.domain.PositionStatus.CLOSED);
        } else {
            position.setStatus(com.tradeoption.domain.PositionStatus.PARTIALLY_CLOSED);
        }

        // Calculate PNL
        com.tradeoption.domain.PositionPnl pnl = com.tradeoption.service.PositionPnlCalculator.calculatePnl(position,
                0.0);
        position.setRealizedPnl(pnl.getRealizedPnl());
        position.setUnrealizedPnl(pnl.getUnrealizedPnl());

        positionRepository.save(position);

        dashboardBroadcaster.broadcastDashboardMetrics();

        return ResponseEntity.ok("Exit processed");
    }

    @org.springframework.web.bind.annotation.PutMapping("/position/entry/{entryId}")
    public ResponseEntity<String> updateEntry(
            @org.springframework.web.bind.annotation.PathVariable String entryId,
            @RequestBody com.tradeoption.domain.EntryUpdateRequest request) {

        java.util.List<com.tradeoption.domain.Position> all = positionRepository.findAll();
        for (com.tradeoption.domain.Position p : all) {
            for (com.tradeoption.domain.PositionEntry e : p.getEntries()) {
                if (e.getId().equals(entryId)) {
                    e.setPrice(request.getPrice());
                    e.setQuantity(request.getQuantity());
                    positionRepository.save(p); // Re-save full obj
                    dashboardBroadcaster.broadcastDashboardMetrics();
                    return ResponseEntity.ok("Entry updated");
                }
            }
        }
        return ResponseEntity.notFound().build();
    }

    @org.springframework.web.bind.annotation.GetMapping("/position/{positionId}/history")
    public ResponseEntity<java.util.List<com.tradeoption.domain.PnlHistoryPoint>> getPositionHistory(
            @org.springframework.web.bind.annotation.PathVariable String positionId) {

        com.tradeoption.domain.Position position = positionRepository.findById(positionId);
        if (position == null) {
            return ResponseEntity.notFound().build();
        }

        java.util.List<com.tradeoption.domain.PnlHistoryPoint> history = new java.util.ArrayList<>();

        // Reconstruct history
        // 1. Create a temp position
        com.tradeoption.domain.Position tempPos = new com.tradeoption.domain.Position(
                position.getSymbol(), position.getExpiryDate(), position.getStrikePrice(), position.getOptionType());

        // 2. Sort entries by timestamp
        java.util.List<com.tradeoption.domain.PositionEntry> sortedEntries = new java.util.ArrayList<>(
                position.getEntries());
        sortedEntries.sort(java.util.Comparator.comparingLong(com.tradeoption.domain.PositionEntry::getTimestamp));

        // 3. Replay
        // Initial point? 0 PNL at start?
        long startTime = sortedEntries.isEmpty() ? System.currentTimeMillis() : sortedEntries.get(0).getTimestamp();
        history.add(new com.tradeoption.domain.PnlHistoryPoint(startTime - 1000, 0.0)); // 1 sec before start

        for (com.tradeoption.domain.PositionEntry entry : sortedEntries) {
            tempPos.addEntry(entry);
            com.tradeoption.domain.PositionPnl pnl = com.tradeoption.service.PositionPnlCalculator.calculatePnl(tempPos,
                    0.0);

            history.add(new com.tradeoption.domain.PnlHistoryPoint(entry.getTimestamp(), pnl.getRealizedPnl()));
        }

        return ResponseEntity.ok(history);
    }

    @org.springframework.web.bind.annotation.GetMapping("/positions")
    public ResponseEntity<java.util.List<com.tradeoption.domain.Position>> getPositions() {
        return ResponseEntity.ok(positionRepository.findAll());
    }

    @org.springframework.web.bind.annotation.PostMapping("/analyze")
    public ResponseEntity<com.tradeoption.domain.DashboardMetrics> analyzeStrategy(@RequestBody Strategy strategy) {
        // 1. Fetch Market Data
        double spot = 0.0;
        if (strategy.getSymbol() != null) {
            spot = marketDataService.getLtp(strategy.getSymbol());
        }

        // 2. Fetch Config
        double rate = systemConfigService.getConfig().getRiskFreeRate();
        double vol = 0.20; // Default or fetch per symbol

        // 3. Time to expiry (approx 30 days if not set, or parse from leg)
        // Need parsing logic. For now, mock 0.1 year
        double time = 0.1;
        if (!strategy.getLegs().isEmpty()) {
            // Try to parse expiry from first leg
            // com.tradeoption.domain.OptionLeg leg = strategy.getLegs().get(0);
            // String expiry = leg.getExpiryDate();
            // TODO: Implement date parsing
        }

        com.tradeoption.domain.DashboardMetrics metrics = dashboardService.calculateMetrics(strategy, spot, vol, time,
                rate);
        return ResponseEntity.ok(metrics);
    }
}
