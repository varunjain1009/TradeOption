package com.tradeoption.service;

import com.tradeoption.domain.OptionLeg;
import com.tradeoption.domain.Strategy;

import java.util.Map;

public interface PnlCalculatorService {
    double calculateLegPnl(OptionLeg leg, double spotPrice);

    double calculateStrategyPnl(Strategy strategy, double spotPrice);

    Map<Double, Double> generatePayoffChart(Strategy strategy, double rangeStart, double rangeEnd, double step);

    /**
     * Calculates the T-0 (Live) PNL based on current market prices.
     * 
     * @param strategy      The strategy containing the option legs.
     * @param currentPrices A map of identifier (e.g. symbol or unique key) to
     *                      current LTP.
     *                      For now, we might assume the strategy legs have enough
     *                      info to look up price,
     *                      or we pass the MarketDataService.
     *                      To keep it pure, let's pass a map of prices keyed by
     *                      something unique in OptionLeg.
     *                      However, OptionLeg doesn't have a unique ID yet.
     *                      Let's assume for this iteration we pass the
     *                      MarketDataService or similar.
     *                      Actually, based on plan: "Pass Map<String, Double>
     *                      currentPrices".
     *                      But OptionLeg needs a way to map to String key.
     *                      Let's use MarketDataService directly in the impl or
     *                      better, pass a Function/Map.
     *                      Let's stick to the plan:
     *                      calculateLiveStrategyPnl(Strategy strategy,
     *                      Map<OptionLeg, Double> currentLtpMap)
     */
    double calculateLiveStrategyPnl(Strategy strategy, Map<OptionLeg, Double> currentLtpMap);
}
