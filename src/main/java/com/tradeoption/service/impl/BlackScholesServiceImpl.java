package com.tradeoption.service.impl;

import com.tradeoption.service.BlackScholesService;
import org.springframework.stereotype.Service;

@Service
public class BlackScholesServiceImpl implements BlackScholesService {

    private static final int MAX_ITERATIONS = 100;
    private static final double PRECISION = 1e-5;

    @Override
    public double calculateOptionPrice(double S, double K, double T, double sigma, double r, boolean isCall) {
        if (T <= 0) {
            // Intrinsic value at expiry
            return isCall ? Math.max(0, S - K) : Math.max(0, K - S);
        }

        double d1 = (Math.log(S / K) + (r + 0.5 * sigma * sigma) * T) / (sigma * Math.sqrt(T));
        double d2 = d1 - sigma * Math.sqrt(T);

        if (isCall) {
            return S * cdf(d1) - K * Math.exp(-r * T) * cdf(d2);
        } else {
            return K * Math.exp(-r * T) * cdf(-d2) - S * cdf(-d1);
        }
    }

    @Override
    public double calculateImpliedVolatility(double marketPrice, double S, double K, double T, double r,
            boolean isCall) {
        double sigma = 0.5; // Initial guess (50% volatility)

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            double price = calculateOptionPrice(S, K, T, sigma, r, isCall);
            double diff = marketPrice - price;

            if (Math.abs(diff) < PRECISION) {
                return sigma;
            }

            double vega = calculateVega(S, K, T, sigma, r);
            if (vega == 0)
                break;

            sigma = sigma + diff / vega;
        }

        return sigma;
    }

    private double calculateVega(double S, double K, double T, double sigma, double r) {
        double d1 = (Math.log(S / K) + (r + 0.5 * sigma * sigma) * T) / (sigma * Math.sqrt(T));
        return S * pdf(d1) * Math.sqrt(T);
    }

    // Standard Normal Probability Density Function
    private double pdf(double x) {
        return Math.exp(-0.5 * x * x) / Math.sqrt(2 * Math.PI);
    }

    // Cumulative Distribution Function using approximation (Abramowitz & Stegun)
    private double cdf(double x) {
        if (x < 0) {
            return 1 - cdf(-x);
        }
        double k = 1.0 / (1.0 + 0.2316419 * x);
        double poly = k * (0.319381530 + k * (-0.356563782 + k * (1.781477937 + k * (-1.821255978 + k * 1.330274429))));
        return 1.0 - pdf(x) * poly;
    }
}
