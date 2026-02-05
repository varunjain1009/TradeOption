package com.tradeoption.service;

public interface StrategyValidationService {
    /**
     * Checks if the Bid-Ask spread for the given option is within acceptable
     * limits.
     * 
     * @param symbol The symbol (e.g., "NIFTY")
     * @param strike The strike price
     * @param type   "CE" or "PE"
     * @return true if spread is valid (or data missing), false if spread is too
     *         wide.
     */
    boolean isSpreadValid(String symbol, double strike, String type);
}
