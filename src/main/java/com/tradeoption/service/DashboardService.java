package com.tradeoption.service;

import com.tradeoption.domain.DashboardMetrics;
import com.tradeoption.domain.Strategy;

public interface DashboardService {
    /**
     * Calculates comprehensive dashboard metrics for a strategy.
     *
     * @param strategy     The active strategy
     * @param currentSpot  Current spot price
     * @param volatility   Volatility
     * @param timeToExpiry Time to expiry
     * @param interestRate Interest rate
     * @return DashboardMetrics object
     */
    DashboardMetrics calculateMetrics(Strategy strategy, double currentSpot, double volatility, double timeToExpiry,
            double interestRate);
}
