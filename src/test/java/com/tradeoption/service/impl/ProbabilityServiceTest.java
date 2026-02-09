package com.tradeoption.service.impl;

import com.tradeoption.domain.LegType;
import com.tradeoption.domain.OptionLeg;
import com.tradeoption.domain.Strategy;
import com.tradeoption.domain.TradeAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProbabilityServiceTest {

    // Use the real PnlCalculatorServiceImpl since it has no external dependencies
    private final PnlCalculatorServiceImpl pnlService = new PnlCalculatorServiceImpl();
    private final ProbabilityServiceImpl probService = new ProbabilityServiceImpl(pnlService);

    @Test
    public void testPoP_LongCall_ATM() {
        // Long Call ATM: Strike 100, Spot 100.
        // Premium paid needs to be recovered.
        // Let's specific entry price.
        // If Market Price is 10 (approx BS price for 1yr, 20% vol),
        // Breakeven = 110.
        // PoP is Prob(St > 110).
        // St ~ Lognormal.
        // Calc N(d2(110)).

        Strategy strategy = new Strategy();
        // Entry at 10.0
        strategy.addLeg(new OptionLeg(100, LegType.CE, TradeAction.BUY, 10.0, 1, "28MAR2024", "NIFTY"));

        double spot = 100;
        double vol = 0.2;
        double time = 1.0;
        double rate = 0.05;

        double pop = probService.calculateProbabilityOfProfit(strategy, spot, vol, time, rate);

        // Logic check:
        // Breakeven is 110.
        // d2_for_ITM = (ln(100/110) + ...) ...
        // Actually our helper calculateProbLessThan(110) gives Prob(St < 110).
        // Profit is St > 110. So PoP = 1 - Prob(St < 110).
        // With code logic: Interval is [110, Max].
        // probCalc = 1.0 - Prob(St < 110). This matches.

        // Exact BS calc check:
        // Limit 110.
        // num = ln(110/100) - (0.05 - 0.5*0.04)*1 = 0.0953 - 0.03 = 0.0653
        // den = 0.2
        // z = 0.3265
        // Prob(St < 110) = CDF(0.3265) ~ 0.628
        // PoP = 1 - 0.628 = 0.372

        assertEquals(0.37, pop, 0.05); // Allow margin for approx
    }

    @Test
    public void testPoP_LongPut_ITM() {
        // Long Put Strike 100. Spot 100.
        // Entry Price 5. Breakeven 95.
        // Profit if St < 95.

        Strategy strategy = new Strategy();
        strategy.addLeg(new OptionLeg(100, LegType.PE, TradeAction.BUY, 5.0, 1, "28MAR2024", "NIFTY"));

        double spot = 100;
        double vol = 0.2;
        double time = 1.0;
        double rate = 0.05;

        double pop = probService.calculateProbabilityOfProfit(strategy, spot, vol, time, rate);

        // Check reasonable range. ITM put usually < 50% pop because of drift?
        // Actually ATM put (K=S). Breakeven is 95.
        // Prob(St < 95).
        // Lower than 50%.

        assertTrue(pop < 0.5);
        assertTrue(pop > 0.2);
    }

    @Test
    public void testPoP_IronCondor() {
        // High Prob Strategy.
        // Spot 100.
        // Sell Put 90, Buy Put 85.
        // Sell Call 110, Buy Call 115.
        // Net Credit received ~ (Say 2.0).
        // Breakevens approx 88 and 112.
        // Large interval [88, 112] is profitable.

        Strategy strategy = new Strategy();
        strategy.addLeg(new OptionLeg(90, LegType.PE, TradeAction.SELL, 2.0, 1, "28MAR2024", "NIFTY"));
        strategy.addLeg(new OptionLeg(85, LegType.PE, TradeAction.BUY, 1.0, 1, "28MAR2024", "NIFTY"));
        strategy.addLeg(new OptionLeg(110, LegType.CE, TradeAction.SELL, 2.0, 1, "28MAR2024", "NIFTY"));
        strategy.addLeg(new OptionLeg(115, LegType.CE, TradeAction.BUY, 1.0, 1, "28MAR2024", "NIFTY"));
        // Net credit = +2 -1 +2 -1 = +2.

        // Put Spread: Max Loss if < 85. Max Profit if > 90.
        // Call Spread: Max Profit if < 110. Max Loss if > 115.
        // Breakeven Lower: 90 - 2 = 88.
        // Breakeven Upper: 110 + 2 = 112.

        double spot = 100;
        double vol = 0.2;
        double time = 0.1; // Short time
        double rate = 0.05;

        double pop = probService.calculateProbabilityOfProfit(strategy, spot, vol, time, rate);

        // Should be high
        assertTrue(pop > 0.6, "Iron condor should have high PoP");
    }
}
