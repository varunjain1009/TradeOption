package com.tradeoption.service.impl;

import com.tradeoption.domain.LegType;
import com.tradeoption.domain.OptionLeg;
import com.tradeoption.domain.Strategy;
import com.tradeoption.domain.TradeAction;
import com.tradeoption.service.PnlCalculatorService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.TreeMap;

@Service
public class PnlCalculatorServiceImpl implements PnlCalculatorService {

    @Override
    public double calculateLegPnl(OptionLeg leg, double spotPrice) {
        double intrinsicValue = 0;
        if (leg.getType() == LegType.CE) {
            intrinsicValue = Math.max(0, spotPrice - leg.getStrikePrice());
        } else {
            intrinsicValue = Math.max(0, leg.getStrikePrice() - spotPrice);
        }

        double pnl = 0;
        if (leg.getAction() == TradeAction.BUY) {
            // Long: Profit = Intrinsic - Premium Paid
            pnl = intrinsicValue - leg.getEntryPrice();
        } else {
            // Short: Profit = Premium Received - Intrinsic
            pnl = leg.getEntryPrice() - intrinsicValue;
        }

        return pnl * leg.getQuantity();
    }

    @Override
    public double calculateStrategyPnl(Strategy strategy, double spotPrice) {
        return strategy.getLegs().stream()
                .mapToDouble(leg -> calculateLegPnl(leg, spotPrice))
                .sum();
    }

    @Override
    public Map<Double, Double> generatePayoffChart(Strategy strategy, double rangeStart, double rangeEnd, double step) {
        Map<Double, Double> payoffChart = new TreeMap<>();
        for (double spot = rangeStart; spot <= rangeEnd; spot += step) {
            payoffChart.put(spot, calculateStrategyPnl(strategy, spot));
        }
        return payoffChart;
    }

    @Override
    public double calculateLiveStrategyPnl(Strategy strategy, Map<OptionLeg, Double> currentLtpMap) {
        double totalPnl = 0;
        for (OptionLeg leg : strategy.getLegs()) {
            Double ltp = currentLtpMap.get(leg);
            if (ltp != null) {
                double pnl = 0;
                if (leg.getAction() == TradeAction.BUY) {
                    // Long: PNL = (Current Price - Entry Price) * Quantity
                    pnl = (ltp - leg.getEntryPrice()) * leg.getQuantity();
                } else {
                    // Short: PNL = (Entry Price - Current Price) * Quantity
                    pnl = (leg.getEntryPrice() - ltp) * leg.getQuantity();
                }
                totalPnl += pnl;
            }
        }
        return totalPnl;
    }
}
