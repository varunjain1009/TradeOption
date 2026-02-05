package com.tradeoption.service.impl;

import com.tradeoption.domain.LegType;
import com.tradeoption.domain.OptionLeg;
import com.tradeoption.domain.Strategy;
import com.tradeoption.domain.TradeAction;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LivePnlCalculatorTest {

    private final PnlCalculatorServiceImpl service = new PnlCalculatorServiceImpl();

    @Test
    public void testCalculateLiveStrategyPnl_LongCall() {
        Strategy strategy = new Strategy();
        // Buy Call at 50, Qty 100
        OptionLeg leg = new OptionLeg(10000, LegType.CE, TradeAction.BUY, 50, 100, "28MAR2024");
        strategy.addLeg(leg);

        Map<OptionLeg, Double> ltpMap = new HashMap<>();
        // Current Price 60 -> Profit (60 - 50) * 100 = 1000
        ltpMap.put(leg, 60.0);

        double pnl = service.calculateLiveStrategyPnl(strategy, ltpMap);
        assertEquals(1000.0, pnl, 0.01);
    }

    @Test
    public void testCalculateLiveStrategyPnl_ShortPut() {
        Strategy strategy = new Strategy();
        // Sell Put at 40, Qty 100
        OptionLeg leg = new OptionLeg(10000, LegType.PE, TradeAction.SELL, 40, 100, "28MAR2024");
        strategy.addLeg(leg);

        Map<OptionLeg, Double> ltpMap = new HashMap<>();
        // Current Price 30 -> Loss (30 - 40) is wrong direction for thinking
        // Short Profit = (Entry - Current) * Qty
        // Profit = (40 - 30) * 100 = 1000
        ltpMap.put(leg, 30.0);

        double pnl = service.calculateLiveStrategyPnl(strategy, ltpMap);
        assertEquals(1000.0, pnl, 0.01);

        // Current Price 50 -> Loss (40 - 50) * 100 = -1000
        ltpMap.put(leg, 50.0);
        pnl = service.calculateLiveStrategyPnl(strategy, ltpMap);
        assertEquals(-1000.0, pnl, 0.01);
    }

    @Test
    public void testCalculateLiveStrategyPnl_MultiLeg() {
        Strategy strategy = new Strategy();
        // Bull Call Spread
        // Long Call 10000 CE @ 50
        OptionLeg longLeg = new OptionLeg(10000, LegType.CE, TradeAction.BUY, 50, 100, "28MAR2024");
        // Short Call 10200 CE @ 20
        OptionLeg shortLeg = new OptionLeg(10200, LegType.CE, TradeAction.SELL, 20, 100, "28MAR2024");

        strategy.addLeg(longLeg);
        strategy.addLeg(shortLeg);

        Map<OptionLeg, Double> ltpMap = new HashMap<>();
        // Market moves up
        // Long Leg -> 70 (+20 profit) -> 2000
        // Short Leg -> 30 (-10 loss for short) -> (20 - 30) * 100 = -1000
        // Total = 1000
        ltpMap.put(longLeg, 70.0);
        ltpMap.put(shortLeg, 30.0);

        double pnl = service.calculateLiveStrategyPnl(strategy, ltpMap);
        assertEquals(1000.0, pnl, 0.01);
    }
}
