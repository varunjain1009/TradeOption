package com.tradeoption.service;

import com.tradeoption.domain.PayoffGraphData;
import com.tradeoption.domain.Strategy;

public interface PayoffGraphService {
    /**
     * Generates data for the Payoff Graph (Expiry and T-0 curves).
     *
     * @param strategy        The derivative strategy
     * @param currentSpot     Current trading price of the underlying
     * @param volatility      Annualized volatility (e.g., 0.20)
     * @param timeToExpiry    Time to expiry in years
     * @param interestRate    Risk-free interest rate
     * @param rangePercentage Percentage range to cover +/- around spot (e.g., 0.20
     *                        for 20%)
     * @return PayoffGraphData object containing coordinate lists
     */
    PayoffGraphData generatePayoffGraph(Strategy strategy, double currentSpot, double volatility, double timeToExpiry,
            double interestRate, double rangePercentage);
}
