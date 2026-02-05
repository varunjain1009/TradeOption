package com.tradeoption.service;

import com.tradeoption.domain.Strategy;

public interface ProbabilityService {
    /**
     * Calculates the Probability of Profit (PoP) for a given strategy.
     * PoP is the probability that the strategy PNL > 0 at expiry.
     *
     * @param strategy     The option strategy
     * @param spot         Current spot price of underlying
     * @param volatility   Annualized volatility
     * @param timeToExpiry Time to expiry in years
     * @param interestRate Risk-free interest rate
     * @return Probability between 0.0 and 1.0
     */
    double calculateProbabilityOfProfit(Strategy strategy, double spot, double volatility, double timeToExpiry,
            double interestRate);
}
