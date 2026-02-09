package com.tradeoption.repository;

import com.tradeoption.controller.StrategyController;
import com.tradeoption.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class PositionHistoryTest {

    @Autowired
    private StrategyController strategyController;

    @Autowired
    private PositionRepository positionRepository;

    @MockBean
    private com.tradeoption.scheduler.DashboardBroadcaster dashboardBroadcaster;

    @Autowired
    private com.tradeoption.repository.RocksDBRepository rocksDBRepository; // To
    // ensure DB is ready

    @BeforeEach
    public void setup() {
        // Can't easily clear rocksdb in shared env without restart or custom method.
        // We will maintain unique IDs by using random symbols or independent test
        // data.
    }

    @Test
    public void testPositionLifecycle() {
        // 1. Open Position
        TradeOrder order = new TradeOrder();
        order.setSymbol("TEST_HIST_" + System.currentTimeMillis());
        order.setExpiryDate("28MAR2024");
        order.setStrikePrice(22000);
        order.setOptionType(LegType.CE);
        order.setPrice(100);
        order.setQuantity(50);
        order.setTradeAction(TradeAction.BUY);

        strategyController.placeTrade(order);

        String posId = Position.generateId(order.getSymbol(), order.getExpiryDate(),
                order.getStrikePrice(),
                order.getOptionType());
        Position pos = positionRepository.findById(posId);
        assertNotNull(pos);
        assertEquals(PositionStatus.OPEN, pos.getStatus());

        // 2. Partial Exit
        ExitRequest exitPart = new ExitRequest();
        exitPart.setPrice(120);
        exitPart.setQuantity(25);
        strategyController.exitPosition(posId, exitPart);

        pos = positionRepository.findById(posId);
        assertEquals(PositionStatus.PARTIALLY_CLOSED, pos.getStatus());
        assertEquals(25, pos.getNetQuantity());

        // 3. Full Exit
        ExitRequest exitFull = new ExitRequest();
        exitFull.setPrice(130);
        exitFull.setQuantity(25);
        strategyController.exitPosition(posId, exitFull);

        pos = positionRepository.findById(posId);
        assertEquals(PositionStatus.CLOSED, pos.getStatus());
        assertEquals(0, pos.getNetQuantity());
    }
}
