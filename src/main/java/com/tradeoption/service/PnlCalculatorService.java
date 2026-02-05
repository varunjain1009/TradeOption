package com.tradeoption.service;

import com.tradeoption.domain.OptionLeg;
import com.tradeoption.domain.Strategy;

import java.util.Map;

public interface PnlCalculatorService {
    double calculateLegPnl(OptionLeg leg, double spotPrice);

    double calculateStrategyPnl(Strategy strategy, double spotPrice);

    Map<Double, Double> generatePayoffChart(Strategy strategy, double rangeStart, double rangeEnd, double step);
}
