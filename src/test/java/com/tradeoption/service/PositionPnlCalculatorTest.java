package com.tradeoption.service;

import com.tradeoption.domain.LegType;
import com.tradeoption.domain.Position;
import com.tradeoption.domain.PositionEntry;
import com.tradeoption.domain.PositionPnl;
import com.tradeoption.domain.TradeAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PositionPnlCalculatorTest {

    @Test
    public void testFifoMathing() {
        Position position = new Position("NIFTY", "28MAR", 22000, LegType.CE);

        // Buy 100 @ 10
        position.addEntry(createEntry(10, 100, TradeAction.BUY));
        // Sell 50 @ 20
        position.addEntry(createEntry(20, 50, TradeAction.SELL));

        // Realized: 50 * (20 - 10) = 500
        // Open: 50 @ 10.
        // LTP: 15. Unrealized: 50 * (15 - 10) = 250.

        PositionPnl pnl = PositionPnlCalculator.calculatePnl(position, 15.0);

        assertEquals(500.0, pnl.getRealizedPnl());
        assertEquals(250.0, pnl.getUnrealizedPnl());
        assertEquals(50, pnl.getNetQuantity());
    }

    @Test
    public void testLinkedMatching() {
        Position position = new Position("NIFTY", "28MAR", 22000, LegType.CE);

        PositionEntry buy1 = createEntry(10, 50, TradeAction.BUY); // FIFO 1
        // sleep to ensure timestamp diff?
        try {
            Thread.sleep(1);
        } catch (Exception e) {
        }
        PositionEntry buy2 = createEntry(12, 50, TradeAction.BUY); // FIFO 2

        position.addEntry(buy1);
        position.addEntry(buy2);

        // Sell 50 @ 20 linked to buy2 (specific lot exit)
        PositionEntry sell = createEntry(20, 50, TradeAction.SELL);
        sell.setLinkedEntryId(buy2.getId());
        position.addEntry(sell);

        // Realized: 50 * (20 - 12) = 400. (Not 500 from buy1)
        // Open: buy1 (50 @ 10).
        // LTP: 15. Unrealized: 50 * (15 - 10) = 250.

        PositionPnl pnl = PositionPnlCalculator.calculatePnl(position, 15.0);

        assertEquals(400.0, pnl.getRealizedPnl());
        assertEquals(250.0, pnl.getUnrealizedPnl());
        assertEquals(50, pnl.getNetQuantity());
    }

    private PositionEntry createEntry(double price, int qty, TradeAction action) {
        try {
            Thread.sleep(1);
        } catch (Exception e) {
        } // ensure unique timestamps
        return new PositionEntry(price, qty, action);
    }
}
