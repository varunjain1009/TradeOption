package com.tradeoption.service.impl;

import com.tradeoption.domain.DashboardMetrics;
import com.tradeoption.domain.Greeks;
import com.tradeoption.domain.Strategy;
import com.tradeoption.service.*;
import org.springframework.stereotype.Service;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final GreeksService greeksService;
    private final ProbabilityService probabilityService;
    private final PnlCalculatorService pnlCalculatorService;
    private final BlackScholesService blackScholesService;

    public DashboardServiceImpl(GreeksService greeksService, ProbabilityService probabilityService,
            PnlCalculatorService pnlCalculatorService, BlackScholesService blackScholesService) {
        this.greeksService = greeksService;
        this.probabilityService = probabilityService;
        this.pnlCalculatorService = pnlCalculatorService;
        this.blackScholesService = blackScholesService;
    }

    @Override
    public DashboardMetrics calculateMetrics(Strategy strategy, double currentSpot, double volatility,
            double timeToExpiry, double interestRate) {
        // 1. Greeks
        Greeks greeks = greeksService.calculateStrategyGreeks(strategy, currentSpot, volatility, interestRate,
                timeToExpiry);

        // 2. Current PNL (T-0 Theoretical)
        // Similar logic to PayoffGraphServiceImpl, consider refactoring to shared util
        // later
        double currentPnl = calculateTZeroPnl(strategy, currentSpot, volatility, timeToExpiry, interestRate);

        // 3. PoP
        double pop = probabilityService.calculateProbabilityOfProfit(strategy, currentSpot, volatility, timeToExpiry,
                interestRate);

        // 4. Max Profit / Max Loss
        // Scan expiry pnl over range
        double[] maxMin = calculateMaxMinPnl(strategy, currentSpot);
        double maxProfit = maxMin[0];
        double maxLoss = maxMin[1];

        // 5. Risk Reward
        double riskReward = 0.0;
        if (Math.abs(maxLoss) > 0.0001) {
            riskReward = maxProfit / Math.abs(maxLoss);
        }

        return new DashboardMetrics(
                greeks,
                currentPnl,
                maxProfit,
                maxLoss,
                pop,
                riskReward,
                System.currentTimeMillis());
    }

    private double calculateTZeroPnl(Strategy strategy, double spot, double vol, double t, double r) {
        double theoreticalValue = 0.0;
        for (com.tradeoption.domain.OptionLeg leg : strategy.getLegs()) {
            boolean isCall = leg.getType() == com.tradeoption.domain.LegType.CE;
            double legPrice = blackScholesService.calculateOptionPrice(spot, leg.getStrikePrice(), t, vol, r, isCall);

            if (leg.getAction() == com.tradeoption.domain.TradeAction.BUY) {
                theoreticalValue += (legPrice * leg.getQuantity());
            } else {
                theoreticalValue -= (legPrice * leg.getQuantity());
            }
        }

        double netEntryCost = 0.0;
        for (com.tradeoption.domain.OptionLeg leg : strategy.getLegs()) {
            if (leg.getAction() == com.tradeoption.domain.TradeAction.BUY) {
                netEntryCost += (leg.getEntryPrice() * leg.getQuantity());
            } else {
                netEntryCost -= (leg.getEntryPrice() * leg.getQuantity());
            }
        }

        return theoreticalValue - netEntryCost;
    }

    private double[] calculateMaxMinPnl(Strategy strategy, double currentSpot) {
        // Scan logic: +/- 50% range, 200 points
        double range = 0.50;
        double minSpot = currentSpot * (1 - range);
        double maxSpot = currentSpot * (1 + range);
        int steps = 200;
        double stepSize = (maxSpot - minSpot) / steps;

        double maxPnl = Double.NEGATIVE_INFINITY;
        double minPnl = Double.POSITIVE_INFINITY;

        for (int i = 0; i <= steps; i++) {
            double s = minSpot + (i * stepSize);
            double pnl = pnlCalculatorService.calculateStrategyPnl(strategy, s);
            if (pnl > maxPnl)
                maxPnl = pnl;
            if (pnl < minPnl)
                minPnl = pnl;
        }

        // Edge case: if minPnl is still positive (risk free arbitrage?), max loss is 0
        // or profit
        // Usually max loss is negative.
        return new double[] { maxPnl, minPnl };
    }
}
