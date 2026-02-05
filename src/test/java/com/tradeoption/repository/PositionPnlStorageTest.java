package com.tradeoption.repository;

import com.tradeoption.controller.StrategyController;
import com.tradeoption.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class PositionPnlStorageTest {

    @Autowired
    private StrategyController strategyController;

    @Autowired
    private PositionRepository positionRepository;

    @MockBean
    private com.tradeoption.scheduler.DashboardBroadcaster dashboardBroadcaster;

    // Ensure RocksDB is loaded
    @Autowired
    private com.tradeoption.repository.RocksDBRepository rocksDBRepository;

    @Test
    public void testPnlStorage() {
        // 1. Buy 50 @ 100
        TradeOrder order = new TradeOrder();
        order.setSymbol("PNL_TEST_" + System.currentTimeMillis());
        order.setExpiryDate("28MAR2024");
        order.setStrikePrice(22000);
        order.setOptionType(LegType.CE);
        order.setPrice(100);
        order.setQuantity(50);
        order.setTradeAction(TradeAction.BUY);

        strategyController.placeTrade(order);

        String posId = Position.generateId(order.getSymbol(), order.getExpiryDate(), order.getStrikePrice(),
                order.getOptionType());
        Position pos = positionRepository.findById(posId);
        assertNotNull(pos);
        // Realized PNL should be 0 initially
        assertEquals(0.0, pos.getRealizedPnl(), 0.01);

        // 2. Sell 50 @ 120 (Full Exit)
        ExitRequest exitFull = new ExitRequest();
        exitFull.setPrice(120);
        exitFull.setQuantity(50);
        strategyController.exitPosition(posId, exitFull);

        // 3. Verify PNL
        // Profit = (120 - 100) * 50 = 1000
        pos = positionRepository.findById(posId);
        assertNotNull(pos);
        assertEquals(1000.0, pos.getRealizedPnl(), 0.01);
        assertEquals(PositionStatus.CLOSED, pos.getStatus());
    }
}
