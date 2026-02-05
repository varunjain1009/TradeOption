package com.tradeoption.service.impl;

import com.tradeoption.domain.LegType;
import com.tradeoption.domain.OptionLeg;
import com.tradeoption.domain.PayoffGraphData;
import com.tradeoption.domain.Strategy;
import com.tradeoption.domain.TradeAction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PayoffGraphServiceTest {

    private final PnlCalculatorServiceImpl pnlService = new PnlCalculatorServiceImpl();
    private final BlackScholesServiceImpl bsService = new BlackScholesServiceImpl();
    private final PayoffGraphServiceImpl graphService = new PayoffGraphServiceImpl(pnlService, bsService);

    @Test
    public void testGeneratePayoffGraph_LongCall() {
        // Long Call: Strike 100.
        // Current Spot: 100.
        // Range: 20% (80 to 120).

        Strategy strategy = new Strategy();
        // Assume simplified model where we buy at theoretical price or explicit entry
        // Let's set entry price to BS price approx for T=1, Vol=0.2, R=0.05
        // Price ~ 10.45
        strategy.addLeg(new OptionLeg(100, LegType.CE, TradeAction.BUY, 10.45, 1));

        double spot = 100;
        double vol = 0.2;
        double time = 1.0;
        double rate = 0.05;
        double range = 0.2;

        PayoffGraphData data = graphService.generatePayoffGraph(strategy, spot, vol, time, rate, range);

        List<Double> spots = data.getSpotPrices();
        List<Double> expiry = data.getExpiryPnl();
        List<Double> t0 = data.getTZeroPnl();

        assertEquals(spots.size(), expiry.size());
        assertEquals(spots.size(), t0.size());

        // Check approx middle (Spot 100)
        int midIndex = spots.size() / 2;
        double midSpot = spots.get(midIndex);
        assertEquals(100.0, midSpot, 1.0); // Approx

        // Expiry PNL at 100 should be: Max(0, 100-100) - 10.45 = -10.45
        double midExpiry = expiry.get(midIndex);
        assertEquals(-10.45, midExpiry, 0.1);

        // T-0 PNL at 100 should be ~0 (since entry price is roughly theoretical price)
        double midT0 = t0.get(midIndex);
        assertEquals(0.0, midT0, 0.5); // Allow small diff due to BS calc precision vs hardcoded entry

        // Check far OTM (Spot 80)
        // Expiry: -10.45 (loss limited to premium)
        // T-0: Should be loss, but slightly less than max loss because option still has
        // time value
        assertTrue(t0.get(0) > expiry.get(0));

        // Check far ITM (Spot 120)
        // Expiry: (120 - 100) - 10.45 = 9.55
        // T-0: Should be higher than Expiry curve because of time value?
        // Actually for deep ITM Call, time value -> 0, but usually extrinsic value is
        // positive.
        // So T-0 > Expiry generally (unless arbitrage/neg rates).
        assertTrue(t0.get(t0.size() - 1) >= expiry.get(expiry.size() - 1));
    }
}
