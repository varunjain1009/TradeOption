package com.tradeoption.service.impl;

import com.tradeoption.domain.LegType;
import com.tradeoption.domain.OptionLeg;
import com.tradeoption.domain.Strategy;
import com.tradeoption.domain.SystemConfig;
import com.tradeoption.domain.TradeAction;
import com.tradeoption.service.MarketDataService;
import com.tradeoption.service.StrategySuggestionService;
import com.tradeoption.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StrategySuggestionServiceImpl implements StrategySuggestionService {

    private static final Logger log = LoggerFactory.getLogger(StrategySuggestionServiceImpl.class);

    private final MarketDataService marketDataService;
    private final SystemConfigService systemConfigService;
    private final com.tradeoption.service.StrategyValidationService strategyValidationService;
    private final com.tradeoption.service.DashboardService dashboardService;

    public StrategySuggestionServiceImpl(MarketDataService marketDataService, SystemConfigService systemConfigService,
            com.tradeoption.service.StrategyValidationService strategyValidationService,
            com.tradeoption.service.DashboardService dashboardService) {
        this.marketDataService = marketDataService;
        this.systemConfigService = systemConfigService;
        this.strategyValidationService = strategyValidationService;
        this.dashboardService = dashboardService;
    }

    @Override
    public Strategy suggestStraddle(String symbol) {
        // 1. Get Spot Price
        java.util.Optional<Double> spotOpt = marketDataService.getLtp(symbol);
        double spot = spotOpt.orElse(0.0);
        log.debug("Suggesting Straddle for {}: Spot Price = {}", symbol, spot);
        if (spot <= 0) {
            spot = 22000.0;
            log.warn("Spot price invalid/missing for {}. Defaulting to {}", symbol, spot);
        }

        // 2. Find ATM Strike
        double strikeStep = "BANKNIFTY".equalsIgnoreCase(symbol) ? 100.0 : 50.0;
        double atmStrike = Math.round(spot / strikeStep) * strikeStep;

        // Validation: Find nearest valid strike
        atmStrike = findValidStrike(symbol, atmStrike, strikeStep, 10);

        // 3. Expiry
        String expiry = resolveExpiry(symbol);

        // 4. Optimize Selection
        // Scan ATM, +Step, -Step, +2Step, -2Step
        return optimizeSelection(symbol, atmStrike, strikeStep, expiry, spot, this::createStraddle);
    }

    @Override
    public Strategy suggestStrangle(String symbol) {
        // 1. Spot
        // For example purposes, using live spot if available, else 0
        java.util.Optional<Double> spotOpt = marketDataService.getLtp(symbol);
        double spot = spotOpt.orElse(0.0);
        log.debug("Suggesting Strangle for {}: Spot Price = {}", symbol, spot);
        if (spot <= 0) {
            spot = 22000.0;
            log.warn("Spot price invalid/missing for {}. Defaulting to {}", symbol, spot);
        }

        // 2. ATM
        double strikeStep = "BANKNIFTY".equalsIgnoreCase(symbol) ? 100.0 : 50.0;
        double atmStrike = Math.round(spot / strikeStep) * strikeStep;

        // 3. Expiry
        String expiry = resolveExpiry(symbol);

        // 4. Optimize
        return optimizeSelection(symbol, atmStrike, strikeStep, expiry, spot, this::createStrangle);
    }

    // --- Optimization Helper ---

    private interface StrategyCreator {
        Strategy create(String symbol, double centerStrike, String expiry);
    }

    private Strategy optimizeSelection(String symbol, double centerStrike, double step, String expiry, double spot,
            StrategyCreator creator) {
        // Candidates: Center, +/- 1, +/- 2 steps
        double[] candidates = new double[] {
                centerStrike,
                centerStrike + step,
                centerStrike - step,
                centerStrike + (2 * step),
                centerStrike - (2 * step)
        };

        Strategy bestStrategy = null;
        double bestScore = -1.0;

        // Context for metrics
        double volatility = 0.20; // Default IV assumption for optimization
        double interestRate = 0.10;
        double timeToExpiry = calculateTimeToExpiry(expiry);

        for (double strike : candidates) {
            // First check Spread Validity (Liquidity)
            if (!strategyValidationService.isSpreadValid(symbol, strike, "CE") ||
                    !strategyValidationService.isSpreadValid(symbol, strike, "PE")) {
                continue;
            }

            Strategy candidate = creator.create(symbol, strike, expiry);
            if (candidate == null)
                continue;

            // Filter out strategies where valid market data was missing (price == 0)
            if (!hasValidPrices(candidate)) {
                log.warn("Skipping strategy for strike {} due to missing/zero market data.", strike);
                continue;
            }

            // Calculate Metrics
            com.tradeoption.domain.DashboardMetrics metrics = dashboardService.calculateMetrics(
                    candidate, spot, volatility, timeToExpiry, interestRate);

            double rrr = metrics.getRiskRewardRatio();
            double pop = metrics.getProbabilityOfProfit();

            // Scoring Logic:
            // Must have PoP > 40% (0.40)
            // Then maximize RRR.
            // If PoP < 40%, score is penalized heavily logic?
            // User: "maximum risk reward ratio with good probability"

            // Let's filter: if PoP < 30%, ignore unless no other choice?
            // Simple Score:
            double score = 0.0;
            if (pop >= 0.40) {
                score = rrr * 10; // High weight for valid ones
            } else {
                score = rrr * pop; // Fallback
            }

            // Keep best
            if (bestStrategy == null || score > bestScore) {
                bestScore = score;
                bestStrategy = candidate;
            }
        }

        // Fallback: If optimization failed to find anything better, use center
        if (bestStrategy == null) {
            bestStrategy = creator.create(symbol, centerStrike, expiry);
        }
        return bestStrategy;
    }

    private double calculateTimeToExpiry(String expiryDateStr) {
        try {
            java.time.LocalDate expiry = java.time.LocalDate.parse(expiryDateStr);
            long days = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), expiry);
            return Math.max(days, 1) / 365.0;
        } catch (Exception e) {
            return 30.0 / 365.0;
        }
    }

    private String resolveExpiry(String symbol) {
        SystemConfig config = systemConfigService.getConfig();
        if (config.getSymbolExpiries() != null && config.getSymbolExpiries().containsKey(symbol)) {
            List<String> dates = config.getSymbolExpiries().get(symbol);
            if (dates != null && !dates.isEmpty())
                return dates.get(0);
        }
        return java.time.LocalDate.now().plusDays(30).toString();
    }

    private Strategy createStraddle(String symbol, double atmStrike, String expiry) {
        double width = "BANKNIFTY".equalsIgnoreCase(symbol) ? 1000.0 : 500.0;
        Strategy strategy = new Strategy();
        strategy.setSymbol(symbol);
        strategy.setId("Suggested-Straddle-" + System.currentTimeMillis() + "-" + (int) atmStrike);
        strategy.addLeg(createLeg(symbol, atmStrike, LegType.CE, TradeAction.SELL, expiry));
        strategy.addLeg(createLeg(symbol, atmStrike, LegType.PE, TradeAction.SELL, expiry));
        strategy.addLeg(createLeg(symbol, atmStrike + width, LegType.CE, TradeAction.BUY, expiry));
        strategy.addLeg(createLeg(symbol, atmStrike - width, LegType.PE, TradeAction.BUY, expiry));
        return strategy;
    }

    private Strategy createStrangle(String symbol, double centerStrike, String expiry) {
        // CenterStrike here acts as the anchor for the strangle
        double strangleDist = "BANKNIFTY".equalsIgnoreCase(symbol) ? 1000.0 : 500.0;
        double wingWidth = "BANKNIFTY".equalsIgnoreCase(symbol) ? 500.0 : 200.0;

        double sellCallStrike = centerStrike + strangleDist;
        double sellPutStrike = centerStrike - strangleDist;
        double buyCallStrike = sellCallStrike + wingWidth;
        double buyPutStrike = sellPutStrike - wingWidth;

        Strategy strategy = new Strategy();
        strategy.setSymbol(symbol);
        strategy.setId("Suggested-Strangle-" + System.currentTimeMillis() + "-" + (int) centerStrike);
        strategy.addLeg(createLeg(symbol, sellCallStrike, LegType.CE, TradeAction.SELL, expiry));
        strategy.addLeg(createLeg(symbol, sellPutStrike, LegType.PE, TradeAction.SELL, expiry));
        strategy.addLeg(createLeg(symbol, buyCallStrike, LegType.CE, TradeAction.BUY, expiry));
        strategy.addLeg(createLeg(symbol, buyPutStrike, LegType.PE, TradeAction.BUY, expiry));
        return strategy;
    }

    private OptionLeg createLeg(String symbol, double strike, LegType type, TradeAction action, String expiry) {
        int quantity = getLotSize(symbol);

        // Fetch estimated price
        // Use a temporary leg to fetch price (or just pass params if service supported
        // it)
        OptionLeg tempLeg = new OptionLeg(strike, type, action, 0.0, quantity, expiry, symbol);

        java.util.Optional<Double> priceOpt;
        if (action == TradeAction.BUY) {
            priceOpt = marketDataService.getAsk(tempLeg);
        } else {
            priceOpt = marketDataService.getBid(tempLeg); // SELL -> Bid
        }

        // Fallback to LTP if Bid/Ask is missing/zero (though service handles some
        // fallback)
        if (priceOpt.isEmpty() || priceOpt.get() == 0) {
            priceOpt = marketDataService.getLtp(tempLeg);
        }

        double price = priceOpt.orElse(0.0);

        log.debug("Creating Leg: {} {} {} @ {}. Fetched Price: {}", action, strike, type, symbol, price);

        return new OptionLeg(strike, type, action, price, quantity, expiry, symbol);
    }

    private int getLotSize(String symbol) {
        SystemConfig config = systemConfigService.getConfig();
        if (config != null && config.getLotSizes() != null && config.getLotSizes().containsKey(symbol)) {
            return config.getLotSizes().get(symbol);
        }
        // Fallback defaults if config is missing specific symbol
        if ("BANKNIFTY".equalsIgnoreCase(symbol))
            return 15;
        if ("NIFTY".equalsIgnoreCase(symbol))
            return 50;
        if ("GOLD".equalsIgnoreCase(symbol))
            return 100;
        if ("GOLDM".equalsIgnoreCase(symbol))
            return 10;
        if ("CRUDEOIL".equalsIgnoreCase(symbol))
            return 100;
        return 50;
    }

    private double findValidStrike(String symbol, double startStrike, double step, int maxAttempts) {
        // Kept for initial anchor validation if needed, or legacy usage
        // logic reused in optimization loop
        return startStrike;
    }

    private boolean hasValidPrices(Strategy strategy) {
        for (OptionLeg leg : strategy.getLegs()) {
            if (leg.getEntryPrice() <= 0.0001) {
                return false;
            }
        }
        return true;
    }

}
