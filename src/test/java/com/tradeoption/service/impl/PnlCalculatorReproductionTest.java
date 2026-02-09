package com.tradeoption.service.impl;

import com.tradeoption.domain.LegType;
import com.tradeoption.domain.OptionLeg;
import com.tradeoption.domain.Strategy;
import com.tradeoption.domain.TradeAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PnlCalculatorReproductionTest {

    private PnlCalculatorServiceImpl calculator;

    @BeforeEach
    public void setup() {
        calculator = new PnlCalculatorServiceImpl();
    }

    @Test
    public void testShortStraddlePnl_PositiveQuantity() {
        // Sell Call Strike 100 @ 10.0, Qty 50
        OptionLeg shortCall = new OptionLeg(100.0, LegType.CE, TradeAction.SELL, 10.0, 50, "28MAR2024", "NIFTY");
        // Sell Put Strike 100 @ 10.0, Qty 50
        OptionLeg shortPut = new OptionLeg(100.0, LegType.PE, TradeAction.SELL, 10.0, 50, "28MAR2024", "NIFTY");

        Strategy straddle = new Strategy();
        straddle.addLeg(shortCall);
        straddle.addLeg(shortPut);

        // Spot 100 (ATM): Intrinsic 0.
        // Call PNL: (10 - 0) * 50 = 500
        // Put PNL: (10 - 0) * 50 = 500
        // Total PNL: 1000
        double pnl = calculator.calculateStrategyPnl(straddle, 100.0);

        System.out.println("Short Straddle PnL (Spot 100): " + pnl);

        assertEquals(1000.0, pnl, 0.001);
    }

    @Test
    public void testShortStraddlePnl_NegativeQuantityCheck() {
        // What if quantity is -50?
        // Sell Call Strike 100 @ 10.0, Qty -50
        OptionLeg shortCall = new OptionLeg(100.0, LegType.CE, TradeAction.SELL, 10.0, -50, "28MAR2024", "NIFTY");
        // Sell Put Strike 100 @ 10.0, Qty -50
        OptionLeg shortPut = new OptionLeg(100.0, LegType.PE, TradeAction.SELL, 10.0, -50, "28MAR2024", "NIFTY");

        Strategy straddle = new Strategy();
        straddle.addLeg(shortCall);
        straddle.addLeg(shortPut);

        // Spot 100 (ATM): Intrinsic 0.
        // Call PNL: Premium Received (10) * Qty (50) = 500
        // Put PNL: Premium Received (10) * Qty (50) = 500
        // Total PNL: 1000 (PROFIT) - Since we fixed the double negative
        double pnl = calculator.calculateStrategyPnl(straddle, 100.0);

        System.out.println("Short Straddle PnL (Spot 100, Neg Qty): " + pnl);

        assertEquals(1000.0, pnl, 0.001);
    }

    @Test
    public void testShortStraddlePnl_ZeroEntryPrice() {
        // Simulating missing entry price (deserialization issue)
        // Sell Call Strike 100 @ 0.0, Qty 50
        OptionLeg shortCall = new OptionLeg(100.0, LegType.CE, TradeAction.SELL, 0.0, 50, "28MAR2024", "NIFTY");
        // Sell Put Strike 100 @ 0.0, Qty 50
        OptionLeg shortPut = new OptionLeg(100.0, LegType.PE, TradeAction.SELL, 0.0, 50, "28MAR2024", "NIFTY");

        Strategy straddle = new Strategy();
        straddle.addLeg(shortCall);
        straddle.addLeg(shortPut);

        // Spot 100 (ATM): Intrinsic 0. PnL = (0 - 0) * 50 = 0.
        assertEquals(0.0, calculator.calculateStrategyPnl(straddle, 100.0));

        // Spot 110:
        // Call Intrinsic 10. PnL = (0 - 10) * 50 = -500.
        // Put Intrinsic 0.
        // Total PnL = -500.
        assertEquals(-500.0, calculator.calculateStrategyPnl(straddle, 110.0));

        // Spot 90:
        // Call Intrinsic 0.
        // Put Intrinsic 10. PnL = (0 - 10) * 50 = -500.
        assertEquals(-500.0, calculator.calculateStrategyPnl(straddle, 90.0));
    }

    @Test
    public void testShortStraddlePnl_WithSetters() {
        // Verify setters work (which Jackson will use)
        OptionLeg shortCall = new OptionLeg();
        shortCall.setStrikePrice(100.0);
        shortCall.setType(LegType.CE);
        shortCall.setAction(TradeAction.SELL);
        shortCall.setEntryPrice(10.0);
        shortCall.setQuantity(50);
        shortCall.setExpiryDate("28MAR2024");
        shortCall.setSymbol("NIFTY");

        OptionLeg shortPut = new OptionLeg();
        shortPut.setStrikePrice(100.0);
        shortPut.setType(LegType.PE);
        shortPut.setAction(TradeAction.SELL);
        shortPut.setEntryPrice(10.0);
        shortPut.setQuantity(50);

        Strategy straddle = new Strategy();
        straddle.addLeg(shortCall);
        straddle.addLeg(shortPut);

        // Spot 100: Expected Profit 1000
        assertEquals(1000.0, calculator.calculateStrategyPnl(straddle, 100.0));
    }
}
