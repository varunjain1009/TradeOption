package com.tradeoption.scheduler;

import com.tradeoption.domain.DashboardMetrics;
import com.tradeoption.domain.Strategy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest
public class DashboardBroadcasterIntegrationTest {

    @Autowired
    private DashboardBroadcaster dashboardBroadcaster;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @Test
    public void testDashboardMetricsBroadcast() {
        // Add a mock strategy to ensure broadcaster runs
        Strategy strat = new Strategy();
        // Add some legs if needed for calculations not to crash?
        // DashboardService handles empty strategy?
        // DashboardServiceImpl uses GreeksService which iterates legs. Empty legs -> 0
        // Greeks.
        // ProbabilityService might need legs?
        // Let's rely on empty strategy being safe or basic calc resulting in zeros.
        dashboardBroadcaster.addStrategy(strat);

        dashboardBroadcaster.broadcastDashboardMetrics();

        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/dashboard"), any(DashboardMetrics.class));
    }
}
