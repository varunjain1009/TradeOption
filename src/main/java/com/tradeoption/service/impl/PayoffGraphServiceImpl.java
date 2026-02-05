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

        double minSpot = currentSpot * (1 - rangePercentage);
        double maxSpot = currentSpot * (1 + rangePercentage);

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

            tZeroPnl.add(theoreticalValue - netEntryCost);
        }

        return new PayoffGraphData(spotPrices, expiryPnl, tZeroPnl);
    }
}
