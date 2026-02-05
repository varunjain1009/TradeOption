package com.tradeoption.scheduler;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class VolatilityBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public VolatilityBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Scheduled(fixedRate = 1000)
    public void broadcastVolatility() {
        // Mock volatility for now, would typically come from a VolatilityService
        double currentIV = 0.15 + (Math.random() * 0.01);
        messagingTemplate.convertAndSend("/topic/volatility", currentIV);
    }
}
