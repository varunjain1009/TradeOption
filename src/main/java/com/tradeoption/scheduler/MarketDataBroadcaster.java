package com.tradeoption.scheduler;

import com.tradeoption.domain.MarketDataUpdate;
import com.tradeoption.service.MarketDataService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MarketDataBroadcaster {

    private final MarketDataService marketDataService;
    private final SimpMessagingTemplate messagingTemplate;

    public MarketDataBroadcaster(MarketDataService marketDataService, SimpMessagingTemplate messagingTemplate) {
        this.marketDataService = marketDataService;
        this.messagingTemplate = messagingTemplate;
    }

    @Scheduled(fixedRate = 1000)
    public void broadcastSpotPrice() {
        // For demonstration, strictly using NIFTY
        double price = marketDataService.getLtp("NIFTY");
        MarketDataUpdate update = new MarketDataUpdate("NIFTY", price, System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/spot", update);
    }
}
