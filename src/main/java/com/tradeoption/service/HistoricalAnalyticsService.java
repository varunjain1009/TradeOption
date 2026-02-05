package com.tradeoption.service;

import com.tradeoption.domain.AnalyticsSnapshot;
import com.tradeoption.domain.Greeks;
import com.tradeoption.domain.Strategy;

import java.util.List;

public interface HistoricalAnalyticsService {
    /**
     * Captures a snapshot of the strategy's current metrics.
     * 
     * @param strategy  The strategy with valid ID
     * @param greeks    The calculated greeks
     * @param pnl       The current PNL
     * @param spotPrice The current spot price
     */
    void captureSnapshot(Strategy strategy, Greeks greeks, double pnl, double spotPrice);

    /**
     * Retrieves the history of snapshots for a given strategy.
     * 
     * @param strategyId The strategy ID
     * @return List of snapshots ordered by time (implicit in key)
     */
    List<AnalyticsSnapshot> getHistory(String strategyId);
}
