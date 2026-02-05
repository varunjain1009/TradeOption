package com.tradeoption.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BlackScholesTest {

    private final BlackScholesServiceImpl service = new BlackScholesServiceImpl();

    @Test
    public void testCalculateOptionPrice_Call() {
        // Example: S=100, K=100, T=1, r=0.05, sigma=0.2
        // Call Price ~ 10.45
        double S = 100;
        double K = 100;
        double T = 1;
        double r = 0.05;
        double sigma = 0.2;

        double price = service.calculateOptionPrice(S, K, T, sigma, r, true);
        assertEquals(10.45, price, 0.01);
    }

    @Test
    public void testCalculateOptionPrice_Put() {
        // Example: S=100, K=100, T=1, r=0.05, sigma=0.2
        // Put Price using Parity: P = C - S + K*e^(-rt)
        // P ~ 10.45 - 100 + 100*0.9512 = 10.45 - 4.88 = 5.57
        double S = 100;
        double K = 100;
        double T = 1;
        double r = 0.05;
        double sigma = 0.2;

        double price = service.calculateOptionPrice(S, K, T, sigma, r, false);
        assertEquals(5.57, price, 0.01);
    }

    @Test
    public void testCalculateImpliedVolatility() {
        // Using the Call Price computed approx 10.45
        double marketPrice = 10.4505;
        double S = 100;
        double K = 100;
        double T = 1;
        double r = 0.05;

        double iv = service.calculateImpliedVolatility(marketPrice, S, K, T, r, true);
        assertEquals(0.20, iv, 0.001);
    }
}
