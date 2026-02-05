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

    public StrategyController(com.tradeoption.scheduler.DashboardBroadcaster dashboardBroadcaster,
            com.tradeoption.repository.PositionRepository positionRepository) {
        this.dashboardBroadcaster = dashboardBroadcaster;
        this.positionRepository = positionRepository;
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
}
