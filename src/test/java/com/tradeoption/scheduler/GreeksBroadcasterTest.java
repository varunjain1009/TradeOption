package com.tradeoption.scheduler;

import com.tradeoption.domain.LegType;
import com.tradeoption.domain.OptionLeg;
import com.tradeoption.domain.Strategy;
import com.tradeoption.domain.TradeAction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@SpringBootTest
public class GreeksBroadcasterTest {

    @Autowired
    private GreeksBroadcaster broadcaster;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @MockBean
    private com.tradeoption.repository.RocksDBRepository rocksDBRepository;

    @Test
    public void testBroadcastGreeks() {
        // Setup Strategy
        Strategy strategy = new Strategy();
        strategy.addLeg(new OptionLeg(100, LegType.CE, TradeAction.BUY, 10, 1, "28MAR2024"));

        broadcaster.addStrategy(strategy);

        // Trigger broadcast
        broadcaster.broadcastGreeks();

        // Verify message sent to /topic/greeks
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/greeks"), any(Object.class));
    }
}
