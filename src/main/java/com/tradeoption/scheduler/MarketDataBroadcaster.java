package com.tradeoption.scheduler;

import com.tradeoption.domain.MarketDataUpdate;
import com.tradeoption.service.MarketDataService;
import com.tradeoption.service.SystemConfigService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Duration;

@Component
public class MarketDataBroadcaster {

    private final MarketDataService marketDataService;
    private final SimpMessagingTemplate messagingTemplate;
    private final SystemConfigService systemConfigService;
    private final TaskScheduler taskScheduler;

    public MarketDataBroadcaster(MarketDataService marketDataService,
            SimpMessagingTemplate messagingTemplate,
            SystemConfigService systemConfigService,
            TaskScheduler taskScheduler) {
        this.marketDataService = marketDataService;
        this.messagingTemplate = messagingTemplate;
        this.systemConfigService = systemConfigService;
        this.taskScheduler = taskScheduler;
    }

    @PostConstruct
    public void init() {
        // Schedule with configurable interval
        // long intervalMs =
        // systemConfigService.getConfig().getSpotBroadcastIntervalMs();
        // taskScheduler.scheduleAtFixedRate(this::broadcastSpotPrice,
        // Duration.ofMillis(intervalMs));
        System.out.println("Market Data Broadcasting DISABLED due to manual mode.");
    }

    public void broadcastSpotPrice() {
        // Using GOLD as it's available in MCX
        java.util.Optional<Double> ltpOpt = marketDataService.getLtp("GOLD");
        if (ltpOpt.isPresent()) {
            double price = ltpOpt.get();
            MarketDataUpdate update = new MarketDataUpdate("GOLD", price, System.currentTimeMillis());

            messagingTemplate.convertAndSend("/topic/spot", update);
            System.out.println("Broadcasting spot price: " + price);
        } else {
            System.out.println("No spot price available for broadcasting");
        }
    }
}
