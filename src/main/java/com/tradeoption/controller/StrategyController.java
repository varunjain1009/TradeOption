package com.tradeoption.controller;

import com.tradeoption.domain.*;
import com.tradeoption.repository.PositionRepository;
import com.tradeoption.scheduler.DashboardBroadcaster;
import com.tradeoption.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api")
public class StrategyController {

    // Dependencies for Legacy Functionality
    private final PositionRepository positionRepository;
    private final MarketDataService marketDataService;
    private final StrategySuggestionService strategySuggestionService;
    private final DashboardService dashboardService;
    private final DashboardBroadcaster dashboardBroadcaster;
    private final SystemConfigService systemConfigService;

    // Dependency for New Persistence Functionality
    private final StrategyService strategyService;

    public StrategyController(PositionRepository positionRepository,
            MarketDataService marketDataService,
            StrategySuggestionService strategySuggestionService,
            DashboardService dashboardService,
            DashboardBroadcaster dashboardBroadcaster,
            SystemConfigService systemConfigService,
            StrategyService strategyService) {
        this.positionRepository = positionRepository;
        this.marketDataService = marketDataService;
        this.strategySuggestionService = strategySuggestionService;
        this.dashboardService = dashboardService;
        this.dashboardBroadcaster = dashboardBroadcaster;
        this.systemConfigService = systemConfigService;
        this.strategyService = strategyService;
    }

    // ----------------------------------------------------------------
    // NEW ENDPOINTS: Strategy Persistence (/api/strategies)
    // ----------------------------------------------------------------

    @PostMapping("/strategies")
    public ResponseEntity<Strategy> saveStrategy(@RequestBody Strategy strategy, java.security.Principal principal) {
        if (strategy.getLegs() == null || strategy.getLegs().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Auto-Close Logic
        boolean allClosed = true;
        double realizedPnl = 0.0;
        for (OptionLeg leg : strategy.getLegs()) {
            if (leg.getExitPrice() == null) {
                allClosed = false;
                break;
            }
            // Calculate PnL for this leg
            // BUY: (Exit - Entry) * Q
            // SELL: (Entry - Exit) * Q (if Q is positive in DB? No, usually Q is signed or
            // Action determines sign)
            // Let's assume standard: PnL = (Exit - Entry) * Quantity
            // If BUY, Q > 0. If SELL, Q < 0?
            // Checking StrategyController.getCurrentPnl logic (lines 86+):
            // It uses (Exit - Entry) * Q.
            // But we need to verify if Q is signed.
            // In StrategyController.placeTrade (lines 149-160), Q is just passed.
            // OptionLeg has Action.

            // Standardizing PnL calc here to match expected behavior:
            double q = leg.getQuantity(); // Absolute usually?
            if (leg.getAction() == TradeAction.SELL) {
                q = -1 * Math.abs(q);
            } else {
                q = Math.abs(q);
            }

            realizedPnl += (leg.getExitPrice() - leg.getEntryPrice()) * q;
        }

        if (allClosed) {
            strategy.setStatus("CLOSED");
            strategy.setClosedTimestamp(System.currentTimeMillis());
            strategy.setRealizedPnl(realizedPnl);
        } else {
            strategy.setStatus("ACTIVE");
            strategy.setClosedTimestamp(null);
            strategy.setRealizedPnl(null);
        }

        Strategy saved = strategyService.saveStrategy(strategy, principal.getName());
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/strategies")
    public List<Strategy> getAllStrategies(java.security.Principal principal) {
        return strategyService.getAllStrategies(principal.getName());
    }

    @GetMapping("/strategies/{id}")
    public ResponseEntity<Strategy> getStrategy(@PathVariable String id, java.security.Principal principal) {
        return strategyService.getStrategy(id, principal.getName())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/strategies/{id}")
    public ResponseEntity<Void> deleteStrategy(@PathVariable String id, java.security.Principal principal) {
        strategyService.deleteStrategy(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/strategies/{id}/current-pnl")
    public ResponseEntity<java.util.Map<String, Object>> getCurrentPnl(
            @PathVariable String id,
            java.security.Principal principal) {

        return strategyService.getStrategy(id, principal.getName())
                .map(strategy -> {
                    double totalPnl = 0.0;
                    boolean isClosed = "CLOSED".equals(strategy.getStatus());

                    for (OptionLeg leg : strategy.getLegs()) {
                        double pnl = 0.0;
                        double entryPrice = leg.getEntryPrice();
                        int quantity = leg.getQuantity();

                        // Quantity is signed (+ for BUY, - for SELL)
                        // PNL = (Current/Exit - Entry) * Quantity
                        // If BUY (Q>0): (Current - Entry) * Q. (Gain if Current > Entry)
                        // If SELL (Q<0): (Current - Entry) * Q = (Entry - Current) * |Q|. (Gain if
                        // Entry > Current)

                        if (isClosed && leg.getExitPrice() != null) {
                            // Realized PnL
                            pnl = (leg.getExitPrice() - entryPrice) * quantity;
                        } else if (!isClosed) {
                            // Unrealized PnL
                            java.util.Optional<Double> currentPriceOpt;
                            if (leg.getAction() == TradeAction.BUY) {
                                currentPriceOpt = marketDataService.getBid(leg);
                            } else {
                                currentPriceOpt = marketDataService.getAsk(leg);
                            }

                            if (currentPriceOpt.isPresent()) {
                                pnl = (currentPriceOpt.get() - entryPrice) * quantity;
                            }
                        }

                        totalPnl += pnl;
                    }

                    java.util.Map<String, Object> result = new java.util.HashMap<>();
                    result.put("currentPnl", totalPnl);
                    result.put("isClosed", isClosed);
                    return ResponseEntity.ok(result);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ----------------------------------------------------------------
    // LEGACY ENDPOINTS: Trading & Analysis (/api/strategy)
    // ----------------------------------------------------------------

    @PostMapping("/strategy/trade")
    public ResponseEntity<String> placeTrade(@RequestBody TradeOrder order) {
        // 1. Create/Find Position
        String id = Position.generateId(
                order.getSymbol(),
                order.getExpiryDate(),
                order.getStrikePrice(),
                order.getOptionType());

        Position position = positionRepository.findById(id);
        if (position == null) {
            position = new Position(
                    order.getSymbol(),
                    order.getExpiryDate(),
                    order.getStrikePrice(),
                    order.getOptionType());
        }

        // 2. Add Entry
        PositionEntry entry = new PositionEntry(
                order.getPrice(),
                order.getQuantity(),
                order.getTradeAction());
        position.addEntry(entry);

        // 3. Update Status & Timestamp
        position.setUpdatedTimestamp(System.currentTimeMillis());
        if (position.getNetQuantity() == 0) {
            position.setStatus(PositionStatus.CLOSED);
        } else {
            position.setStatus(PositionStatus.OPEN);
        }

        // Calculate PNL
        PositionPnl pnl = PositionPnlCalculator.calculatePnl(position, 0.0);
        position.setRealizedPnl(pnl.getRealizedPnl());
        position.setUnrealizedPnl(pnl.getUnrealizedPnl());

        // 4. Save
        positionRepository.save(position);

        // 5. Trigger Broadcast
        dashboardBroadcaster.broadcastDashboardMetrics();

        return ResponseEntity.ok("Trade processed successfully");
    }

    @PostMapping("/strategy/position/{positionId}/exit")
    public ResponseEntity<String> exitPosition(
            @PathVariable String positionId,
            @RequestBody ExitRequest request) {

        Position position = positionRepository.findById(positionId);
        if (position == null) {
            return ResponseEntity.notFound().build();
        }

        TradeAction action = position.getNetQuantity() > 0
                ? TradeAction.SELL
                : TradeAction.BUY;

        PositionEntry entry = new PositionEntry(
                request.getPrice(),
                request.getQuantity(),
                action);
        entry.setLinkedEntryId(request.getLinkedEntryId());

        position.addEntry(entry);

        position.setUpdatedTimestamp(System.currentTimeMillis());
        int netQty = position.getNetQuantity();
        if (netQty == 0) {
            position.setStatus(PositionStatus.CLOSED);
        } else {
            position.setStatus(PositionStatus.PARTIALLY_CLOSED);
        }

        // Calculate PNL
        PositionPnl pnl = PositionPnlCalculator.calculatePnl(position, 0.0);
        position.setRealizedPnl(pnl.getRealizedPnl());
        position.setUnrealizedPnl(pnl.getUnrealizedPnl());

        positionRepository.save(position);

        dashboardBroadcaster.broadcastDashboardMetrics();

        return ResponseEntity.ok("Exit processed");
    }

    @PutMapping("/strategy/position/entry/{entryId}")
    public ResponseEntity<String> updateEntry(
            @PathVariable String entryId,
            @RequestBody EntryUpdateRequest request) {

        List<Position> all = positionRepository.findAll();
        for (Position p : all) {
            for (PositionEntry e : p.getEntries()) {
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

    @GetMapping("/strategy/position/{positionId}/history")
    public ResponseEntity<List<PnlHistoryPoint>> getPositionHistory(
            @PathVariable String positionId) {

        Position position = positionRepository.findById(positionId);
        if (position == null) {
            return ResponseEntity.notFound().build();
        }

        List<PnlHistoryPoint> history = new ArrayList<>();

        // Reconstruct history
        // 1. Create a temp position
        Position tempPos = new Position(
                position.getSymbol(), position.getExpiryDate(), position.getStrikePrice(), position.getOptionType());

        // 2. Sort entries by timestamp
        List<PositionEntry> sortedEntries = new ArrayList<>(position.getEntries());
        sortedEntries.sort(Comparator.comparingLong(PositionEntry::getTimestamp));

        // 3. Replay
        long startTime = sortedEntries.isEmpty() ? System.currentTimeMillis() : sortedEntries.get(0).getTimestamp();
        history.add(new PnlHistoryPoint(startTime - 1000, 0.0)); // 1 sec before start

        for (PositionEntry entry : sortedEntries) {
            tempPos.addEntry(entry);
            PositionPnl pnl = PositionPnlCalculator.calculatePnl(tempPos, 0.0);

            history.add(new PnlHistoryPoint(entry.getTimestamp(), pnl.getRealizedPnl()));
        }

        return ResponseEntity.ok(history);
    }

    @GetMapping("/strategy/positions")
    public ResponseEntity<List<Position>> getPositions() {
        return ResponseEntity.ok(positionRepository.findAll());
    }

    @PostMapping("/strategy/analyze")
    public ResponseEntity<DashboardMetrics> analyzeStrategy(@RequestBody Strategy strategy) {
        // 1. Fetch Market Data
        double spot = 0.0;
        if (strategy.getSymbol() != null) {
            java.util.Optional<Double> spotPriceOpt = marketDataService.getLtp(strategy.getSymbol());
            spot = spotPriceOpt.orElse(0.0);
        }

        // Fallback: If spot is 0.0 (data not available), use average strike
        if (spot == 0.0 && strategy.getLegs() != null && !strategy.getLegs().isEmpty()) {
            double totalStrike = strategy.getLegs().stream()
                    .mapToDouble(com.tradeoption.domain.OptionLeg::getStrikePrice)
                    .sum();
            spot = totalStrike / strategy.getLegs().size();
        }

        // 2. Fetch Config
        double rate = systemConfigService.getConfig().getRiskFreeRate();
        double vol = 0.20; // Default or fetch per symbol

        // 3. Time to expiry (approx 30 days if not set, or parse from leg)
        // Need parsing logic. For now, mock 0.1 year
        double time = 0.1;
        if (strategy.getLegs() != null && !strategy.getLegs().isEmpty()) {
            // Placeholder for real logic
        }

        DashboardMetrics metrics = dashboardService.calculateMetrics(strategy, spot, vol, time, rate);

        // 4. Populate Leg LTPs (All-or-Nothing Logic) - REMOVED for manual mode
        // if (strategy.getLegs() != null && !strategy.getLegs().isEmpty()) { ... }
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/strategy/suggest/straddle")
    public ResponseEntity<Strategy> suggestStraddle(
            @RequestParam(defaultValue = "NIFTY") String symbol) {
        return ResponseEntity.ok(strategySuggestionService.suggestStraddle(symbol));
    }

    @GetMapping("/strategy/suggest/strangle")
    public ResponseEntity<Strategy> suggestStrangle(
            @RequestParam(defaultValue = "NIFTY") String symbol) {
        return ResponseEntity.ok(strategySuggestionService.suggestStrangle(symbol));
    }
}
