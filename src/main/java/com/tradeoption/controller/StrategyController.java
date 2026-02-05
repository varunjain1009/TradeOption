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
        // This ensures the UI works without major changes yet
        for (com.tradeoption.domain.OptionLeg leg : strategy.getLegs()) {
            com.tradeoption.domain.TradeOrder order = new com.tradeoption.domain.TradeOrder();
            order.setSymbol(strategy.getSymbol() != null ? strategy.getSymbol() : "NIFTY"); // Default fallback
            // Assuming leg expiry is passed or defaulting to nearest (logic to be improved
            // later)
            // For now, let's assume the Strategy object *should* have expiry or we default
            // Actually, OptionLeg doesn't have expiry, Strategy doesn't have expiry field
            // in older model...
            // Wait, UI sends: symbol, expiryDate inside legs? No, check app.js
            // app.js sends: { symbol, legs: [ {expiryDate: ...} ] }

            // We need to extract expiry from the incoming map if possible or add it to
            // Leg/Strategy model
            // For this quick port, let's assume we update Position from the leg details.

            // Since app.js sends legs with expiryDate, we need to ensure OptionLeg has it
            // or we parse from map
            // BUT OptionLeg is stiff Java class.
            // Better approach: Since `submitStrategy` sends JSON, we can map it to a custom
            // DTO or just process here.
        }

        // Actually, let's just make a new endpoint for TradeOrder and update app.js to
        // call it?
        // OR better: process the incoming Strategy completely into Positions.

        // Let's implement the `processStrategy` logic properly.
        // NOTE: The Strategy class itself might need fields like `expiry` if they are
        // global to strategy.
        // app.js sends: symbol, legs [ {expiryDate...} ]
        // We will loop through legs.

        // However, `updateStrategy` is what app.js calls.
        // Strategy.java doesn't have `expiryDate` but we can add it or read from legs
        // (if OptionLeg has it).
        // OptionLeg.java doesn't have expiryDate.

        // Plan: app.js sends `expiryDate` in the leg object. We should add it to
        // OptionLeg class or handle dynamic map.
        // Since OptionLeg is compiled class, let's assume we can map it.

        // To simplify for Story 9.2:
        // We will persist the *Strategy* legs as *Positions*.
        // But since OptionLeg ignores unknown fields (expiryDate), we lose it during
        // deserialization!
        // We need to update OptionLeg to include expiryDate first.

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

        // 3. Save
        positionRepository.save(position);

        // 4. Trigger Broadcast
        dashboardBroadcaster.broadcastDashboardMetrics();

        return ResponseEntity.ok("Trade processed successfully");
    }
}
