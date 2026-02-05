package com.tradeoption.service.impl;

import com.tradeoption.domain.Greeks;
import com.tradeoption.domain.LegType;
import com.tradeoption.domain.OptionLeg;
import com.tradeoption.domain.Strategy;
import com.tradeoption.domain.TradeAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GreeksServiceTest {

    private final GreeksServiceImpl service = new GreeksServiceImpl();

    @Test
    public void testCalculateGreeks_Call() {
        // ATM Call: S=100, K=100, T=1, r=0.05, sigma=0.2
        double S = 100;
        double K = 100;
        double T = 1;
        double r = 0.05;
        double sigma = 0.2;

        Greeks g = service.calculateGreeks(S, K, T, sigma, r, true);

        // Delta for ATM Call should be > 0.5 because of r (interest rate drift)
        // With r=0, Delta is approx 0.5. With r>0, d1 shifts slightly higher.
        assertTrue(g.getDelta() > 0.5 && g.getDelta() < 0.7, "Delta should be approx 0.6");

        // Theta should be negative (time decay)
        assertTrue(g.getTheta() < 0, "Theta should be negative for Long Call");

        // Vega should be positive
        assertTrue(g.getVega() > 0, "Vega should be positive for Long Call");
    }

    @Test
    public void testCalculateGreeks_Put() {
        // ATM Put: S=100, K=100, T=1, r=0.05, sigma=0.2
        double S = 100;
        double K = 100;
        double T = 1;
        double r = 0.05;
        double sigma = 0.2;

        Greeks g = service.calculateGreeks(S, K, T, sigma, r, false);

        // Delta for ATM Put should be negative
        assertTrue(g.getDelta() < -0.3 && g.getDelta() > -0.5, "Delta should be approx -0.4");

        // Theta should be negative usually, but deep ITM puts can have positive theta
        // with high interest rates.
        // For ATM, it should be negative.
        assertTrue(g.getTheta() < 0, "Theta should be negative for Long Put");
    }

    @Test
    public void testStrategyAggregation_DeltaNeutral() {
        Strategy strategy = new Strategy();
        // Buy 1 ATM Call (Delta ~ 0.6)
        OptionLeg callLeg = new OptionLeg(10000, LegType.CE, TradeAction.BUY, 0, 100, "28MAR2024"); // 1 lot = 100 qty
        // Sell appropriate amount of underlying or options to neutralise.
        // Let's try a Straddle: Buy Call + Buy Put.
        // Delta Call ~ 0.6, Delta Put ~ -0.4. Sum ~ +0.2. Not neutral.

        // Let's just verify the sum logic.
        // Buy 1 Call, Sell 1 Call (same strike). Net should be 0.
        OptionLeg buyCall = new OptionLeg(10000, LegType.CE, TradeAction.BUY, 0, 100, "28MAR2024");
        OptionLeg sellCall = new OptionLeg(10000, LegType.CE, TradeAction.SELL, 0, 100, "28MAR2024");

        strategy.addLeg(buyCall);
        strategy.addLeg(sellCall);

        double S = 100;
        double K = 100; // Assuming legs mapped to this strike
        // In real service, leg has strike.
        // Here we mock the leg properties actually used in calculation.
        // But the service takes leg.getStrikePrice().
        // So we need to ensure local legs match the parameters we expect.
        // Re-creating legs with correct strike
        OptionLeg leg1 = new OptionLeg(100, LegType.CE, TradeAction.BUY, 0, 1, "28MAR2024");
        OptionLeg leg2 = new OptionLeg(100, LegType.CE, TradeAction.SELL, 0, 1, "28MAR2024");

        Strategy s = new Strategy();
        s.addLeg(leg1);
        s.addLeg(leg2);

        Greeks total = service.calculateStrategyGreeks(s, 100, 0.2, 0.05, 1);

        assertEquals(0.0, total.getDelta(), 0.0001);
        assertEquals(0.0, total.getGamma(), 0.0001);
        assertEquals(0.0, total.getTheta(), 0.0001);
    }
}
