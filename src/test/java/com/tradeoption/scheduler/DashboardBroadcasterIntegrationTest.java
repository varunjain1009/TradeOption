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

    @Test
    public void testDashboardMetricsBroadcast() {
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
}
