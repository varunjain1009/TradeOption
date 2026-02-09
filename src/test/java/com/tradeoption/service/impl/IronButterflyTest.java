package com.tradeoption.service.impl;

import com.tradeoption.domain.LegType;
import com.tradeoption.domain.OptionLeg;
import com.tradeoption.domain.Strategy;
import com.tradeoption.domain.TradeAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class IronButterflyTest {

    private PnlCalculatorServiceImpl calculator;

    @BeforeEach
    public void setup() {
        calculator = new PnlCalculatorServiceImpl();
    }

    @Test
    public void testShortIronButterfly_AtCenter() {
        // Short Iron Butterfly: Sell ATM Call+Put, Buy OTM Call+Put
        // CE SELL @ 62300, qty -50
        OptionLeg shortCall = new OptionLeg(62300.0, LegType.CE, TradeAction.SELL, 813.5818, -50, "27FEB2026", "GOLD");
        // PE SELL @ 62300, qty -50
        OptionLeg shortPut = new OptionLeg(62300.0, LegType.PE, TradeAction.SELL, 1105.341, -50, "27FEB2026", "GOLD");
        // CE BUY @ 62400, qty 50
        OptionLeg longCall = new OptionLeg(62400.0, LegType.CE, TradeAction.BUY, 627.9869, 50, "27FEB2026", "GOLD");
        // PE BUY @ 61900, qty 50
        OptionLeg longPut = new OptionLeg(61900.0, LegType.PE, TradeAction.BUY, 606.2702, 50, "27FEB2026", "GOLD");

        Strategy ironButterfly = new Strategy();
        ironButterfly.addLeg(shortCall);
        ironButterfly.addLeg(shortPut);
        ironButterfly.addLeg(longCall);
        ironButterfly.addLeg(longPut);

        // At spot 62300 (center), all options are at their strike
        // Short Call: Intrinsic = 0, PnL = (813.58 - 0) * 50 = 40,679
        // Short Put: Intrinsic = 0, PnL = (1105.34 - 0) * 50 = 55,267
        // Long Call: Intrinsic = 0, PnL = (0 - 627.99) * 50 = -31,399.5
        // Long Put: Intrinsic = 0, PnL = (0 - 606.27) * 50 = -30,313.5
        // Total: 40,679 + 55,267 - 31,399.5 - 30,313.5 = 34,233

        double pnlAtCenter = calculator.calculateStrategyPnl(ironButterfly, 62300.0);
        System.out.println("Short Iron Butterfly PnL at 62300: " + pnlAtCenter);

        // Should be maximum profit (positive)
        assertTrue(pnlAtCenter > 0, "PnL at center should be positive (max profit)");

        // At spot 62400 (long call strike)
        // Short Call: Intrinsic = 100, PnL = (813.58 - 100) * 50 = 35,679
        // Short Put: Intrinsic = 0, PnL = (1105.34 - 0) * 50 = 55,267
        // Long Call: Intrinsic = 0, PnL = (0 - 627.99) * 50 = -31,399.5
        // Long Put: Intrinsic = 500, PnL = (500 - 606.27) * 50 = -5,313.5
        double pnlAtLongCall = calculator.calculateStrategyPnl(ironButterfly, 62400.0);
        System.out.println("Short Iron Butterfly PnL at 62400: " + pnlAtLongCall);

        // At spot 61900 (long put strike)
        double pnlAtLongPut = calculator.calculateStrategyPnl(ironButterfly, 61900.0);
        System.out.println("Short Iron Butterfly PnL at 61900: " + pnlAtLongPut);

        // PnL at center should be greater than at the wings
        assertTrue(pnlAtCenter > pnlAtLongCall, "PnL at center should be greater than at upper wing");
        assertTrue(pnlAtCenter > pnlAtLongPut, "PnL at center should be greater than at lower wing");
    }
}
