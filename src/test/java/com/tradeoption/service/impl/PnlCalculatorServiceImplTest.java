package com.tradeoption.service.impl;

import com.tradeoption.domain.LegType;
import com.tradeoption.domain.OptionLeg;
import com.tradeoption.domain.Strategy;
import com.tradeoption.domain.TradeAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PnlCalculatorServiceImplTest {

    private PnlCalculatorServiceImpl calculator;

    @BeforeEach
    public void setup() {
        calculator = new PnlCalculatorServiceImpl();
    }

    @Test
    public void testLongCallPnl() {
        // Buy Call Strike 100 @ 5.0
        OptionLeg longCall = new OptionLeg(100.0, LegType.CE, TradeAction.BUY, 5.0, 1, "28MAR2024", "NIFTY");

        // Spot 90 (OTM): Loss = Premium = -5
        assertEquals(-5.0, calculator.calculateLegPnl(longCall, 90.0));

        // Spot 100 (ATM): Loss = Premium = -5
        assertEquals(-5.0, calculator.calculateLegPnl(longCall, 100.0));

        // Spot 105 (ITM): Intrinsic 5, Paid 5 => PNL 0 (Breakeven)
        assertEquals(0.0, calculator.calculateLegPnl(longCall, 105.0));

        // Spot 110 (ITM): Intrinsic 10, Paid 5 => PNL 5
        assertEquals(5.0, calculator.calculateLegPnl(longCall, 110.0));
    }

    @Test
    public void testShortPutPnl() {
        // Sell Put Strike 100 @ 5.0
        OptionLeg shortPut = new OptionLeg(100.0, LegType.PE, TradeAction.SELL, 5.0, 1, "28MAR2024", "NIFTY");

        // Spot 110 (OTM): Profit = Premium = 5
        assertEquals(5.0, calculator.calculateLegPnl(shortPut, 110.0));

        // Spot 90 (ITM): Intrinsic 10, Received 5 => PNL -5
        assertEquals(-5.0, calculator.calculateLegPnl(shortPut, 90.0));
    }

    @Test
    public void testStraddlePnl() {
        // Long Straddle: Buy Call 100 @ 5, Buy Put 100 @ 5. Total Debit 10.
        Strategy straddle = new Strategy();
        straddle.addLeg(new OptionLeg(100.0, LegType.CE, TradeAction.BUY, 5.0, 1, "28MAR2024", "NIFTY"));
        straddle.addLeg(new OptionLeg(100.0, LegType.PE, TradeAction.BUY, 5.0, 1, "28MAR2024", "NIFTY"));

        // Spot 100 (ATM): Both expire worthless (relative to strike/action logic for
        // intrinsic).
        // CE Intrinsic 0, PE Intrinsic 0. Total Loss = 10.
        assertEquals(-10.0, calculator.calculateStrategyPnl(straddle, 100.0));

        // Spot 120: CE ITM 20 (PNL 15), PE OTM (PNL -5). Total PNL 10.
        assertEquals(10.0, calculator.calculateStrategyPnl(straddle, 120.0));

        // Spot 80: CE OTM (PNL -5), PE ITM 20 (PNL 15). Total PNL 10.
        assertEquals(10.0, calculator.calculateStrategyPnl(straddle, 80.0));
    }
}
