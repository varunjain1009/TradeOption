package com.tradeoption.service.impl;

import com.tradeoption.domain.PayoffGraphData;
import com.tradeoption.domain.Strategy;
import com.tradeoption.service.BlackScholesService;
import com.tradeoption.service.PayoffGraphService;
import com.tradeoption.service.PnlCalculatorService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PayoffGraphServiceImpl implements PayoffGraphService {

    private final PnlCalculatorService pnlCalculatorService;
    private final BlackScholesService blackScholesService;

    public PayoffGraphServiceImpl(PnlCalculatorService pnlCalculatorService, BlackScholesService blackScholesService) {
        this.pnlCalculatorService = pnlCalculatorService;
        this.blackScholesService = blackScholesService;
    }

    @Override
    public PayoffGraphData generatePayoffGraph(Strategy strategy, double currentSpot, double volatility,
            double timeToExpiry, double interestRate, double rangePercentage) {
        List<Double> spotPrices = new ArrayList<>();
        List<Double> expiryPnl = new ArrayList<>();
        List<Double> tZeroPnl = new ArrayList<>();

        // 1. Determine Range (Include Spot AND all Strikes)
        double minStrike = strategy.getLegs().stream().mapToDouble(com.tradeoption.domain.OptionLeg::getStrikePrice)
                .min().orElse(currentSpot);
        double maxStrike = strategy.getLegs().stream().mapToDouble(com.tradeoption.domain.OptionLeg::getStrikePrice)
                .max().orElse(currentSpot);

        double minSpot = Math.min(currentSpot * (1 - rangePercentage), minStrike * 0.90);
        double maxSpot = Math.max(currentSpot * (1 + rangePercentage), maxStrike * 1.10);

        int steps = 100; // Resolution
        double stepSize = (maxSpot - minSpot) / steps;

        for (int i = 0; i <= steps; i++) {
            double spot = minSpot + (i * stepSize);
            spotPrices.add(spot);

            // 1. Expiry PNL (Time = 0)
            double pnlAtExpiry = pnlCalculatorService.calculateStrategyPnl(strategy, spot);
            expiryPnl.add(pnlAtExpiry);

            // 2. T-0 PNL (Time = timeToExpiry)
            double theoreticalValue = 0.0;
            for (com.tradeoption.domain.OptionLeg leg : strategy.getLegs()) {
                boolean isCall = leg.getType() == com.tradeoption.domain.LegType.CE;
                double legPrice = blackScholesService.calculateOptionPrice(spot, leg.getStrikePrice(), timeToExpiry,
                        volatility, interestRate, isCall);

                if (leg.getAction() == com.tradeoption.domain.TradeAction.BUY) {
                    theoreticalValue += (legPrice * Math.abs(leg.getQuantity()));
                } else {
                    theoreticalValue -= (legPrice * Math.abs(leg.getQuantity()));
                }
            }

            double netEntryCost = 0.0;
            for (com.tradeoption.domain.OptionLeg leg : strategy.getLegs()) {
                if (leg.getAction() == com.tradeoption.domain.TradeAction.BUY) {
                    netEntryCost += (leg.getEntryPrice() * Math.abs(leg.getQuantity()));
                } else {
                    netEntryCost -= (leg.getEntryPrice() * Math.abs(leg.getQuantity()));
                }
            }

            // 3. Calculate Net Credit
            tZeroPnl.add(theoreticalValue - netEntryCost);
        }

        // 3. Calculate Net Credit
        double netCredit = 0.0;
        for (com.tradeoption.domain.OptionLeg leg : strategy.getLegs()) {
            if (leg.getAction() == com.tradeoption.domain.TradeAction.SELL) {
                netCredit += (leg.getEntryPrice() * Math.abs(leg.getQuantity()));
            } else {
                netCredit -= (leg.getEntryPrice() * Math.abs(leg.getQuantity()));
            }
        }

        // 4. Calculate Breakevens
        List<Double> breakevens = new ArrayList<>();
        if (expiryPnl.size() >= 2) {
            for (int j = 0; j < expiryPnl.size() - 1; j++) {
                double pnl1 = expiryPnl.get(j);
                double pnl2 = expiryPnl.get(j + 1);

                if ((pnl1 <= 0 && pnl2 >= 0) || (pnl1 >= 0 && pnl2 <= 0)) {
                    if (Math.abs(pnl2 - pnl1) > 0.0001) { // Avoid division by zero
                        double spot1 = spotPrices.get(j);
                        double spot2 = spotPrices.get(j + 1);
                        double fraction = (0 - pnl1) / (pnl2 - pnl1);
                        double crossingSpot = spot1 + fraction * (spot2 - spot1);
                        breakevens.add(Math.round(crossingSpot * 100.0) / 100.0);
                    }
                }
            }
        }

        return new PayoffGraphData(spotPrices, expiryPnl, tZeroPnl, breakevens, netCredit);
    }
}
