package com.tradeoption.scheduler;

import com.tradeoption.domain.MarketDataUpdate;
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
public class WebSocketIntegrationTest {

    @Autowired
    private MarketDataBroadcaster marketDataBroadcaster;

    @Autowired
    private PnlBroadcaster pnlBroadcaster;

    @Autowired
    private VolatilityBroadcaster volatilityBroadcaster;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @MockBean
    private com.tradeoption.repository.PositionRepository positionRepository;

    @MockBean
    private com.tradeoption.repository.RocksDBRepository rocksDBRepository;

    @Test
    public void testMarketDataBroadcast() {
        marketDataBroadcaster.broadcastSpotPrice();
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/spot"), any(MarketDataUpdate.class));
    }

    @Test
    public void testPnlBroadcast() {
        pnlBroadcaster.broadcastPnl();
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/pnl"), any(Double.class));
    }

    @Test
    public void testVolatilityBroadcast() {
        volatilityBroadcaster.broadcastVolatility();
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/volatility"), any(Double.class));
    }
}
