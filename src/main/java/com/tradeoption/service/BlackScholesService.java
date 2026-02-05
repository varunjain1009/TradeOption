package com.tradeoption.service;

public interface BlackScholesService {
    /**
     * Calculates the theoretical price of an option using Black-Scholes formula.
     *
     * @param spotPrice    Current price of the underlying asset
     * @param strikePrice  Strike price of the option
     * @param timeToExpiry Time to expiry in years
     * @param volatility   Annualized volatility (sigma), e.g., 0.20 for 20%
     * @param interestRate Risk-free interest rate, e.g., 0.05 for 5%
     * @param isCall       True for Call option, False for Put option
     * @return The theoretical option price
     */
    double calculateOptionPrice(double spotPrice, double strikePrice, double timeToExpiry, double volatility,
            double interestRate, boolean isCall);

    /**
     * Calculates the Implied Volatility (IV) given the market price of an option.
     * Uses Newton-Raphson method to solve for volatility.
     *
     * @param marketPrice  Current market price of the option
     * @param spotPrice    Current price of the underlying asset
     * @param strikePrice  Strike price of the option
     * @param timeToExpiry Time to expiry in years
     * @param interestRate Risk-free interest rate
     * @param isCall       True for Call option, False for Put option
     * @return The implied volatility
     */
    double calculateImpliedVolatility(double marketPrice, double spotPrice, double strikePrice, double timeToExpiry,
            double interestRate, boolean isCall);
}
