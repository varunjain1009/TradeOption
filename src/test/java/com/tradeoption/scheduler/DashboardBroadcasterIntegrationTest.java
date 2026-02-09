package com.tradeoption.scheduler;

import com.tradeoption.domain.DashboardMetrics;
import com.tradeoption.domain.Position;
import com.tradeoption.domain.PositionEntry;
import com.tradeoption.domain.TradeAction;
import com.tradeoption.domain.LegType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
public class DashboardBroadcasterIntegrationTest {

    @Autowired
    private DashboardBroadcaster dashboardBroadcaster;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @MockBean
    private com.tradeoption.repository.RocksDBRepository rocksDBRepository;

    @MockBean
    private com.tradeoption.repository.PositionRepository positionRepository;

    @MockBean
    private com.tradeoption.service.MarketStatusService marketStatusService;

    @MockBean
    private com.tradeoption.service.MarketDataService marketDataService;

    @Test
    public void testDashboardMetricsBroadcast() {
        // Ensure market is OPEN
        when(marketStatusService.isMarketOpen()).thenReturn(true);
        // Ensure valid spot price
        when(marketDataService.getLtp(any(String.class))).thenReturn(java.util.Optional.of(22000.0));

        // Mock DB behavior: Return a list of positions
        Position position = new Position("NIFTY", "28MAR2024", 22000.0, LegType.CE);
        position.addEntry(new PositionEntry(100.0, 50, TradeAction.BUY));

        List<Position> positions = Collections.singletonList(position);

        when(positionRepository.findAll()).thenReturn(positions);

        // Trigger broadcast
        dashboardBroadcaster.broadcastDashboardMetrics();

        // Verify broadcast happened
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/dashboard"), any(DashboardMetrics.class));
    }

    @Test
    public void testInactivePositionNoBroadcast() {
        // Mock DB behavior: Return a list of positions with net qty 0
        Position position = new Position("NIFTY", "28MAR2024", 22000.0, LegType.CE);
        position.addEntry(new PositionEntry(100.0, 50, TradeAction.BUY));
        position.addEntry(new PositionEntry(110.0, 50, TradeAction.SELL)); // Net Qty 0

        List<Position> positions = Collections.singletonList(position);

        when(positionRepository.findAll()).thenReturn(positions);

        // Trigger broadcast
        dashboardBroadcaster.broadcastDashboardMetrics();

        // Verify broadcast did NOT happen
        verify(messagingTemplate, org.mockito.Mockito.never()).convertAndSend(eq("/topic/dashboard"),
                any(DashboardMetrics.class));
    }

    @Test
    public void testClosedPositionNoBroadcast() {
        // Mock DB behavior: Return a list of positions where net qty is 0.
        Position position = new Position("NIFTY", "28MAR2024", 22000.0, LegType.CE);
        position.addEntry(new PositionEntry(100.0, 50, TradeAction.BUY));
        // Add closing entry
        position.addEntry(new PositionEntry(110.0, 50, TradeAction.SELL)); // Net qty 0

        List<Position> positions = Collections.singletonList(position);

        when(positionRepository.findAll()).thenReturn(positions);

        // Trigger broadcast
        dashboardBroadcaster.broadcastDashboardMetrics();

        // Verify broadcast did NOT happen
        verify(messagingTemplate, org.mockito.Mockito.never()).convertAndSend(eq("/topic/dashboard"),
                any(DashboardMetrics.class));
    }
}
